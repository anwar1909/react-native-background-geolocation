/*
According to apache license

This is fork of christocracy cordova-plugin-background-geolocation plugin
https://github.com/christocracy/cordova-plugin-background-geolocation

This is a new class
*/

package com.anwar1909.bgloc.service;

import android.content.pm.ServiceInfo;
import android.annotation.SuppressLint;
import android.accounts.Account;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.os.PowerManager;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.anwar1909.bgloc.Config;
import com.anwar1909.bgloc.ConnectivityListener;
import com.anwar1909.bgloc.Setting;
import com.anwar1909.bgloc.data.SettingDAO;
import com.anwar1909.bgloc.sync.NotificationHelper;
import com.anwar1909.bgloc.PluginException;
import com.anwar1909.bgloc.PostLocationTask;
import com.anwar1909.bgloc.ResourceResolver;
import com.anwar1909.bgloc.data.BackgroundActivity;
import com.anwar1909.bgloc.data.BackgroundLocation;
import com.anwar1909.bgloc.data.ConfigurationDAO;
import com.anwar1909.bgloc.data.DAOFactory;
import com.anwar1909.bgloc.data.LocationDAO;
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
import com.anwar1909.bgloc.sync.AccountHelper;
import com.anwar1909.bgloc.sync.SyncService;
import com.anwar1909.logging.LoggerManager;
import com.anwar1909.logging.UncaughtExceptionLogger;


