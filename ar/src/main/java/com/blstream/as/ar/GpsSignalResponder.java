package com.blstream.as.ar;

import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.LocationManager;

/**
 * Created by Damian on 2015-05-02.
 */
public class GpsSignalResponder {
    private static final int MINIMUM_SATELLITES_FIXED = 4;

    public interface Callback {
        void enableAugmentedReality();
        void disableAugmentedReality();
        void showGpsUnavailable();
        void showSearchingSignal();
        void hideSearchingSignal();
    }
    private LocationManager locationManager;
    private Callback callbackFromGps;
    private GpsStatusListener gpsStatusListener;
    private boolean isAvailable;
    private boolean isLocated;
    private boolean isFirstFixed;

    public GpsSignalResponder() {
        isAvailable = isLocated = false;
    }
    public void setLocationManager(LocationManager locationManager) {
        this.locationManager = locationManager;
    }

    private class GpsStatusListener implements GpsStatus.Listener {
        @Override
        public void onGpsStatusChanged(int event) {
            switch(event)
            {
                case GpsStatus.GPS_EVENT_STARTED:
                    break;
                case GpsStatus.GPS_EVENT_STOPPED:
                    break;
                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    break;
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    GpsStatus status = locationManager.getGpsStatus(null);
                    Iterable<GpsSatellite> allSatellites = status.getSatellites();
                    int numFixedSatellites = 0;
                    boolean areFixed = false;
                    for(GpsSatellite satellite : allSatellites) {
                        if(satellite.usedInFix()) {
                            ++numFixedSatellites;
                        }
                    }
                    if(numFixedSatellites >= MINIMUM_SATELLITES_FIXED) {
                        areFixed = true;
                    }
                    if(!isFirstFixed && !areFixed) {
                        callbackFromGps.showSearchingSignal();
                        isFirstFixed = true;
                    }
                    if(isLocated != areFixed) {
                        isLocated = areFixed;
                        if(isLocated) {
                            callbackFromGps.enableAugmentedReality();
                            callbackFromGps.hideSearchingSignal();
                        } else {
                            callbackFromGps.disableAugmentedReality();
                            callbackFromGps.showSearchingSignal();
                        }
                    }
                    break;
            }
        }
    }
    public void attachCallback(Callback callbackFromGps) {
        this.callbackFromGps = callbackFromGps;
        if(locationManager == null) {
            isAvailable = false;
            return;
        }
        isAvailable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if(isAvailable) {
            initGps();
        } else
        {
            callbackFromGps.showGpsUnavailable();
        }
    }
    private void initGps() {
        if(locationManager != null) {
            gpsStatusListener = new GpsStatusListener();
            locationManager.addGpsStatusListener(gpsStatusListener);
        }
    }
    public void detachCallback() {
        if(locationManager != null) {
            locationManager.removeGpsStatusListener(gpsStatusListener);
        }
    }
    public boolean isLocated() {
        return isLocated;
    }
}
