package com.sdsmdg.skipthequeue;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.sdsmdg.skipthequeue.models.User;

import java.net.MalformedURLException;
import java.util.List;

public class StartingActivity extends AppCompatActivity {

    MobileServiceClient mClient;
    MobileServiceTable<User> table;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_starting);
        //This defines the query address to the client
        try {
            mClient = new MobileServiceClient(
                    "https://skipthequeue.azurewebsites.net",
                    this
            );
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        table = mClient.getTable("User1",User.class);

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                try {
                    final List<User> results = table.where().execute().get();
                    runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(StartingActivity.this, "Length of table is : " + results.size(), Toast.LENGTH_SHORT).show();
                            }
                        });

                } catch (final Exception e) {
                    e.printStackTrace();
                }

                return null;
            }
        };

        task.execute();

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
