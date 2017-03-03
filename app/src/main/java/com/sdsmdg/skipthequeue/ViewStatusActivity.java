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
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.TableOperationCallback;
import com.sdsmdg.skipthequeue.BeaconFinder.BeaconFinderService;
import com.sdsmdg.skipthequeue.models.Machine;
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

import static com.sdsmdg.skipthequeue.Helper.machine;

public class ViewStatusActivity extends AppCompatActivity {

    TextView tokenTextView;
    TextView timeTextView;
    TextView reportTextView;
    MobileServiceClient mClient;
    MobileServiceTable<User> userTable;
    MobileServiceTable<Machine> machinesTable;
    ArrayList<IEddystoneDevice> beaconsArray;
    private IEddystoneDevice beacon;
    BroadcastReceiver broadcastReceiver;
    int queueNo = 0;
    int queueSize = 0;
    boolean allowReport;

    FancyButton useTokenButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_status);
        getExtras();
        useTokenButton = (FancyButton) findViewById(R.id.use_token_button);
        allowReport = getIntent().getBooleanExtra("allowReport", false);
        reportTextView = (TextView) findViewById(R.id.out_of_cash);
        //Removes shadow from under the action bar
        getSupportActionBar().setElevation(0);
        int expectedTime = 2 * (queueSize);
        timeTextView = (TextView) findViewById(R.id.user_time);
        timeTextView.setText("Expected time : " + expectedTime + " min");
        tokenTextView = (TextView) findViewById(R.id.token_text_view);
        tokenTextView.setText("#" + queueNo);
        makeClient();
        makeReceiver();
        if (!allowReport) {
            //Remove if only status is to be viewed.
            reportTextView.setVisibility(View.GONE);
            useTokenButton.setVisibility(View.GONE);
        }
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

        userTable = mClient.getTable(Helper.machine.tableName,User.class);

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
        try {
            mClient = new MobileServiceClient(
                    "https://skipthequeue.azurewebsites.net",
                    this
            );
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        userTable = mClient.getTable(User.class);
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                try {

                    Intent i = getIntent();
                    User user = (User) i.getSerializableExtra("user");
                    User nextUser = (User) i.getSerializableExtra("nextOTPuser");

                    if (nextUser != null) {
                        //sendNextOTP(nextUser);
                    }

                    //This deletes on the database as well
                    userTable.delete(user);
                    Toast.makeText(ViewStatusActivity.this, "Token Deleted", Toast.LENGTH_SHORT).show();
                } catch (final Exception e) {
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
        new AlertDialog.Builder(this, R.style.YourAlertDialogTheme)
                .setTitle("Deleting Token")
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
                .setIcon(android.R.drawable.ic_dialog_alert)
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

    public void reportOFC(View view) {
        new AlertDialog.Builder(this, R.style.YourAlertDialogTheme)
                .setTitle("Report out of cash?")
                .setMessage("Are you sure you want report this ATM as out of cash?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //Argument passed will be the Manager Table.
                        setMachineStatusToOFC();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .show();
    }

    public void setMachineStatusToOFC() {
        final Machine currentMachine = machine;
        machinesTable = mClient.getTable("Manager", Machine.class);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //Stores the reference to the current machine, which it receives from the previous activity
                    currentMachine.statusWorking = false;
                    machinesTable.update(currentMachine, new TableOperationCallback<Machine>() {
                        @Override
                        public void onCompleted(Machine entity, Exception exception, ServiceFilterResponse response) {
                            if(exception == null) {
                                Toast.makeText(ViewStatusActivity.this, "Successfully updated ATM status!", Toast.LENGTH_SHORT).show();
                                askUserPreferenceForToken();
                            } else {
                                Toast.makeText(ViewStatusActivity.this, "Please Try Again!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(ViewStatusActivity.this, "Please Try Again", Toast.LENGTH_SHORT).show();
                }
            }
        }).start();
    }

    /**
     * This function creates a dialog which asks the user, if he wants to:-
     * 1. delete his token
     * 2. shift to another queue
     * 3. preserve his token for next transaction on the same ATM, when there is a refill
     */
    public void askUserPreferenceForToken() {
        new AlertDialog.Builder(this, R.style.YourAlertDialogTheme)
                .setTitle("What to do with your token?")
                .setMessage("Do you want to delete your token(delete), get token for another queue(shift) nearby, or preserve your token for the same ATM(preserve)")
                .setPositiveButton("delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //delete his entry from the azure database
                        deleteToken();
                    }
                })
                .setNeutralButton("shift", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //shift the token => go to first activity showing list of ATMs
                    }
                })
                .setNegativeButton("preserve", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //preserve the token => exit the dialog => do nothing
                    }
                }).show();
    }
}
