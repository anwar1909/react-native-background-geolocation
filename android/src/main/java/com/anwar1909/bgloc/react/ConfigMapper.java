package com.anwar1909.bgloc.react;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableMap;
import com.com.anwar1909.bgloc.iodine.start.ArrayUtil;
import com.com.anwar1909.bgloc.iodine.start.MapUtil;
import com.anwar1909.bgloc.Config;
import com.anwar1909.bgloc.data.ArrayListLocationTemplate;
import com.anwar1909.bgloc.data.HashMapLocationTemplate;
import com.anwar1909.bgloc.data.LocationTemplate;
import com.anwar1909.bgloc.data.LocationTemplateFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by finch on 29.11.2016.
 */

public class ConfigMapper {
    public static Config fromMap(ReadableMap options) throws JSONException {
        Config config = new Config();

        // Basic float & int
        config.setStationaryRadius(options.hasKey("stationaryRadius") ? (float) options.getDouble("stationaryRadius") : 50f);
        config.setDistanceFilter(options.hasKey("distanceFilter") ? options.getInt("distanceFilter") : 50);
        config.setDesiredAccuracy(options.hasKey("desiredAccuracy") ? options.getInt("desiredAccuracy") : 100);

        // Boolean
        config.setDebugging(options.hasKey("debug") && options.getBoolean("debug"));
        config.setStopOnTerminate(options.hasKey("stopOnTerminate") && options.getBoolean("stopOnTerminate"));
        config.setStartOnBoot(options.hasKey("startOnBoot") && options.getBoolean("startOnBoot"));
        config.setStartForeground(options.hasKey("startForeground") && options.getBoolean("startForeground"));
        config.setNotificationsEnabled(options.hasKey("notificationsEnabled") && options.getBoolean("notificationsEnabled"));
        config.setStopOnStillActivity(options.hasKey("stopOnStillActivity") && options.getBoolean("stopOnStillActivity"));

        // Integers with fallback
        config.setLocationProvider(options.hasKey("locationProvider") ? options.getInt("locationProvider") : 1);
        config.setInterval(options.hasKey("interval") ? options.getInt("interval") : 10000);
        config.setFastestInterval(options.hasKey("fastestInterval") ? options.getInt("fastestInterval") : 5000);
        config.setActivitiesInterval(options.hasKey("activitiesInterval") ? options.getInt("activitiesInterval") : 10000);
        config.setSyncThreshold(options.hasKey("syncThreshold") ? options.getInt("syncThreshold") : 50);
        config.setMaxLocations(options.hasKey("maxLocations") ? options.getInt("maxLocations") : 100);

        // String config
        config.setNotificationTitle(options.hasKey("notificationTitle") && !options.isNull("notificationTitle") ? options.getString("notificationTitle") : Config.NullString);
        config.setNotificationText(options.hasKey("notificationText") && !options.isNull("notificationText") ? options.getString("notificationText") : Config.NullString);
        config.setLargeNotificationIcon(options.hasKey("notificationIconLarge") && !options.isNull("notificationIconLarge") ? options.getString("notificationIconLarge") : Config.NullString);
        config.setSmallNotificationIcon(options.hasKey("notificationIconSmall") && !options.isNull("notificationIconSmall") ? options.getString("notificationIconSmall") : Config.NullString);
        config.setNotificationIconColor(options.hasKey("notificationIconColor") && !options.isNull("notificationIconColor") ? options.getString("notificationIconColor") : Config.NullString);
        config.setUrl(options.hasKey("url") && !options.isNull("url") ? options.getString("url") : Config.NullString);
        config.setSyncUrl(options.hasKey("syncUrl") && !options.isNull("syncUrl") ? options.getString("syncUrl") : Config.NullString);

        // HTTP headers
        if (options.hasKey("httpHeaders")) {
            ReadableType type = options.getType("httpHeaders");
            if (type != ReadableType.Map) {
                throw new JSONException("httpHeaders must be object");
            }
            JSONObject httpHeadersJson = MapUtil.toJSONObject(options.getMap("httpHeaders"));
            config.setHttpHeaders(httpHeadersJson);
        }

        // Post template
        if (options.hasKey("postTemplate")) {
            if (options.isNull("postTemplate")) {
                config.setTemplate(LocationTemplateFactory.getDefault());
            } else {
                ReadableType type = options.getType("postTemplate");
                Object postTemplate = null;
                if (type == ReadableType.Map) {
                    postTemplate = MapUtil.toJSONObject(options.getMap("postTemplate"));
                } else if (type == ReadableType.Array) {
                    postTemplate = ArrayUtil.toJSONArray(options.getArray("postTemplate"));
                }
                config.setTemplate(LocationTemplateFactory.fromJSON(postTemplate));
            }
        }

        return config;
    }

