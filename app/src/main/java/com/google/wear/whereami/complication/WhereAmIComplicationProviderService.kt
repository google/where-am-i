// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.wear.whereami.complication;

import static android.location.LocationManager.GPS_PROVIDER;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.location.Address;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.wear.complications.data.*;

import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import androidx.wear.complications.datasource.ComplicationDataSourceService;
import androidx.wear.complications.datasource.ComplicationRequest;

import com.google.android.gms.location.LocationRequest;
import com.google.wear.whereami.R;
import com.google.wear.whereami.WhereAmIActivity;
import com.patloew.rxlocation.FusedLocation;
import com.patloew.rxlocation.RxLocation;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class WhereAmIComplicationProviderService extends ComplicationDataSourceService {

    private static final String TAG = "WhereAmIComplication";

    private RxLocation rxLocation;
    private final CompositeDisposable subscriptions = new CompositeDisposable();

    @Override
    public void onCreate() {
        super.onCreate();

        rxLocation = new RxLocation(this);
    }

    @Override
    public void onDestroy() {
        subscriptions.dispose();
        super.onDestroy();
    }

    @Override
    public void onComplicationRequest(@NonNull ComplicationRequest complicationRequest, @NonNull ComplicationRequestListener complicationRequestListener) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onComplicationUpdate(): " + complicationRequest);
        }

        final Observable<Pair<Location, Address>> task;
        if (checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            FusedLocation locationProvider = rxLocation.location();
            task = locationProvider
                    .isLocationAvailable()
                    .subscribeOn(Schedulers.io())
                    .flatMapObservable((hasLocation) -> hasLocation ?
                            locationProvider.lastLocation().toObservable() :
                            locationProvider.updates(createLocationRequest()))
                    .flatMapMaybe((location) -> rxLocation.geocoding()
                            .fromLocation(location)
                            .map(address -> Pair.create(location, address)));
        } else {
            task = Observable.error(new SecurityException("No location permission!"));
        }

        subscriptions.add(
            task
                .subscribe(
                        // onNext
                        (locationAddressPair -> updateComplication(complicationRequest, complicationRequestListener, locationAddressPair.first, locationAddressPair.second)),
                        // onError
                        (error) -> {
                            Log.w(TAG, "Error retrieving location", error);
                            updateComplication(complicationRequest, complicationRequestListener, null, null);
                        }
                )
        );
    }

    @Nullable
    @Override
    public ComplicationData getPreviewData(@NonNull ComplicationType complicationType) {
        Location location = new Location(GPS_PROVIDER);
        location.setLongitude(0.0);
        location.setLatitude(0.0);

        Address address = new Address(Locale.ENGLISH);
        address.setCountryName("Null Island");

        switch (complicationType) {
            case SHORT_TEXT:
                return new ShortTextComplicationData.Builder(getTimeAgo(location), getFullDescription(location, address))
                                .setMonochromaticImage(new MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_my_location)).build())
                                .setTapAction(getTapAction())
                                .build();
            case LONG_TEXT:
                return new LongTextComplicationData.Builder(getAddressDescriptionText(this, address), getFullDescription(location, address))
                                .setTitle(getTimeAgo(location))
                                .setMonochromaticImage(new MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_my_location)).build())
                                .setTapAction(getTapAction())
                                .build();
            default:
                throw new IllegalArgumentException("Unexpected complication type " + complicationType);
        }
    }

    private void updateComplication(ComplicationRequest complicationRequest, ComplicationRequestListener complicationRequestListener, Location location, Address address) throws RemoteException {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Address: " + address);
        }

        ComplicationData complicationData = null;
        switch (complicationRequest.getComplicationType()) {
            case SHORT_TEXT:
                complicationData =
                        new ShortTextComplicationData.Builder(getTimeAgo(location), getFullDescription(location, address))
                                .setMonochromaticImage(new MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_my_location)).build())
                                .setTapAction(getTapAction())
                                .build();
                break;
            case LONG_TEXT:
                complicationData =
                        new LongTextComplicationData.Builder(getAddressDescriptionText(this, address), getFullDescription(location, address))
                                .setTitle(getTimeAgo(location))
                                .setMonochromaticImage(new MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_my_location)).build())
                                .setTapAction(getTapAction())
                                .build();
                break;
            default:
                if (Log.isLoggable(TAG, Log.WARN)) {
                    Log.w(TAG, "Unexpected complication type " + complicationRequest);
                }
        }

        // If no data is sent, we still need to inform the ComplicationManager, so
        // the update job can finish and the wake lock isn't held any longer.
        complicationRequestListener.onComplicationData(complicationData);
    }

    private PendingIntent getTapAction() {
        Intent intent = new Intent(this, WhereAmIActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private ComplicationText getFullDescription(Location location, Address address) {
        if (location == null || address == null) return new PlainComplicationText.Builder(getString(R.string.no_location)).build();

        return getTimeAgo(location.getTime())
                .build();
    }

    public static ComplicationText getAddressDescriptionText(Context context, Address address) {
        return new PlainComplicationText.Builder(getAddressDescription(context, address)).build();
    }

    public static String getAddressDescription(Context context, Address address) {
        if (address == null) return context.getString(R.string.no_location);
        String subThoroughfare = address.getSubThoroughfare();
        String thoroughfare = address.getThoroughfare();
        if (thoroughfare == null) return address.getCountryName();
        return (TextUtils.isEmpty(subThoroughfare) ? "" : subThoroughfare +  " ") + thoroughfare;
    }


    private ComplicationText getTimeAgo(Location location) {
        if (location == null) return new PlainComplicationText.Builder("--").build();
        return getTimeAgo(location.getTime()).build();
    }

    private TimeDifferenceComplicationText.Builder getTimeAgo(long fromTime) {
        return new TimeDifferenceComplicationText.Builder(TimeDifferenceStyle.SHORT_SINGLE_UNIT, new CountUpTimeReference(fromTime))
                .setMinimumTimeUnit(TimeUnit.MINUTES)
                .setDisplayAsNow(true);
    }

    public static LocationRequest createLocationRequest() {
        return LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setNumUpdates(1)
                .setExpirationDuration(TimeUnit.SECONDS.toMillis(30));
    }
}
