package com.alibaba.china.talos.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.china.shared.talos.order.model.AuthOrderRequest;
import com.alibaba.china.shared.talos.order.service.AuthOrderReadService;
import com.alibaba.china.shared.talos.order.service.AuthOrderWriteService;
import com.alibaba.china.shared.talos.platform.constants.AuthStatusEnum;
import com.alibaba.china.shared.talos.platform.constants.ResultStatusEnum;
import com.alibaba.china.shared.talos.platform.model.AtomAuthInstance;
import com.alibaba.china.shared.talos.platform.model.AuthOrder;
import com.alibaba.china.shared.talos.product.common.util.JsonSerializeUtil;
import com.alibaba.china.shared.talos.product.personal.model.PersonalAuthObject;
import com.alibaba.china.shared.talos.result.service.enums.SiteEnum;
import com.alibaba.china.shared.talos.tbpersonseller.model.PersonalSellerAuthData;
import com.alibaba.china.talos.daemon.BaseAO;
import com.alibaba.china.talos.dal.dao.personal.PersonalAuthDataDAO;
import com.alibaba.china.talos.file.FileAnalyzer;
import com.alibaba.china.talos.file.FileProcessParam;
import com.alibaba.china.talos.file.FileProcessor;
import com.alibaba.china.talos.file.LineAnalyzer;
import com.alibaba.china.talos.file.LineHandle;
import com.alibaba.china.talos.platform.dataitem.constants.AuthDataItemStatusEnum;
import com.alibaba.china.talos.platform.dataitem.objs.AtomAuthStuffUpdateParam;
import com.alibaba.china.talos.platform.dataitem.service.AuthStuffService;
import com.alibaba.china.talos.platform.flow.AuthFlowEngine;
import com.alibaba.china.talos.platform.product.atom.AtomAuthService;

import com.alibaba.common.logging.Logger;
import com.alibaba.common.logging.LoggerFactory;


public class Taobao4V1006DataCorrectionTaskAO implements BaseAO {

    @Autowired
    private AtomAuthService           atomAuthService;
    @Autowired
    private PersonalAuthDataDAO       personalAuthDataDAO;
    @Autowired
    private AuthFlowEngine            authFlowEngine;
    @Autowired
    private AuthOrderReadService      authOrderReadService;
    @Autowired
    private AuthOrderWriteService authOrderWriteService;
    @Autowired
    private AuthStuffService      authStuffService;


    private static final Logger       log = LoggerFactory.getLogger(Taobao4V1006DataCorrectionTaskAO.class);

    @Override
    public void execute() {
        LineAnalyzer<HandleModel> lineAnalyzer = getLineAnalyzer();
        LineHandle<HandleModel> lineHandle = getLineHandle();
        FileAnalyzer<HandleModel> fileAnalyzer = new FileAnalyzer<HandleModel>(lineAnalyzer, lineHandle);
        FileProcessParam fpp = getFileProcessParam();
        FileProcessor fp = new FileProcessor(fpp, fileAnalyzer);
        fp.process();
    }

    private LineAnalyzer<HandleModel> getLineAnalyzer() {
        return new LineAnalyzer<HandleModel>() {

            @Override
            public HandleModel analyse(String lineText) {

                HandleModel model = new HandleModel();

                String[] dataList = lineText.split(",");
                if (null == dataList || dataList.length < 4) {
                    log.warn("data is invalid where line is " + lineText);
                    return null;
                }
                model.userId = Long.parseLong(getMetaData(dataList[0]));
                model.orderId = Long.parseLong(getMetaData(dataList[1]));
                model.status = getMetaData(dataList[2]);
                model.origin = getMetaData(dataList[3]);

                return model;
            }

            private String getMetaData(String data) {
                return data.substring(1, data.length() - 1);
            }
        };
    }