    public static ReadableMap toMap(Config config) {
        WritableMap out = Arguments.createMap();
        WritableMap httpHeaders = Arguments.createMap();
        if (config.getStationaryRadius() != null) {
            out.putDouble("stationaryRadius", config.getStationaryRadius());
        }
        if (config.getDistanceFilter() != null) {
            out.putInt("distanceFilter", config.getDistanceFilter());
        }
        if (config.getDesiredAccuracy() != null) {
            out.putInt("desiredAccuracy", config.getDesiredAccuracy());
        }
        if (config.isDebugging() != null) {
            out.putBoolean("debug", config.isDebugging());
        }
        if (config.getNotificationTitle() != null) {
            if (config.getNotificationTitle() != Config.NullString) {
                out.putString("notificationTitle", config.getNotificationTitle());
            } else {
                out.putNull("notificationTitle");
            }
        }
        if (config.getNotificationText() != null) {
            if (config.getNotificationText() != Config.NullString) {
                out.putString("notificationText", config.getNotificationText());
            } else {
                out.putNull("notificationText");
            }
        }
        if (config.getLargeNotificationIcon() != null) {
            if (config.getLargeNotificationIcon() != Config.NullString) {
                out.putString("notificationIconLarge", config.getLargeNotificationIcon());
            } else {
                out.putNull("notificationIconLarge");
            }
        }
        if (config.getSmallNotificationIcon() != null) {
            if (config.getSmallNotificationIcon() != Config.NullString) {
                out.putString("notificationIconSmall", config.getSmallNotificationIcon());
            } else {
                out.putNull("notificationIconSmall");
            }
        }
        if (config.getNotificationIconColor() != null) {
            if (config.getNotificationIconColor() != Config.NullString) {
                out.putString("notificationIconColor", config.getNotificationIconColor());
            } else {
                out.putNull("notificationIconColor");
            }
        }
        if (config.getStopOnTerminate() != null) {
            out.putBoolean("stopOnTerminate", config.getStopOnTerminate());
        }
        if (config.getStartOnBoot() != null) {
            out.putBoolean("startOnBoot", config.getStartOnBoot());
        }
        if (config.getStartForeground() != null) {
            out.putBoolean("startForeground", config.getStartForeground());
        }
        if (config.getNotificationsEnabled() != null) {
            out.putBoolean("notificationsEnabled", config.getNotificationsEnabled());
        }
        if (config.getLocationProvider() != null) {
            out.putInt("locationProvider", config.getLocationProvider());
        }
        if (config.getInterval() != null) {
            out.putInt("interval", config.getInterval());
        }
        if (config.getFastestInterval() != null) {
            out.putInt("fastestInterval", config.getFastestInterval());
        }
        if (config.getActivitiesInterval() != null) {
            out.putInt("activitiesInterval", config.getActivitiesInterval());
        }
        if (config.getStopOnStillActivity() != null) {
            out.putBoolean("stopOnStillActivity", config.getStopOnStillActivity());
        }
        if (config.getUrl() != null) {
            if (config.getUrl() != Config.NullString) {
                out.putString("url", config.getUrl());
            } else {
                out.putNull("url");
            }
        }
        if (config.getSyncUrl() != null) {
            if (config.getSyncUrl() != Config.NullString) {
                out.putString("syncUrl", config.getSyncUrl());
            } else {
                out.putNull("syncUrl");
            }
        }
        if (config.getSyncThreshold() != null) {
            out.putInt("syncThreshold", config.getSyncThreshold());
        }
        // httpHeaders
        Iterator<Map.Entry<String, String>> it = config.getHttpHeaders().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> pair = it.next();
            httpHeaders.putString(pair.getKey(), pair.getValue());
        }
        out.putMap("httpHeaders", httpHeaders);
        if (config.getMaxLocations() != null) {
            out.putInt("maxLocations", config.getMaxLocations());
        }

        LocationTemplate tpl = config.getTemplate();
        if (tpl instanceof HashMapLocationTemplate) {
            Map map = ((HashMapLocationTemplate)tpl).toMap();
            if (map != null) {
                out.putMap("postTemplate", MapUtil.toWritableMap(map));
            } else {
                out.putNull("postTemplate");
            }
        } else if (tpl instanceof ArrayListLocationTemplate) {
            Object[] keys = ((ArrayListLocationTemplate)tpl).toArray();
            if (keys != null) {
                out.putArray("postTemplate", ArrayUtil.toWritableArray(keys));
            } else {
                out.putNull("postTemplate");
            }
        }
        return out;
    }
}
