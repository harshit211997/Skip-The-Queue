package com.sdsmdg.skipthequeue.BeaconFinder;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.TwoLineListItem;

import com.kontakt.sdk.android.common.profile.IEddystoneDevice;

import java.util.ArrayList;



public class BeaconAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<IEddystoneDevice> beacons;


    public BeaconAdapter(Context context, ArrayList<IEddystoneDevice> beacons)
    {
        this.context = context;
        this.beacons = beacons;
    }


    @Override
    public int getCount() {
        return beacons.size();
    }

    @Override
    public Object getItem(int i) {
        return beacons.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        TwoLineListItem twoLineListItem;

        if(view == null)
        {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            twoLineListItem = (TwoLineListItem) layoutInflater.inflate(android.R.layout.simple_list_item_2,null);
        }

        else
        {
            twoLineListItem = (TwoLineListItem) view;
        }

        TextView tv1 = twoLineListItem.getText1();
        TextView tv2 = twoLineListItem.getText2();

        tv1.setText("Demo Bank ATM Service.");
        //Temporary using just Instance Id
        tv2.setText("UID : " + beacons.get(i).getInstanceId());

        return twoLineListItem;
    }
}
