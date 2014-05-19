package com.alibaba.china.talos.model;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;

import org.apache.commons.lang.time.DateUtils;

import com.alibaba.china.shared.talos.tbpersonseller.model.BopsCheckPersonalAuthResult;
import com.alibaba.china.shared.talos.tbpersonseller.model.PersonalSellerAuthData;
import com.alibaba.common.lang.StringUtil;

/**
 * @description 除了身份证背面照，其他数据都存在，同步所有数据给talos的model
 * @author karry
 * @date 2014-1-21 下午9:41:10
 */
public class PersonWholeModel implements Serializable {

    private static final long serialVersionUID = 9175267485775630983L;

    long                      userId;
    String                    cert_status;
    String                    comments;
    String                    cert_pic_url;
    String                    passDate;
    String                    operatorNick;
    String                    source;
    String                    idCardNumber;
    String                    name;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getCert_status() {
        return cert_status;
    }

    public void setCert_status(String cert_status) {
        this.cert_status = cert_status;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getCert_pic_url() {
        return cert_pic_url;
    }

    public void setCert_pic_url(String cert_pic_url) {
        this.cert_pic_url = cert_pic_url;
    }

    public String getPassDate() {
        return passDate;
    }

    public void setPassDate(String passDate) {
        this.passDate = passDate;
    }

    public String getOperatorNick() {
        return operatorNick;
    }

    public void setOperatorNick(String operatorNick) {
        this.operatorNick = operatorNick;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getIdCardNumber() {
        return idCardNumber;
    }

    public void setIdCardNumber(String idCardNumber) {
        this.idCardNumber = idCardNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PersonalSellerAuthData getPersonalSellerAuthData() {
        PersonalSellerAuthData personalSellerAuthData = new PersonalSellerAuthData();
        personalSellerAuthData.setUserId(this.userId);
        personalSellerAuthData.setSource(this.source);
        personalSellerAuthData.setMemberName(this.name);
        personalSellerAuthData.setIdCardNumber(this.idCardNumber);
        personalSellerAuthData.setIdCardOnHandPhoto(getIdCardOnHandPhoto(this.cert_pic_url));
        personalSellerAuthData.setHalfProfilePhoto(getHalfProfilePhoto(this.cert_pic_url));
        return personalSellerAuthData;
    }
    
    public BopsCheckPersonalAuthResult getBopsCheckPersonalAuthResult() {
        BopsCheckPersonalAuthResult bopsCheckPersonalAuthResult = new BopsCheckPersonalAuthResult();
        bopsCheckPersonalAuthResult.setUserId(this.userId);
        bopsCheckPersonalAuthResult.setMemberName(this.name);
        bopsCheckPersonalAuthResult.setIdCard(this.idCardNumber);
        bopsCheckPersonalAuthResult.setHalfProfilePhoto(getHalfProfilePhoto(this.cert_pic_url));
        bopsCheckPersonalAuthResult.setIdCardOnHandPhoto(getIdCardOnHandPhoto(this.cert_pic_url));
        bopsCheckPersonalAuthResult.setCheckResult(this.cert_status);
        bopsCheckPersonalAuthResult.setFailReason(this.comments);
        bopsCheckPersonalAuthResult.setOperator(this.operatorNick);
        bopsCheckPersonalAuthResult.setCheckDate(getPassDate(this.passDate));
        return bopsCheckPersonalAuthResult;
    }

    private String getHalfProfilePhoto(String certPicUrl) {
        if (StringUtil.isBlank(certPicUrl)) {
            return null;
        }
        String[] sepPic = certPicUrl.split(";");
        if (null == sepPic || sepPic.length < 2) {
            return null;
        }
        return sepPic[1];
    }

    private String getIdCardOnHandPhoto(String certPicUrl) {
        if (StringUtil.isBlank(certPicUrl)) {
            return null;
        }
        String[] sepPic = certPicUrl.split(";");
        if (null == sepPic || sepPic.length < 1) {
            return null;
        }
        return sepPic[0];
    }

    private Date getPassDate(String passDateStr) {
        Date passDate = new Date();
        try {
            passDate = DateUtils.parseDate(passDateStr, new String[] { "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss" });
        } catch (ParseException e) {
        }
        return passDate;
    }


}
