package com.sdsmdg.skipthequeue;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.sdsmdg.skipthequeue.Adapters.adapter_class;
import com.sdsmdg.skipthequeue.models.Machine;

import java.util.ArrayList;
import java.util.List;

public class Atm_details extends AppCompatActivity {

    private List<Machine> mList = new ArrayList<>();
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
        Machine m1 = new Machine("Atm name", "LOACTION", "QUEUE", 34, true, "User");
        mList.add(m1);

        mAdapter.notifyDataSetChanged();
    }
}


