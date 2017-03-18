package com.sdsmdg.skipthequeue.movies;

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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.kontakt.sdk.android.common.profile.IEddystoneDevice;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.TableDeleteCallback;
import com.sdsmdg.skipthequeue.BeaconFinder.BeaconFinderService;
import com.sdsmdg.skipthequeue.BeaconScannerActivity;
import com.sdsmdg.skipthequeue.Keys;
import com.sdsmdg.skipthequeue.R;
import com.sdsmdg.skipthequeue.StartingActivity;
import com.sdsmdg.skipthequeue.models.Response;
import com.sdsmdg.skipthequeue.models.User;
import com.sdsmdg.skipthequeue.otp.MSGApi;

import java.net.MalformedURLException;
import java.util.ArrayList;

import mehdi.sakout.fancybuttons.FancyButton;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ViewStatusActivity extends AppCompatActivity {

    TextView tokenTextView;
    TextView timeTextView;
    MobileServiceClient mClient;
    MobileServiceTable<User> userTable;
    ArrayList<IEddystoneDevice> beaconsArray;
    private IEddystoneDevice beacon;
    BroadcastReceiver broadcastReceiver;
    int queueNo = 0;
    int queueSize = 0;
    TextView yourQueueNoTextView;
    TextView expectedTimeTextView;
    TextView deleteTokenTextView;

    FancyButton useTokenButton;

    private String tableName = "User";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_movie_queue_status);
        getExtras();

        yourQueueNoTextView = (TextView)findViewById(R.id.your_queue_no_textview);
        expectedTimeTextView = (TextView)findViewById(R.id.user_time);
        deleteTokenTextView = (TextView)findViewById(R.id.delete_token_textview);

        useTokenButton = (FancyButton) findViewById(R.id.use_token_button);
        //Removes shadow from under the action bar
        getSupportActionBar().setElevation(0);
        int expectedTime = 2 * (queueSize);
        timeTextView = (TextView) findViewById(R.id.user_time);
        timeTextView.setText("Expected time : " + expectedTime + " min");
        tokenTextView = (TextView) findViewById(R.id.token_text_view);
        tokenTextView.setText("#" + queueNo);
        makeClient();
        makeReceiver();

        boolean showOrderCompleteDialog = getIntent().getBooleanExtra("showOrderCompleteDialog", false);
        if(showOrderCompleteDialog) {
            showOrderCompleteDialog();
        }
    }

    private void showOrderCompleteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();

        builder.setView(inflater.inflate(R.layout.dialog_order_complete, null));
        builder.show();

    }

    private void makeClient() {
        try {
            mClient = new MobileServiceClient(
                    "https://skipthequeue.azurewebsites.net",
                    this
            );
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        userTable = mClient.getTable(tableName, User.class);

    }

    private void getExtras() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            queueNo = extras.getInt("queue_no");
            queueSize = extras.getInt("queue_size");

            beacon = (IEddystoneDevice) extras.getSerializable(BeaconScannerActivity.BEACON);

        }
    }


    private void deleteToken() {
        deleteUser();
    }

    private void deleteUser() {

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                try {

                    Intent i = getIntent();
                    User user = (User) i.getSerializableExtra("user");
                    User nextUser = (User) i.getSerializableExtra("nextOTPuser");

                    if (nextUser != null) {
                        sendNextOTP(nextUser);
                    }

                    //This deletes on the database as well
                    userTable.delete(user, new TableDeleteCallback() {
                        @Override
                        public void onCompleted(Exception exception, ServiceFilterResponse response) {

                            //Also update the queue length in manager table.
                            if (exception == null) {
                                Toast.makeText(ViewStatusActivity.this, "Token Deleted.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(ViewStatusActivity.this, "Please Try Again.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });


                } catch (final Exception e) {
                    Toast.makeText(ViewStatusActivity.this, "Please Try Again.", Toast.LENGTH_SHORT).show();
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
                if (!beaconsArray.contains(beacon)) {
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
                Keys.MSG_KEY,
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
        new AlertDialog.Builder(this, R.style.YourAlertDialogTheme)
                .setTitle("Delete Token?")
                .setMessage("Are you sure you want to delete this token?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        deleteToken();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //dismiss
                    }
                })
                .show();
    }

    public void useTokenOnClick(View view) {
        //Go ahead only if the Queue no is 1 and expected time is zero
        if (queueSize == 0) {
            Toast.makeText(ViewStatusActivity.this, "Token Utilized.", Toast.LENGTH_SHORT).show();
            deleteToken();

        } else {
            Toast.makeText(ViewStatusActivity.this, "Your chance has not arrived yet.", Toast.LENGTH_SHORT).show();
        }
    }

    private void redirectMain() {
        Intent i = new Intent(getApplicationContext(), StartingActivity.class);
        i.putExtra("Privileges", 1);
        startActivity(i);
    }
}
