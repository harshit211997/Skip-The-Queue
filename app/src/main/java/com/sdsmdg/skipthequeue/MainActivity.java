package com.sdsmdg.skipthequeue;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.sdsmdg.skipthequeue.models.User;

import java.net.MalformedURLException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    MobileServiceClient mClient;
    MobileServiceTable<User> table;
    EditText mobileEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mobileEditText = (EditText) findViewById(R.id.mobile_editText);

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

    public void signinClicked(View view) {
        final String mobileNo = mobileEditText.getText().toString();

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {

                try {
                    final List<User> results = table.where().field("mobile").eq(mobileNo).execute().get();
                    if(results.get(0).mobile.equals(mobileNo)) {
                        Log.i(TAG, "signin successful");
                    }

                } catch (final Exception e){
                    e.printStackTrace();
                }

                return null;
            }
        };

        task.execute();
    }

    public void signupClicked(View view) {
        Intent i = new Intent(this, SignupActivity.class);
        startActivity(i);
    }
}
