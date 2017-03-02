package com.sdsmdg.skipthequeue.otp;

/**
 * Created by shree on 3/2/2017.
 */




/**
 * Created by shree on 3/1/2017.
 */



public class list {
    private String atm, location,queue,time,status_of_atm;

    public list() {
    }

    public list(String atm, String location, String queue , String time , String Status_of_atm) {
        this.atm = atm;
        this.location = location;
        this.queue= queue;
        this.time = time;
        this.status_of_atm = Status_of_atm;
    }

    public String getatm() {
        return atm;
    }

    public void setatm(String name) {
        this.atm = name;
    }

    public String getlocation() {
        return location;
    }

    public void setlocation(String location) {
        this.location = location;
    }
    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
    public String getStatus_of_atm() {
        return status_of_atm;
    }

    public void setStatus_of_atm(String status_of_atm) {
        this.status_of_atm = status_of_atm;
    }


}
