package com.sdsmdg.skipthequeue;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.kontakt.sdk.android.common.profile.IEddystoneDevice;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.sdsmdg.skipthequeue.BeaconFinder.BeaconAdapter;
import com.sdsmdg.skipthequeue.BeaconFinder.BeaconFinderService;
import com.sdsmdg.skipthequeue.models.User;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {


    private final static String TAG = MainActivity.class.getSimpleName();
    BroadcastReceiver broadcastReceiver;
    MobileServiceClient mClient;
    MobileServiceTable<User> table;
    ArrayList<IEddystoneDevice> beaconsArray;
    EditText mobileEditText;
    private IEddystoneDevice beacon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mobileEditText = (EditText) findViewById(R.id.mobile_editText);

        try {
            mClient = new MobileServiceClient(
                    "https://skipthequeue.azurewebsites.net",
                    this
            );
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        table = mClient.getTable(User.class);
        beacon = (IEddystoneDevice) getIntent().getSerializableExtra(StartingActivity.BEACON);
        makeReceiver();

    }

    public void signinClicked(View view) {
        final String clientId = mobileEditText.getText().toString();

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                try {
                    final List<User> results = table.where().execute().get();
                    final User user = verifyClientId(results, clientId);
                    if (user != null) {
                        final int queueNo = user.queueNo;
                        Log.i(TAG, "signin successful");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Sign In successful", Toast.LENGTH_SHORT).show();
                                Intent i = new Intent(MainActivity.this, ShowQueueNoActivity.class);
                                i.putExtra("queue_no", queueNo);
                                i.putExtra("queue_size", getQueueBehind(results, queueNo));
                                i.putExtra("user", user);
                                i.putExtra(StartingActivity.BEACON, beacon);
                                startActivity(i);
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Sign in failed", Toast.LENGTH_SHORT).show();
                            }
                        });

                    }

                } catch (final Exception e) {
                    e.printStackTrace();
                }

                return null;
            }
        };

        task.execute();
    }

    //Verifies client id and returns the queue no.
    User verifyClientId(List<User> users, String clientId) {
        for (User user : users) {
            if (user.ClientId.equals(clientId)) {
                return user;
            }
        }
        return null;
    }

    public int getQueueBehind(List<User> users, int queueNo) {

        int count = 0;
        for (User user : users) {
            if (user.queueNo < queueNo) {
                return users.size() - count;
            }
            count++;
        }

        return 0;
    }

    public void signupClicked(View view) {

        Intent i = new Intent(this, SignupActivity.class);
        i.putExtra(StartingActivity.BEACON, beacon);
        startActivity(i);
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
                    Toast.makeText(MainActivity.this, "Beacon Lost, please stay into proximity.", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(MainActivity.this, StartingActivity.class);
                    startActivity(i);
                }

            }
        };
    }

}
