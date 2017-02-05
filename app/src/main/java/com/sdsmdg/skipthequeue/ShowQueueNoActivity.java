package com.sdsmdg.skipthequeue;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.sdsmdg.skipthequeue.models.User;

import java.net.MalformedURLException;

public class ShowQueueNoActivity extends AppCompatActivity {

    TextView tokenTextView;
    TextView timeTextView;
    MobileServiceClient mClient;
    MobileServiceTable<User>table;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_queue_no);

        int queueNo = 0;
        int queueSize = 0;

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            queueNo = extras.getInt("queue_no");
            queueSize = extras.getInt("queue_size");
        }

        tokenTextView = (TextView) findViewById(R.id.token_text_view);
        tokenTextView.setText(queueNo + "");

        int expectedTime = 2 * (queueSize);

        timeTextView =(TextView) findViewById(R.id.user_time);
        timeTextView.setText( "Expected time till your chance : " + expectedTime + " min");

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

        table = mClient.getTable(User.class);
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {

                try {
                    Intent i = getIntent();
                    User user = (User) i.getSerializableExtra("user");
                    table.delete(user);
                }

                catch (final Exception e){
                    e.printStackTrace();
                }

                return null;
            }
        };

        task.execute();
    }

}
