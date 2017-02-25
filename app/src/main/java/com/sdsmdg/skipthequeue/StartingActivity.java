package com.sdsmdg.skipthequeue;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class StartingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_starting);
    }

    public void presentClick(View view) {

        Intent i = new Intent(this, MainActivity.class );
        i.putExtra("allowGenerate", false);
        i.putExtra("allowReport", false);
        startActivity(i);
    }

    public void beacon_scanner_button(View view) {
        Intent i = new Intent(this, BeaconScannerActivity.class );
        startActivity(i);
    }
}
