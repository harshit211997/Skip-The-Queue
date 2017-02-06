package com.sdsmdg.skipthequeue;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.kontakt.sdk.android.common.profile.IEddystoneDevice;
import com.sdsmdg.skipthequeue.BeaconFinder.BeaconAdapter;
import com.sdsmdg.skipthequeue.BeaconFinder.BeaconFinderService;

import java.util.ArrayList;

public class StartingActivity extends AppCompatActivity {


    private final static int REQUEST_ENABLE_BT = 1;
    private final static int REQUEST_ENABLE_LOCATION = 2;
    private final static int REQUEST_ENABLE_FINE_LOCATION = 3;
    public final static String BEACON = "UID";

    private NotificationCompat.Builder builder;
    private NotificationManager notificationManager;
    ListView beaconsListView;
    BroadcastReceiver broadcastReceiver;
    ArrayList<IEddystoneDevice> beaconsArray;
    BeaconAdapter beaconAdapter;
    private BluetoothAdapter bluetoothAdapter;
    private RemoteViews remoteViews;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_starting);
        makeListView();
        makeReceiver();
        checkPermissions();
        makeSnackbar();
        makeTestNotification();

    }

    private void makeTestNotification() {

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        remoteViews =  new RemoteViews(getPackageName(),R.layout.notification_layout);
        Intent button_intent = new Intent("connectBeacon");
        PendingIntent pi = PendingIntent.getBroadcast(getApplicationContext(),123,button_intent,0);
        remoteViews.setOnClickPendingIntent(R.id.connectBeacon,pi);

        Intent new_button_intent = new Intent("disconnectBeacon");
        PendingIntent pii = PendingIntent.getBroadcast(getApplicationContext(),1234,new_button_intent,0);
        remoteViews.setOnClickPendingIntent(R.id.disconnectBeacon,pii);

        //Create the notification here

        Intent notification_intent = new Intent(getApplicationContext(), StartingActivity.class);
        PendingIntent piii = PendingIntent.getActivity(getApplicationContext(),0,notification_intent,0);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        builder = new NotificationCompat.Builder(getApplicationContext());
        builder.setContentIntent(piii)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Simulate Test Beacon")
                .setCustomBigContentView(remoteViews)
                .setSound(alarmSound);
        //Sticky notification made
        Notification notification = builder.build();
        //notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        notificationManager.notify(01,notification);

    }

    private void makeListView() {
        beaconsArray = new ArrayList<>();
        beaconsListView = (ListView) findViewById(R.id.beacons_list_view);
        beaconAdapter = new BeaconAdapter(this,beaconsArray);
        beaconsListView.setAdapter(beaconAdapter);
        beaconsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                if(isServiceRunning(BeaconFinderService.class))
                {
                    IEddystoneDevice item = (IEddystoneDevice) beaconsListView.getItemAtPosition(i);
                    Toast.makeText(getApplicationContext(),"You selected : " + String.valueOf(item.getInstanceId()),Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(StartingActivity.this, MainActivity.class);
                    intent.putExtra(BEACON , item);
                    startActivity(intent);
                }

                else
                {
                    Toast.makeText(getApplicationContext(),"Please switch the beacons scanning service on.",Toast.LENGTH_SHORT).show();

                }
            }
        });
    }

    private void makeSnackbar() {

        if(isServiceRunning(BeaconFinderService.class))
        {
            Snackbar snackbar = Snackbar.make(this.findViewById(android.R.id.content), "Scanning For Beacons", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Stop", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Toast.makeText(getApplicationContext(),"Stop", Toast.LENGTH_SHORT).show();
                            Intent i = new Intent(StartingActivity.this,BeaconFinderService.class);
                            StartingActivity.this.stopService(i);
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

        else
        {
            Snackbar snackbar = Snackbar.make(this.findViewById(android.R.id.content), "Scan For Beacons", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Start", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Toast.makeText(getApplicationContext(),"Start", Toast.LENGTH_SHORT).show();
                            Intent i = new Intent(StartingActivity.this,BeaconFinderService.class);
                            StartingActivity.this.startService(i);
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

    private void makeReceiver() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                beaconsArray = intent.getParcelableArrayListExtra(BeaconFinderService.beacons_array);
                Toast.makeText(getApplicationContext() , String.valueOf(beaconsArray.size()),Toast.LENGTH_SHORT).show();
                beaconAdapter = new BeaconAdapter(getApplicationContext(),beaconsArray);
                beaconsListView.setAdapter(beaconAdapter);

            }
        };
    }

    private void startService() {
        Intent i = new Intent(this, BeaconFinderService.class);
        startService(i);
    }

    @Override
    protected void onStart() {

        //Register the receiver
        LocalBroadcastManager.getInstance(this).registerReceiver((broadcastReceiver),
                new IntentFilter(BeaconFinderService.intent_filter)
        );

        super.onStart();
    }

    @Override
    protected void onStop() {

        //Unregister the receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        super.onStop();

    }


    private void checkPermissions()
    {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!bluetoothAdapter.isEnabled())
        {
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(i,REQUEST_ENABLE_BT);
        }


        if(!isLocationEnabled(getApplicationContext()))
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Location Settings").setMessage("Please Turn on Location Settings to proceed further.").setPositiveButton("Ok",new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                    Intent ii = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(ii,REQUEST_ENABLE_LOCATION);

                }
            }).show();
        }

        if(Build.VERSION.SDK_INT >= 23 && (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED))
        {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_ENABLE_FINE_LOCATION);
        }

        else
        {
            startService();
        }

    }

    public static boolean isLocationEnabled(Context context) {
        int locationMode = 0;
        String locationProviders;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

            return locationMode != Settings.Secure.LOCATION_MODE_OFF;

        }else{
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,  int[] grantResults) {

        if(requestCode == REQUEST_ENABLE_FINE_LOCATION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
            startService();
        }

        else
        {
            Toast.makeText(this,"Please accept the Runtime Permission", Toast.LENGTH_SHORT).show();
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode){

            case REQUEST_ENABLE_BT:{

                if(resultCode != RESULT_OK)
                    Toast.makeText(this,"Bluetooth is Required.",Toast.LENGTH_SHORT).show();
                    break;


            }

            case REQUEST_ENABLE_LOCATION:{

                if(!isLocationEnabled(getApplicationContext()))
                    Toast.makeText(this,"Location is required.",Toast.LENGTH_SHORT).show();
                    break;
            }


        }

        super.onActivityResult(requestCode, resultCode, data);
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


}
