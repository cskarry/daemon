package com.alibaba.china.talos.model;


public class BindRelDO {

    String memberId;
    String alipayUserId;

    long   userId;

    String pnaName;
    String pnaIdcard;

    String alipayName;
    String alipayIdcard;

    String uicName;
    String uicIdcard;

    public BindRelDO(String memberId, String alipayUserId){
        this.memberId = memberId;
        this.alipayUserId = alipayUserId;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getAlipayUserId() {
        return alipayUserId;
    }

    public void setAlipayUserId(String alipayUserId) {
        this.alipayUserId = alipayUserId;
    }

    public String getPnaName() {
        return pnaName;
    }

    public void setPnaName(String pnaName) {
        this.pnaName = pnaName;
    }

    public String getPnaIdcard() {
        return pnaIdcard;
    }

    public void setPnaIdcard(String pnaIdcard) {
        this.pnaIdcard = pnaIdcard;
    }

    public String getAlipayName() {
        return alipayName;
    }

    public void setAlipayName(String alipayName) {
        this.alipayName = alipayName;
    }

    public String getAlipayIdcard() {
        return alipayIdcard;
    }

    public void setAlipayIdcard(String alipayIdcard) {
        this.alipayIdcard = alipayIdcard;
    }

    public String getUicName() {
        return uicName;
    }

    public void setUicName(String uicName) {
        this.uicName = uicName;
    }

    public String getUicIdcard() {
        return uicIdcard;
    }

    public void setUicIdcard(String uicIdcard) {
        this.uicIdcard = uicIdcard;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }


}