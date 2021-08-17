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

package com.google.wear.whereami;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
import android.text.format.DateUtils;
import android.util.Pair;
import android.widget.TextView;
import androidx.wear.complications.datasource.ComplicationDataSourceUpdateRequester;

import com.google.android.gms.location.LocationRequest;
import com.google.wear.whereami.complication.WhereAmIComplicationProviderService;
import com.patloew.rxlocation.RxLocation;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class WhereAmIActivity extends FragmentActivity {

    private final CompositeDisposable subscriptions = new CompositeDisposable();

    private TextView textView;
    private RxLocation rxLocation;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.where_am_i_activity);

        textView = (TextView) findViewById(R.id.text);

        rxLocation = new RxLocation(this);

        subscriptions.add(
            checkPermissions()
                .subscribeOn(Schedulers.io())
                .flatMap((isGranted) -> rxLocation.location().updates(createLocationRequest()))
                .flatMapMaybe((location ->
                    rxLocation.geocoding()
                            .fromLocation(location)
                            .map(address -> Pair.create(location, address))
                ))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    // onNext
                    (locationAndAddress) -> {
                        textView.setText(getString(
                            R.string.address_as_of_time_activity,
                            WhereAmIComplicationProviderService.getAddressDescription(this, locationAndAddress.second),
                            getTimeAgo(locationAndAddress.first.getTime())));
                    },
                    // onError
                    (error) -> textView.setText(R.string.location_error)
                )
        );
    }

    @Override
    public void onDestroy() {
        subscriptions.dispose();
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        forceComplicationUpdate();
        super.onStop();
    }

    private void forceComplicationUpdate() {
        if (checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            ComplicationDataSourceUpdateRequester request =
                    new ComplicationDataSourceUpdateRequester(getApplicationContext(), ComponentName.createRelative(getApplicationContext(), ".complication.WhereAmIComplicationProviderService"));
            request.requestUpdateAll();
        }
    }

    private Observable<Boolean> checkPermissions() {
        return new RxPermissions(this)
                .request(Manifest.permission.ACCESS_FINE_LOCATION)
                .map(isGranted -> {
                    if (isGranted) return true;
                    throw new SecurityException("No location permission");
                });
    }

    private CharSequence getTimeAgo(long time) {
        return DateUtils.getRelativeTimeSpanString(time);
    }

    private static LocationRequest createLocationRequest() {
        return LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(TimeUnit.SECONDS.toMillis(10))
                .setSmallestDisplacement(50);
    }
}
