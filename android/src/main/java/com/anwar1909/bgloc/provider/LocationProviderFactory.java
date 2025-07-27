/*
According to apache license

This is fork of christocracy cordova-plugin-background-geolocation plugin
https://github.com/christocracy/cordova-plugin-background-geolocation

This is a new class
*/

package com.anwar1909.bgloc.provider;

import android.content.Context;
import android.location.LocationManager;

import com.anwar1909.bgloc.Config;
import com.anwar1909.bgloc.tenforwardconsulting.bgloc.DistanceFilterLocationProvider;

import java.lang.IllegalArgumentException;

/**
 * LocationProviderFactory
 */
public class LocationProviderFactory {

    private Context mContext;
    private LocationManager mLocationManager;

    public LocationProviderFactory(Context context) {
        this.mContext = context;
        this.mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    };

    public LocationProvider getInstance (Integer locationProvider, Config config) {
        LocationProvider provider;
        switch (locationProvider) {
            case Config.DISTANCE_FILTER_PROVIDER:
                // provider = new DistanceFilterLocationProvider(mContext);
                provider = new DistanceFilterLocationProvider(mContext, (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE), config);
                break;
            case Config.ACTIVITY_PROVIDER:
                provider = new ActivityRecognitionLocationProvider(mContext);
                break;
            case Config.RAW_PROVIDER:
                provider = new RawLocationProvider(mContext);
                break;
            default:
                throw new IllegalArgumentException("Provider not found");
        }

        return provider;
    }
}
