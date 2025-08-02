package com.anwar1909.bgloc.data;

import com.anwar1909.bgloc.Config;
import com.anwar1909.bgloc.Setting;

import org.json.JSONException;

public interface SettingDAO {
    boolean persistSetting(Setting setting) throws NullPointerException;
    Setting retrieveSetting() throws JSONException;
}