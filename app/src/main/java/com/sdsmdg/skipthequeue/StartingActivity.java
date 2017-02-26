package com.sdsmdg.skipthequeue;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.TableOperationCallback;
import com.microsoft.windowsazure.mobileservices.table.TableQueryCallback;
import com.sdsmdg.skipthequeue.models.Machine;
import com.sdsmdg.skipthequeue.models.User;

import java.net.MalformedURLException;
import java.util.List;

public class StartingActivity extends AppCompatActivity {

    MobileServiceClient mClient;
    MobileServiceTable<Machine> table;
    Machine machine;
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

        //Change the type and table name here that has to be queried.

        table = mClient.getTable("Manager",Machine.class);

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                try {

                    final List<Machine> results = table.where().execute().get();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(StartingActivity.this, "Length of table is : " + results.size(), Toast.LENGTH_SHORT).show();
                        }
                    });

                    machine = new Machine();
                    machine.beaconUID = "Rockabye";
                    machine.location = "GB Road";
                    machine.queueLength = 6;
                    machine.statusWorking = false;

                    table.insert(machine, new TableOperationCallback<Machine>() {
                        @Override
                        public void onCompleted(Machine entity, Exception exception, ServiceFilterResponse response) {

                            //Oncompleted runs on the UI Thread.
                            if(exception == null)
                            {
                                Toast.makeText(StartingActivity.this, "Insert suceeded in Manager Tabel" , Toast.LENGTH_SHORT).show();

                            }
                            else
                            {
                                Toast.makeText(StartingActivity.this, "Insert failed in Manager Tabel" , Toast.LENGTH_SHORT).show();

                            }
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
