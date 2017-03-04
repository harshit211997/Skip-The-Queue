package com.sdsmdg.skipthequeue;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.azoft.carousellayoutmanager.CarouselLayoutManager;
import com.azoft.carousellayoutmanager.CarouselZoomPostLayoutListener;
import com.azoft.carousellayoutmanager.CenterScrollListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.TableQueryCallback;
import com.sdsmdg.skipthequeue.Adapters.adapter_class;
import com.sdsmdg.skipthequeue.models.Machine;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

public class StartingActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = StartingActivity.class.getSimpleName();

    private final static int REQUEST_ENABLE_BT = 1;
    private final static int REQUEST_ENABLE_LOCATION = 2;
    private final static int REQUEST_ENABLE_FINE_LOCATION = 3;
    private BluetoothAdapter bluetoothAdapter;

    private GoogleApiClient mGoogleApiClient;

    private List<Machine> mList = new ArrayList<>();
    private RecyclerView recyclerView;
    private adapter_class mAdapter;

    MobileServiceClient mClient;
    MobileServiceTable<Machine> table;

    public Double lat, lng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_starting);
        checkPermissions();
        buildClient();
        makeRecyclerView();
        initalizeClient();
        getAtmList();
//        initializeManagerTable();
    }

    private void initalizeClient() {
        //Get a reference to the manager table
        try {
            mClient = new MobileServiceClient(
                    "https://skipthequeue.azurewebsites.net",
                    this
            );
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        table = mClient.getTable("Manager", Machine.class);
    }

    private void makeRecyclerView() {
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mAdapter = new adapter_class(mList, new adapter_class.OnItemClickListener() {
            @Override
            public void onClick(int position) {
                //Onclick is set on the adapter.

                if(lat != null && lng != null)
                {
                    //Making the static object of the machine being used for further transactions.

                    Helper.machine = mList.get(position);
                    openMapsActivity(
                            lat,//current latitude(if available)
                            lng//current longitude(if available)
                    );
                }
                else
                {
                    //Open Maps before using.
                    Toast.makeText(StartingActivity.this, " Please make a Location Request.", Toast.LENGTH_LONG).show();
                }

            }
        });

        final CarouselLayoutManager layoutManager = new CarouselLayoutManager(CarouselLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);
        recyclerView.addOnScrollListener(new CenterScrollListener());
        layoutManager.setPostLayoutListener(new CarouselZoomPostLayoutListener());

    }

    public void getAtmList() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                table.where().execute(new TableQueryCallback<Machine>() {
                    @Override
                    public void onCompleted(List<Machine> result, int count, Exception exception, ServiceFilterResponse response) {
                        if(exception == null) {
                            mList.addAll(result);
                            mAdapter.notifyDataSetChanged();
                            Log.i(TAG, result.get(0).tableName);
                        } else {
                            exception.printStackTrace();
                            getAtmList();
                        }
                    }
                });
            }
        }).start();

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

        //If machine is null, that means we haven't received the machine, user wishes to view his status for
        if(Helper.machine != null) {
            Intent i = new Intent(this, MainActivity.class);
            i.putExtra("allowGenerate", false);
            i.putExtra("allowReport", false);
            startActivity(i);
        } else {
            Toast.makeText(StartingActivity.this, "Please specify the atm, for which, you wish to view your status", Toast.LENGTH_SHORT).show();
        }
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
            lat = mLastLocation.getLatitude();
            lng = mLastLocation.getLongitude();
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

//        Geocoder geocoder;
//        List<Address> addresses;
//        geocoder = new Geocoder(this, Locale.getDefault());
//
//        addresses = geocoder.getFromLocation(lat, lng, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
//
//        address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
//        city = addresses.get(0).getLocality();
//        state = addresses.get(0).getAdminArea();
//        country = addresses.get(0).getCountryName();
//        postalCode = addresses.get(0).getPostalCode();
//        knownName = addresses.get(0).getFeatureName(); // Only if available else return NULL
//
//        String s = address + "\n" + city + "\n" + state + "\n" + country + "\n" + postalCode + "\n" + knownName;
//
//        TextView demo = (TextView)findViewById(R.id.demo);
//        demo.setText(s);

//        Toast.makeText(this, "Got the location :)", Toast.LENGTH_SHORT).show();
//
//        Intent i = new Intent(this, MapsActivity.class);
//        i.putExtra("lat", lat);
//        i.putExtra("lng",lng);
//        startActivity(i);


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

        if(mGoogleApiClient != null)
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
                else
                {
                    Toast.makeText(this, "Location is granted!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(getApplicationContext(), StartingActivity.class));
                }

            }

            case REQUEST_ENABLE_BT:{

                if(resultCode != RESULT_OK)
                    Toast.makeText(this, "Bluetooth is Required.",Toast.LENGTH_SHORT).show();
                break;

            }

        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void openMapsActivity(double lat, double lng) {
        Intent i = new Intent(this, MapsActivity.class);
        i.putExtra("lat", lat);
        i.putExtra("lng", lng);

        startActivity(i);
    }


    public void initializeManagerTable() {

        try {
            mClient = new MobileServiceClient(
                    "https://skipthequeue.azurewebsites.net",
                    this
            );
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                MobileServiceTable<Machine> managerTable = mClient.getTable("Manager", Machine.class);
                Machine m2 = new Machine("UID", "29.864468", "77.895905", 0, true, "SBI_ATM");
                managerTable.insert(m2);
                Toast.makeText(getApplicationContext(), "Db updated", Toast.LENGTH_LONG).show();

            }
        }).start();

    }

}
