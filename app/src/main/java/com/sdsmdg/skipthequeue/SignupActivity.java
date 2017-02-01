package com.sdsmdg.skipthequeue;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.TableOperationCallback;
import com.sdsmdg.skipthequeue.models.User;

import java.net.MalformedURLException;

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
        final User user = new User();
        user.mobile = mobileEditText.getText().toString();

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

    }
}
