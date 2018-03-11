package com.example.prem.taskmanager.feature;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;

import com.cloudrail.si.CloudRail;
import com.cloudrail.si.exceptions.ParseException;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.Dropbox;
import com.example.prem.taskmanager.feature.adapters.ViewPagerAdapter;
import com.example.prem.taskmanager.feature.dataobjects.TaskObject;
import com.example.prem.taskmanager.feature.room.AppDatabase;
import com.example.prem.taskmanager.feature.util.Communicator;

/**
 * Main Activity that displays two fragments, Pending and Completed fragments
 * This is the first screen displayed
 */
public class TaskScreen extends AppCompatActivity implements Communicator {

    private android.support.v4.app.Fragment fragment;
    private android.support.v4.app.FragmentManager fragmentManager;
    private CustomViewPager viewPager;
    private static PendingTaskFragment pendingTaskFragment;
    private CompletedTaskFragment completedTaskFragment;
    private MenuItem prevMenuItem;
    private BottomNavigationView navigation;
    private FloatingActionButton floatingActionButton;
    private AppDatabase db;
    private CloudStorage dropbox;
    SharedPreferences sharedPreferences;
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            // Bottom Navigation drawer
            int id = item.getItemId();
            final FragmentTransaction transaction = fragmentManager.beginTransaction();
            if (id == R.id.navigation_home) {
                viewPager.setCurrentItem(0);
                return true;
            } else if (id == R.id.navigation_dashboard) {
                viewPager.setCurrentItem(1);
                return true;
            }
            return false;
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_screen);
        setupCloudRail();
        while (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }
        init();
        setupViewPager(viewPager);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }
            @Override
            public void onPageSelected(int position) {
                if (prevMenuItem != null) {
                    prevMenuItem.setChecked(false);
                }
                else {
                    navigation.getMenu().getItem(0).setChecked(false);
                }
                navigation.getMenu().getItem(position).setChecked(true);
                prevMenuItem = navigation.getMenu().getItem(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TaskScreen.this,CreateTask.class);
                intent.putExtra("class","newtask");
                startActivityForResult(intent,1);
            }
        });

    }

    /**
     * Initialize all the views
     */
    public void init(){
        sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        fragmentManager = getSupportFragmentManager();
        navigation = (BottomNavigationView) findViewById(R.id.navigation);
        floatingActionButton = findViewById(R.id.btn_create_task);
        viewPager = findViewById(R.id.viewpager);
        viewPager.setPagingEnabled(false);
        db = AppDatabase.getAppDatabase(this);
    }

    /**
     * Set up Cloud rail
     */
    public void setupCloudRail(){
        CloudRail.setAppKey("5aa1dc832d1ce0242d1e8528");
        dropbox = new Dropbox(this,"1x72g2dv7fv75sh","tomqkde82yx75cg", "https://auth.cloudrail.com/com.example.prem.taskmanager.feature", "someState");
    }

    /**
     * Returns onedrive instance using Cloud Rail Api in an Async task
     * @return
     */
    public CloudStorage getdropBox() {
        String loginCredentials = sharedPreferences.getString("dropboxPersistent", null);
        if(loginCredentials == null) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    dropbox.login();
                    sharedPreferences.edit().putString("dropboxPersistent",dropbox.saveAsString()).apply();
                    return null;
                }
            }.execute();
        }else{
            try {
                dropbox.loadAsString(loginCredentials);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return dropbox;
    }

    /**
     * Gets result from CreateTask page and uses it to add the new task to Pending list
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if(resultCode == Activity.RESULT_OK){
                TaskObject newTask = (TaskObject) data.getParcelableExtra("data");
                pendingTaskFragment.addNewTask(newTask);
                pendingTaskFragment.startTaskUpload();
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }
        if (requestCode == 2) {
            if(resultCode == Activity.RESULT_OK){
                TaskObject newTask = (TaskObject) data.getParcelableExtra("data");
                if(newTask.getStatus().equals("Completed")){
                    completedTaskFragment.updateTask(newTask, data.getIntExtra("position", 0));
                }else {
                    pendingTaskFragment.updateTask(newTask, data.getIntExtra("position", 0));
                }
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }
    }

    /**
     * Setup view pager and all the fragments
     * @param viewPager
     */
    public void setupViewPager(ViewPager viewPager){
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        pendingTaskFragment = new PendingTaskFragment();
        completedTaskFragment = new CompletedTaskFragment();
        viewPagerAdapter.addFragment(pendingTaskFragment);
        viewPagerAdapter.addFragment(completedTaskFragment);
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setCurrentItem(0);
    }

    /**
     * Send data to from the fragments and call different functions.
     * reExecuteTask is called to Re Upload the file from the completed list
     * @param task
     * @param toFragment
     */
    @Override
    public void sendData(TaskObject task, String toFragment) {
        FragmentManager fm = getSupportFragmentManager();
        if(toFragment.equals("fragmentCompleted")){
            completedTaskFragment.addNewTask(task);
        }else{
            pendingTaskFragment.reExecuteTask(task);
        }
    }
}
