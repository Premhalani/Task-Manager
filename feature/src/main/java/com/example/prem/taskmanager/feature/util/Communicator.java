package com.example.prem.taskmanager.feature.util;

import com.example.prem.taskmanager.feature.dataobjects.TaskObject;

/**
 * Created by Prem on 08-Mar-18.
 */

/**
 *  Interface to send data between Pending and Completed fragments
 */
public interface Communicator {
    public void sendData(TaskObject taskObject, String toFragment);
}
