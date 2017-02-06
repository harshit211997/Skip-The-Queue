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
import android.widget.Toast;

import com.github.glomadrian.codeinputlib.CodeInput;
import com.kontakt.sdk.android.common.profile.IEddystoneDevice;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.sdsmdg.skipthequeue.BeaconFinder.BeaconFinderService;
import com.sdsmdg.skipthequeue.models.User;
import com.victor.loading.rotate.RotateLoading;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {


    private final static String TAG = MainActivity.class.getSimpleName();
    BroadcastReceiver broadcastReceiver;
    MobileServiceClient mClient;
    MobileServiceTable<User> table;
    ArrayList<IEddystoneDevice> beaconsArray;
    CodeInput codeInput;
    private IEddystoneDevice beacon;
    RotateLoading rotateLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rotateLoading = (RotateLoading) findViewById(R.id.rotateloading);

        codeInput = (CodeInput) findViewById(R.id.client_id_input);

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

        //The following code converts Character array to String
        Character[] code = codeInput.getCode();
        char[] codeChar = new char[code.length];
        for(int i = 0; i < code.length; i++) {
            codeChar[i] = code[i].charValue();
        }
        String codeString = new String(codeChar);

        final String clientId = new String(codeString);
        rotateLoading.start();
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
                                i.putExtra("queue_size", getQueueAhead(results, queueNo));
                                i.putExtra("user", user);
                                i.putExtra("nextOTPuser", getnextuser(results, user));
                                i.putExtra(StartingActivity.BEACON, beacon);
                                startActivity(i);
                                rotateLoading.stop();
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Sign in failed", Toast.LENGTH_SHORT).show();
                                rotateLoading.stop();
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

    private User getnextuser(List<User> users, User user) {

        return users.get(users.indexOf(user)+1);

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

    public int getQueueAhead(List<User> users, int queueNo) {

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