    private LineHandle<HandleModel> getLineHandle() {
        return new LineHandle<HandleModel>() {

            @Override
            public boolean handle(HandleModel model) {
                if (null == model) {
                    return true;
                }
                if (model.status.equalsIgnoreCase("init")) {
                    if (submit(model)) {
                        if (bopsCheck(model)) {
                            if (resultCheck(model.orderId)) {
                                log.info("process succeed where userId:" + model.userId);
                                return true;
                            } else {
                                log.error("process failed beacuse of resultCheck error where userId:" + model.userId);
                                return false;
                            }
                        } else {
                            log.error("process failed where userId:" + model.userId);
                            return false;
                        }
                    } else {
                        log.error("submit error where userId:" + model.userId);
                        return false;
                    }

                } else if (model.status.equalsIgnoreCase("process")) {
                    boolean res1 = bopsCheck(model);
                    boolean res2 = bopsCheck(model);
                    if (res1 & res2) {
                        if (resultCheck(model.orderId)) {
                            log.info("process succeed where userId:" + model.userId);
                            return true;
                        } else {
                            log.error("process failed beacuse of resultCheck error where userId:" + model.userId);
                            return false;
                        }

                    } else {
                        log.error("process failed where userId:" + model.userId);
                        return false;
                    }
                }
                return true;
            }

            private boolean submit(HandleModel model) {
                // 通过订单的ID查询对应的原子认证
                AtomAuthInstance atomAuthInstance = new AtomAuthInstance();
                atomAuthInstance.setOrderId(model.orderId);
                // 设置对应的原子认证code
                atomAuthInstance.setPackageCode("PERSONAL");
                List<AtomAuthInstance> list = atomAuthService.findAll(atomAuthInstance);
                if (list == null || list.isEmpty()) {
                    log.warn("atom not found where userId:" + model.userId);
                    return false;
                }
                AtomAuthInstance instance = list.get(0);
                AuthOrderRequest authOrderRequest = new AuthOrderRequest();
                authOrderRequest.setEntityId(String.valueOf(model.userId));
                authOrderRequest.setAuthProductPackageCode("V1006");
                authOrderRequest.setSite(SiteEnum.TAOBAO);
                PersonalSellerAuthData personalSellerAuthData = new PersonalSellerAuthData();
                personalSellerAuthData.setOrderId(model.orderId);
                personalSellerAuthData.setUserId(model.userId);
                personalSellerAuthData.setSource("POS");
                try {
                    // 开启认证流程
                    boolean isStartAuthFlowOk = authOrderWriteService.startAuth(authOrderRequest);
                    if (!isStartAuthFlowOk) {
                        log.error("[PersonalAuth ] the authOrderWriteService.startAuth failed, param: authOrderRequest="
                                     + JsonSerializeUtil.toJSONStringFormatDate(authOrderRequest)
                                     + ", result: isStartAuthFlowOk=" + isStartAuthFlowOk);
                    }
                    AtomAuthStuffUpdateParam<PersonalSellerAuthData> atomAuthStuffUpdateParam = new AtomAuthStuffUpdateParam<PersonalSellerAuthData>();
                    atomAuthStuffUpdateParam.setAtomAuthId(instance.getId());
                    atomAuthStuffUpdateParam.setPojo(personalSellerAuthData);
                    atomAuthStuffUpdateParam.setAuthStuffStatus(AuthDataItemStatusEnum.SUBMIT);
                    atomAuthStuffUpdateParam.setModifier(personalSellerAuthData.getUserId().toString());
                    // 更新申请单资料
                    authStuffService.updateByAtomAuthId(atomAuthStuffUpdateParam);
                    // 触发当前状态到等待审核
                    authFlowEngine.triggerAfterConditionChange("atomAuthStatus", instance.getId(), true);

                } catch (Exception e) {
                    log.error("submit is error", e);
                    return false;
                }
                return true;
            }

            private boolean bopsCheck(HandleModel model) {
                // 通过订单的ID查询对应的原子认证
                AtomAuthInstance atomAuthInstance = new AtomAuthInstance();
                atomAuthInstance.setOrderId(model.orderId);
                // 设置对应的原子认证code
                atomAuthInstance.setPackageCode("PERSONAL");
                List<AtomAuthInstance> list = atomAuthService.findAll(atomAuthInstance);
                if (list == null || list.isEmpty()) {
                    log.warn("atom not found where userId:" + model.userId);
                    return false;
                }
                AtomAuthInstance instance = list.get(0);
                // 保存审核原因
                PersonalAuthObject data = new PersonalAuthObject();
                data.setAtomAuthId(instance.getId());
                data.setTargetId(instance.getTargetId());
                data.setReason("taobao dev error used V1006 instead of V1001");
                data.setUserId(model.userId);
                data.setOperatorNick("dev");
                data.setStatus(ResultStatusEnum.failure.name());

                try {
                    PersonalAuthObject authObject = personalAuthDataDAO.findByAtomId(data.getAtomAuthId(),
                                                                                     data.getTargetId());
                    if (authObject == null) {
                        personalAuthDataDAO.insert(data);
                    } else {
                        personalAuthDataDAO.update(data);
                    }
                    // 触发当前状态到等待审核通过
                    authFlowEngine.triggerByAtomAuthId(instance.getId(), true);
                } catch (Exception e) {
                    log.error("bopsCheck  is error user_id is " + model.userId, e);
                    return false;
                }
                return true;

            }

            private boolean resultCheck(long orderId) {
                // 反查下order确认认证通过
                AuthOrder authOrder = authOrderReadService.findAuthOrder(orderId);
                if (null != authOrder && authOrder.getStatus().equals(AuthStatusEnum.end)
                    && authOrder.getResultStatus().equals(ResultStatusEnum.failure)) {
                    return true;
                } else {
                    return false;
                }
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
        long   userId;
        long   orderId;
        String status;
        String origin;
    }

}
