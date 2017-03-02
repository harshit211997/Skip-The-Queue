package com.sdsmdg.skipthequeue.Adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sdsmdg.skipthequeue.R;
import com.sdsmdg.skipthequeue.models.Machine;

import java.util.List;

public class adapter_class extends RecyclerView.Adapter<adapter_class.MyViewHolder> {

    private List<Machine> mList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        public void onClick(int position);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView atm , location,queue , time,status;

        public MyViewHolder(View view) {
            super(view);
            atm = (TextView) view.findViewById(R.id.atm);
            location = (TextView) view.findViewById(R.id.location);
            queue = (TextView) view.findViewById(R.id.queue);
            time = (TextView) view.findViewById(R.id.time);
            status = (TextView) view.findViewById(R.id.status);

        }
    }

    public adapter_class(List<Machine> mList, OnItemClickListener listener) {
        this.mList = mList;
        this.listener = listener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        Machine machine = mList.get(position);
        holder.atm.setText(machine.tableName);
        //holder.location.setText(machine.getlocation());
        holder.queue.setText(machine.queueLength + "");
        holder.time.setText(machine.queueLength * 2 + "");

        String s = "out of cash";
        if(machine.statusWorking) {
            s = "working";
        }
        holder.status.setText(s);

        //set onclick listener to the row
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onClick(position);
            }
        });

    }

    @Override
    public int getItemCount() {
        return mList.size();
    }
}