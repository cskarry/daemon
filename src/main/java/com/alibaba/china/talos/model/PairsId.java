package com.alibaba.china.talos.model;


public class PairsId {

    long orderId;
    long targetId;

    public PairsId(long orderId, long targetId){
        this.orderId = orderId;
        this.targetId = targetId;
    }

    public long getOrderId() {
        return orderId;
    }

    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }

    public long getTargetId() {
        return targetId;
    }

    public void setTargetId(long targetId) {
        this.targetId = targetId;
    }

}
