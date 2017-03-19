package com.sdsmdg.skipthequeue.movies.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.TableOperationCallback;
import com.microsoft.windowsazure.mobileservices.table.TableQueryCallback;
import com.sdsmdg.skipthequeue.Keys;
import com.sdsmdg.skipthequeue.R;
import com.sdsmdg.skipthequeue.SignupActivity;
import com.sdsmdg.skipthequeue.models.Order;
import com.sdsmdg.skipthequeue.models.Response;
import com.sdsmdg.skipthequeue.movies.Activities.ViewStatusActivity;
import com.sdsmdg.skipthequeue.otp.MSGApi;
import com.victor.loading.rotate.RotateLoading;

import java.net.MalformedURLException;
import java.util.List;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

public class GenOrderDialogFragment extends DialogFragment {

    private final static String TAG = SignupActivity.class.getSimpleName();

    private MobileServiceClient mClient;
    private MobileServiceTable<Order> table;
    EditText mobileEditText;
    private int maxqueueNo = 0;
    private TextView time ;
    private RotateLoading rotateLoading;
    private int activeOrders;
    private Button getOrderIdButton;

    SharedPreferences prefs;

    private String tableName = "OrderTable";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_gen_order, null);

        prefs = getDefaultSharedPreferences(getActivity());

        maxqueueNo = 0;
        rotateLoading = (RotateLoading)view.findViewById(R.id.rotate_loading);
        mobileEditText = (EditText) view.findViewById(R.id.mobile_editText);

        makeClient();
        //makeReceiver(); // Receiver not required any more here once the URL is known.
        getOrderIdButton = (Button)view.findViewById(R.id.get_order_button);
        getOrderIdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                payClicked();
            }
        });

        return view;
    }

    private void makeClient() {

        try {
            mClient = new MobileServiceClient(
                    "https://skipthequeue.azurewebsites.net",
                    getActivity()
            );
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        table = mClient.getTable(tableName, Order.class);
    }

    public void payClicked() {

        //Generates a random no as clientId
        getOrderIdButton.setEnabled(false);
        int orderId = (int) (1000 + Math.random() * 9000);
        generateOrder(String.valueOf(orderId));
        rotateLoading.start();
        getOrderIdButton.setVisibility(View.INVISIBLE);
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
                    Toast.makeText(getActivity(), "Problem with message API", Toast.LENGTH_LONG).show();
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
                            Toast.makeText(getActivity(), "Order Placed", Toast.LENGTH_SHORT).show();
                            rotateLoading.stop();
                            getOrderIdButton.setVisibility(View.VISIBLE);
                            openViewStatusActivity(order);
                        } else {
                            // Insert failed
                            exception.printStackTrace();
                            rotateLoading.stop();
                            getOrderIdButton.setVisibility(View.VISIBLE);
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
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Order order = new Order();
                //91 added to user's mobile no.
                order.mobile = "91"+ mobileEditText.getText().toString();
                order.orderId = orderId;
                order.queueNo = maxqueueNo + 1;

                if(order.mobile.length() != 12 )
                {
                    Toast.makeText(getActivity(),"Please Enter a valid Mobile No.",Toast.LENGTH_SHORT).show();
                    rotateLoading.stop();
                    getOrderIdButton.setVisibility(View.VISIBLE);
                }
                else {
                    //TODO : Iterate through db and allow token generation only once for a particular mobile no.
                    sendOrderId(order);
                }

            }
        });
    }

    //This function is called just after the signup is successful
    private void openViewStatusActivity(Order order) {
        Intent i = new Intent(getActivity(), ViewStatusActivity.class);
        i.putExtra("queue_no", order.queueNo);
        i.putExtra("queue_no", order.queueNo);
        i.putExtra("queue_size", activeOrders);
        i.putExtra("showOrderCompleteDialog", true);
        i.putExtra("order",order);
        //nextOrder is null
        startActivity(i);

    }

}
