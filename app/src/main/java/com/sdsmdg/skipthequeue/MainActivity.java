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
import android.widget.TextView;
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
    boolean allowGenerate;
    boolean allowReport;
    TextView generate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Default value is false
        allowGenerate = getIntent().getBooleanExtra("allowGenerate", false);
        allowReport = getIntent().getBooleanExtra("allowReport",false);

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
        beacon = (IEddystoneDevice) getIntent().getSerializableExtra(BeaconScannerActivity.BEACON);
        makeReceiver();

        if(!allowGenerate)
        {
            //view gone means the view no longer needs UI space, whereas view invisible means it is just not visible
            generate = (TextView) findViewById(R.id.signup_button);
            generate.setVisibility(View.GONE);

        }

    }

    public void viewStatusClicked(View view) {

        //The following code converts Character array to String
        Character[] code = codeInput.getCode();
        char[] codeChar = new char[code.length];

        for(int i = 0; i < code.length; i++) {

            if(code[i]== null || Character.isLetter(code[i].charValue()) )
            {
                Toast.makeText(MainActivity.this, "Please enter a valid token.", Toast.LENGTH_SHORT).show();
                return;
            }

        }

        for(int i = 0; i < code.length; i++) {
            codeChar[i] = code[i].charValue();
        }


        String codeString = new String(codeChar);

        final String clientId = new String(codeString);
        //here we've obtained the client id entered by the user

        rotateLoading.start();

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                try {
                    //user list from the backend
                    final List<User> results = table.where().execute().get();
                    final User user = getUser(results, clientId);
                    if (user != null) {
                        final int queueNo = user.queueNo;
                        Log.i(TAG, "signin successful");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Intent i = new Intent(MainActivity.this, ViewStatusActivity.class);
                                //If the user is found forward the data to showqueueno activity

                                i.putExtra("queue_no", queueNo);
                                i.putExtra("queue_size", getQueueAhead(results, queueNo));
                                i.putExtra("user", user);
                                i.putExtra("allowReport",allowReport);
                                //This sends the detail of the next user
                                i.putExtra("nextOTPuser", getnextuser(results, user));
                                //sends the beacon to which the app is connected, so that it checks after connection lost in case of multiple beacons
                                i.putExtra(BeaconScannerActivity.BEACON, beacon);
                                startActivity(i);
                                rotateLoading.stop();
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Token does not Exist.", Toast.LENGTH_SHORT).show();
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

        //get next user if available, else return null
        if(users.indexOf(user) + 1 < users.size()) {
            return users.get(users.indexOf(user) + 1);
        }
        return null;

    }

    //Returns the user if exists else null.
    User getUser(List<User> users, String clientId) {

        for (User user : users) {
            if (user.ClientId.equals(clientId)) {
                return user;
            }
        }
        return null;
    }

    //Returns the no. of people standing ahead in the queue
    public int getQueueAhead(List<User> users, int queueNo) {

        int count = 0;
        for (User user : users) {
            if (user.queueNo == queueNo) {
                Log.i(TAG, "getQueueAhead: " + user.queueNo + " " + queueNo);
                return count;
            }
            count++;
        }

        return 0;
    }

    public void signupClicked(View view) {

        Intent i = new Intent(this, SignupActivity.class);
        i.putExtra(BeaconScannerActivity.BEACON, beacon);
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
                if(!beaconsArray.contains(beacon))
                {
                    Toast.makeText(MainActivity.this, "Beacon Lost, please stay into proximity.", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(MainActivity.this, BeaconScannerActivity.class);
                    startActivity(i);
                }

            }
        };
    }

}
