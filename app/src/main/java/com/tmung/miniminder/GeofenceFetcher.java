package com.tmung.miniminder;

import java.util.List;

public interface GeofenceFetcher {
    void onFetched(List<LocationData> geofenceLocations);
}
