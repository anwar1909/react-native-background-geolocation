package com.anwar1909.bgloc.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.pm.ServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.anwar1909.bgloc.BackgroundGeolocationFacade;
import com.anwar1909.bgloc.Config;
import com.anwar1909.bgloc.ConnectivityListener;
import com.anwar1909.bgloc.PluginException;
import com.anwar1909.bgloc.PluginDelegate;
import com.anwar1909.bgloc.ResourceResolver;
import com.anwar1909.bgloc.data.BackgroundActivity;
import com.anwar1909.bgloc.data.BackgroundLocation;
import com.anwar1909.bgloc.data.LocationTransform;
import com.anwar1909.bgloc.headless.AbstractTaskRunner;
import com.anwar1909.bgloc.headless.ActivityTask;
import com.anwar1909.bgloc.headless.LocationTask;
import com.anwar1909.bgloc.headless.StationaryTask;
import com.anwar1909.bgloc.headless.Task;
import com.anwar1909.bgloc.headless.TaskRunner;
import com.anwar1909.bgloc.headless.TaskRunnerFactory;
import com.anwar1909.bgloc.provider.LocationProvider;
import com.anwar1909.bgloc.provider.LocationProviderFactory;
import com.anwar1909.bgloc.provider.ProviderDelegate;
import com.anwar1909.bgloc.react.BackgroundGeolocationModule;
import com.anwar1909.bgloc.sync.NotificationHelper;
import com.anwar1909.bgloc.logging.LoggerManager;
import com.anwar1909.bgloc.logging.UncaughtExceptionLogger;

import com.anwar1909.bgloc.org.chromium.content.browser.ThreadUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import static com.anwar1909.bgloc.service.LocationServiceIntentBuilder.containsCommand;
import static com.anwar1909.bgloc.service.LocationServiceIntentBuilder.containsMessage;
import static com.anwar1909.bgloc.service.LocationServiceIntentBuilder.getCommand;
import static com.anwar1909.bgloc.service.LocationServiceIntentBuilder.getMessage;

public class LocationServiceImpl extends Service implements ProviderDelegate, LocationService {

    public static final String ACTION_BROADCAST = ".broadcast";
    /**
     * CommandId sent by the service to
     * any registered clients with error.
     */
    public static final int MSG_ON_ERROR = 100;

    /**
     * CommandId sent by the service to
     * any registered clients with the new position.
     */
    public static final int MSG_ON_LOCATION = 101;

    /**
     * CommandId sent by the service to
     * any registered clients whenever the devices enters "stationary-mode"
     */
    public static final int MSG_ON_STATIONARY = 102;

    /**
     * CommandId sent by the service to
     * any registered clients with new detected activity.
     */
    public static final int MSG_ON_ACTIVITY = 103;

    public static final int MSG_ON_SERVICE_STARTED = 104;

    public static final int MSG_ON_SERVICE_STOPPED = 105;

    public static final int MSG_ON_ABORT_REQUESTED = 106;

    public static final int MSG_ON_HTTP_AUTHORIZATION = 107;

    /** notification id */
    private static int NOTIFICATION_ID = 1;

    private ResourceResolver mResolver;
    private Config mConfig;
    private LocationProvider mProvider;
    private TaskRunner mHeadlessTaskRunner;
    private String mHeadlessTaskRunnerClass;

    private org.slf4j.Logger logger;

    // private final IBinder mBinder = new LocalBinder();
    private HandlerThread mHandlerThread;
    private ServiceHandler mServiceHandler;

    private long mServiceId = -1;
    private static boolean sIsRunning = false;
    private boolean mIsInForeground = false;

    private static LocationTransform sLocationTransform;
    private static LocationProviderFactory sLocationProviderFactory;
    private PluginDelegate mPluginDelegate;
    private static PluginDelegate sPluginDelegate;
    private final IBinder mBinder = new ServiceBinder();

    public class ServiceBinder extends Binder {
        public LocationService getService() {
            return LocationServiceImpl.this;
        }
    }

    public static void setPluginDelegate(PluginDelegate delegate) {
        sPluginDelegate = delegate;
    }

