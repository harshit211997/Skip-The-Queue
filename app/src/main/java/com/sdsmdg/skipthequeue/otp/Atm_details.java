package com.sdsmdg.skipthequeue.otp;

import android.graphics.Movie;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.sdsmdg.skipthequeue.R;

import java.util.ArrayList;
import java.util.List;

public class Atm_details extends AppCompatActivity {



    private List<list> mList = new ArrayList<>();
    private RecyclerView recyclerView;
    private adapter_class mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_atm_details);


        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        mAdapter = new adapter_class(mList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);

        prepareMovieData();
    }

    private void prepareMovieData() {
        list m1 = new list ("Atm name","LOACTION","QUEUE","TIME","STATUS");
        mList.add(m1);

        mAdapter.notifyDataSetChanged();
    }
}


