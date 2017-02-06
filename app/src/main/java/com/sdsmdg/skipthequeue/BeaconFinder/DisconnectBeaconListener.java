package com.sdsmdg.skipthequeue.BeaconFinder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.sdsmdg.skipthequeue.StartingActivity;

/**
 * Created by yash on 6/2/17.
 */

public class DisconnectBeaconListener extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context,"Beacon Disconnected.", Toast.LENGTH_SHORT).show();
        Intent i  = new Intent(context, StartingActivity.class);
        context.startActivity(i);
    }
}
