package com.nickilous;


import java.io.Serializable;

/**
 * Created by Nick on 10/21/13.
 */
public class OrderMessage extends Object implements Serializable {
    private int action;
    private OrderInfo orderInfo = new OrderInfo();

    public OrderMessage(){

    }

    public OrderMessage(String message){
        String[] splitMessage = message.split("%");
        this.action = Integer.valueOf(splitMessage[0]);
        orderInfo.setmOrderNumber(splitMessage[1]);
        orderInfo.setmOrderTime(splitMessage[2]);
    }

    public OrderMessage(int action, OrderInfo orderInfo){
        this.action = action;
        this.orderInfo = orderInfo;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(String.valueOf(action)).append("%").append(orderInfo);
        return sb.toString();
    }

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public OrderInfo getOrderInfo() {
        return orderInfo;
    }

    public void setOrderInfo(OrderInfo orderInfo) {
        this.orderInfo = orderInfo;
    }
}