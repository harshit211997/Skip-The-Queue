package com.sdsmdg.skipthequeue.models;

import java.io.Serializable;

public class Machine implements Serializable{

    public String id;
    public String longi;
    public String lat;
    public String beaconUID;
    public boolean statusWorking;
    public int queueLength;
    public String tableName;

    public Machine(String beaconUID, String lat, String longi, int queueLength, boolean statusWorking, String tableName) {
        this.beaconUID = beaconUID;
        this.id = id;
        this.lat = lat;
        this.longi = longi;
        this.queueLength = queueLength;
        this.statusWorking = statusWorking;
        this.tableName = tableName;
    }
}
