package com.example.prem.taskmanager.feature.room;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import com.example.prem.taskmanager.feature.dataobjects.TaskObject;

/**
 * Created by Prem on 07-Mar-18.
 */

/**
 * THe Class creates data base. This is used by Room
 */
@Database(entities = {TaskObject.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract TaskDao taskDao();
    private static AppDatabase INSTANCE;

    public static AppDatabase getAppDatabase(Context context){
        if(INSTANCE == null)
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(),AppDatabase.class,"task-database").build();
        return INSTANCE;
    }

    public void destroyInstance(){
        INSTANCE = null;
    }
}
