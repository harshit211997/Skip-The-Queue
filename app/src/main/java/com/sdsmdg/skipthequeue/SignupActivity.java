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
import android.widget.TextView;
import android.widget.Toast;

import com.kontakt.sdk.android.common.profile.IEddystoneDevice;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.TableOperationCallback;
import com.sdsmdg.skipthequeue.BeaconFinder.BeaconFinderService;
import com.sdsmdg.skipthequeue.models.Response;
import com.sdsmdg.skipthequeue.models.User;
import com.sdsmdg.skipthequeue.otp.MSGApi;
import com.victor.loading.rotate.RotateLoading;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SignupActivity extends AppCompatActivity {

    private final static String TAG = SignupActivity.class.getSimpleName();

    ArrayList<IEddystoneDevice> beaconsArray;
    private IEddystoneDevice beacon;
    BroadcastReceiver broadcastReceiver;

    private MobileServiceClient mClient;
    private MobileServiceTable<User> table;
    EditText mobileEditText;
    private int maxqueueNo = 0;
    private TextView infoTextView;
    private TextView time ;
    private RotateLoading rotateLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        //Removes shadow from under the action bar
        getSupportActionBar().setElevation(0);

        maxqueueNo = 0;

        rotateLoading = (RotateLoading)findViewById(R.id.rotateloading);
        infoTextView = (TextView)findViewById(R.id.infoTextView);
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

    private void makeReceiver() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                beaconsArray = intent.getParcelableArrayListExtra(BeaconFinderService.beacons_array);
                Toast.makeText(getApplicationContext() , String.valueOf(beaconsArray.size()),Toast.LENGTH_SHORT).show();
                if(!beaconsArray.contains(beacon))
                {
                    Toast.makeText(SignupActivity.this, "Beacon Lost, please stay into proximity.", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(SignupActivity.this, StartingActivity.class);
                    startActivity(i);
                }

            }
        };
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

    public void signupClicked(View view) {
        int clientId = generateClientId();
        generateQueueNo(String.valueOf(clientId));
        rotateLoading.start();
    }

    private void sendClientId(final User user) {
        OkHttpClient.Builder client = new OkHttpClient.Builder();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://control.msg91.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client.build())
                .build();

        MSGApi api = retrofit.create(MSGApi.class);

        Call<Response> call = api.sendOTP(
                "137205Asp4V4I7km85878def9",
                user.mobile,
                generateMessage(user),
                "SKIPTQ",
                4,
                91,
                "json"
        );

        call.enqueue(new Callback<Response>() {
            @Override
            public void onResponse(Call<Response> call, retrofit2.Response<Response> response) {
                if (response.body().getType().equals("success")) {
                    Toast.makeText(getApplicationContext(), "Client id sent", Toast.LENGTH_SHORT).show();
                    insertEntry(user);

                } else {
                    Log.i(TAG, response.body().getType());
                    Log.i(TAG, response.body().getMessage());
                    Toast.makeText(getApplicationContext(), "CLient id sent", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Response> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Client id failed to send", Toast.LENGTH_SHORT).show();
                rotateLoading.stop();
            }
        });
    }

    private String generateMessage(User user) {

        String str = "Your Token Number is "+ user.ClientId + ".\n" +
                "Your Queue Number is XX."+ user.queueNo +".\n" +
                "\n" +
                "You are required to enter the above token number in order to utilise it.\n" +
                "Do not use the token unless, the queue is finished "+
                "Thanks for using Skip the Queue service, have a nice day.";
        return str;

    }

    private void insertEntry(final User user) {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                table.insert(user, new TableOperationCallback<User>() {
                    public void onCompleted(User entity, Exception exception, ServiceFilterResponse response) {
                        if (exception == null) {
                            // Insert succeeded
                            Log.i(TAG, "insert succeded");
                            rotateLoading.stop();
                            infoTextView.setVisibility(View.VISIBLE);
                        } else {
                            // Insert failed
                            exception.printStackTrace();
                            Log.i(TAG, "insert failed");
                            rotateLoading.stop();
                            insertEntry(user);
                            Toast.makeText(SignupActivity.this, "Server error! Retry", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                return null;
            }
        };
        task.execute();
    }

    private int generateClientId() {
        int id = (int) (1000 + Math.random() * 9000);
        return id;
    }

    //Generates the queue no. then send client id, and finally use that queue no. to enter data in database
    private void generateQueueNo(final String clientId) {

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                try {
                    final List<User> results = table.where().execute().get();
                    Log.i(TAG, "doInBackground: " + results.get(0).ClientId + "");
                    for (User user : results) {
                        if (user.queueNo > maxqueueNo) {
                            maxqueueNo = user.queueNo;
                        }
                    }

                } catch (final Exception e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                final User user = new User();
                //91 added to user's mobile no.
                user.mobile = "91"+ mobileEditText.getText().toString();
                user.ClientId = String.valueOf(clientId);
                user.queueNo = maxqueueNo + 1;

                sendClientId(user);

            }
        };

        task.execute();
    }
}
