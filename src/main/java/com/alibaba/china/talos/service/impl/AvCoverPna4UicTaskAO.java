package com.alibaba.china.talos.service.impl;

import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.china.auth.service.AuthReadService;
import com.alibaba.china.auth.service.models.Owner;
import com.alibaba.china.auth.service.models.UnitedAuthModel;
import com.alibaba.china.auth.service.models.read.av.AVInfoModel;
import com.alibaba.china.auth.service.models.read.av.EnterpriseAVInfoModel;
import com.alibaba.china.auth.service.models.read.av.EnterpriseInfoModel;
import com.alibaba.china.member.service.MemberReadService;
import com.alibaba.china.auth.service.enums.AVTypeEnum;
import com.alibaba.china.auth.service.enums.AuthTypeEnum;
import com.alibaba.china.talos.daemon.BaseAO;
import com.alibaba.china.talos.file.FileAnalyzer;
import com.alibaba.china.talos.file.FileProcessParam;
import com.alibaba.china.talos.file.FileProcessor;
import com.alibaba.china.talos.file.LineAnalyzer;
import com.alibaba.china.talos.file.LineHandle;
import com.alibaba.china.talos.util.UICUtilService;
import com.alibaba.china.talos.util.UICUtilService.AlipayAuthStatus;
import com.alibaba.common.logging.Logger;
import com.alibaba.common.logging.LoggerFactory;
import com.taobao.uic.common.domain.ExtraUserDO;


public class AvCoverPna4UicTaskAO implements BaseAO {

    @Autowired
    private AuthReadService     authReadService;

    @Autowired
    private UICUtilService      uicUtilService;

    @Autowired
    private MemberReadService   memberReadService;


    private static final Logger       log = LoggerFactory.getLogger(AvCoverPna4UicTaskAO.class);

    @Override
    public void execute() {
        LineAnalyzer<String> lineAnalyzer = getLineAnalyzer();
        LineHandle<String> lineHandle = getLineHandle();
        FileAnalyzer<String> fileAnalyzer = new FileAnalyzer<String>(lineAnalyzer, lineHandle);
        FileProcessParam fpp = getFileProcessParam();
        FileProcessor fp = new FileProcessor(fpp, fileAnalyzer);
        fp.process();
    }

    private LineAnalyzer<String> getLineAnalyzer() {
        return new LineAnalyzer<String>() {

            @Override
            public String analyse(String lineText) {

                return getMetaData(lineText);
            }

            private String getMetaData(String data) {
                return data.substring(1, data.length() - 1);
            }
        };
    }

    private LineHandle<String> getLineHandle() {
        return new LineHandle<String>() {

            @Override
            public boolean handle(String model) {
                // get userId
                long userId = memberReadService.convertMemberIdToUserId(model);
                HandleModel handleModel = needHandle(model, userId);
                if (handleModel.needHandle) {
                    uicUtilService.unTagAvUicTag(userId);
                    boolean unTagRes = uicUtilService.unTagPnaUicTag(userId);
                    boolean updateRes = uicUtilService.updateUser(userId, handleModel.fullName,
                                                                  handleModel.idCardNumber);
                    if (unTagRes && updateRes) {
                        log.info("process succeed where memberId:" + model + ",fullname:" + handleModel.fullName
                                 + "idcardnumber:" + handleModel.idCardNumber);
                        return true;
                    } else {
                        log.error("process failed where memberId:" + model + ",fullname:" + handleModel.fullName
                                  + "idcardnumber:" + handleModel.idCardNumber);
                        return false;
                    }
                }

                return true;
            }

            private HandleModel needHandle(String memberId, long userId) {
                HandleModel handleModel = new HandleModel();
                UnitedAuthModel uam = authReadService.findAuthByOwner(new Owner(memberId), AuthTypeEnum.AV,
                                                                      AuthTypeEnum.PNA);
                if (null != uam && uam.hasAV() && uam.hasPNA()) {
                    AVTypeEnum avTypeEnum = uam.getAVInfoModel().getAVType();
                    if (AVTypeEnum.EnterpriseAV.equals(avTypeEnum)) {
                        ExtraUserDO extraUserDO = uicUtilService.findExtraUserDO(userId);
                        if (!hasTalos(extraUserDO) && !hasTalosing(extraUserDO)) {
                            AlipayAuthStatus alipayAuthStatus = uicUtilService.getAlipayAuthStatus(userId);
                            if (!AlipayAuthStatus.ENTERPRISE_AUTH.equals(alipayAuthStatus)
                                && !AlipayAuthStatus.ENTERPRISE_AUTH.equals(alipayAuthStatus)) {
                                AVInfoModel avInfoModel = uam.getAVInfoModel();
                                EnterpriseAVInfoModel etpInfoModel = avInfoModel.getEnterpriseAV();
                                if (null == etpInfoModel) {
                                    return handleModel;
                                }
                                EnterpriseInfoModel etpInfo = etpInfoModel.getEnterpriseInfo();
                                handleModel.fullName = etpInfo.getAuthorizorInfo().getMemberName();
                                handleModel.idCardNumber = etpInfo.getRegCode();
                                handleModel.needHandle = true;
                                return handleModel;
                            }
                        }
                    }
                }
                return handleModel;
            }
            
            private boolean hasTalos(ExtraUserDO extraUserDO) {
                
                long userTag6 = extraUserDO.getUserTag6();
                return (userTag6 >> 57 & 1) == 1;
            }
            
            private boolean hasTalosing(ExtraUserDO extraUserDO) {
                
                long userTag6 = extraUserDO.getUserTag6();
                return (userTag6 >> 55 & 1) == 1;
            }

        };
    }

    private FileProcessParam getFileProcessParam() {
        String path = System.getProperty("filePath");
        String maxThreadNum = System.getProperty("maxThreadNum");
        String minThreadNum = System.getProperty("minThreadNum");
        return new FileProcessParam(path, Integer.parseInt(minThreadNum), Integer.parseInt(maxThreadNum));
    }
    
    class HandleModel {

        boolean needHandle;
        String  fullName;
        String  idCardNumber;
    }

}
