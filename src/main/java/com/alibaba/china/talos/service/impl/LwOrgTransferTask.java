/*
 * Copyright 1999-2004 Alibaba.com All right reserved. This software is the confidential and proprietary information of
 * Alibaba.com ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with Alibaba.com.
 */
package com.alibaba.china.talos.service.impl;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.china.biz.common.util.DateUtil;
import com.alibaba.china.shared.talos.laiwang.constants.LwAuthStatus;
import com.alibaba.china.shared.talos.laiwang.constants.LwAuthType;
import com.alibaba.china.shared.talos.laiwang.model.LwAuthImage;
import com.alibaba.china.shared.talos.laiwang.model.LwAuthOrg;
import com.alibaba.china.shared.talos.order.exception.AuthOrderWriteException;
import com.alibaba.china.shared.talos.order.model.AuthOrderRequest;
import com.alibaba.china.shared.talos.order.service.AuthOrderReadService;
import com.alibaba.china.shared.talos.order.service.AuthOrderWriteService;
import com.alibaba.china.shared.talos.platform.model.AtomAuthInstance;
import com.alibaba.china.shared.talos.platform.model.AuthOrder;
import com.alibaba.china.shared.talos.product.common.constants.AuditStatusEnum;
import com.alibaba.china.shared.talos.product.common.model.AuthAuditResult;
import com.alibaba.china.shared.talos.product.common.util.AuditInfoJsonUtil;
import com.alibaba.china.shared.talos.product.laiwang.org.model.LwOrgAuditInfoObject;
import com.alibaba.china.shared.talos.product.laiwang.org.model.LwOrgAuthObject;
import com.alibaba.china.shared.talos.product.multicerts.model.ImageAuditInfoObject;
import com.alibaba.china.shared.talos.product.multicerts.model.MultiCertsAuthObject;
import com.alibaba.china.shared.talos.result.model.AuthTarget;
import com.alibaba.china.shared.talos.result.service.AuthTargetService;
import com.alibaba.china.shared.talos.result.service.enums.AuthTypeEnum;
import com.alibaba.china.shared.talos.result.service.enums.EntityTypeEnum;
import com.alibaba.china.shared.talos.result.service.enums.SiteEnum;
import com.alibaba.china.talos.audit.service.AuditOperateSerivce;
import com.alibaba.china.talos.audit.service.inner.AuditRequestService;
import com.alibaba.china.talos.daemon.BaseAO;
import com.alibaba.china.talos.dal.dao.audit.AuthAuditInfoDAO;
import com.alibaba.china.talos.dal.dao.laiwang.AuthLwImageRelationDAO;
import com.alibaba.china.talos.dal.dao.laiwang.AuthLwOrgDAO;
import com.alibaba.china.talos.dal.dataobject.audit.AuthAuditInfoDO;
import com.alibaba.china.talos.dal.dataobject.laiwang.AuthLwImageRelationDO;
import com.alibaba.china.talos.dal.dataobject.laiwang.AuthLwOrgDO;
import com.alibaba.china.talos.laiwang.cache.IAuthLwAuditReasonCache;
import com.alibaba.china.talos.laiwang.service.LwAuthAuditService;
import com.alibaba.china.talos.laiwang.utils.LwAuthUtils;
import com.alibaba.common.lang.StringUtil;
import com.alibaba.common.logging.Logger;
import com.alibaba.common.logging.LoggerFactory;
import com.alibaba.fastjson.JSON;

/**
 * 类LwOrgTransferTask.java的实现描述：来往企业数据迁移
 * 
 * <pre>
 * 数据迁移完成后，需要将此代码下线
 * </pre>
 * 
 * @author huiran.wenghr@alibaba-inc.com 2014-2-21 下午1:46:42
 */
public class LwOrgTransferTask implements BaseAO {

    private static final Logger     LOGGER   = LoggerFactory.getLogger("lwOrgTransferTaskLogger");
    private static final String     START_ID = "data.startId";
    private static final String     MAX_ID   = "data.maxId";
    private static final String     LIMIT    = "data.limit";
    // private static final int DEFAULT_LIMIT = 1000;

    private AuthLwOrgDAO            authLwOrgDAO;
    private AuthLwImageRelationDAO  authLwImageRelationDAO;
    private AuthOrderWriteService   authOrderWriteService;
    private AuthTargetService       authTargetService;
    private LwAuthAuditService      lwAuthAuditService;
    private AuthOrderReadService    authOrderReadService;

    private AuditOperateSerivce     auditOperateSerivce;
    private AuditRequestService     authAuditRequestService;
    private AuthAuditInfoDAO        authAuditInfoDAO;
    private IAuthLwAuditReasonCache authLwAuditReasonCache;

