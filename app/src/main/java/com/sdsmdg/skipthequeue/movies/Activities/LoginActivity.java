package com.sdsmdg.skipthequeue.movies.Activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.github.glomadrian.codeinputlib.CodeInput;
import com.kontakt.sdk.android.common.profile.IEddystoneDevice;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.sdsmdg.skipthequeue.BeaconFinder.BeaconFinderService;
import com.sdsmdg.skipthequeue.BeaconScannerActivity;
import com.sdsmdg.skipthequeue.MainActivity;
import com.sdsmdg.skipthequeue.R;
import com.sdsmdg.skipthequeue.StartingActivity;
import com.sdsmdg.skipthequeue.models.Order;
import com.victor.loading.rotate.RotateLoading;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();
    BroadcastReceiver broadcastReceiver;
    MobileServiceClient mClient;
    MobileServiceTable<Order> table;
    ArrayList<IEddystoneDevice> beaconsArray;
    CodeInput codeInput;
    private IEddystoneDevice beacon;
    RotateLoading rotateLoading;

    private String tableName = "OrderTable";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        rotateLoading = (RotateLoading) findViewById(R.id.rotateloading);
        codeInput = (CodeInput) findViewById(R.id.order_id_input);
//        beacon ain't required anymore
//        beacon = (IEddystoneDevice) getIntent().getSerializableExtra(BeaconScannerActivity.BEACON);
        makeClient();
//        makeReceiver();

    }

    private void makeClient() {
        //This defines the query address to the client
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

    public void viewOrderStatusClicked(View view) {

        //The following code converts Character array to String
        Character[] code = codeInput.getCode();
        char[] codeChar = new char[code.length];

        for (int i = 0; i < code.length; i++) {

            if (code[i] == null || Character.isLetter(code[i].charValue())) {
                Toast.makeText(LoginActivity.this, "Please enter a valid token.", Toast.LENGTH_SHORT).show();
                return;
            }

        }

        for (int i = 0; i < code.length; i++) {
            codeChar[i] = code[i].charValue();
        }


        String codeString = new String(codeChar);

        final String orderId = new String(codeString);
        //here we've obtained the client id entered by the user

        rotateLoading.start();

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                try {
                    //user list from the backend
                    final List<Order> results = table.where().execute().get();
                    final Order order = getUser(results, orderId);
                    if (order != null) {
                        final int queueNo = order.queueNo;
                        Log.i(TAG, "signin successful");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Intent i = new Intent(LoginActivity.this, ViewStatusActivity.class);
                                //If the oder is found forward the data to ViewStatus activity
//                                i.putExtra("machine", machine);
                                i.putExtra("queue_no", queueNo);
                                i.putExtra("queue_size", getQueueAhead(results, queueNo));
                                i.putExtra("order", order);
                                //This sends the detail of the next user
                                i.putExtra("nextOrder", getNextOrder(results, order));
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
                                Toast.makeText(LoginActivity.this, "Token does not Exist.", Toast.LENGTH_SHORT).show();
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

    private Order getNextOrder(List<Order> orders, Order order) {

        //get next user if available, else return null
        if (orders.indexOf(order) + 1 < orders.size()) {
            return orders.get(orders.indexOf(order) + 1);
        }
        return null;

    }

    //Returns the user if exists else null.
    Order getUser(List<Order> orders, String orderId) {

        for (Order order : orders) {
            if (order.orderId.equals(orderId)) {
                return order;
            }
        }
        return null;
    }

    //Returns the no. of people standing ahead in the queue
    public int getQueueAhead(List<Order> orders, int queueNo) {

        int count = 0;
        for (Order order : orders) {
            if (order.queueNo == queueNo) {
                Log.i(TAG, "getQueueAhead: " + order.queueNo + " " + queueNo);
                return count;
            }
            count++;
        }

        return 0;
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
                    Toast.makeText(LoginActivity.this, "Beacon Lost, please stay into proximity.", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(LoginActivity.this, StartingActivity.class);
                    startActivity(i);
                }
            }
        };
    }

}
