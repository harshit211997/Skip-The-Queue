package com.sdsmdg.skipthequeue.BeaconFinder;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;


import com.kontakt.sdk.android.ble.configuration.scan.ScanMode;
import com.kontakt.sdk.android.ble.connection.OnServiceReadyListener;
import com.kontakt.sdk.android.ble.manager.ProximityManager;
import com.kontakt.sdk.android.ble.manager.ProximityManagerContract;
import com.kontakt.sdk.android.ble.manager.listeners.EddystoneListener;
import com.kontakt.sdk.android.ble.spec.EddystoneFrameType;
import com.kontakt.sdk.android.common.KontaktSDK;
import com.kontakt.sdk.android.common.profile.IEddystoneDevice;
import com.kontakt.sdk.android.common.profile.IEddystoneNamespace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class BeaconFinderService extends Service {


    private ProximityManagerContract proximityManager;
    private ArrayList<IEddystoneDevice> beaconArray;
    private LocalBroadcastManager localBroadcastManager;

    //Testing
    static final public String intent_filter = "just_fucking_around";
    static final public String beacons_array = "writing_cause_no_one_would_ever_read";
    static final public String string_test = "writing_cause_read";


    @Override
    public void onCreate() {

        Log.i("HEy","Started");
        intializeSDK();
        beaconArray = new ArrayList<>();
        localBroadcastManager = LocalBroadcastManager.getInstance(this);

    }

    private void sendResult(String message) {
        Intent intent = new Intent(intent_filter);
        if(message != null)
            intent.putExtra(string_test, message);
            intent.putParcelableArrayListExtra(beacons_array, beaconArray);

        localBroadcastManager.sendBroadcast(intent);
    }

    private void intializeSDK() {

        KontaktSDK.initialize("UNgjRaTWqvEYlAVPXZwCouILLDqahvWi");
        configureProxyManager();

    }

    private void configureProxyManager() {
        proximityManager = new ProximityManager(this);

        proximityManager.configuration().
                eddystoneFrameTypes(Arrays.asList(EddystoneFrameType.UID)).
                scanMode(ScanMode.LOW_LATENCY);

        //Setting up the listeners
        proximityManager.setEddystoneListener(eddy_listener());

    }

    private EddystoneListener eddy_listener() {
        return new EddystoneListener() {
            @Override
            public void onEddystoneDiscovered(IEddystoneDevice eddystone, IEddystoneNamespace namespace) {

                if(!beaconArray.contains(eddystone))
                {
                    beaconArray.add(eddystone);
                    Log.i("Service", "Beacon Found");
                    sendResult("Beacon Found");
                }

            }

            @Override
            public void onEddystonesUpdated(List<IEddystoneDevice> eddystones, IEddystoneNamespace namespace) {

            }

            @Override
            public void onEddystoneLost(IEddystoneDevice eddystone, IEddystoneNamespace namespace) {

                if(beaconArray.contains(eddystone))
                {
                    beaconArray.remove(eddystone);
                    Log.i("Service", "Beacon Lost");
                    sendResult("Beacon Lost");
                }

            }
        };
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        startScanning();

        return super.onStartCommand(intent, flags, startId);
    }

    private void startScanning() {

        proximityManager.connect(new OnServiceReadyListener() {
            @Override
            public void onServiceReady() {
                proximityManager.startScanning();
            }
        });

    }

    @Override
    public void onDestroy() {

        stopScanning();


    }

    private void stopScanning() {

        proximityManager.stopScanning();
        proximityManager.disconnect();

    }

}