import org.chromium.content.browser.ThreadUtils;
import org.json.JSONException;

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
    private static int PERMISSION_NOTIFICATION_ID = 2;

    private ResourceResolver mResolver;
    private Config mConfig;
    private Setting mSetting;
    private LocationProvider mProvider;
    private Account mSyncAccount;

    private org.slf4j.Logger logger;

    private final IBinder mBinder = new LocalBinder();
    private HandlerThread mHandlerThread;
    private ServiceHandler mServiceHandler;
    private LocationDAO mLocationDAO;
    private PostLocationTask mPostLocationTask;
    private String mHeadlessTaskRunnerClass;
    private TaskRunner mHeadlessTaskRunner;

    private long mServiceId = -1;
    private static boolean sIsRunning = false;
    private boolean mIsInForeground = false;

    private static LocationTransform sLocationTransform;
    private static LocationProviderFactory sLocationProviderFactory;
    private PowerManager.WakeLock wakeLock;                 // PARTIAL_WAKELOCK

    private class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @SuppressLint("WakelockTimeout")
    @Override
    public IBinder onBind(Intent intent) {
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire();
            logger.debug("WAKELOCK acquired");
        }
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
        // All clients have unbound with unbindService()
        logger.debug("All clients have been unbound from service");

        return true; // Ensures onRebind() is called when a client re-binds.
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sIsRunning = false;

        UncaughtExceptionLogger.register(this);

        logger = LoggerManager.getLogger(LocationServiceImpl.class);
        logger.info("Creating LocationServiceImpl");

        mServiceId = System.currentTimeMillis();

        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        if (mHandlerThread == null) {
            mHandlerThread = new HandlerThread("LocationServiceImpl.Thread", Process.THREAD_PRIORITY_BACKGROUND);
        }
        mHandlerThread.start();
        // An Android service handler is a handler running on a specific background thread.
        mServiceHandler = new ServiceHandler(mHandlerThread.getLooper());

        mResolver = ResourceResolver.newInstance(this);

        mSyncAccount = AccountHelper.CreateSyncAccount(this, mResolver.getAccountName(),
                mResolver.getAccountType());

        String authority = mResolver.getAuthority();
        ContentResolver.setIsSyncable(mSyncAccount, authority, 1);
        ContentResolver.setSyncAutomatically(mSyncAccount, authority, true);

        mLocationDAO = DAOFactory.createLocationDAO(this);

        // PARTIAL_WAKELOCK
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"com.anwar1909.backgroundgeolocation:wakelock");

        mPostLocationTask = new PostLocationTask(mLocationDAO,
                new PostLocationTask.PostLocationTaskListener() {
                    @Override
                    public void onRequestedAbortUpdates() {
                        handleRequestedAbortUpdates();
                    }

                    @Override
                    public void onHttpAuthorizationUpdates() {
                        handleHttpAuthorizationUpdates();
                    }

                    @Override
                    public void onSyncRequested() {
                        SyncService.sync(mSyncAccount, mResolver.getAuthority(), false);
                    }
                }, new ConnectivityListener() {
            @Override
            public boolean hasConnectivity() {
                return isNetworkAvailable();
            }
        });

        registerReceiver(connectivityChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        NotificationHelper.registerServiceChannel(this);
    }

    @Override
    public void onDestroy() {
        logger.info("Destroying LocationServiceImpl");

        // PARTIAL_WAKELOCK
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            logger.info("WAKELOCK released");
        }

        // workaround for issue #276
        if (mProvider != null) {
            mProvider.onDestroy();
        }

        if (mHandlerThread != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mHandlerThread.quitSafely();
            } else {
                mHandlerThread.quit(); //sorry
            }
        }

        if (mPostLocationTask != null) {
            mPostLocationTask.shutdown();
        }


        unregisterReceiver(connectivityChangeReceiver);

        sIsRunning = false;
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        logger.debug("Task has been removed");
        // workaround for issue #276
        Config config = getConfig();
        Setting setting = getSetting();
        if (config.getStopOnTerminate() || !setting.isStarted()) {
            logger.info("Stopping self");
            stopSelf();
        } else {
            logger.info("Continue running in background");
        }
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        logger.debug("onStartCommand() startId: {} intent: {} flags: {}", startId, intent, flags);
        if (intent == null || !containsCommand(intent)) {
            // when service was killed and restarted we will restart service
            start();
            return START_STICKY;
        }

        boolean containsCommand = containsCommand(intent);
        logger.debug("onStartCommand() containsCommand: {}", containsCommand);
        logger.debug(
                String.format("Service in [%s] state. cmdId: [%s]. startId: [%d]",
                        sIsRunning ? "STARTED" : "NOT STARTED",
                        containsCommand ? getCommand(intent).getId() : "N/A",
                        startId)
        );

        if (containsCommand) {
            LocationServiceIntentBuilder.Command cmd = getCommand(intent);
            processCommand(cmd.getId(), cmd.getArgument());
        }

        if (containsMessage(intent)) {
            processMessage(getMessage(intent));
        }

        return START_STICKY;
    }

    private void processMessage(String message) {
        // currently we do not process any message
    }

    private void processCommand(int command, Object arg) {
        logger.debug("processCommand: command: {} arg: {}", command, arg);
        try {
            switch (command) {
                case CommandId.START:
                    start();
                    break;
                case CommandId.START_FOREGROUND_SERVICE:
                    startForegroundService();
                    break;
                case CommandId.STOP:
                    stop();
                    break;
                case CommandId.CONFIGURE:
                    configure((Config) arg);
                    break;
                case CommandId.STOP_FOREGROUND:
                    stopForeground();
                    break;
                case CommandId.START_FOREGROUND:
                    startForeground();
                    break;
                case CommandId.REGISTER_HEADLESS_TASK:
                    registerHeadlessTask((String) arg);
                    break;
                case CommandId.START_HEADLESS_TASK:
                    startHeadlessTask();
                    break;
                case CommandId.STOP_HEADLESS_TASK:
                    stopHeadlessTask();
                    break;
            }
        } catch (Exception e) {
            logger.error("processCommand: exception", e);
        }
    }

    @Override
    public synchronized void start() {
        logger.debug("!Will start service with: {}", mConfig);
        if (sIsRunning) {
            return;
        }

        if (mSetting == null) {
            logger.warn("Attempt to start unset service. Will use stored or default.");
            mSetting = getSetting();
            // TODO: throw JSONException if config cannot be obtained from db
        }

        if(!mSetting.isStarted()){
            sIsRunning = false;
            return;
        }

        if (mConfig == null) {
            logger.warn("Attempt to start unconfigured service. Will use stored or default.");
            mConfig = getConfig();
            // TODO: throw JSONException if config cannot be obtained from db
        }

        logger.debug("Will start service with: {}", mConfig.toString());

        mPostLocationTask.setConfig(mConfig);
        mPostLocationTask.clearQueue();

        LocationProviderFactory spf = sLocationProviderFactory != null
                ? sLocationProviderFactory : new LocationProviderFactory(this);
        mProvider = spf.getInstance(mConfig.getLocationProvider());
        mProvider.setDelegate(this);
        mProvider.onCreate();
        mProvider.onConfigure(mConfig);

        sIsRunning = true;
        ThreadUtils.runOnUiThreadBlocking(new Runnable() {
            @Override
            public void run() {
                mProvider.onStart();
                if (mConfig.getStartForeground()) {
                    startForeground();
                }
            }
        });

        Bundle bundle = new Bundle();
        bundle.putInt("action", MSG_ON_SERVICE_STARTED);
        bundle.putLong("serviceId", mServiceId);
        broadcastMessage(bundle);
    }

    @Override
    public synchronized void startForegroundService() {
        start();
        startForeground();
    }

    @Override
    public synchronized void stop() {
        if (!sIsRunning) {
            return;
        }

        if (mProvider != null) {
            mProvider.onStop();
        }

        stopForeground(true);
        stopSelf();

        broadcastMessage(MSG_ON_SERVICE_STOPPED);
        sIsRunning = false;
    }

    @Override
    public void startForeground() {
        if (sIsRunning && !mIsInForeground) {
            Config config = getConfig();
            Notification notification = new NotificationHelper.NotificationFactory(this).getNotification(
                    config.getNotificationTitle(),
                    config.getNotificationText(),
                    config.getLargeNotificationIcon(),
                    config.getSmallNotificationIcon(),
                    config.getNotificationIconColor());

            if (mProvider != null) {
                mProvider.onCommand(LocationProvider.CMD_SWITCH_MODE,
                        LocationProvider.FOREGROUND_MODE);
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    super.startForeground(NOTIFICATION_ID, notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
                }
                else {
                    super.startForeground(NOTIFICATION_ID, notification);
                }
                mIsInForeground = true;
            } catch(Exception error) {
                logger.error("Forground Error: {}", error.getMessage());
            }

        }
    }

    @Override
    public synchronized void stopForeground() {
        if (sIsRunning && mIsInForeground) {
            stopForeground(true);
            if (mProvider != null) {
                mProvider.onCommand(LocationProvider.CMD_SWITCH_MODE,
                        LocationProvider.BACKGROUND_MODE);
            }
            mIsInForeground = false;
        }
    }

    @Override
    public void setting(Setting setting) {
        mSetting = setting;
    }

    @Override
    public synchronized void configure(Config config) {
        logger.debug("configure() isi config: {}", config);
        logger.debug("configure() isi mConfig: {}", mConfig);
        if (mConfig == null) {
            mConfig = config;
            logger.debug("configure() isi return mConfig==null: {}", mConfig);
            return;
        }
        logger.debug("configure() isi after mConfig!=null: {}", mConfig);
        final Config currentConfig = mConfig;
        mConfig = config;

        logger.debug("configure() isi final Config currentConfig = mConfig: {}", mConfig);

        mPostLocationTask.setConfig(mConfig);

        logger.debug("configure() isi mSetting: {}", mSetting);
        if (mSetting == null) {
            logger.warn("Attempt to start unset service. Will use stored or default.");
            mSetting = getSetting();
            // TODO: throw JSONException if config cannot be obtained from db
            logger.debug("configure()->getSetting() isi mSetting: {}", mSetting);
        }

        logger.debug("configure() isi mSetting.isStarted(): {}", mSetting.isStarted());
        if(!mSetting.isStarted()){
            sIsRunning = false;
        }

        ThreadUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (sIsRunning) {
                    if (currentConfig.getStartForeground() == true && mConfig.getStartForeground() == false) {
                        stopForeground(true);
                    }

                    if (mConfig.getStartForeground() == true) {
                        if (currentConfig.getStartForeground() == false) {
                            // was not running in foreground, so start in foreground
                            startForeground();
                        } else {
                            // was running in foreground, so just update existing notification
                            Notification notification = new NotificationHelper.NotificationFactory(LocationServiceImpl.this).getNotification(
                                    mConfig.getNotificationTitle(),
                                    mConfig.getNotificationText(),
                                    mConfig.getLargeNotificationIcon(),
                                    mConfig.getSmallNotificationIcon(),
                                    mConfig.getNotificationIconColor());

                            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                            notificationManager.notify(NOTIFICATION_ID, notification);
                            notificationManager.cancel(PERMISSION_NOTIFICATION_ID);
                        }
                    }
                }

                if (currentConfig.getLocationProvider() != mConfig.getLocationProvider()) {
                    boolean shouldStart = mProvider.isStarted();
                    mProvider.onDestroy();
                    LocationProviderFactory spf = new LocationProviderFactory(LocationServiceImpl.this);
                    mProvider = spf.getInstance(mConfig.getLocationProvider());
                    mProvider.setDelegate(LocationServiceImpl.this);
                    mProvider.onCreate();
                    mProvider.onConfigure(mConfig);
                    if (shouldStart) {
                        mProvider.onStart();
                    }
                } else {
                    mProvider.onConfigure(mConfig);
                }
            }
        });
    }

    @Override
    public synchronized void registerHeadlessTask(String taskRunnerClass) {
        logger.debug("Registering headless task");
        mHeadlessTaskRunnerClass = taskRunnerClass;
    }

    @Override
    public synchronized void startHeadlessTask() {
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
    public synchronized void stopHeadlessTask() {
        mHeadlessTaskRunner = null;
    }

    @Override
    public synchronized void executeProviderCommand(final int command, final int arg1) {
        if (mProvider == null) {
            return;
        }

        ThreadUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProvider.onCommand(command, arg1);
            }
        });
    }

    @Override
    public void onLocation(BackgroundLocation location) {
        logger.debug("New location {}", location.toString());

        location = transformLocation(location);
        if (location == null) {
            logger.debug("Skipping location as requested by the locationTransform");
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putInt("action", MSG_ON_LOCATION);
        bundle.putParcelable("payload", location);
        broadcastMessage(bundle);

        runHeadlessTask(new LocationTask(location) {
            @Override
            public void onError(String errorMessage) {
                logger.error("Location task error: {}", errorMessage);
            }

            @Override
            public void onResult(String value) {
                logger.debug("Location task result: {}", value);
            }
        });

        postLocation(location);
    }

    @Override
    public void onStationary(BackgroundLocation location) {
        logger.debug("New stationary {}", location.toString());

        location = transformLocation(location);
        if (location == null) {
            logger.debug("Skipping location as requested by the locationTransform");
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putInt("action", MSG_ON_STATIONARY);
        bundle.putParcelable("payload", location);
        broadcastMessage(bundle);

        runHeadlessTask(new StationaryTask(location){
            @Override
            public void onError(String errorMessage) {
                logger.error("Stationary task error: {}", errorMessage);
            }

            @Override
            public void onResult(String value) {
                logger.debug("Stationary task result: {}", value);
            }
        });

        postLocation(location);
    }

    @Override
    public void onActivity(BackgroundActivity activity) {
        logger.debug("New activity {}", activity.toString());

        Bundle bundle = new Bundle();
        bundle.putInt("action", MSG_ON_ACTIVITY);
        bundle.putParcelable("payload", activity);
        broadcastMessage(bundle);

        runHeadlessTask(new ActivityTask(activity){
            @Override
            public void onError(String errorMessage) {
                logger.error("Activity task error: {}", errorMessage);
            }

            @Override
            public void onResult(String value) {
                logger.debug("Activity task result: {}", value);
            }
        });
    }

    private void postError(PluginException error) {
        mPostLocationTask.add(error);
    }

    @Override
    public void onError(PluginException error) {
        Config config = getConfig();
        Bundle bundle = new Bundle();
        bundle.putInt("action", MSG_ON_ERROR);
        bundle.putBundle("payload", error.toBundle());
        broadcastMessage(bundle);
        postError(error);
        if(error.getCode() == PluginException.PERMISSION_DENIED_ERROR) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(LocationServiceImpl.this, NotificationHelper.ANDROID_PERMISSIONS_CHANNEL_ID);
            builder.setContentTitle("Permission Denied");
            builder.setContentText("Location Permission is denied. Please Allow the location.");
            builder.setSmallIcon(android.R.drawable.ic_dialog_info);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(PERMISSION_NOTIFICATION_ID, builder.build());
        }

    }

    private void broadcastMessage(int msgId) {
        Bundle bundle = new Bundle();
        bundle.putInt("action", msgId);
        broadcastMessage(bundle);
    }

    private void broadcastMessage(Bundle bundle) {
        Intent intent = new Intent(ACTION_BROADCAST);
        intent.putExtras(bundle);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return super.registerReceiver(receiver, filter, null , mServiceHandler, Context.RECEIVER_EXPORTED);
        } else {
           return super.registerReceiver(receiver, filter, null, mServiceHandler);
        }
        
    }

    @Override
    public void unregisterReceiver(BroadcastReceiver receiver) {
        try {
            super.unregisterReceiver(receiver);
        } catch (IllegalArgumentException ex) {
            // if was not registered ignore exception
        }
    }

    public Config getConfig() {
        Config config = mConfig;
        if (config == null) {
            ConfigurationDAO dao = DAOFactory.createConfigurationDAO(this);
            try {
                config = dao.retrieveConfiguration();
            } catch (JSONException e) {
                logger.error("Config exception: {}", e.getMessage());
            }
        }

        if (config == null) {
            config = Config.getDefault();
        }

        mConfig = config;
        return mConfig;
    }

    public Setting getSetting() {
        logger.debug("getSetting() called, mSetting: {}", mSetting);
        Setting setting = mSetting;
        if (setting == null) {
            SettingDAO dao = DAOFactory.createSettingDAO(this);
            try {
                setting = dao.retrieveSetting();
            } catch (JSONException e) {
                logger.error("Setting exception: {}", e.getMessage());
            }
        }

        if (setting == null) {
            setting = Setting.getDefault();
            logger.debug("getSetting() Setting.getDefault(): {}", Setting.getDefault());
        }

        mSetting = setting;
        return mSetting;
    }

    public static void setLocationProviderFactory(LocationProviderFactory factory) {
        sLocationProviderFactory = factory;
    }

    private void runHeadlessTask(Task task) {
        if (mHeadlessTaskRunner == null) {
            return;
        }

        logger.debug("Running headless task: {}", task);
        mHeadlessTaskRunner.runTask(task);
    }

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public LocationServiceImpl getService() {
            return LocationServiceImpl.this;
        }
    }

    private BackgroundLocation transformLocation(BackgroundLocation location) {
        if (sLocationTransform != null) {
            return sLocationTransform.transformLocationBeforeCommit(this, location);
        }

        return location;
    }

    private void postLocation(BackgroundLocation location) {
        mPostLocationTask.add(location);
    }

    public void handleRequestedAbortUpdates() {
        broadcastMessage(MSG_ON_ABORT_REQUESTED);
    }

    public void handleHttpAuthorizationUpdates() {
        broadcastMessage(MSG_ON_HTTP_AUTHORIZATION);
    }

    /**
     * Broadcast receiver which detects connectivity change condition
     */
    private BroadcastReceiver connectivityChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean hasConnectivity = isNetworkAvailable();
            mPostLocationTask.setHasConnectivity(hasConnectivity);
            logger.info("Network condition changed has connectivity: {}", hasConnectivity);
        }
    };

    private boolean isNetworkAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public long getServiceId() {
        return mServiceId;
    }

    public boolean isBound() {
        LocationServiceInfo info = new LocationServiceInfoImpl(this);
        return info.isBound();
    }

    public static boolean isRunning() {
        return sIsRunning;
    }

    public static void setLocationTransform(@Nullable LocationTransform transform) {
        sLocationTransform = transform;
    }

    public static @Nullable LocationTransform getLocationTransform() {
        return sLocationTransform;
    }
}
