package com.sdsmdg.skipthequeue.movies;

import android.app.Dialog;
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
import android.widget.Button;
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
import com.sdsmdg.skipthequeue.models.Order;
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
    MobileServiceTable<Order> orderTable;
    ArrayList<IEddystoneDevice> beaconsArray;
    private IEddystoneDevice beacon;
    BroadcastReceiver broadcastReceiver;
    int queueNo = 0;
    int queueSize = 0;
    TextView yourQueueNoTextView;
    TextView expectedTimeTextView;
    TextView deleteTokenTextView;
    FancyButton useTokenButton;
    private String tableName = "OrderTable";

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
        //makeReceiver();

        boolean showOrderCompleteDialog = getIntent().getBooleanExtra("showOrderCompleteDialog", false);
        if(showOrderCompleteDialog) {
            showOrderCompleteDialog();
        }
    }

    private void showOrderCompleteDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_order_complete, null);

        Button okButton = (Button)view.findViewById(R.id.ok_button);

        builder.setView(view);
        final Dialog dialog = builder.create();

        dialog.show();

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

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

        orderTable = mClient.getTable(tableName, Order.class);

    }

    private void getExtras() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            queueNo = extras.getInt("queue_no");
            queueSize = extras.getInt("queue_size");

            beacon = (IEddystoneDevice) extras.getSerializable(BeaconScannerActivity.BEACON);

        }
    }

    private void deleteOrder() {

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                try {

                    //Just delete the order do not send alert to the next user.
                    Intent i = getIntent();
                    Order order = (Order) i.getSerializableExtra("order");

                    //This deletes on the database as well
                    orderTable.delete(order, new TableDeleteCallback() {
                        @Override
                        public void onCompleted(Exception exception, ServiceFilterResponse response) {

                            //Also update the queue length in manager table.
                            if (exception == null) {
                                Toast.makeText(ViewStatusActivity.this, "Order Deleted.", Toast.LENGTH_SHORT).show();
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

    //TODO: call this method after the order is completed.
    private void completeOrder() {

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                try {

                    Intent i = getIntent();
                    Order order = (Order) i.getSerializableExtra("order");
                    Order nextOrder = (Order) i.getSerializableExtra("nextOrder");

                    //Send alert to the next user.
                    if (nextOrder != null) {
                        sendNextOrderAlert(nextOrder);
                    }

                    //This deletes on the database as well
                    orderTable.delete(order, new TableDeleteCallback() {
                        @Override
                        public void onCompleted(Exception exception, ServiceFilterResponse response) {

                            //Also update the queue length in manager table.
                            if (exception == null) {
                                Toast.makeText(ViewStatusActivity.this, "Order Deleted.", Toast.LENGTH_SHORT).show();
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

//        //Register the receiver
//        LocalBroadcastManager.getInstance(this).registerReceiver((broadcastReceiver),
//                new IntentFilter(BeaconFinderService.intent_filter)
//        );

        super.onStart();
    }

    @Override
    protected void onStop() {

        //Unregister the receiver
//        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
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

    //Adding the code to send the alert to the next person
    private void sendNextOrderAlert(final Order order) {

        OkHttpClient.Builder client = new OkHttpClient.Builder();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://control.msg91.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client.build())
                .build();

        MSGApi api = retrofit.create(MSGApi.class);

        Call<Response> call = api.sendOTP(
                //TODO : Change the order text here
                Keys.MSG_KEY,
                order.mobile,
                "Your Order"+" will be ready in 5-10 mins.",
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
                Toast.makeText(getApplicationContext(), "Next Notification send failed. ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void deleteOrderOnClick(View view) {
        createAlert();
    }

    private void createAlert() {
        new AlertDialog.Builder(this, R.style.YourAlertDialogTheme)
                .setTitle("Delete Order?")
                .setMessage("Are you sure you want to delete your order?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        deleteOrder();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        completeOrder();
                    }
                })
                .show();
    }

}
