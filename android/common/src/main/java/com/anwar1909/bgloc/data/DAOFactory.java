package com.anwar1909.bgloc.data;

import android.content.Context;

import com.anwar1909.bgloc.data.provider.ContentProviderLocationDAO;
import com.anwar1909.bgloc.data.sqlite.SQLiteConfigurationDAO;
import com.anwar1909.bgloc.data.sqlite.SQLiteSettingDAO;

public abstract class DAOFactory {
    public static LocationDAO createLocationDAO(Context context) {
        return new ContentProviderLocationDAO(context);
    }

    public static ConfigurationDAO createConfigurationDAO(Context context) {
        return new SQLiteConfigurationDAO(context);
    }
    public static SQLiteSettingDAO createSettingDAO(Context context) {
        return new SQLiteSettingDAO(context);
    }
}