    @Override
    public void execute() {
        LOGGER.info("LwOrgTransferTask start");
        long startId = Integer.parseInt(System.getProperty(START_ID));
        long maxId = Integer.parseInt(System.getProperty(MAX_ID));
        long limit = Integer.parseInt(System.getProperty(LIMIT));

        List<AuthLwOrgDO> list = null;
        while (true) {
            LOGGER.info("param: startId=" + startId + ",maxId=" + maxId + ",limit=" + limit);
            // 查出等待审核的数据
            list = authLwOrgDAO.getAuthLwOrgDOByIdRange(startId, maxId, limit);
            if (list == null || list.isEmpty()) {
                break;
            }
            startId = batchMigrate(list);
        }
    }

    /**
     * 批量处理
     * 
     * @param list
     * @return
     */
    private long batchMigrate(List<AuthLwOrgDO> list) {
        long id = 0;
        AuthOrderRequest authOrderRequest;
        String productPacageCode = null;
        for (AuthLwOrgDO authLwOrgDO : list) {
            id = authLwOrgDO.getId();
            authOrderRequest = new AuthOrderRequest();
            if (StringUtil.equals(authLwOrgDO.getLwAuthType(), LwAuthType.ENTERPRISE_PUB_ACCOUNT.toString())) {
                productPacageCode = "V1009";
            } else if (StringUtil.equals(authLwOrgDO.getLwAuthType(), LwAuthType.OTHER_PUB_ACCOUNT.toString())) {
                productPacageCode = "V1010";
            }

            if (StringUtil.equals(authLwOrgDO.getStatus(), LwAuthStatus.FAILURE.toString())
                || StringUtil.equals(authLwOrgDO.getStatus(), LwAuthStatus.WAIT.toString())) {
                oneMigrate(productPacageCode, authLwOrgDO, authOrderRequest);
            }
        }
        return id;
    }

    /**
     * 单条数据处理
     * 
     * @param productPacageCode
     * @param authLwOrgDO
     * @param authOrderRequest
     */
    private void oneMigrate(String productPacageCode, AuthLwOrgDO authLwOrgDO, AuthOrderRequest authOrderRequest) {
        try {
            authOrderRequest.setAuthProductPackageCode(productPacageCode);
            AuthTarget authTarget = authTargetService.getAuthTarget(authLwOrgDO.getTargetId());
            if (authTarget == null) {
                return;
            }
            authOrderRequest.setEntityId(authTarget.getEntityId());
            authOrderRequest.setEntityType(EntityTypeEnum.USER);
            authOrderRequest.setSite(SiteEnum.LAIWANG);
            authOrderRequest.setAuthOrigin("laiwang");
            // 查询是否有未完结订单
            AuthOrder unFinishAuthOrder = authOrderReadService.findLatestUnFinishAuthOrder(authLwOrgDO.getTargetId(),
                                                                                           productPacageCode);
            Long orderId = null;
            // 有未完结的authOrder则不操作
            if (unFinishAuthOrder != null) {
                orderId = unFinishAuthOrder.getId();
                LOGGER.info("unFinishAuthOrder,orderId=" + orderId);
                return;
            }

            // 初始化申请单
            orderId = authOrderWriteService.generateAuthOrder(authOrderRequest);
            LOGGER.info("generateAuthOrder,orderId=" + orderId);

            LwAuthOrg lwAuthOrg = LwAuthUtils.toLwAuthOrg(authLwOrgDO);
            List<AuthLwImageRelationDO> imageRelations = authLwImageRelationDAO.getImgRelationListByInfoIdAndType(authLwOrgDO.getId(),
                                                                                                                  authLwOrgDO.getLwAuthType());
            List<LwAuthImage> imgList = LwAuthUtils.toLwAuthImageList(imageRelations);
            lwAuthOrg.setImgList(imgList);

            if (orderId != null && orderId > 0) {
                // 启动流程
                boolean result = lwAuthAuditService.startAudit(lwAuthOrg,
                                                               LwAuthType.typeOf(authLwOrgDO.getLwAuthType()));
                LOGGER.info("startAudit,authLwOrgDO=" + JSON.toJSONString(authLwOrgDO) + ",orderId=" + orderId
                            + ",result=" + result);

                if (result && StringUtil.equals(authLwOrgDO.getStatus(), LwAuthStatus.FAILURE.toString())) {
                    if (StringUtil.equals(LwAuthType.ENTERPRISE_PUB_ACCOUNT.name(), authLwOrgDO.getLwAuthType())) {
                        auditEnterprise(authLwOrgDO, orderId);
                    } else if (StringUtil.equals(LwAuthType.OTHER_PUB_ACCOUNT.name(), authLwOrgDO.getLwAuthType())) {
                        auditOrg(authLwOrgDO, orderId);
                    }
                }
            }
        } catch (AuthOrderWriteException e) {
            LOGGER.error("generateAuthOrder error! param:" + JSON.toJSONString(authLwOrgDO), e);
        } catch (Throwable e) {
            LOGGER.error("startAudit error! param:" + JSON.toJSONString(authLwOrgDO), e);
        }
    }

