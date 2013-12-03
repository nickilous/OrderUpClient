package com.nickilous;

import java.io.Serializable;
import java.lang.String;
import java.lang.StringBuilder;
/**
 * Created by Nick on 10/16/13.
 */
public class OrderInfo extends Object implements Serializable {
    private String mOrderNumber;
    private String mOrderTime;

    public String getmOrderNumber() {
        return mOrderNumber;
    }

    public void setmOrderNumber(String mOrderNumber) {
        this.mOrderNumber = mOrderNumber;
    }

    public String getmOrderTime() {
        return mOrderTime;
    }

    public void setmOrderTime(String mOrderTime) {
        this.mOrderTime = mOrderTime;
    }


    public String toString() {
        final StringBuilder sb = new StringBuilder(mOrderNumber);
        sb.append("%").append(mOrderTime);
        return sb.toString();
    }
}