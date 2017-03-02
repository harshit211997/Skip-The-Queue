package com.sdsmdg.skipthequeue.otp;

import android.graphics.Movie;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sdsmdg.skipthequeue.R;

import java.util.List;




public class adapter_class extends RecyclerView.Adapter<adapter_class.MyViewHolder> {

    private List<list> mList;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView atm , location,queue , time,status;

        public MyViewHolder(View view) {
            super(view);
            atm = (TextView) view.findViewById(R.id.title);
            location = (TextView) view.findViewById(R.id.location);
            queue = (TextView) view.findViewById(R.id.queue);
            time = (TextView) view.findViewById(R.id.time);
            status = (TextView) view.findViewById(R.id.status);

        }
    }


    public adapter_class(List<list> mList) {
        this.mList = mList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_atm_details, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        list movie = mList.get(position);
        holder.atm.setText(movie.getatm());
        holder.location.setText(movie.getlocation());
        holder.queue.setText(movie.getQueue());
        holder.time.setText(movie.getTime());
        holder.status.setText(movie.getStatus_of_atm());

    }

    @Override
    public int getItemCount() {
        return mList.size();
    }
}