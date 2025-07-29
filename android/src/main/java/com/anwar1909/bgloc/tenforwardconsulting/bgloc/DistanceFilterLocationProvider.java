package com.anwar1909.bgloc.tenforwardconsulting.bgloc;

import android.location.Criteria;
import android.location.LocationManager;
import android.location.LocationListener;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.content.Context;

import com.anwar1909.bgloc.Config;
import com.anwar1909.bgloc.provider.AbstractLocationProvider;
import com.anwar1909.bgloc.provider.LocationProvider;
import com.anwar1909.bgloc.logging.LoggerManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DistanceFilterLocationProvider extends AbstractLocationProvider implements LocationProvider, LocationListener {
    private static final String TAG = "BGGeo";
    private static final Logger logger = LoggerFactory.getLogger(DistanceFilterLocationProvider.class);

    private LocationManager locationManager;
    private Criteria criteria;
    private Config mConfig;
    private boolean isStarted = false;
    private boolean isMoving = false;
    private boolean isAcquiringSpeed = false;
    private boolean isAcquiringStationaryLocation = false;
    private Location stationaryLocation = null;
    private int locationAcquisitionAttempts = 0;
    private float scaledDistanceFilter = 0;

    public DistanceFilterLocationProvider(Context context, LocationManager locationManager, Config config) {
        super(context);
        this.locationManager = locationManager;
        this.mConfig = config;
        this.criteria = new Criteria();
        this.criteria.setAccuracy(Criteria.ACCURACY_FINE);
        this.criteria.setPowerRequirement(Criteria.POWER_HIGH);
    }

    // public DistanceFilterLocationProvider(Context context) {
    //     this(
    //         (LocationManager) context.getSystemService(Context.LOCATION_SERVICE),
    //         new Config() // Jika ada default config
    //     );
    // }

    public void onCreate() {
        logger.debug("DistanceFilterLocationProvider: onCreate()");
        // Initialize provider, if needed
    }

    public void onStart() {
        logger.debug("DistanceFilterLocationProvider: onStart()");
        isStarted = true;
        setPace(true);
    }

    public void onStop() {
        logger.debug("DistanceFilterLocationProvider: onStop()");
        isStarted = false;
        locationManager.removeUpdates(this);
    }

    @Override
    public void onCommand(int commandId, int arg1) {
        switch(commandId) {
            case CMD_SWITCH_MODE:
                setPace(arg1 == BACKGROUND_MODE ? false : true);
                return;
        }
    }

    @Override
    public void onConfigure(Config config) {
        super.onConfigure(config);
        if (isStarted) {
            onStop();
            onStart();
        }
    }

    @Override
    public boolean isStarted() {
        return isStarted;
    }

    public void setPace(Boolean value) {
        logger.trace("BGGeo: setPace() called " + value);
        if (!isStarted) {
            return;
        }

        logger.info("Setting pace: {}", value);

        Boolean wasMoving = isMoving;
        isMoving = value;
        isAcquiringStationaryLocation = false;
        isAcquiringSpeed = false;
        stationaryLocation = null;

        try {
            logger.trace("BGGeo: setPace() try");
            locationManager.removeUpdates(this);
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            criteria.setHorizontalAccuracy(translateDesiredAccuracy(mConfig.getDesiredAccuracy()));
            criteria.setPowerRequirement(Criteria.POWER_HIGH);

            if (isMoving) {
                logger.trace("BGGeo: setPace() isMoving " + isMoving);
                if (!wasMoving) {
                    isAcquiringSpeed = true;
                }
            } else {
                logger.trace("BGGeo: setPace() not isMoving " + isMoving);
                isAcquiringStationaryLocation = true;
            }

            // logger.trace("BGGeo: setPace() isAcquiringSpeed=" + isAcquiringSpeed + ", isAcquiringStationaryLocation=" + isAcquiringStationaryLocation);

            if (isAcquiringSpeed || isAcquiringStationaryLocation) {
                locationAcquisitionAttempts = 0;
                List<String> matchingProviders = locationManager.getAllProviders();
                if (matchingProviders == null || matchingProviders.isEmpty()) {
                    // logger.warn("BGGeo: ❌ No location providers available");
                } else {
                    for (String provider : matchingProviders) {
                        // logger.debug("BGGeo: 📡 Found provider: " + provider);
                        if (!LocationManager.PASSIVE_PROVIDER.equals(provider)) {
                            // logger.trace("BGGeo: ⚙️ Trying to request location updates for: " + provider);
                            try {
                                locationManager.requestLocationUpdates(provider, 0, 0, this);
                                // logger.trace("BGGeo: ✅ Success request location for: " + provider);
                            } catch (SecurityException | IllegalArgumentException e) {
                                logger.trace("BGGeo: ❌ Failed to request for " + provider + ": " + e.getMessage());
                            }
                        }
                        // try {
                        //     Location lastKnown = locationManager.getLastKnownLocation(provider);
                        //     logger.debug("BGGeo: 🧭 Last known location from " + provider + ": " + lastKnown);
                        // } catch (SecurityException e) {
                        //     logger.error("BGGeo: 🛑 No permission for " + provider);
                        // }
                    }
                }
            } else {
                String bestProvider = locationManager.getBestProvider(criteria, true);
                logger.debug("BGGeo: Best provider: " + bestProvider);
                logger.trace("BGGeo: setPace() using best provider only");
                locationManager.requestLocationUpdates(bestProvider, mConfig.getInterval(), scaledDistanceFilter, this);
            }
        } catch (SecurityException e) {
            logger.trace("BGGeo: setPace() catch try");
            logger.error("Security exception: {}", e.getMessage());
            // Optional: handleSecurityException(e);
        }
    }

    private int translateDesiredAccuracy(int desiredAccuracy) {
        // Convert your accuracy config to horizontal accuracy enum value
        return Criteria.ACCURACY_HIGH;
    }

    @Override
    public void onLocationChanged(Location location) {
        // logger.debug("BGGeo: 📍 onLocationChanged() called: " + location);
        // logger.debug("BGGeo: 📍 Location changed: {}", location);
        // You can broadcast or send this to server here
        // if (getPluginDelegate() != null) {
        //     getPluginDelegate().onLocation(new BackgroundLocation(location), new PluginDelegate.Callback() {
        //         @Override
        //         public void onSuccess() {
        //             logger.debug("BGGeo: ✅ JS location callback success");
        //         }

        //         @Override
        //         public void onFailure() {
        //             logger.warn("BGGeo: ❌ JS location callback failed");
        //         }
        //     });
        // }
        handleLocation(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Deprecated, implement only if targeting lower APIs
    }

    @Override
    public void onProviderEnabled(String provider) {
        logger.debug("BGGeo: ✅ Provider enabled: {}", provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        logger.debug("BGGeo: ❌ Provider disabled: {}", provider);
    }
}
