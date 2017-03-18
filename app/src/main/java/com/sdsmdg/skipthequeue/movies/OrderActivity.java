package com.sdsmdg.skipthequeue.movies;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.sdsmdg.skipthequeue.R;

public class OrderActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);
    }

    public void getTokenClicked(View view) {
        Intent i = new Intent(this, MovieGenTokenActivity.class);
        startActivity(i);
    }

    public void giveOrderClicked(View view) {
        //TODO: make a webview that renders the page provided by the food caterer
    }

}
