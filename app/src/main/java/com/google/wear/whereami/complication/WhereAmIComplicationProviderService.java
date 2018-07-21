package com.google.wear.whereami.complication;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.location.Address;
import android.location.Location;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;
import android.support.wearable.complications.ComplicationText;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.google.android.gms.location.LocationRequest;
import com.google.wear.whereami.R;
import com.google.wear.whereami.WhereAmIActivity;
import com.patloew.rxlocation.FusedLocation;
import com.patloew.rxlocation.RxLocation;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class WhereAmIComplicationProviderService extends ComplicationProviderService {

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
    public void onComplicationUpdate(int complicationId, int dataType, ComplicationManager manager) {

        Log.d(TAG, "onComplicationUpdate() id: " + complicationId);

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
                        (locationAddressPair -> updateComplication(complicationId, dataType, manager, locationAddressPair.first, locationAddressPair.second)),
                        // onError
                        (error) -> {
                            Log.e(TAG, "Error retreiving location", error);
                            updateComplication(complicationId, dataType, manager, null, null);
                        }
                )
        );
    }

    private void updateComplication(int complicationId, int dataType, ComplicationManager manager, Location location, Address address) {
        Log.d(TAG, "Address: " + address);

        ComplicationData complicationData = null;
        switch (dataType) {
            case ComplicationData.TYPE_SHORT_TEXT:
                complicationData =
                        new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                                .setShortText(getTimeAgo(location))
                                .setContentDescription(getFullDescription(location, address))
                                .setIcon(Icon.createWithResource(this, R.drawable.ic_my_location))
                                .setTapAction(getTapAction())
                                .build();
                break;
            case ComplicationData.TYPE_LONG_TEXT:
                complicationData =
                        new ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                                .setLongTitle(getTimeAgo(location))
                                .setLongText(getAddressDescriptionText(this, address))
                                .setContentDescription(getFullDescription(location, address))
                                .setIcon(Icon.createWithResource(this, R.drawable.ic_my_location))
                                .setTapAction(getTapAction())
                                .build();
                break;
            default:
                if (Log.isLoggable(TAG, Log.WARN)) {
                    Log.w(TAG, "Unexpected complication type " + dataType);
                }
        }

        if (complicationData != null) {
            manager.updateComplicationData(complicationId, complicationData);
        } else {
            // If no data is sent, we still need to inform the ComplicationManager, so
            // the update job can finish and the wake lock isn't held any longer.
            manager.noUpdateRequired(complicationId);
        }
    }

    private PendingIntent getTapAction() {
        Intent intent = new Intent(this, WhereAmIActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private ComplicationText getFullDescription(Location location, Address address) {
        if (location == null || address == null) return ComplicationText.plainText(getString(R.string.no_location));

        return getTimeAgo(location.getTime())
                .setSurroundingText(getString(R.string.address_as_of_time_ago, getAddressDescription(this, address), "^1"))
                .build();
    }

    public static ComplicationText getAddressDescriptionText(Context context, Address address) {
        return ComplicationText.plainText(getAddressDescription(context, address));
    }

    public static String getAddressDescription(Context context, Address address) {
        if (address == null) return context.getString(R.string.no_location);
        String subThoroughfare = address.getSubThoroughfare();
        String thoroughfare = address.getThoroughfare();
        if (thoroughfare == null) return address.toString();
        return (TextUtils.isEmpty(subThoroughfare) ? "" : subThoroughfare +  " ") + thoroughfare;
    }


    private ComplicationText getTimeAgo(Location location) {
        if (location == null) return ComplicationText.plainText("--");
        return getTimeAgo(location.getTime()).build();
    }

    private ComplicationText.TimeDifferenceBuilder getTimeAgo(long fromTime) {
        return new ComplicationText.TimeDifferenceBuilder()
                .setStyle(ComplicationText.DIFFERENCE_STYLE_SHORT_WORDS_SINGLE_UNIT)
                .setMinimumUnit(TimeUnit.MINUTES)
                .setReferencePeriodEnd(fromTime)
                .setShowNowText(true);
    }

    public static LocationRequest createLocationRequest() {
        return LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setExpirationDuration(TimeUnit.SECONDS.toMillis(30));
    }
}
