/*
 * Copyright 1999-2004 Alibaba.com All right reserved. This software is the confidential and proprietary information of
 * Alibaba.com ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with Alibaba.com.
 */
package com.alibaba.china.talos.service.impl;

import java.util.List;

import com.alibaba.china.shared.talos.laiwang.constants.LwAuthStatus;
import com.alibaba.china.shared.talos.laiwang.constants.LwAuthType;
import com.alibaba.china.shared.talos.laiwang.model.LwAuthImage;
import com.alibaba.china.shared.talos.laiwang.model.LwAuthPna;
import com.alibaba.china.shared.talos.order.exception.AuthOrderWriteException;
import com.alibaba.china.shared.talos.order.model.AuthOrderRequest;
import com.alibaba.china.shared.talos.order.service.AuthOrderReadService;
import com.alibaba.china.shared.talos.order.service.AuthOrderWriteService;
import com.alibaba.china.shared.talos.platform.model.AuthOrder;
import com.alibaba.china.shared.talos.product.common.constants.AuditStatusEnum;
import com.alibaba.china.shared.talos.result.model.AuthTarget;
import com.alibaba.china.shared.talos.result.service.AuthTargetService;
import com.alibaba.china.shared.talos.result.service.enums.EntityTypeEnum;
import com.alibaba.china.shared.talos.result.service.enums.SiteEnum;
import com.alibaba.china.talos.daemon.BaseAO;
import com.alibaba.china.talos.dal.dao.laiwang.AuthLwImageRelationDAO;
import com.alibaba.china.talos.dal.dao.laiwang.AuthLwPnaDAO;
import com.alibaba.china.talos.dal.dataobject.laiwang.AuthLwImageRelationDO;
import com.alibaba.china.talos.dal.dataobject.laiwang.AuthLwPnaDO;
import com.alibaba.china.talos.laiwang.audit.ILwAuthAudit;
import com.alibaba.china.talos.laiwang.audit.LwAuthAuditFactory;
import com.alibaba.china.talos.laiwang.cache.IAuthLwPNACache;
import com.alibaba.china.talos.laiwang.service.LwAuthAuditService;
import com.alibaba.china.talos.laiwang.utils.LwAuthUtils;
import com.alibaba.common.lang.StringUtil;
import com.alibaba.common.logging.Logger;
import com.alibaba.common.logging.LoggerFactory;
import com.alibaba.fastjson.JSON;

/**
 * 类LwOrgTransferTask.java的实现描述：来往个人数据迁移
 * 
 * @author huiran.wenghr@alibaba-inc.com 2014-2-21 下午1:46:42
 */
public class LwPnaTransferTask implements BaseAO {

    private static final Logger    LOGGER               = LoggerFactory.getLogger("lwOrgTransferTaskLogger");
    private static final String    START_ID             = "data.startId";
    private static final String    MAX_ID               = "data.maxId";
    private static final String    LIMIT                = "data.limit";

    private static final String    PRODUCT_PACKAGE_CODE = "V1008";

    private AuthLwPnaDAO           authLwPnaDAO;
    private AuthLwImageRelationDAO authLwImageRelationDAO;
    private AuthOrderWriteService  authOrderWriteService;
    private AuthTargetService      authTargetService;
    private LwAuthAuditService     lwAuthAuditService;
    private AuthOrderReadService   authOrderReadService;
    private IAuthLwPNACache        authLwPNACache;

    @Override
    public void execute() {
        LOGGER.info("LwPnaTransferTask start");
        long startId = Integer.parseInt(System.getProperty(START_ID));
        long maxId = Integer.parseInt(System.getProperty(MAX_ID));
        long limit = Integer.parseInt(System.getProperty(LIMIT));

        List<AuthLwPnaDO> list = null;
        while (true) {
            LOGGER.info("param: startId=" + startId + ",maxId=" + maxId + ",limit=" + limit);
            // 查出等待审核的数据
            list = authLwPnaDAO.getAuthLwPnaDOByIdRange(startId, maxId, limit);
            if (list == null || list.isEmpty()) {
                break;
            }
            startId = batchMigrate(list);
        }
    }

    /**
     * @param list
     * @return
     */
    private long batchMigrate(List<AuthLwPnaDO> list) {
        long id = 0;
        AuthOrderRequest authOrderRequest;
        for (AuthLwPnaDO authLwPnaDO : list) {
            id = authLwPnaDO.getId();
            // ADDITIVE的情况也需要处理
            if (!StringUtil.equals(authLwPnaDO.getStatus(), LwAuthStatus.WAIT.toString())
                && !StringUtil.equals(authLwPnaDO.getStatus(), LwAuthStatus.ADDITIVE.toString())) {
                continue;
            }
            authOrderRequest = new AuthOrderRequest();
            oneMigrate(authLwPnaDO, authOrderRequest);
        }
        return id;
    }

