package com.sdsmdg.skipthequeue.movies.Activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.sdsmdg.skipthequeue.Adapters.FoodListAdapter;
import com.sdsmdg.skipthequeue.R;
import com.sdsmdg.skipthequeue.models.Food;

import java.util.ArrayList;
import java.util.List;

public class OrderSummary extends AppCompatActivity {

    RecyclerView foodListRecyclerView;
    FoodListAdapter adapter;
    List<Food> foodList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_summary);

        foodListRecyclerView = (RecyclerView)findViewById(R.id.food_list);
        adapter = new FoodListAdapter(foodList);

        foodListRecyclerView.setAdapter(adapter);
        foodListRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        initializeDemoItems();
    }

    public void initializeDemoItems() {
        Food ham = new Food("25", "2017/05/20", R.drawable.hamburger, "Hamburger");
        Food fries = new Food("30", "2017/05/20", R.drawable.fries, "Fries");
        Food coke = new Food("10", "2017/05/20", R.drawable.coke, "Coke");
        Food iceCream = new Food("15", "2017/05/20", R.drawable.ice_cream, "Ice cream");

        foodList.add(ham);
        foodList.add(fries);
        foodList.add(coke);
        foodList.add(iceCream);

        adapter.notifyDataSetChanged();
    }

}
