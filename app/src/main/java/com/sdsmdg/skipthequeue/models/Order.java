package com.sdsmdg.skipthequeue.models;

import java.io.Serializable;

public class Order implements Serializable{

    //Id is alloted implicitly by backend
    public String id;

    public String mobile;

    //Change in backend as well.
    public String orderId;

    public int queueNo;

}
