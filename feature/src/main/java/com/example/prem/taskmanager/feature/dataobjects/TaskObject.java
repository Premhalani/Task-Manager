package com.example.prem.taskmanager.feature.dataobjects;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.ArrayList;

/**
 * Created by Prem on 07-Mar-18.
 */

/**
 * Custom Task Object
 * This is also used as a Room object to store locally
 */
@Entity
public class TaskObject implements Parcelable{
    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "tid")
    private String tid;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "keyword_str")
    private String keyword_str;

    @ColumnInfo(name = "file_path")
    private String file_path;

    @ColumnInfo(name = "status")
    private String status;

    @ColumnInfo(name = "priority")
    private int priority;
    @Ignore
    ArrayList<String> keywords;
    @Ignore
    public TaskObject(String name, String description, String file_path, ArrayList<String> keywords){

        this.name = name;
        Long tsLong = System.currentTimeMillis()/1000;
        this.tid =tsLong.toString();
        this.description = description;
        this.file_path = file_path;
        this.keywords = keywords;
        this.priority = 0;
    }
    public TaskObject(){
        Long tsLong = System.nanoTime();
        this.tid =tsLong.toString();
    }
    public TaskObject(String id,String name, String description, String file_path){
        this.name = name;Long tsLong = System.currentTimeMillis();
        this.tid =tsLong.toString();
        this.description = description;
        this.file_path = file_path;
        this.priority = 0;
    }
    @Ignore
    protected TaskObject(Parcel in) {
        tid=in.readString();
        name = in.readString();
        description = in.readString();
        file_path = in.readString();
        keyword_str = in.readString();
        keywords = in.createStringArrayList();
        status = in.readString();
        priority = in.readInt();
    }
    @Ignore
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(tid);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeString(file_path);
        dest.writeString(keyword_str);
        dest.writeStringList(keywords);
        dest.writeString(status);
        dest.writeInt(priority);
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public String getTid() {
        return tid;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getDescription() {
        return description;
    }

    public void setKeyword_str(String keyword_str) {
        this.keyword_str = keyword_str;
    }

    public String getKeyword_str() {
        return keyword_str;
    }
    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<TaskObject> CREATOR = new Creator<TaskObject>() {
        @Override
        public TaskObject createFromParcel(Parcel in) {
            return new TaskObject(in);
        }

        @Override
        public TaskObject[] newArray(int size) {
            return new TaskObject[size];
        }
    };

    public void giveHighPriority(){
        this.priority = 1;
    }

    public void giveLowPriority(){
        this.priority = 0;
    }

    public void setStatus(String status){
        this.status = status;
    }

    public int getPriority(){
        return this.priority;
    }

    public String getName(){
        return this.name;
    }

    public String getStatus() {
        return status;
    }

    public ArrayList<String> getKeywords() {
        return keywords;
    }

    public String getFile_path() {
        return file_path;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setFile_path(String file_path) {
        this.file_path = file_path;
    }

    public void setKeywords(ArrayList<String> keywords) {
        this.keywords = keywords;
    }

    public void setName(String name) {
        this.name = name;
    }
}
