package com.anwar1909.bgloc.data;

import android.content.Context;

import com.anwar1909.bgloc.data.provider.ContentProviderLocationDAO;
import com.anwar1909.bgloc.data.sqlite.SQLiteLocationDAO;
import com.anwar1909.bgloc.data.sqlite.SQLiteConfigurationDAO;

public abstract class DAOFactory {
    public static LocationDAO createLocationDAO(Context context) {
        return new ContentProviderLocationDAO(context);
    }

    public static ConfigurationDAO createConfigurationDAO(Context context) {
        return new SQLiteConfigurationDAO(context);
    }
}
