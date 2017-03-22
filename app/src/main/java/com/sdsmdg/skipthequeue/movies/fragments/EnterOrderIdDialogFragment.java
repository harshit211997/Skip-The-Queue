package com.sdsmdg.skipthequeue.movies.fragments;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.github.glomadrian.codeinputlib.CodeInput;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.sdsmdg.skipthequeue.R;
import com.sdsmdg.skipthequeue.models.Order;
import com.sdsmdg.skipthequeue.models.User;
import com.sdsmdg.skipthequeue.movies.Activities.ViewStatusActivity;
import com.victor.loading.rotate.RotateLoading;

import java.net.MalformedURLException;
import java.util.List;

import mehdi.sakout.fancybuttons.FancyButton;

import static android.content.ContentValues.TAG;
import static com.sdsmdg.skipthequeue.Helper.machine;

public class EnterOrderIdDialogFragment extends DialogFragment {

    RotateLoading rotateLoading;
    FancyButton signInButton;
    MobileServiceClient mClient;
    MobileServiceTable<Order> table;
    CodeInput codeInput;

    private String tableName = "OrderTable";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_enter_order_id, null);
        signInButton = (FancyButton)view.findViewById(R.id.get_token_button);
        rotateLoading = (RotateLoading)view.findViewById(R.id.rotate_loading);
        codeInput = (CodeInput) view.findViewById(R.id.order_id_input);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rotateLoading.start();
                signInButton.setVisibility(View.INVISIBLE);
                viewStatusClicked();
            }
        });

        makeClient();

        return view;
    }

    private void makeClient() {
        //This defines the query address to the client
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

    public void viewStatusClicked() {

        //The following code converts Character array to String
        Character[] code = codeInput.getCode();
        char[] codeChar = new char[code.length];

        for (int i = 0; i < code.length; i++) {

            if (code[i] == null || Character.isLetter(code[i].charValue())) {
                Toast.makeText(getActivity(), "Please enter a valid order id.", Toast.LENGTH_SHORT).show();
                return;
            }

        }

        for (int i = 0; i < code.length; i++) {
            codeChar[i] = code[i].charValue();
        }

        String codeString = new String(codeChar);

        final String clientId = new String(codeString);
        //here we've obtained the client id entered by the user

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                try {
                    //order list from the backend
                    final List<Order> results = table.where().execute().get();
                    final Order order = getOrder(results, clientId);
                    if (order != null) {
                        final int queueNo = order.queueNo;
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Intent i = new Intent(getActivity(), ViewStatusActivity.class);
                                //If the user is found forward the data to showqueueno activity
//                                i.putExtra("machine", machine);
                                i.putExtra("queue_no", queueNo);
                                i.putExtra("queue_size", getQueueAhead(results, queueNo));
                                i.putExtra("order", order);
                                //This sends the detail of the next user
                                i.putExtra("nextOrder", getNextOrder(results, order));
                                //sends the beacon to which the app is connected, so that it checks after connection lost in case of multiple beacons
                                startActivity(i);
                                rotateLoading.stop();
                                signInButton.setVisibility(View.VISIBLE);
                            }
                        });
                    } else {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getActivity(), "Order does not Exist.", Toast.LENGTH_SHORT).show();
                                rotateLoading.stop();
                                signInButton.setVisibility(View.VISIBLE);
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
    Order getOrder(List<Order> orders, String orderId) {

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

}
