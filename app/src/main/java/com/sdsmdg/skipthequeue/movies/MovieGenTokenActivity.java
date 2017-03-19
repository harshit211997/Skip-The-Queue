package com.sdsmdg.skipthequeue.movies;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
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
import com.microsoft.windowsazure.mobileservices.table.TableQueryCallback;
import com.sdsmdg.skipthequeue.BeaconFinder.BeaconFinderService;
import com.sdsmdg.skipthequeue.BeaconScannerActivity;
import com.sdsmdg.skipthequeue.Keys;
import com.sdsmdg.skipthequeue.R;
import com.sdsmdg.skipthequeue.SignupActivity;
import com.sdsmdg.skipthequeue.StartingActivity;
import com.sdsmdg.skipthequeue.models.Order;
import com.sdsmdg.skipthequeue.models.Response;
import com.sdsmdg.skipthequeue.otp.MSGApi;
import com.victor.loading.rotate.RotateLoading;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import mehdi.sakout.fancybuttons.FancyButton;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

public class MovieGenTokenActivity extends AppCompatActivity {

    private final static String TAG = SignupActivity.class.getSimpleName();

    ArrayList<IEddystoneDevice> beaconsArray;
    private IEddystoneDevice beacon;
    BroadcastReceiver broadcastReceiver;

    private MobileServiceClient mClient;
    private MobileServiceTable<Order> table;
    EditText mobileEditText;
    private int maxqueueNo = 0;
    private TextView infoTextView;
    private TextView time ;
    private RotateLoading rotateLoading;
    private int activeOrders;
    private FancyButton getToken;

    SharedPreferences prefs;

