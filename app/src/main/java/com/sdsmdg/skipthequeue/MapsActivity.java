package com.sdsmdg.skipthequeue;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    //https://www.tutorialspoint.com/android/android_google_maps.htm
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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
        LatLng ATM = new LatLng(getIntent().getDoubleExtra("lat",21) + .01, getIntent().getDoubleExtra("lng",57) +.01);
        LatLng user = new LatLng(getIntent().getDoubleExtra("lat",21), getIntent().getDoubleExtra("lng",57));

        mMap.addMarker(new MarkerOptions().position(user).title("Your current Position."));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(user));

        mMap.addMarker(new MarkerOptions().position(ATM).title("Position of ATM."));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(ATM));
        googleMap.getUiSettings().setZoomGesturesEnabled(true);
        LatLngBounds bounds = new LatLngBounds.Builder().include(ATM)
                .include(user).build();
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200));

        //Adding the code to draw route between two markers here.


    }
}
