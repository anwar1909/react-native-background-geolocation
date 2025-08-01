package com.anwar1909.bgloc;

import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Formatter;

public class PluginException extends Exception {
    public static final int PERMISSION_DENIED_ERROR = 1000;
    public static final int SETTINGS_ERROR = 1001;
    public static final int CONFIGURE_ERROR = 1002;
    public static final int SERVICE_ERROR = 1003;
    public static final int JSON_ERROR = 1004;

    private Integer code;

    public PluginException(String message, Throwable cause, int code) {
        super(message, cause);
        this.code = code;
    }

    public PluginException(String message, int code) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putInt("code", this.code);
        bundle.putString("message", this.getMessage());

        return bundle;
    }

    public String toJsonString() {
        StringBuilder string = new StringBuilder();
        Formatter formatter = new Formatter(string);
        formatter.format("{%n");
        formatter.format("\"code\": \"%d\",", this.code);
        formatter.format("\"message\": \"%s\"", this.getMessage());
        formatter.format("}");
        formatter.flush();
        return string.toString();
    }

}