    private String tableName = "OrderTable";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_gen_token);

        prefs = getDefaultSharedPreferences(this);

        //Removes shadow from under the action bar
        getSupportActionBar().setElevation(0);
        maxqueueNo = 0;
        rotateLoading = (RotateLoading)findViewById(R.id.rotateloading);
        infoTextView = (TextView)findViewById(R.id.infoTextView);
        mobileEditText = (EditText) findViewById(R.id.mobile_editText);
        beacon = (IEddystoneDevice) getIntent().getSerializableExtra(BeaconScannerActivity.BEACON);
        makeClient();
        //makeReceiver(); // Receiver not required any more here once the URL is known.
        getToken = (FancyButton)findViewById(R.id.get_token_button);
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

        table = mClient.getTable(tableName, Order.class);
    }

    private void makeReceiver() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                beaconsArray = intent.getParcelableArrayListExtra(BeaconFinderService.beacons_array);
                if(!beaconsArray.contains(beacon))
                {
                    Toast.makeText(MovieGenTokenActivity.this, "Beacon Lost, please stay into proximity.", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(MovieGenTokenActivity.this, StartingActivity.class);
                    startActivity(i);
                }

            }
        };
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

    public void payClicked(View view) {

        //Generates a random no as clientId
        getToken.setEnabled(false);
        int orderId = (int) (1000 + Math.random() * 9000);
        generateOrder(String.valueOf(orderId));
        rotateLoading.start();
    }

    private void sendOrderId(final Order order) {
        OkHttpClient.Builder client = new OkHttpClient.Builder();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://control.msg91.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client.build())
                .build();

        MSGApi api = retrofit.create(MSGApi.class);

        Call<Response> call = api.sendOTP(
                Keys.MSG_KEY,
                order.mobile,
                generateMessage(order),
                "SKIPTQ",
                4,
                91,
                "json"
        );

        call.enqueue(new Callback<Response>() {
            @Override
            public void onResponse(Call<Response> call, retrofit2.Response<Response> response) {
                if (response.body().getType().equals("success")) {
                    insertEntry(order);
                    //save the atm table name in user preferences(For use in view status button on starting activity)
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("table_name", "User");
                    editor.apply();
                } else {
                    Toast.makeText(getApplicationContext(), "Problem with message API", Toast.LENGTH_LONG).show();
                    Log.i(TAG, response.body().getType());
                    Log.i(TAG, response.body().getMessage());
                }
            }

            @Override
            public void onFailure(Call<Response> call, Throwable t) {
                rotateLoading.stop();
            }
        });
    }

    private String generateMessage(Order order) {

        String str = "Your Order Id is "+ order.orderId + ".\n" +
                "Your Order number is "+ order.queueNo +" in the queue. "+ "\n"+
                "\n" +
                "Please Monitor your order from the above Order Id mentioned. \n\n"+
                "Thanks for using Skip the Queue service, have a nice day.";
        return str;

    }

    private String getApproxtime() {

        //Waiting time
        int mins = activeOrders*20;


        if(mins > 50 && mins < 70)
            return "one hour.";

        else if(mins > 110 && mins < 130)
            return "two hours.";
        else if(mins > 170 && mins < 190)
            return "two hours.";
        else if(mins > 230 && mins < 250)
            return "two hours.";
        else if(mins > 290 && mins < 310)
            return "two hours.";


        else
        {
            int hours = mins/60;
            mins = mins%60;

            if (hours > 0)
            {
                return hours + " hours and " + mins + "minutes.";
            }

            else
                return  mins + " minutes.";
        }
    }

    private void insertEntry(final Order order) {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params)
            {

                table.insert(order, new TableOperationCallback<Order>() {
                    public void onCompleted(Order entity, Exception exception, ServiceFilterResponse response) {
                        if (exception == null) {
                            // Insert succeeded
                            Toast.makeText(MovieGenTokenActivity.this, "Order Placed", Toast.LENGTH_SHORT).show();
                            rotateLoading.stop();
                            infoTextView.setVisibility(View.VISIBLE);
                            openViewStatusActivity(order);
                        } else {
                            // Insert failed
                            exception.printStackTrace();
                            rotateLoading.stop();
                            insertEntry(order);
                        }
                    }
                });

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {

            }
        };
        task.execute();
    }

    //Generates the queue no. then send client id, and finally use that queue no. to enter data in database
    private void generateOrder(final String orderId) {

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    //Receive results from backend, only first 50 entries are received.
                    table.where().execute(new TableQueryCallback<Order>() {
                        @Override
                        public void onCompleted(List<Order> result, int count, Exception exception, ServiceFilterResponse response) {
                            if(exception == null)
                            {
                                //This will give the approximate Time for preparing the order, by knowing presently active orders.
                                activeOrders = result.size();
                                for (Order order : result) {
                                    if (order.queueNo > maxqueueNo) {
                                        //This is used to generate the next queue no.
                                        maxqueueNo = order.queueNo;
                                    }
                                }
                                //This runs the supposed Post Execute method only when the call succeeds.
                                onUIthread(orderId);
                            }
                            else {
                                generateOrder(orderId);
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

    private void onUIthread(final String orderId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Order order = new Order();
                //91 added to user's mobile no.
                order.mobile = "91"+ mobileEditText.getText().toString();
                order.orderId = orderId;
                order.queueNo = maxqueueNo + 1;

                if(order.mobile.length() != 12 )
                {
                    Toast.makeText(getApplicationContext(),"Please Enter a valid Mobile No.",Toast.LENGTH_SHORT).show();
                    rotateLoading.stop();
                    getToken.setEnabled(true);
                }
                else {
                    //TODO : Iterate through db and allow token generation only once for a particular mobile no.
                    sendOrderId(order);
                }

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        getToken.setEnabled(true);
    }

    //This function is called just after the signup is successful
    private void openViewStatusActivity(Order order) {
        Intent i = new Intent(this, ViewStatusActivity.class);
        i.putExtra("queue_no", order.queueNo);
        i.putExtra("queue_size", activeOrders);
        i.putExtra("showOrderCompleteDialog", true);
        i.putExtra("order",order);
        //nextOrder is null
        startActivity(i);

    }

}
