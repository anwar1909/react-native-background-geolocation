package com.anwar1909.bgloc.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MyBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "MyBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Broadcast received: " + intent.getAction());

        // Misalnya tangani STATIONARY_ALARM_ACTION
        if (intent.getAction() != null) {
            switch (intent.getAction()) {
                case "com.tenforwardconsulting.cordova.bgloc.STATIONARY_ALARM_ACTION":
                    // Trigger ulang setPace(false) atau langsung jalankan logic
                    Log.d(TAG, "Handling STATIONARY_ALARM_ACTION");
                    break;

                case "com.tenforwardconsulting.cordova.bgloc.SINGLE_LOCATION_UPDATE_ACTION":
                    Log.d(TAG, "Handling SINGLE_LOCATION_UPDATE_ACTION");
                    break;

                case "com.tenforwardconsulting.cordova.bgloc.STATIONARY_REGION_ACTION":
                    Log.d(TAG, "Handling STATIONARY_REGION_ACTION");
                    break;

                case "com.tenforwardconsulting.cordova.bgloc.STATIONARY_LOCATION_MONITOR_ACTION":
                    Log.d(TAG, "Handling STATIONARY_LOCATION_MONITOR_ACTION");
                    break;
            }
        }
    }
}
