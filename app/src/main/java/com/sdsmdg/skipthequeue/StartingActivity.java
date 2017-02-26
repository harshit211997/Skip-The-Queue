package com.sdsmdg.skipthequeue;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.TableOperationCallback;
import com.sdsmdg.skipthequeue.models.Machine;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Locale;

public class StartingActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {


    private final static int REQUEST_ENABLE_BT = 1;
    private final static int REQUEST_ENABLE_LOCATION = 2;
    private final static int REQUEST_ENABLE_FINE_LOCATION = 3;
    private BluetoothAdapter bluetoothAdapter;

    private GoogleApiClient mGoogleApiClient;
    private String address;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String knownName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_starting);
        checkPermissions();
        buildClient();




    }

    private void buildClient() {
        //Working to retrive the location using google play services.
        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null)
        {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    public void presentClick(View view) {

        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("allowGenerate", false);
        i.putExtra("allowReport", false);
        startActivity(i);
    }

    public void beacon_scanner_button(View view) {
        Intent i = new Intent(this, BeaconScannerActivity.class);
        startActivity(i);
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

        Toast.makeText(this, "Api Client Connected", Toast.LENGTH_SHORT).show();
        try {
            getLocation();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void getLocation() throws IOException {


        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this,"Not all the permissions are granted.", Toast.LENGTH_SHORT).show();

            return;
        }
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {

            Double lat = mLastLocation.getLatitude();
            Double lng = mLastLocation.getLongitude();
            getAdress(lat,lng);
        }
        else
        {
            Toast.makeText(this,"Location received as null.",Toast.LENGTH_SHORT).show();
        }

    }

    private void getAdress(Double lat, Double lng) throws IOException {

        // A class for handling geocoding and reverse geocoding.
        // Geocoding is the process of transforming a street address or other description of a location into a (latitude, longitude)
        // coordinate. Reverse geocoding is the process of transforming a (latitude, longitude) coordinate into a (partial) address.
        // The amount of detail in a reverse geocoded location description may vary,
        // for example one might contain the full street address of the closest building,
        // while another might contain only a city name and postal code.
        // The Geocoder class requires a backend service that is not included in the core android framework.
        // The Geocoder query methods will return an empty list if there no backend service in the platform.
        // Use the isPresent() method to determine whether a Geocoder implementation exists.

        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());

        addresses = geocoder.getFromLocation(lat, lng, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5

        address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
        city = addresses.get(0).getLocality();
        state = addresses.get(0).getAdminArea();
        country = addresses.get(0).getCountryName();
        postalCode = addresses.get(0).getPostalCode();
        knownName = addresses.get(0).getFeatureName(); // Only if available else return NULL

        String s = address + "\n" + city + "\n" + state + "\n" + country + "\n" + postalCode + "\n" + knownName;

        TextView demo = (TextView)findViewById(R.id.demo);
        demo.setText(s);



    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this,"Api Client Disconnected", Toast.LENGTH_SHORT).show();


    }

    //Connect Apiclient in OnStart and OnStop
    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this,"Api Client Connection Failed.", Toast.LENGTH_SHORT).show();

    }

    private void checkPermissions()
    {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!bluetoothAdapter.isEnabled())
        {
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(i,REQUEST_ENABLE_BT);
        }

        //Turn Location Settings On
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

        //Runtime Permission
        if(Build.VERSION.SDK_INT >= 23 && (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED))
        {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_ENABLE_FINE_LOCATION);
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

        if (requestCode != REQUEST_ENABLE_FINE_LOCATION || grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this,"Please accept the Runtime Permission", Toast.LENGTH_SHORT).show();
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode){

            case REQUEST_ENABLE_LOCATION:
            {
                if(!isLocationEnabled(getApplicationContext()))
                    Toast.makeText(this, "Location is required!", Toast.LENGTH_SHORT).show();

            }

            case REQUEST_ENABLE_BT:{

                if(resultCode != RESULT_OK)
                    Toast.makeText(this, "Bluetooth is Required.",Toast.LENGTH_SHORT).show();
                break;

            }

        }

        super.onActivityResult(requestCode, resultCode, data);
    }

}







//   MobileServiceClient mClient;
//    MobileServiceTable<Machine> table;
//    Machine machine;


//        //This defines the query address to the client
//        try {
//            mClient = new MobileServiceClient(
//                    "https://skipthequeue.azurewebsites.net",
//                    this
//            );
//        } catch (MalformedURLException e) {
//            e.printStackTrace();
//        }
//
//        //Change the type and table name here that has to be queried.
//
//        table = mClient.getTable("Manager",Machine.class);
//
//        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
//            @Override
//            protected Void doInBackground(Void... params) {
//
//                try {
//
//                    final List<Machine> results = table.where().execute().get();
//
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast.makeText(StartingActivity.this, "Length of table is : " + results.size(), Toast.LENGTH_SHORT).show();
//                        }
//                    });
//
//                    machine = new Machine();
//                    machine.beaconUID = "Rockabye";
//                    machine.location = "GB Road";
//                    machine.queueLength = 6;
//                    machine.statusWorking = false;
//
//                    table.insert(machine, new TableOperationCallback<Machine>() {
//                        @Override
//                        public void onCompleted(Machine entity, Exception exception, ServiceFilterResponse response) {
//
//                            //Oncompleted runs on the UI Thread.
//                            if(exception == null)
//                            {
//                                Toast.makeText(StartingActivity.this, "Insert suceeded in Manager Tabel" , Toast.LENGTH_SHORT).show();
//
//                            }
//                            else
//                            {
//                                Toast.makeText(StartingActivity.this, "Insert failed in Manager Tabel" , Toast.LENGTH_SHORT).show();
//
//                            }
//                        }
//                    });
//
//
//                } catch (final Exception e) {
//                    e.printStackTrace();
//                }
//
//                return null;
//            }
//        };
//
//        task.execute();