    /**
     * @param authLwOrgDO
     * @param authOrderRequest
     */
    private void oneMigrate(AuthLwPnaDO authLwPnaDO, AuthOrderRequest authOrderRequest) {
        try {
            authOrderRequest.setAuthProductPackageCode(PRODUCT_PACKAGE_CODE);
            AuthTarget authTarget = authTargetService.getAuthTarget(authLwPnaDO.getTargetId());
            if (authTarget == null) {
                return;
            }
            authOrderRequest.setEntityId(authTarget.getEntityId());
            authOrderRequest.setEntityType(EntityTypeEnum.USER);
            authOrderRequest.setSite(SiteEnum.LAIWANG);
            authOrderRequest.setAuthOrigin("laiwang");

            // 查询是否有未完结订单
            AuthOrder unFinishAuthOrder = authOrderReadService.findLatestUnFinishAuthOrder(authLwPnaDO.getTargetId(),
                                                                                           PRODUCT_PACKAGE_CODE);
            Long orderId = null;
            // 有未完结的authOrder则不操作
            if (unFinishAuthOrder != null) {
                orderId = unFinishAuthOrder.getId();
                LOGGER.info("unFinishAuthOrder,orderId=" + orderId);
                return;
            }

            // 初始化申请单
            orderId = authOrderWriteService.generateAuthOrder(authOrderRequest);
            LwAuthPna lwAuthPna = LwAuthUtils.toLwAuthPna(authLwPnaDO);

            List<AuthLwImageRelationDO> imageRelations = authLwImageRelationDAO.getImgRelationListByInfoIdAndType(authLwPnaDO.getId(),
                                                                                                                  authLwPnaDO.getLwAuthType());
            List<LwAuthImage> imgList = LwAuthUtils.toLwAuthImageList(imageRelations);
            lwAuthPna.setImgList(imgList);

            LOGGER.info("generateAuthOrder,orderId=" + orderId);

            if (orderId != null && orderId > 0) {
                Boolean result = lwAuthAuditService.startAudit(lwAuthPna, LwAuthType.PERSONAL_PUB_ACCOUNT);
                LOGGER.info("startAudit,lwAuthPna=" + JSON.toJSONString(lwAuthPna) + ",result=" + result + ",orderId:"
                            + orderId);

                if (result != null && result.booleanValue()) {
                    authLwPNACache.putStatus(authTarget.getEntityId(), authLwPnaDO.getStatus());
                    // 如果是additive状态的则，要审核TBD
                    auditPna(lwAuthPna, authLwPnaDO, orderId);
                }

            }
        } catch (AuthOrderWriteException e) {
            LOGGER.error("generateAuthOrder error! param:" + JSON.toJSONString(authLwPnaDO), e);
        }
    }

    @SuppressWarnings("unchecked")
    private void auditPna(LwAuthPna lwAuthPna, AuthLwPnaDO authLwPnaDO, Long orderId) {
        try {
            if (StringUtil.equals(authLwPnaDO.getStatus(), LwAuthStatus.ADDITIVE.toString())) {
                ILwAuthAudit<LwAuthPna> lwAuthAudit = (ILwAuthAudit<LwAuthPna>) LwAuthAuditFactory.create(LwAuthType.PERSONAL_PUB_ACCOUNT);
                boolean autoAuditResult = lwAuthAudit.autoProcessAudit(lwAuthPna, orderId, AuditStatusEnum.TBD);
                LOGGER.info("autoProcessAudit result=" + autoAuditResult + ",orderId=" + orderId);
            }
        } catch (Exception e) {
            LOGGER.error("handler auditPna error,param: authLwPnaDO=" + JSON.toJSONString(authLwPnaDO) + ",orderId="
                         + orderId, e);
        }
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

    public void setAuthLwPnaDAO(AuthLwPnaDAO authLwPnaDAO) {
        this.authLwPnaDAO = authLwPnaDAO;
    }

    public void setAuthOrderReadService(AuthOrderReadService authOrderReadService) {
        this.authOrderReadService = authOrderReadService;
    }

    public void setAuthLwPNACache(IAuthLwPNACache authLwPNACache) {
        this.authLwPNACache = authLwPNACache;
    }

    public void setAuthLwImageRelationDAO(AuthLwImageRelationDAO authLwImageRelationDAO) {
        this.authLwImageRelationDAO = authLwImageRelationDAO;
    }

}
