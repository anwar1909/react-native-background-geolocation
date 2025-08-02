package com.anwar1909.bgloc.service;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

import com.anwar1909.bgloc.Config;
import com.anwar1909.bgloc.Setting;
import com.anwar1909.bgloc.data.DAOFactory;
import com.anwar1909.bgloc.data.SettingDAO;

import org.json.JSONException;

public class LocationServiceInfoImpl implements LocationServiceInfo {
    private Context mContext;

    public LocationServiceInfoImpl(Context context) {
        mContext = context;
    }

    @Override
    public boolean isStarted() {
        Log.d("isStarted()", "called mContext: "+mContext);
        SettingDAO settingDao = DAOFactory.createSettingDAO(mContext);
        Log.d("isStarted()", "DAOFactory.createSettingDAO() settingDao: "+settingDao);
        try {
            Log.d("isStarted()", "try");
            Setting setting = settingDao.retrieveSetting();
            Log.d("isStarted()", "settingDao.retrieveSetting() setting: "+setting);
            if(setting != null) {
                return setting.isStarted();
            }
        } catch (JSONException ignored) {

        }
        Log.d("isStarted()", "catch empty next to return false");
        // ActivityManager.RunningServiceInfo info = getRunningServiceInfo();
        // if (info != null) {
        //     return info.started;
        // }
        return false;
    }

    @Override
    public boolean isBound() {
        ActivityManager.RunningServiceInfo info = getRunningServiceInfo();
        if (info != null) {
            return info.clientCount > 0;
        }
        return false;
    }

    public ActivityManager.RunningServiceInfo getRunningServiceInfo() {
        String serviceName = LocationServiceImpl.class.getName();
        ActivityManager manager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo info : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceName.equals(info.service.getClassName())) {
                return info;
            }
        }
        return null;
    }
}
