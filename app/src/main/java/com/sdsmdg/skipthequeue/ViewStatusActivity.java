package com.sdsmdg.skipthequeue;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.kontakt.sdk.android.common.profile.IEddystoneDevice;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.sdsmdg.skipthequeue.BeaconFinder.BeaconFinderService;
import com.sdsmdg.skipthequeue.models.Response;
import com.sdsmdg.skipthequeue.models.User;
import com.sdsmdg.skipthequeue.otp.MSGApi;

import java.net.MalformedURLException;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ViewStatusActivity extends AppCompatActivity {

    TextView tokenTextView;
    TextView timeTextView;
    MobileServiceClient mClient;
    MobileServiceTable<User>table;
    ArrayList<IEddystoneDevice> beaconsArray;
    private IEddystoneDevice beacon;
    BroadcastReceiver broadcastReceiver;
    int queueNo = 0;
    int queueSize = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_status);

        //Removes shadow from under the action bar
        getSupportActionBar().setElevation(0);



        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            queueNo = extras.getInt("queue_no");
            queueSize = extras.getInt("queue_size");

            beacon = (IEddystoneDevice) extras.getSerializable(StartingActivity.BEACON);

        }

        tokenTextView = (TextView) findViewById(R.id.token_text_view);
        tokenTextView.setText("#" + queueNo);


        int expectedTime = 2 * (queueSize);

        try {
            mClient = new MobileServiceClient(
                    "https://skipthequeue.azurewebsites.net",
                    this
            );
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        table = mClient.getTable(User.class);

        timeTextView =(TextView) findViewById(R.id.user_time);
        timeTextView.setText( "Expected time : " + expectedTime + " min");

        makeReceiver();
    }


    private void deleteToken() {
        deleteUser();
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
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>()
        {
            @Override
            protected Void doInBackground(Void... params) {

                try {

                    Intent i = getIntent();
                    User user = (User) i.getSerializableExtra("user");
                    User nextUser = (User) i.getSerializableExtra("nextOTPuser");

                    if(nextUser != null) {
                        //sendNextOTP(nextUser);
                    }

                    //This deletes on the database as well
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
                if(!beaconsArray.contains(beacon))
                {
                    Toast.makeText(ViewStatusActivity.this, "Beacon Lost, please stay into proximity.", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(ViewStatusActivity.this, StartingActivity.class);
                    startActivity(i);
                }

            }
        };
    }

    //Adding the code to send the otp to 11th guy

    private void sendNextOTP(final User user) {


        OkHttpClient.Builder client = new OkHttpClient.Builder();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://control.msg91.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client.build())
                .build();

        MSGApi api = retrofit.create(MSGApi.class);

        Call<Response> call = api.sendOTP(
                "140306A0DRBmkqhhJw589a2609",
                user.mobile,
                "Your chance has arrived. Now you can go in!",
                "SKIPTQ",
                4,
                91,
                "json"
        );

        call.enqueue(new Callback<Response>() {
            @Override
            public void onResponse(Call<Response> call, retrofit2.Response<Response> response) {
                if (response.body().getType().equals("success")) {

                } else {
                    Log.i("TAG", response.body().getType());
                    Log.i("TAG", response.body().getMessage());
                }
            }

            @Override
            public void onFailure(Call<Response> call, Throwable t) {
//                Toast.makeText(getApplicationContext(), "Notification send failed. ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void deleteTokenOnClick(View view) {

        createAlert();

    }

    private void createAlert() {
        new AlertDialog.Builder(this)
                .setTitle("Deleting Token")
                .setMessage("Are you sure you want to delete this token?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(ViewStatusActivity.this, "Token Deleted", Toast.LENGTH_SHORT ).show();
                        deleteToken();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //dismiss
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    public void useTokenOnClick(View view) {
        //Go ahead only if the Queue no is 1 and expected time is zero
        if(queueNo == 1)
        {
            Toast.makeText(ViewStatusActivity.this, "Token Utilized.", Toast.LENGTH_SHORT ).show();
            deleteToken();

        }
        else
        {
            Toast.makeText(ViewStatusActivity.this, "Your chance has not arrived yet.", Toast.LENGTH_SHORT ).show();
        }
    }
}