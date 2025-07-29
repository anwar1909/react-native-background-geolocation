package com.anwar1909.bgloc.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.anwar1909.bgloc.BackgroundGeolocationFacade;
import com.anwar1909.bgloc.Config;

public class LocationServiceProxy implements LocationService, LocationServiceInfo {
    private final Context mContext;
    private final LocationServiceIntentBuilder mIntentBuilder;
    private LocationServiceConnection mServiceConnection;
    private Config mConfig;

    public LocationServiceProxy(Context context) {
        mContext = context;
        mIntentBuilder = new LocationServiceIntentBuilder(context);
    }

    public void bindService(Context context) {
        if (mServiceConnection == null) {
            mServiceConnection = new LocationServiceConnection();
            Intent intent = new Intent(context, LocationServiceImpl.class);
            intent.putExtra("config", mConfig);
            context.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
            Log.d("BGGeo", "🔗 bindService() berhasil dipanggil");
        }
    }

    @Override
    public void configure(Config config) {
        Log.v("BGGeo", "✅ LocationServiceProxy -> configure(): " + isStarted());
        if (!isStarted()) {
            Log.v("BGGeo", "✅ LocationServiceProxy -> configure() tidak ada isStarted(): ");
            return;
        }

        Intent intent = mIntentBuilder
                .setCommand(CommandId.CONFIGURE, config)
                .build();
        executeIntentCommand(intent);
    }

    @Override
    public void setConfig(Config config) {
        Log.d("BGGeo", "📦 LocationServiceProxy.setConfig() dipanggil");

        if (mServiceConnection != null) {
            LocationService service = mServiceConnection.getService();
            if (service != null) {
                Log.d("BGGeo", "✅ Service sudah terhubung, kirim config langsung");
                service.setConfig(config);
                service.configure(config);
            } else {
                Log.d("BGGeo", "⏳ Service belum terhubung, simpan config sementara");
                mServiceConnection.setPendingConfig(config);
            }
        } else {
            Log.w("BGGeo", "❌ mServiceConnection null di setConfig");
        }
    }

    @Override
    public void start() {
        BackgroundGeolocationFacade facade = BackgroundGeolocationFacade.getInstance(mContext);
        Config config = facade.getConfig();
        if (config != null) {
            Intent configIntent = mIntentBuilder.setCommand(CommandId.CONFIGURE, config).build();
            Log.d("BGGeo", "✅ LocationServiceProxy -> start(): Sending CONFIGURE with config");
            executeIntentCommand(configIntent);
        } else {
            Log.w("BGGeo", "⚠️ LocationServiceProxy -> start(): Config is null, skip CONFIGURE");
        }
        Intent intent = mIntentBuilder.setCommand(CommandId.START).build();
        Log.d("BGGeo", "✅ LocationServiceProxy -> start() called with intent: " + intent);
        executeIntentCommand(intent);
    }

    @Override
    public void startForegroundService() {
        Intent intent = mIntentBuilder.setCommand(CommandId.START_FOREGROUND_SERVICE).build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mContext.startForegroundService(intent);
        } else {
            mContext.startService(intent);
        }
    }

    @Override
    public void stop() {
        if (!isStarted()) return;
        Intent intent = mIntentBuilder.setCommand(CommandId.STOP).build();
        executeIntentCommand(intent);
    }

    @Override
    public void stopForeground() {
        if (!isStarted()) return;
        Intent intent = mIntentBuilder.setCommand(CommandId.STOP_FOREGROUND).build();
        executeIntentCommand(intent);
    }

    @Override
    public void startForeground() {
        if (!isStarted()) return;
        Intent intent = mIntentBuilder.setCommand(CommandId.START_FOREGROUND).build();
        executeIntentCommand(intent);
    }

    @Override
    public void registerHeadlessTask(String taskRunnerClass) {
        Intent intent = mIntentBuilder.setCommand(CommandId.REGISTER_HEADLESS_TASK, taskRunnerClass).build();
        executeIntentCommand(intent);
    }

    @Override
    public void startHeadlessTask() {
        if (!isStarted()) return;
        Intent intent = mIntentBuilder.setCommand(CommandId.START_HEADLESS_TASK).build();
        executeIntentCommand(intent);
    }

    @Override
    public void stopHeadlessTask() {
        if (!isStarted()) return;
        Intent intent = mIntentBuilder.setCommand(CommandId.STOP_HEADLESS_TASK).build();
        executeIntentCommand(intent);
    }

    @Override
    public void executeProviderCommand(int command, int arg) {
        // Optional
    }

    @Override
    public boolean isStarted() {
        return new LocationServiceInfoImpl(mContext).isStarted();
    }

    @Override
    public boolean isBound() {
        return new LocationServiceInfoImpl(mContext).isBound();
    }

    public boolean isRunning() {
        return isStarted() && LocationServiceImpl.isRunning();
    }

    private void executeIntentCommand(Intent intent) {
        Log.v("LocationServiceProxy", "executeIntentCommand(Intent intent) " + intent);
        mContext.startService(intent);
    }

    public LocationService getConnectedService() {
        return mServiceConnection != null ? mServiceConnection.getService() : null;
    }

    // INNER CLASS
    private class LocationServiceConnection implements ServiceConnection {
        private LocationService mService;
        private Config pendingConfig;

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            if (binder instanceof LocationServiceImpl.ServiceBinder) {
                mService = ((LocationServiceImpl.ServiceBinder) binder).getService();
                Log.d("BGGeo", "✅ onServiceConnected: service terhubung");

                if (pendingConfig != null) {
                    Log.d("BGGeo", "📦 Mengirim pendingConfig ke service");
                    mService.setConfig(pendingConfig);
                    mService.configure(pendingConfig);
                    pendingConfig = null;
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            Log.d("BGGeo", "⚠️ onServiceDisconnected: service terputus");
        }

        public LocationService getService() {
            return mService;
        }

        public void setPendingConfig(Config config) {
            pendingConfig = config;
        }
    }
}