    /**
     * 组织审核
     * 
     * @param authLwOrgDO
     * @param orderId
     */
    private void auditOrg(AuthLwOrgDO authLwOrgDO, Long orderId) {
        try {
            AuthTarget authTarget = authTargetService.getAuthTarget(authLwOrgDO.getTargetId());
            String key = authTarget.getEntityId() + "_" + LwAuthType.OTHER_PUB_ACCOUNT.toString();
            String failReason = authLwAuditReasonCache.getAuditReason(key);

            List<AtomAuthInstance> atomList = authOrderReadService.queryAtomAuthList(orderId);
            for (AtomAuthInstance atomAuthInstance : atomList) {
                if (StringUtil.equals(atomAuthInstance.getPackageCode(), AuthTypeEnum.LWPUBORG.name())) {
                    // 原子认证实例ID
                    Long atomAuthId = atomAuthInstance.getId();
                    // 审核申请单ID
                    Long auditRequestId = authAuditRequestService.findAuditRequestId(atomAuthId);

                    AuthAuditResult results = new AuthAuditResult();
                    results.setAuditPerson("sys");
                    results.setAuditTime(DateUtil.getCurrentTime());
                    results.setReasonDetailCode("LW_SENCOND");
                    // 从缓存获取
                    results.setReasonDetailText(failReason);
                    results.setReasonTypeCode("LW_FIRST");
                    results.setReasonTypeText("task fail");
                    results.setRemark("");

                    List<AuthAuditInfoDO> auditInfoDOList = authAuditInfoDAO.findByAuditRequestId(auditRequestId);
                    // 构造两证审核的子审核项的审核结果
                    List<LwOrgAuditInfoObject> auditInfoList = new ArrayList<LwOrgAuditInfoObject>();
                    for (AuthAuditInfoDO auditInfoDO : auditInfoDOList) {
                        try {
                            LwOrgAuditInfoObject auditInfo = (LwOrgAuditInfoObject) AuditInfoJsonUtil.parseObject(auditInfoDO.getInfo());
                            if (auditInfo != null) {
                                auditInfo.setAuditInfoId(auditInfoDO.getId());
                                auditInfo.setStatus(AuditStatusEnum.FAIL);
                                auditInfo.setAuthAuditResult(results);
                                auditInfoList.add(auditInfo);
                            }
                        } catch (Exception e) {
                            LOGGER.error("get ImageAuditInfoOject for OTHER_PUB_ACCOUNT auto failed exception", e);
                        }
                    }

                    LwOrgAuthObject lwOrgAuthObject = new LwOrgAuthObject();
                    lwOrgAuthObject.setAuditTime(DateUtil.getCurrentTime());
                    lwOrgAuthObject.setAtomAuthId(atomAuthId);
                    lwOrgAuthObject.setAuditRequestId(auditRequestId);
                    lwOrgAuthObject.setPackageCode(AuthTypeEnum.LWPUBORG.name());
                    lwOrgAuthObject.setTargetId(authLwOrgDO.getTargetId());
                    lwOrgAuthObject.setStatus(AuditStatusEnum.FAIL);
                    lwOrgAuthObject.setInfoList(auditInfoList);

                    auditOperateSerivce.processAudit(lwOrgAuthObject);
                }
            }
        } catch (Exception e) {
            LOGGER.error("handler auditOrg error,param: authLwOrgDO=" + JSON.toJSONString(authLwOrgDO) + ",orderId="
                         + orderId, e);
        }

    }

