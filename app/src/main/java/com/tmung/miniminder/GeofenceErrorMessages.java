package com.tmung.miniminder;

import android.content.Context;

import com.google.android.gms.location.GeofenceStatusCodes;

public class GeofenceErrorMessages {

    public static String getErrorString(Context context, int errorCode) {
        switch (errorCode) {
            case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
                return "geofence_not_available";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
                return "geofence_too_many_geofences";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
                return "geofence_too_many_pending_intents";
            default:
                return "unknown_geofence_error";
        }
    }
}
