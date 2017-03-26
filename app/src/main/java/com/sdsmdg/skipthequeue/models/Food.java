package com.sdsmdg.skipthequeue.models;

public class Food {

    private String item;
    private int imgResId;
    private String date;
    private String cost;

    public Food() { }

    public Food(String cost, String date, int imgResId, String item) {
        this.cost = cost;
        this.date = date;
        this.imgResId = imgResId;
        this.item = item;
    }

    public String getCost() {
        return cost;
    }

    public String getDate() {
        return date;
    }

    public int getImgResId() {
        return imgResId;
    }

    public String getItem() {
        return item;
    }
}
