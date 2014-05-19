package com.alibaba.china.talos.model;

import java.io.Serializable;
import java.util.Date;

/**
 * @description ֻͬ����֤ͨ���Ļ�Ա
 * @author karry
 * @date 2014-1-21 ����9:02:20
 */
public class PersonalModel implements Serializable {

    private static final long serialVersionUID = 7212044172442218142L;

    private Long              userId;
    /**
     * ��֤��Դ
     */
    private String            source;
    /**
     * ����
     */
    private String            memberName;
    /**
     * ���֤����
     */
    private String            idCardNumber;
    /**
     * ���֤��Ч��
     */
    private Date              idCardExpireDate;
    
    private boolean           isSuccess;

    /**
     * @fields passDate : ��֤ͨ��ʱ��
     */
    private Date              passDate;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public String getIdCardNumber() {
        return idCardNumber;
    }

    public void setIdCardNumber(String idCardNumber) {
        this.idCardNumber = idCardNumber;
    }

    public Date getIdCardExpireDate() {
        return idCardExpireDate;
    }

    public void setIdCardExpireDate(Date idCardExpireDate) {
        this.idCardExpireDate = idCardExpireDate;
    }

    public Date getPassDate() {
        return passDate;
    }

    public void setPassDate(Date passDate) {
        this.passDate = passDate;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }

}