    /**
     * 企业审核
     * 
     * @param authLwOrgDO
     * @param orderId
     */
    private void auditEnterprise(AuthLwOrgDO authLwOrgDO, Long orderId) {
        try {
            AuthTarget authTarget = authTargetService.getAuthTarget(authLwOrgDO.getTargetId());
            String key = authTarget.getEntityId() + "_" + LwAuthType.ENTERPRISE_PUB_ACCOUNT.toString();
            String failReason = authLwAuditReasonCache.getAuditReason(key);
            List<AtomAuthInstance> atomList = authOrderReadService.queryAtomAuthList(orderId);
            for (AtomAuthInstance atomAuthInstance : atomList) {
                if (StringUtil.equals(atomAuthInstance.getPackageCode(), AuthTypeEnum.MULTICERTS.name())) {
                    // 原子认证实例ID
                    Long atomAuthId = atomAuthInstance.getId();
                    // 审核申请单ID
                    Long auditRequestId = authAuditRequestService.findAuditRequestId(atomAuthId);

                    AuthAuditResult results = new AuthAuditResult();
                    results.setAuditPerson("sys");
                    results.setAuditTime(DateUtil.getCurrentTime());
                    results.setReasonDetailCode("LW_SENCOND");
                    // 从缓存获取
                    results.setReasonDetailText(failReason);
                    results.setReasonTypeCode("LW_FIRST");
                    results.setReasonTypeText("task fail");
                    results.setRemark("");

                    List<AuthAuditInfoDO> auditInfoDOList = authAuditInfoDAO.findByAuditRequestId(auditRequestId);
                    // 构造两证审核的子审核项的审核结果
                    List<ImageAuditInfoObject> auditInfoList = new ArrayList<ImageAuditInfoObject>();
                    for (AuthAuditInfoDO auditInfoDO : auditInfoDOList) {
                        try {
                            ImageAuditInfoObject imageAuditInfoObject = (ImageAuditInfoObject) AuditInfoJsonUtil.parseObject(auditInfoDO.getInfo());
                            if (imageAuditInfoObject != null) {
                                imageAuditInfoObject.setAuditInfoId(auditInfoDO.getId());
                                imageAuditInfoObject.setStatus(AuditStatusEnum.FAIL);
                                imageAuditInfoObject.setAuthAuditResult(results);
                                auditInfoList.add(imageAuditInfoObject);
                            }
                        } catch (Exception e) {
                            LOGGER.error("get ImageAuditInfoOject for ENTERPRISE_PUB_ACCOUNT auto failed exception", e);
                        }
                    }

                    // 构造两证审核的审核结果
                    MultiCertsAuthObject multiCertsAuthObject = new MultiCertsAuthObject();
                    multiCertsAuthObject.setInfoList(auditInfoList);
                    multiCertsAuthObject.setAuditRequestId(auditRequestId);
                    multiCertsAuthObject.setAtomAuthId(atomAuthId);
                    multiCertsAuthObject.setAuditTime(DateUtil.getCurrentTime());
                    multiCertsAuthObject.setStatus(AuditStatusEnum.FAIL);
                    multiCertsAuthObject.setTargetId(authLwOrgDO.getTargetId());
                    multiCertsAuthObject.setPackageCode(AuthTypeEnum.MULTICERTS.name());
                    auditOperateSerivce.processAudit(multiCertsAuthObject);
                }
            }
        } catch (Exception e) {
            LOGGER.error("handler auditEnterprise error,param: authLwOrgDO=" + JSON.toJSONString(authLwOrgDO)
                         + ",orderId=" + orderId, e);
        }
    }

    public void setAuthLwOrgDAO(AuthLwOrgDAO authLwOrgDAO) {
        this.authLwOrgDAO = authLwOrgDAO;
    }

    public void setAuthOrderWriteService(AuthOrderWriteService authOrderWriteService) {
        this.authOrderWriteService = authOrderWriteService;
    }

    public void setAuthTargetService(AuthTargetService authTargetService) {
        this.authTargetService = authTargetService;
    }

    public void setLwAuthAuditService(LwAuthAuditService lwAuthAuditService) {
        this.lwAuthAuditService = lwAuthAuditService;
    }

    public void setAuthOrderReadService(AuthOrderReadService authOrderReadService) {
        this.authOrderReadService = authOrderReadService;
    }

    public void setAuthLwImageRelationDAO(AuthLwImageRelationDAO authLwImageRelationDAO) {
        this.authLwImageRelationDAO = authLwImageRelationDAO;
    }

    public void setAuditOperateSerivce(AuditOperateSerivce auditOperateSerivce) {
        this.auditOperateSerivce = auditOperateSerivce;
    }

    public void setAuthAuditRequestService(AuditRequestService authAuditRequestService) {
        this.authAuditRequestService = authAuditRequestService;
    }

    public void setAuthAuditInfoDAO(AuthAuditInfoDAO authAuditInfoDAO) {
        this.authAuditInfoDAO = authAuditInfoDAO;
    }

    public void setAuthLwAuditReasonCache(IAuthLwAuditReasonCache authLwAuditReasonCache) {
        this.authLwAuditReasonCache = authLwAuditReasonCache;
    }

}
