package com.sdsmdg.skipthequeue.movies;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.kontakt.sdk.android.common.profile.IEddystoneDevice;
import com.sdsmdg.skipthequeue.BeaconFinder.BeaconFinderService;
import com.sdsmdg.skipthequeue.MainActivity;
import com.sdsmdg.skipthequeue.MapsActivity;
import com.sdsmdg.skipthequeue.R;

import java.util.ArrayList;

public class OrderActivity extends AppCompatActivity {

    BroadcastReceiver broadcastReceiver;
    ArrayList<IEddystoneDevice> beaconsArray;
    public final static String BEACON = "UID";
    Button giveOrder,getToken;
    boolean beacon_found, order_placed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);
        makeReceiver();
        makeSnackbar();
        disableButtonsSetFlags();
    }

    private void disableButtonsSetFlags() {
        beacon_found = false;
        order_placed = false;
    }

    public void getTokenClicked(View view) {
        if(beacon_found && order_placed)
        {
            Intent i = new Intent(this, MovieGenTokenActivity.class);
            startActivity(i);
        }
        else if(beacon_found)
        {
            Toast.makeText(this,"Place the order first then proceed.", Toast.LENGTH_LONG).show();

        }
        else {
            Toast.makeText(this,"Please Connect to a beacon first.", Toast.LENGTH_LONG).show();
        }
    }

    public void giveOrderClicked(View view) {

        if(beacon_found)
        {
            //TODO: make a webview that renders the page provided by the food caterer

            order_placed = true;
            Toast.makeText(this,"Order is Placed.", Toast.LENGTH_LONG).show();

        }
        else
        {
            Toast.makeText(this,"Please Connect to a beacon first.", Toast.LENGTH_LONG).show();
        }
    }


    public void viewOrderStatus(View view) {
        //TODO : Add the view status activity here.
    }

    @Override
    protected void onStart() {
        //Register the receiver
        LocalBroadcastManager.getInstance(this).registerReceiver((broadcastReceiver),
                new IntentFilter(BeaconFinderService.intent_filter)
        );
        super.onStart();
    }

    protected void onDestroy() {
        //Unregister the receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    private void makeSnackbar() {

        if (isServiceRunning(BeaconFinderService.class)) {
            Snackbar snackbar = Snackbar.make(this.findViewById(android.R.id.content), "Scanning For the Queue", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Stop", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent i = new Intent(OrderActivity.this, BeaconFinderService.class);
                            OrderActivity.this.stopService(i);
                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    makeSnackbar();
                                }
                            }, 1000);

                        }
                    });
            snackbar.show();
        } else {
            Snackbar snackbar = Snackbar.make(this.findViewById(android.R.id.content), "Scan For the Queue", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Start", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent i = new Intent(OrderActivity.this, BeaconFinderService.class);
                            OrderActivity.this.startService(i);
                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    makeSnackbar();
                                }
                            }, 1000);
                        }
                    });
            snackbar.show();
        }

    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void makeReceiver() {
        broadcastReceiver = new BroadcastReceiver() {
            //This receiver receives the data from background BeaconFinderService.
            @Override
            public void onReceive(Context context, Intent intent) {

                //TODO : make the beacon which is passed here come from the Recycler View activity.
                //This is just a temporary fix.

                String message = intent.getStringExtra(BeaconFinderService.string_test);
                beaconsArray = intent.getParcelableArrayListExtra(BeaconFinderService.beacons_array);
                if (message == "Beacon Found") {
                    beacon_found = true;
                    replaceSnackBar();
                }

            }
        };
    }

    private void replaceSnackBar() {

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {


                Snackbar snackbar = Snackbar.make(OrderActivity.this.findViewById(android.R.id.content), "Vendor Available.", Snackbar.LENGTH_INDEFINITE);
                snackbar.show();

            }
        }, 1000);


    }



}
