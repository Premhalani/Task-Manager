package com.example.prem.taskmanager.feature.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.prem.taskmanager.feature.CreateTask;
import com.example.prem.taskmanager.feature.R;
import com.example.prem.taskmanager.feature.dataobjects.TaskObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Prem on 07-Mar-18.
 *
 */

/**
 * Custom adapter Class for Recycler View to populate
 * the views
 */
public class CustomRecyclerViewAdapter extends RecyclerView.Adapter<CustomRecyclerViewAdapter.ViewHolder> {
    private List<TaskObject> mDataSet = new ArrayList<>();
    private Context mContext;
    public CustomRecyclerViewAdapter(Context context, List<TaskObject> myDataSet){
        mDataSet = myDataSet;
        mContext = context;
    }


    public class ViewHolder extends RecyclerView.ViewHolder{
        public TextView tv_name,tv_status;
        public RelativeLayout relativeLayout;
        public ImageButton btn_edit;
        public ViewHolder(View itemView) {
            super(itemView);
            btn_edit = itemView.findViewById(R.id.btn_edit);
            relativeLayout = itemView.findViewById(R.id.color_relative);
            tv_name = itemView.findViewById(R.id.tv_name);
            tv_status = itemView.findViewById(R.id.tv_status);
        }
    }

    @Override
    public CustomRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_task,parent,false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        final TaskObject task = mDataSet.get(position);
        TextView tv_name = holder.tv_name;
        tv_name.setText(task.getName());
        TextView tv_status = holder.tv_status;
        tv_status.setText(task.getStatus());
        if(task.getStatus().equals("Runnin") || task.getStatus().contains("Uploading")){
            holder.relativeLayout.setBackgroundColor(Color.parseColor("#4CAF50"));
        }else if(task.getStatus().equals("Completed")){
            holder.relativeLayout.setBackgroundColor(Color.parseColor("#2196F3"));
        }else{
            holder.relativeLayout.setBackgroundColor(Color.parseColor("#F44336"));
        }
        holder.btn_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, CreateTask.class);
                intent.putExtra("task",task);
                intent.putExtra("class","adapter");
                intent.putExtra("position",position);
                ((Activity)mContext).startActivityForResult(intent,2);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDataSet.size();
    }


}
