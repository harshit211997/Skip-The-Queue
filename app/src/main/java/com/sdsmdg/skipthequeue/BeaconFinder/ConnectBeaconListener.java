package com.sdsmdg.skipthequeue.BeaconFinder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.sdsmdg.skipthequeue.MainActivity;

public class ConnectBeaconListener extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Toast.makeText(context,"Beacon Connected.", Toast.LENGTH_SHORT).show();
        Intent i = new Intent(context, MainActivity.class );
        i.putExtra("allowGenerate", true);
        i.putExtra("allowReport", true);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivity(i);

    }
}
