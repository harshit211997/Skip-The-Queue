package com.sdsmdg.skipthequeue.models;

import java.io.Serializable;

/**
 * Created by yash on 26/2/17.
 */

public class Machine implements Serializable{

    public String id;
    public String location;
    public String beaconUID;
    public boolean statusWorking;
    public int queueLength;


}
