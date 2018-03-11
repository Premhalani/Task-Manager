package com.example.prem.taskmanager.feature;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.cloudrail.si.interfaces.CloudStorage;
import com.example.prem.taskmanager.feature.adapters.CustomRecyclerViewAdapter;
import com.example.prem.taskmanager.feature.dataobjects.TaskObject;
import com.example.prem.taskmanager.feature.room.AppDatabase;
import com.example.prem.taskmanager.feature.util.Communicator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import br.com.goncalves.pugnotification.notification.Load;
import br.com.goncalves.pugnotification.notification.PugNotification;

/**
 * Created by Prem on 07-Mar-18.
 */

/**
 * Pending task fragment to show all the pending task list and upload them using multi threading 
 * Currently 2 threads are run simultaneously to upload file to cloud.
 * But swipe right can force upload files by creating new threads
 */
public class PendingTaskFragment extends Fragment {
    private RecyclerView mRecyclerView;
    private CustomRecyclerViewAdapter mRecyclerAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private List<TaskObject> pendingTasks = new ArrayList<>();
    private AppDatabase db;
    private TextView tv;
    private static int INITIAL_THREADS = 2;
    private static int MAX_THREADS = 3;
    private static final int KEEP_ALIVE_TIME = 1000;
    private CloudStorage dropBox;
    private Communicator communicator;
    // Sets the Time Unit to Milliseconds
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.MILLISECONDS;
    private Runnable runnable;
    private ThreadPoolExecutor mThreadPoolExecutor;
    private HashMap<String,Boolean> threadSnoozeMap = new HashMap<>();
    private HashMap<String,Boolean> threadCancelMap = new HashMap<>();
    private int notificationIdentifier = 172; //Some random identifier
    private ArrayList<String> notifications = new ArrayList<>();
    public PendingTaskFragment(){

    }
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_pending_task,container,false);
        db = AppDatabase.getAppDatabase(getActivity());
        tv = view.findViewById(R.id.temp);
        mRecyclerView = view.findViewById(R.id.pending_task_list);
        init();
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(0,ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                        TaskObject task = pendingTasks.get(viewHolder.getAdapterPosition());
                        if(direction == ItemTouchHelper.RIGHT) {
                            task.setStatus("Running");
                            Toast.makeText(getContext(),"Task Executing",Toast.LENGTH_SHORT).show();
                            pendingTasks.remove(viewHolder.getAdapterPosition());
                            mRecyclerView.removeView(viewHolder.itemView);
                            mRecyclerAdapter.notifyDataSetChanged();
                            reExecuteTask(task);
                        }else{
                            threadSnoozeMap.put(task.getTid(),true);
                            Toast.makeText(getContext(),"Task Snoozed For 1 Minute",Toast.LENGTH_SHORT).show();
                            mRecyclerAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
                        }
                    }

                    @Override
                    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {

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
        getAllPendingTask();
        return view;
    }

    /**
     * Initialize thread pool and dropBox
     */
    public void init(){
         mThreadPoolExecutor = new ThreadPoolExecutor(
                INITIAL_THREADS,   // Initial pool size
                MAX_THREADS,   // Max pool size
                KEEP_ALIVE_TIME,       // Time idle thread waits before terminating
                KEEP_ALIVE_TIME_UNIT,  // Sets the Time Unit for KEEP_ALIVE_TIME
                new LinkedBlockingDeque<Runnable>());  // Work Queue
        dropBox = ((TaskScreen)getActivity()).getdropBox();
        communicator = (Communicator)getActivity();
    }

    /**
     * Gets all the pending tasks and adds them to thread pool
     */
    public void startTaskUpload(){
        for(int i=0;i<pendingTasks.size();i++){
            if(pendingTasks.get(i).getStatus().equals("Pending")) {
                runnable = new CustomRunnable(pendingTasks.get(i));
                mThreadPoolExecutor.execute(runnable);
            }
        }
    }

    /**
     * helper function to generate some data
     * ONLY CALL THIS FOR TESTING
     */
    public void populateList(){
        for(int i=0;i<6;i++){
            TaskObject taskObject = new TaskObject();
            taskObject.setName("Task"+i);
            taskObject.setDescription("Description"+i);
            taskObject.setFile_path("/mnt/sdcard/DCIM/Camera/20180309_033319.jpg");
            taskObject.setStatus("Pending");
            addNewTask(taskObject);
        }
    }

    /**
     * Custom Runnable to start a thread with custom features
     */
    public class CustomRunnable implements Runnable{
        TaskObject taskObject;
        TextView textView;
        ProgressInputStream progressInputStream;
        InputStream is;
        ProgressInputStream.ProgressListener progressListener;
        Load pugNotification;
        NotificationManagerCompat notificationManager;
        NotificationCompat.Builder mBuilder;
        int ident=1;

        /**
         * Public Constructor takse in taskobject to create a thread.
         * It also takes care of notification mechanism and also
         * displays the progress of the upload and updates various UI elements
         * @param taskObject2
         */
        public CustomRunnable(final TaskObject taskObject2){
            this.taskObject =taskObject2;
            ident = (int)(Long.parseLong(taskObject.getTid())%Integer.MAX_VALUE);       //Unique Identifier
            System.out.println("IDENT : "+ident+" FOR "+taskObject.getName()+" ID="+taskObject.getTid());
            progressListener = new ProgressInputStream.ProgressListener() {
                /**
                 * This is used to update progress to UI and Notification
                 */
                int prev_perc =0;
                @Override
                public void onProgressChanged(final long bytes,final long total) {
                    final int perc = (int)((bytes*100)/total);
                    if(perc != prev_perc) {
                        mBuilder.setProgress(100,perc,false);
                        notificationManager.notify(ident,mBuilder.build());
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                /**
                                 * Update row UI with percentage
                                 */
                                int index = pendingTasks.indexOf(taskObject);
                                if(index != -1) {
                                    pendingTasks.get(index).setStatus("Uploading: " + perc);
                                    mRecyclerAdapter.notifyDataSetChanged();
                                }
                            }
                        });
                        prev_perc = perc;
                    }
                }
            };
            File f = new File(taskObject.getFile_path());
            notificationManager = NotificationManagerCompat.from(getActivity());
        }
        @Override
        public void run() {
            threadSnoozeMap.put(taskObject.getTid(),false);
            Intent createIntent = new Intent(getActivity(), CreateTask.class);
            createIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            createIntent.putExtra("class","notify");
            PendingIntent pendingIntent = PendingIntent.getActivity(getActivity(),0,createIntent,0);
            mBuilder = new NotificationCompat.Builder(getActivity())
                    .setSmallIcon(R.drawable.ic_icon)
                    .setContentTitle(taskObject.getName())
                    .setContentText("Executing")
                    .addAction(R.drawable.ic_icon,"Create Task",pendingIntent);
            mBuilder.setProgress(100,0,false);

            try {
                /**
                 * This block will create notification and start file upload
                 */
                notificationManager.notify(ident,mBuilder.build());
                Thread.sleep(2000);
                File f = new File(taskObject.getFile_path());
                String name = f.getName();
                is = new FileInputStream(f);
                progressInputStream = new ProgressInputStream(is,progressListener,f.length());
                long size = f.length();
                dropBox.upload("/" + name, progressInputStream, size, true);
                if(threadSnoozeMap.get(taskObject.getTid())){
                    try {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                /**
                                 * Update UI to display snooze
                                 * the thread sleeps for 1 minute
                                 */
                                pendingTasks.get(pendingTasks.indexOf(taskObject)).setStatus("Snoozed");
                                mRecyclerAdapter.notifyDataSetChanged();
                            }
                        });
                        Thread.currentThread().sleep(60000);

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }catch (FileNotFoundException e){
                e.printStackTrace();
            } catch (InterruptedException e) {
                System.out.println("INTERRUPTED:"+taskObject.getName());
                Thread.currentThread().interrupt();
            }
            // Update the UI with progress
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int index = pendingTasks.indexOf(taskObject);
                    if(index != -1)
                        taskCompleted(index);
                    PugNotification.with(getActivity()).cancel(ident);
                }
            });
        }
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Re executes the completed task recieved from completed list
     * Also same function is used when Right swiped on a Pending List
     * @param taskObject
     */
    public void reExecuteTask(TaskObject taskObject){
        taskObject.setStatus("Pending");
        pendingTasks.add(0,taskObject);
        mRecyclerAdapter.notifyDataSetChanged();
        runnable = new CustomRunnable(taskObject);
        Thread thread = new Thread(runnable);
        thread.start();
    }

    /**
     * Once the task is completed update the COmpleted Task fragment and remove the item from pending list
     * Also update the database with the new status
     * Add the task to completed notification
     * @param index
     */
    public void taskCompleted(int index){
        TaskObject task = pendingTasks.get(index);
        task.setStatus("Completed");
        updateTaskToDb(task);
        pendingTasks.remove(index);
        communicator.sendData(task,"fragmentCompleted");
        mRecyclerAdapter.notifyDataSetChanged();
        notifications.add(task.getName());
        String[] noti = new String[notifications.size()];
        notifications.toArray(noti);
        PugNotification.with(getActivity())
                .load()
                .identifier(notificationIdentifier)
                .title("Completed")
                .message("Swipe Down to see the Completed Tasks")
                .smallIcon(R.drawable.ic_icon)
                .largeIcon(R.drawable.ic_icon)
                .flags(Notification.DEFAULT_ALL)
                .inboxStyle(noti,"Completed","")
                .simple()
                .build();
    }

    /**
     * Add new task to Pending list and also to the database
     * @param task
     */
    public void addNewTask(TaskObject task){
        pendingTasks.add(task);
        addTaskToDb(task);
        mRecyclerAdapter.notifyDataSetChanged();
    }

    /** Called when the task is updated and edited
     * Also the changes are updated in the db
     * @param task
     * @param position
     */
    public void updateTask(TaskObject task, int position){
        TaskObject taskObject = pendingTasks.get(position);
        taskObject.setDescription(task.getDescription());
        taskObject.setName(task.getName());
        pendingTasks.set(position,taskObject);
        updateTaskToDb(taskObject);
        mRecyclerAdapter.notifyDataSetChanged();
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
     * Adds task to DB
     * @param task
     */
    public void addTaskToDb(final TaskObject task){
        new AsyncTask<Void,Void,Void>(){
            TaskObject taskObject = task;
            @Override
            protected Void doInBackground(Void... voids) {
                db.taskDao().insertALL(taskObject);
                return null;
            }
        }.execute();
    }

    /**
     * Gets all the pending task from the db and populate the list
     */
    public void getAllPendingTask(){
        new AsyncTask<Void,Void,List<TaskObject>>(){

            @Override
            protected List<TaskObject> doInBackground(Void... voids) {
                return  db.taskDao().getAllPendingTasks();
            }

            @Override
            protected void onPostExecute(List<TaskObject> taskObjects) {
                if(taskObjects != null && taskObjects.size() > 0)
                    pendingTasks = taskObjects;
                mRecyclerAdapter = new CustomRecyclerViewAdapter(getActivity(),pendingTasks);
                mLayoutManager = new LinearLayoutManager(getActivity());
                mRecyclerView.setLayoutManager(mLayoutManager);
                mRecyclerView.setAdapter(mRecyclerAdapter);
                // USE this to Test
                // populateList();
                startTaskUpload();
            }
        }.execute();
    }
}
