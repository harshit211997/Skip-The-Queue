package com.sdsmdg.skipthequeue;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.kontakt.sdk.android.common.profile.IEddystoneDevice;
import com.sdsmdg.skipthequeue.BeaconFinder.BeaconAdapter;
import com.sdsmdg.skipthequeue.BeaconFinder.BeaconFinderService;
import com.sdsmdg.skipthequeue.Maps.JSONParser;
import com.sdsmdg.skipthequeue.models.Machine;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    BroadcastReceiver broadcastReceiver;
    BeaconAdapter beaconAdapter;
    ArrayList<IEddystoneDevice> beaconsArray;
    public final static String BEACON = "UID";
    private RemoteViews remoteViews;
    private NotificationCompat.Builder builder;
    private NotificationManager notificationManager;
    private ProgressDialog progressDialog;
    private Machine machine;

    //https://www.tutorialspoint.com/android/android_google_maps.htm
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        makeReceiver();
        makeTestNotification();
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
    protected void onDestroy() {
        //Unregister the receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        //Declare the points.

        Double userLat = getIntent().getDoubleExtra("lat", 21);
        Double userLng = getIntent().getDoubleExtra("lng", 57);
        machine = Helper.machine;
        Double atmLat = Double.parseDouble(machine.lat);
        Double atmLng = Double.parseDouble(machine.longi);

        final LatLng ATM = new LatLng(atmLat, atmLng);
        final LatLng user = new LatLng(userLat, userLng);

        mMap.addMarker(new MarkerOptions().position(user).title("Your current Position."));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(user));

        mMap.addMarker(new MarkerOptions().position(ATM).title("Position of ATM."));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(ATM));

        googleMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.animateCamera( CameraUpdateFactory.zoomTo( 17.0f ) );
        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                LatLngBounds bounds = new LatLngBounds.Builder().include(ATM)
                        .include(user).build();
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200));
                progressDialog.hide();
            }
        });


        //Adding the code to draw route between two markers.
        String urlPass = makeURL(userLat, userLng, atmLat, atmLng);
        ConnectAsyncTask asyncTask = new ConnectAsyncTask(urlPass);
        asyncTask.execute();
        //Make the SnackBar after the request is executed.
        makeSnackbar();


    }

    public String makeURL(double sourcelat, double sourcelog, double destlat, double destlog) {
        StringBuilder urlString = new StringBuilder();
        urlString.append("https://maps.googleapis.com/maps/api/directions/json");
        urlString.append("?origin=");// from
        urlString.append(Double.toString(sourcelat));
        urlString.append(",");
        urlString
                .append(Double.toString(sourcelog));
        urlString.append("&destination=");// to
        urlString
                .append(Double.toString(destlat));
        urlString.append(",");
        urlString.append(Double.toString(destlog));
        urlString.append("&sensor=false&mode=driving&alternatives=true");
        urlString.append("&key=AIzaSyCzMYoH0gJDstUbcWsicITDcIURScd42Y0");
        Log.i("TAG", "makeURL: " + urlString.toString());
        return urlString.toString();

    }

    public void drawPath(String result) {

        try {
            //Tranform the string into a json object
            final JSONObject json = new JSONObject(result);
            JSONArray routeArray = json.getJSONArray("routes");
            JSONObject routes = routeArray.getJSONObject(0);
            JSONObject overviewPolylines = routes.getJSONObject("overview_polyline");
            String encodedString = overviewPolylines.getString("points");
            List<LatLng> list = decodePoly(encodedString);

            Polyline line = mMap.addPolyline(new PolylineOptions()
                    .addAll(list)
                    .width(12)
                    .color(Color.parseColor("#05b1fb"))//Google maps blue color
                    .geodesic(true)
            );


            /*
           for(int z = 0; z<list.size()-1;z++){
                LatLng src= list.get(z);
                LatLng dest= list.get(z+1);
                Polyline line = mMap.addPolyline(new PolylineOptions()
                .add(new LatLng(src.latitude, src.longitude), new LatLng(dest.latitude,   dest.longitude))
                .width(2)
                .color(Color.BLUE).geodesic(true));
            }
           */
        } catch (JSONException e) {

        }
    }

    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    private class ConnectAsyncTask extends AsyncTask<Void, Void, String> {
        String url;

        ConnectAsyncTask(String urlPass) {
            url = urlPass;
        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
            progressDialog = new ProgressDialog(MapsActivity.this);
            progressDialog.setMessage("Fetching route, Please wait...");
            progressDialog.setIndeterminate(true);
            progressDialog.show();

        }

        @Override
        protected String doInBackground(Void... params) {
            JSONParser jParser = new JSONParser();
            String json = jParser.getJSONFromUrl(url);
            return json;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null) {

                Toast.makeText(getApplicationContext(), "Path Available", Toast.LENGTH_SHORT).show();
                drawPath(result);
            } else {
                Toast.makeText(getApplicationContext(), "Path Not Available", Toast.LENGTH_SHORT).show();
            }

            //Make the SnackBar after the request is executed.
            makeSnackbar();

        }

    }


    private void makeSnackbar() {

        if (isServiceRunning(BeaconFinderService.class)) {
            Snackbar snackbar = Snackbar.make(this.findViewById(android.R.id.content), "Scanning For the Queue", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Stop", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent i = new Intent(MapsActivity.this, BeaconFinderService.class);
                            MapsActivity.this.stopService(i);
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
                            Intent i = new Intent(MapsActivity.this, BeaconFinderService.class);
                            MapsActivity.this.startService(i);
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
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                if (message == "Beacon Found") {
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


                Snackbar snackbar = Snackbar.make(MapsActivity.this.findViewById(android.R.id.content), "Queue Found", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Continue", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                Intent i = new Intent(MapsActivity.this, MainActivity.class);
                                i.putExtra("machine", machine);
                                i.putExtra("allowGenerate", true);
                                i.putExtra("allowReport", true);
                                i.putExtra(BEACON, beaconsArray.get(0));
                                startActivity(i);

                            }
                        });
                snackbar.show();

            }
        }, 1000);


    }

    private void makeTestNotification() {

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        remoteViews = new RemoteViews(getPackageName(), R.layout.notification_layout);
        Intent button_intent = new Intent("connectBeacon");
        button_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        PendingIntent pi = PendingIntent.getBroadcast(getApplicationContext(), 0, button_intent, 0);
        remoteViews.setOnClickPendingIntent(R.id.connectBeacon, pi);

        Intent new_button_intent = new Intent("disconnectBeacon");
        new_button_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        PendingIntent pii = PendingIntent.getBroadcast(getApplicationContext(), 0, new_button_intent, 0);
        remoteViews.setOnClickPendingIntent(R.id.disconnectBeacon, pii);

        //Create the notification here

        Intent notification_intent = new Intent(getApplicationContext(), StartingActivity.class);
        notification_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        PendingIntent piii = PendingIntent.getActivity(getApplicationContext(), 0, notification_intent, 0);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        builder = new NotificationCompat.Builder(getApplicationContext());
        builder.setContentIntent(piii)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Simulate Test Beacon")
                .setCustomBigContentView(remoteViews)
                .setSound(alarmSound);
        //Sticky notification made
        Notification notification = builder.build();
        //notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        notificationManager.notify(01, notification);

    }

    @Override
    protected void onResume() {

        makeSnackbar();
        super.onResume();
    }
}
