package com.sdsmdg.skipthequeue;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.sdsmdg.skipthequeue.movies.OrderActivity;

public class CategoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);
    }

    public void atmSelected(View view) {
        Intent i = new Intent(this, StartingActivity.class);
        startActivity(i);
    }

    public void moviesSelected(View view) {
        Intent i = new Intent(this, OrderActivity.class);
        startActivity(i);
        
    }


    public void restaurantSelected(View view) {
        Toast.makeText(this,"Coming Soon",Toast.LENGTH_LONG).show();
    }

    public void ticketsSelected(View view) {
        Toast.makeText(this,"Coming Soon",Toast.LENGTH_LONG).show();
    }
}
