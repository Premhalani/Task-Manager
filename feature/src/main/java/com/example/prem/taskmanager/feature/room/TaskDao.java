package com.example.prem.taskmanager.feature.room;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.example.prem.taskmanager.feature.dataobjects.TaskObject;

import java.util.List;

/**
 * Created by Prem on 07-Mar-18.
 */

/**
 * Task DAO interface is used to make query calls to Room database
 */

@Dao
public interface TaskDao {
    @Query("SELECT * FROM taskobject")
    List<TaskObject> getAll();

    @Query("SELECT * FROM taskobject WHERE tid IN (:taskIds)")
    List<TaskObject> loadAllByIds(int[] taskIds);

    @Query("SELECT * FROM taskobject WHERE name LIKE :name "+" LIMIT 1")
    TaskObject findByName(String name);

    @Query("SELECT * FROM taskobject WHERE tid LIKE :tid "+" LIMIT 1")
    TaskObject findById(int tid);


    @Query("SELECT * FROM taskobject WHERE status like 'Pending' OR status like 'Running'")
    List<TaskObject> getAllPendingTasks();


    @Query("SELECT * FROM taskobject WHERE status like 'Completed'")
    List<TaskObject> getAllCompletedTasks();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertALL(TaskObject...tasks);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    int update(TaskObject task);

    @Delete
    void delete(TaskObject task);

}
