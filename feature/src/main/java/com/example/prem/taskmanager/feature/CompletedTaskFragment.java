package com.example.prem.taskmanager.feature;

import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.prem.taskmanager.feature.adapters.CustomRecyclerViewAdapter;
import com.example.prem.taskmanager.feature.dataobjects.TaskObject;
import com.example.prem.taskmanager.feature.room.AppDatabase;
import com.example.prem.taskmanager.feature.util.Communicator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Prem on 07-Mar-18.
 */

/**
 *  Completed Fragment is used to display all the completed task in a Recycler View
 *  Once the pending tasks are completed they are populated here
 */
public class CompletedTaskFragment extends android.support.v4.app.Fragment {
    private RecyclerView mRecyclerView;
    private CustomRecyclerViewAdapter mRecyclerAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private List<TaskObject> completedTask = new ArrayList<>();
    private AppDatabase db;
    private Communicator communicator;
    public CompletedTaskFragment(){
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_pending_task,container,false);
        mRecyclerView = view.findViewById(R.id.pending_task_list);
        communicator = (Communicator)getActivity();
        db = AppDatabase.getAppDatabase(getActivity());

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(0,ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    // ItemTouchHelper is used to detect swipe on the row
                    @Override
                    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                        TaskObject task = completedTask.get(viewHolder.getAdapterPosition());
                        if(direction == ItemTouchHelper.RIGHT) {
                            //Send task back to pending to reexecute it
                            completedTask.remove(task);
                            mRecyclerAdapter.notifyDataSetChanged();
                            communicator.sendData(task,"fragmentPending");
                        }else{
                            //Don't do anything
                            Toast.makeText(getActivity(),"Left",Toast.LENGTH_SHORT).show();
                            mRecyclerAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
                        }
                    }

                    @Override
                    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                        // Adds effects to swipe
                        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                            float width = (float) viewHolder.itemView.getWidth();
                            float alpha = 1.0f - Math.abs(dX) / width;
                            viewHolder.itemView.setAlpha(alpha);
                            viewHolder.itemView.setTranslationX(dX);
                        } else {
                            super.onChildDraw(c, recyclerView, viewHolder, dX, dY,
                                    actionState, isCurrentlyActive);
                        }
                    }
                }
        );
        itemTouchHelper.attachToRecyclerView(mRecyclerView);
        getAllCompletedTask();
        return view;
    }
    /** Called when the task is updated and edited
     * Also the changes are updated in the db
     * @param task
     * @param position
     */
    public void updateTask(TaskObject task, int position){
        TaskObject taskObject = completedTask.get(position);
        taskObject.setDescription(task.getDescription());
        taskObject.setName(task.getName());
        completedTask.set(position,taskObject);
        updateTaskToDb(taskObject);
        mRecyclerAdapter.notifyDataSetChanged();
    }

    /**
     * Add task to the completed list and update the adapter
     * @param task
     */
    public void addNewTask(TaskObject task){
        completedTask.add(0,task);
        if(mRecyclerAdapter!=null)
            mRecyclerAdapter.notifyDataSetChanged();
        if(mRecyclerView != null)
            mRecyclerView.setAdapter(mRecyclerAdapter);
    }

    /**
     * Updates the task in the db. Uses Room DAO
     * @param task
     * @return
     */
    public int updateTaskToDb(final TaskObject task){
        final int[] i = new int[1];
        new AsyncTask<Void,Void,Void>(){
            @Override
            protected Void doInBackground(Void... voids) {
                i[0] = db.taskDao().update(task);
                return null;
            }
        }.execute();
        return i[0];
    }

    /**
     * Get all the task from the database with status "Completed" and populate the views using CustomAdapter
     */
    public void getAllCompletedTask(){
        new AsyncTask<Void,Void,List<TaskObject>>(){

            @Override
            protected List<TaskObject> doInBackground(Void... voids) {
                return  db.taskDao().getAllCompletedTasks();
            }

            @Override
            protected void onPostExecute(List<TaskObject> taskObjects) {
                if(taskObjects != null && taskObjects.size() > 0) {
                    completedTask = taskObjects;
                    Collections.reverse(completedTask);
                }
                mRecyclerAdapter = new CustomRecyclerViewAdapter(getActivity(),completedTask);
                mLayoutManager = new LinearLayoutManager(getActivity());
                mRecyclerView.setLayoutManager(mLayoutManager);
                mRecyclerView.setAdapter(mRecyclerAdapter);
            }
        }.execute();
    }
}