    private class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        logger.debug("Client binds to service");
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        logger.debug("Client rebinds to service");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        logger.debug("All clients have been unbound from service");
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sIsRunning = false;
        UncaughtExceptionLogger.register(this);
        logger = LoggerManager.getLogger(LocationServiceImpl.class);
        logger.info("Creating LocationServiceImpl");

        mServiceId = System.currentTimeMillis();

        if (mHandlerThread == null) {
            mHandlerThread = new HandlerThread("LocationServiceImpl.Thread", Process.THREAD_PRIORITY_BACKGROUND);
        }
        mHandlerThread.start();
        mServiceHandler = new ServiceHandler(mHandlerThread.getLooper());

        mResolver = ResourceResolver.newInstance(this);
        NotificationHelper.registerServiceChannel(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(connectivityChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(connectivityChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION), null, mServiceHandler);
        }
    }

    @Override
    public void onDestroy() {
        logger.info("Destroying LocationServiceImpl");

        if (mProvider != null) {
            mProvider.onDestroy();
        }

        if (mHandlerThread != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mHandlerThread.quitSafely();
            } else {
                mHandlerThread.quit();
            }
        }

        unregisterReceiver(connectivityChangeReceiver);
        sIsRunning = false;
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        logger.debug("Task has been removed");
        Config config = getConfig();
        if (config.getStopOnTerminate()) {
            stopSelf();
        }
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("config")) {
            Config config = intent.getParcelableExtra("config");
            Log.d("TAG", "Will start service with custom config: " + config.toString());
            this.configure(config);
        } else {
            Log.d("TAG", "Will start service with: default config");
        }

        Log.d("BGGeo", "✅ LocationServiceImpl -> onStartCommand invoked");
        Log.d("BGGeo", "Intent action: " + (intent != null ? intent.getAction() : "null"));
        Log.d("BGGeo", "containsCommand: " + containsCommand(intent));
        Log.d("BGGeo", "containsMessage: " + containsMessage(intent));
        if (intent == null || !containsCommand(intent)) {
            start();
            return START_STICKY;
        }

        if (containsCommand(intent)) {
            LocationServiceIntentBuilder.Command cmd = getCommand(intent);
            if (cmd != null) {
                if (cmd.getId() == CommandId.START) {
                    if (cmd.getArgument() instanceof Config) {
                        this.mConfig = (Config) cmd.getArgument();
                        Log.d("BGGeo", "✅ mConfig diambil dari intent: " + mConfig.toString());
                    } else {
                        Log.w("BGGeo", "⚠️ START arg bukan Config: " + cmd.getArgument());
                    }
                    start();
                    return START_STICKY;
                } else {
                    processCommand(cmd.getId(), cmd.getArgument());
                }
            } else {
                logger.warn("⚠️ Command is null, skipping");
            }
        }

        if (containsMessage(intent)) {
            processMessage(getMessage(intent));
        }

        return START_STICKY;
    }

    private void processMessage(String message) {}

    private void processCommand(int command, Object arg) {
        Log.d("BGGeo", "⚙️ processCommand() -> id: " + command + ", arg: " + arg);
        try {
            switch (command) {
                case CommandId.START: start(); break;
                case CommandId.START_FOREGROUND_SERVICE: startForegroundService(); break;
                case CommandId.STOP: stop(); break;
                case CommandId.CONFIGURE: configure((Config) arg); break;
                case CommandId.STOP_FOREGROUND: stopForeground(); break;
                case CommandId.START_FOREGROUND: startForeground(); break;
                case CommandId.REGISTER_HEADLESS_TASK: registerHeadlessTask((String) arg); break;
                case CommandId.START_HEADLESS_TASK: startHeadlessTask(); break;
                case CommandId.STOP_HEADLESS_TASK: stopHeadlessTask(); break;
                default: logger.warn("⚠️ Unhandled command: " + command); break;
            }
        } catch (Exception e) {
            logger.error("processCommand: exception", e);
        }
    }

    @Override
    public void bindService(Context context) {
    }

    @Override
    public synchronized void start() {
        Log.d("BGGeo", "🚀 LocationServiceImpl -> start() called");
        if (sIsRunning) {
            Log.d("BGGeo", "⚠️ Already running, skip");
            return;
        }

        // 🛠 Tambahkan ini untuk memastikan mConfig selalu terisi
        if (mConfig == null) {
            Log.d("BGGeo", "⚠️ mConfig is null, ambil dari facade.getConfig()");
            // mConfig = BackgroundGeolocationFacade.getInstance(this).getConfig();
            mConfig = BackgroundGeolocationFacade.getStaticConfig();
        }

        Log.d("BGGeo", "⚠️ Existing mConfig di LocationServiceImpl.start(): " + mConfig);
        if (mConfig == null) {
            Log.d("BGGeo", "⚠️ Config is null");
            logger.warn("Starting with default config");
            // mConfig = getConfig();
            return;
        }

        Log.d("BGGeo", "✅ Will start service with config: " + mConfig.toString());
        logger.debug("Will start service withz: ", mConfig.toString());

        try{
            LocationProviderFactory spf = sLocationProviderFactory != null ? sLocationProviderFactory : new LocationProviderFactory(this);
            Log.d("BGGeo", "✅ Created LocationProviderFactory");
            mProvider = spf.getInstance(mConfig.getLocationProvider(), mConfig);
            Log.d("BGGeo", "✅ mProvider created: " + mProvider.getClass().getSimpleName());
            mProvider.setDelegate(this);
            mProvider.onCreate();
            mProvider.onConfigure(mConfig);

            Log.d("BGGeo", "✅ Provider configured");

            sIsRunning = true;
            ThreadUtils.runOnUiThreadBlocking(() -> {
                mProvider.onStart();
                Log.d("BGGeo", "✅ mProvider.onStart called");
                if (mConfig.getStartForeground()) startForeground();
                Log.d("BGGeo", "✅ Foreground started");
            });

            // Log.d("BGGeo", "✅ Provider started");

            Bundle bundle = new Bundle();
            bundle.putInt("action", MSG_ON_SERVICE_STARTED);
            bundle.putLong("serviceId", mServiceId);
            broadcastMessage(bundle);
        } catch (Exception e) {
            Log.d("BGGeo", "❌ Error in start(): " + e.getMessage(), e);
        }
    }

    @Override
    public void setConfig(Config config) {
        Log.d("BGGeo", "🛠 setConfig() dipanggil dengan config: " + config.getUrl());
        this.mConfig = config;
    }

    @Override
    public synchronized void startForegroundService() {
        start();
        startForeground();
    }

    @Override
    public synchronized void stop() {
        if (!sIsRunning) return;

        if (mProvider != null) {
            mProvider.onStop();
        }

        stopForeground(true);
        stopSelf();

        broadcastMessage(MSG_ON_SERVICE_STOPPED);
        sIsRunning = false;
    }

    @Override
    public void onLocation(BackgroundLocation location) {
        Log.d("BGGeo:"," New location {}"+ location.toString());
        sendLocationToServer(location);

        if (sPluginDelegate != null) {
            sPluginDelegate.onLocationChanged(location); // <--- ini trigger JS
        }
    }

    private void sendLocationToServer(BackgroundLocation location) {
        Log.d("BGGeo", "🧪 sendLocationToServer() dipanggil");

        if (mConfig == null) {
            Log.e("BGGeo", "❌ mConfig is NULL di sendLocationToServer()");
            return;
        }

        if (mConfig.getUrl() == null || mConfig.getUrl().isEmpty()) {
            Log.e("BGGeo", "❌ URL di mConfig adalah null / kosong: " + mConfig.getUrl());
            return;
        }

        Log.d("BGGeo", "🌐 Akan kirim ke URL: " + mConfig.getUrl());
        // if (mConfig == null || mConfig.getUrl() == null) return;

        try {
            JSONObject data = new JSONObject();
            data.put("lat", location.getLatitude());
            data.put("lon", location.getLongitude());

            RequestQueue queue = Volley.newRequestQueue(this);
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, mConfig.getUrl(), data,
                response -> {
                    logger.debug("Location sent: " + response.toString());

                    BackgroundGeolocationModule module = BackgroundGeolocationModule.getInstance();
                    if (module != null) {
                        module.onHttpResponse("✅ Your server callback: " + response.toString());
                    }
                },
                error -> {
                    logger.error("Failed to send location: {}", error.getMessage());

                    BackgroundGeolocationModule module = BackgroundGeolocationModule.getInstance();
                    if (module != null) {
                        module.onHttpResponse("❌ Error server callback: " + error.getMessage());
                    }
                }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    return mConfig.getHttpHeaders();
                }
            };
            queue.add(request);
        } catch (JSONException e) {
            logger.error("Failed to build post data: {}", e.getMessage());
        }
    }

    private void broadcastMessage(int msgId) {
        Bundle bundle = new Bundle();
        bundle.putInt("action", msgId);
        broadcastMessage(bundle);
    }

    private void broadcastMessage(Bundle bundle) {
        Log.d("BGGeo", "📢 Broadcasting: " + bundle.toString());
        Intent intent = new Intent(ACTION_BROADCAST);
        intent.putExtras(bundle);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    @Override
    public synchronized void configure(Config config) {
        Log.d("BGGeo", "✅ LocationServiceImpl menerima config: " + config.getUrl());
        if (config.getInterval() == null) config.setInterval(10000);
        this.mConfig = config;
    }

    @Override
    public void onStationary(BackgroundLocation location) {
        Log.d("BGGeo:","Stationary location {}"+ location.toString());
        sendLocationToServer(location);
    }

    @Override
    public void onActivity(BackgroundActivity activity) {}

    @Override
    public void onError(PluginException error) {}

    public Config getConfig() {
        return mConfig != null ? mConfig : Config.getDefault();
    }

    @Override
    public void registerHeadlessTask(String taskRunnerClass) {
        this.mHeadlessTaskRunnerClass = taskRunnerClass;
    }

    @Override
    public void startHeadlessTask() {
        if (mHeadlessTaskRunnerClass != null) {
            TaskRunnerFactory trf = new TaskRunnerFactory();
            try {
                mHeadlessTaskRunner = trf.getTaskRunner(mHeadlessTaskRunnerClass);
                ((AbstractTaskRunner) mHeadlessTaskRunner).setContext(this);
            } catch (Exception e) {
                logger.error("Headless task start failed: {}", e.getMessage());
            }
        }
    }

    @Override
    public void stopHeadlessTask() {
        mHeadlessTaskRunner = null;
    }

    @Override
    public void executeProviderCommand(int command, int arg1) {
        if (mProvider != null) {
            ThreadUtils.runOnUiThread(() -> mProvider.onCommand(command, arg1));
        }
    }

    @Override
    public void startForeground() {
        if (sIsRunning && !mIsInForeground) {
            Notification notification = new NotificationHelper.NotificationFactory(this)
                    .getNotification("Tracking", "Running", null, null, null);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                super.startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            } else {
                super.startForeground(NOTIFICATION_ID, notification);
            }
            mIsInForeground = true;
        }
    }

    @Override
    public void stopForeground() {
        if (sIsRunning && mIsInForeground) {
            stopForeground(true);
            mIsInForeground = false;
        }
    }

    public class LocalBinder extends Binder {
        public LocationServiceImpl getService() {
            return LocationServiceImpl.this;
        }
    }

    // @Override
    // public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
    //     return super.registerReceiver(receiver, filter, null, mServiceHandler);
    // }

    @Override
    public void unregisterReceiver(BroadcastReceiver receiver) {
        try {
            super.unregisterReceiver(receiver);
        } catch (IllegalArgumentException ignored) {}
    }

    private final BroadcastReceiver connectivityChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("BGGeo", "Network condition changed"+intent);
        }
    };

    public long getServiceId() {
        return mServiceId;
    }

    public static boolean isRunning() {
        Log.d("BGGeo", "✅ LocationServiceImpl.isRunning() = " + sIsRunning);
        return sIsRunning;
    }

    public static void setLocationTransform(@Nullable LocationTransform transform) {
        sLocationTransform = transform;
    }

    public static @Nullable LocationTransform getLocationTransform() {
        return sLocationTransform;
    }
} 
