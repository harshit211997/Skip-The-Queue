package com.sdsmdg.skipthequeue.Adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.sdsmdg.skipthequeue.R;
import com.sdsmdg.skipthequeue.models.Food;

import java.util.List;

public class FoodListAdapter extends RecyclerView.Adapter<FoodListAdapter.MyViewHolder>{

    List<Food> foodList;

    public class MyViewHolder extends RecyclerView.ViewHolder {

        public TextView foodNameTextView, dateTextView, amountTextView;
        public ImageView foodImageView;

        public MyViewHolder(View view) {
            super(view);
            foodNameTextView = (TextView)view.findViewById(R.id.food_name_textView);
            dateTextView = (TextView)view.findViewById(R.id.date_textView);
            amountTextView = (TextView)view.findViewById(R.id.amt_textView);
            foodImageView = (ImageView)view.findViewById(R.id.food_imageView);
        }
    }

    public FoodListAdapter(List<Food> foodList) {
        this.foodList = foodList;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.dateTextView.setText(foodList.get(position).getDate());
        holder.foodNameTextView.setText(foodList.get(position).getItem());
        holder.amountTextView.setText("Rs." + foodList.get(position).getCost());
        holder.foodImageView.setImageResource(foodList.get(position).getImgResId());
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_food_item, parent, false);

        return new MyViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return foodList.size();
    }
}
