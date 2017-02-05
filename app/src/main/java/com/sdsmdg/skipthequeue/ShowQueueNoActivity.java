package com.sdsmdg.skipthequeue;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.kontakt.sdk.android.common.profile.IEddystoneDevice;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.sdsmdg.skipthequeue.BeaconFinder.BeaconFinderService;
import com.sdsmdg.skipthequeue.models.User;

import java.net.MalformedURLException;
import java.util.ArrayList;

public class ShowQueueNoActivity extends AppCompatActivity {

    TextView tokenTextView;
    TextView timeTextView;
    MobileServiceClient mClient;
    MobileServiceTable<User>table;
    ArrayList<IEddystoneDevice> beaconsArray;
    private IEddystoneDevice beacon;
    BroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_queue_no);

        int queueNo = 0;
        int queueSize = 0;

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            queueNo = extras.getInt("queue_no");
            queueSize = extras.getInt("queue_size");
            beacon = (IEddystoneDevice) extras.getSerializable(StartingActivity.BEACON);

        }

        tokenTextView = (TextView) findViewById(R.id.token_text_view);
        tokenTextView.setText(queueNo + "");

        //Some problem here.

        int expectedTime = 2 * (queueSize);

        timeTextView =(TextView) findViewById(R.id.user_time);
        timeTextView.setText( "Expected time till your chance : " + expectedTime + " min");
        makeReceiver();
    }

    public void delete_click(View view) {
        deleteUser();
        Toast.makeText(ShowQueueNoActivity.this, "Token Deleted", Toast.LENGTH_SHORT ).show();
        Intent i = new Intent(ShowQueueNoActivity.this, StartingActivity.class);
        startActivity(i);

    }

    private void deleteUser() {
        try {
            mClient = new MobileServiceClient(
                    "https://skipthequeue.azurewebsites.net",
                    this
            );
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        table = mClient.getTable(User.class);
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {

                try {
                    Intent i = getIntent();
                    User user = (User) i.getSerializableExtra("user");
                    table.delete(user);
                }

                catch (final Exception e){
                    e.printStackTrace();
                }

                return null;
            }
        };

        task.execute();
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

    private void makeReceiver() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                beaconsArray = intent.getParcelableArrayListExtra(BeaconFinderService.beacons_array);
                Toast.makeText(getApplicationContext() , String.valueOf(beaconsArray.size()),Toast.LENGTH_SHORT).show();
                if(!beaconsArray.contains(beacon))
                {
                    Toast.makeText(ShowQueueNoActivity.this, "Beacon Lost, please stay into proximity.", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(ShowQueueNoActivity.this, StartingActivity.class);
                    startActivity(i);
                }

            }
        };
    }
}
