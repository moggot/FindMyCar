package com.moggot.findmycarlocation.data.repository.network;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.google.android.gms.maps.model.LatLng;
import com.moggot.findmycarlocation.data.api.LocationApi;
import com.moggot.findmycarlocation.data.model.location.LocationParams;
import com.moggot.findmycarlocation.data.model.route.Path;
import com.moggot.findmycarlocation.data.repository.local.SettingsPreferences;

import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import timber.log.Timber;

public class NetworkRepo {

    private static final int NETWORK_TIMEOUT = 5;
    @NonNull
    private final LocationApi locationApi;
    @NonNull
    private final SettingsPreferences preferences;

    public NetworkRepo(@NonNull LocationApi locationApi, @NonNull SettingsPreferences preferences) {
        this.locationApi = locationApi;
        this.preferences = preferences;
    }

    @WorkerThread
    public Single<Path> getRoute(LatLng origin) {
        LocationParams locationParams = new LocationParams(origin, preferences.loadLocation());
        String originStr = locationParams.getOrigin().latitude + "," + locationParams.getOrigin().longitude;
        String destinationStr = locationParams.getDestination().latitude + "," + locationParams.getDestination().longitude;
        Timber.d("origin = " + originStr + "  dest = " + destinationStr);
        return locationApi.getLocation(originStr, destinationStr)
                .timeout(NETWORK_TIMEOUT, TimeUnit.SECONDS);
    }
}
