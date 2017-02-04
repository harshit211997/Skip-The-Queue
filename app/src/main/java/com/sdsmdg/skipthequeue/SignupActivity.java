package com.sdsmdg.skipthequeue;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.TableOperationCallback;
import com.sdsmdg.skipthequeue.models.Response;
import com.sdsmdg.skipthequeue.models.User;
import com.sdsmdg.skipthequeue.otp.MSGApi;

import java.net.MalformedURLException;
import java.util.List;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SignupActivity extends AppCompatActivity {

    private final static String TAG = SignupActivity.class.getSimpleName();

    private MobileServiceClient mClient;
    private MobileServiceTable<User> table;
    EditText mobileEditText;
    private int maxqueueNo = 0;
    private TextView infoTextView;
    private TextView time ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        maxqueueNo = 0;

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
    }

    public void signupClicked(View view) {
        int clientId = generateClientId();
        generateQueueNo(String.valueOf(clientId));
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
                "Your client id is " + user.ClientId,
                "CITADL",
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
            }
        });
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
                            infoTextView.setVisibility(View.VISIBLE);
                        } else {
                            // Insert failed
                            exception.printStackTrace();
                            Log.i(TAG, "insert failed");
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
                user.mobile = mobileEditText.getText().toString();
                user.ClientId = String.valueOf(clientId);
                user.queueNo = maxqueueNo + 1;

                sendClientId(user);

            }
        };

        task.execute();
    }
}
