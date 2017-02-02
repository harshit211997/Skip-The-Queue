package com.sdsmdg.skipthequeue;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.TableOperationCallback;
import com.sdsmdg.skipthequeue.models.Response;
import com.sdsmdg.skipthequeue.models.User;
import com.sdsmdg.skipthequeue.otp.MSGApi;

import java.net.MalformedURLException;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mobileEditText = (EditText)findViewById(R.id.mobile_editText);

        try {
            mClient = new MobileServiceClient(
                    "https://skipthequeue.azurewebsites.net",
                    this
            );
        } catch(MalformedURLException e) {
            e.printStackTrace();
        }

        table = mClient.getTable(User.class);
    }

    public void signupClicked(View view) {
        int clientId = generateClientId();

        final User user = new User();
        user.mobile = mobileEditText.getText().toString();
        user.ClientId = String.valueOf(clientId);

        new Thread(new Runnable() {
            @Override
            public void run() {
                table.insert(user, new TableOperationCallback<User>() {
                    public void onCompleted(User entity, Exception exception, ServiceFilterResponse response) {
                        if (exception == null) {
                            // Insert succeeded
                            Log.i(TAG, "insert succeded");
                        } else {
                            // Insert failed
                            exception.printStackTrace();
                            Log.i(TAG, "insert failed");
                        }
                    }
                });
            }
        }).run();

        sendClientId(user);

    }

    private void sendClientId(User user) {
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
                if(response.body().getType().equals("success")) {
                    Toast.makeText(getApplicationContext(), "Client id sent", Toast.LENGTH_SHORT).show();
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

    private int generateClientId() {
        int id = (int)(1000 + Math.random() * 9000);
        return id;
    }
}
