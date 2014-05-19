/*
 * Copyright 1999-2004 Alibaba.com All right reserved. This software is the confidential and proprietary information of
 * Alibaba.com ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with Alibaba.com.
 */
package com.alibaba.china.talos.service.impl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.alibaba.china.shared.talos.laiwang.constants.LwAuthStatus;
import com.alibaba.china.shared.talos.laiwang.constants.LwAuthType;
import com.alibaba.china.shared.talos.result.model.AuthTarget;
import com.alibaba.china.shared.talos.result.service.AuthTargetService;
import com.alibaba.china.talos.daemon.BaseAO;
import com.alibaba.china.talos.dal.dao.laiwang.AuthLwOrgDAO;
import com.alibaba.china.talos.dal.dao.laiwang.AuthLwPnaDAO;
import com.alibaba.china.talos.dal.dataobject.laiwang.AuthLwOrgDO;
import com.alibaba.china.talos.dal.dataobject.laiwang.AuthLwPnaDO;
import com.alibaba.china.talos.laiwang.cache.IAuthLwAuditReasonCache;
import com.alibaba.china.talos.laiwang.cache.IAuthLwPNACache;
import com.alibaba.common.lang.StringUtil;
import com.alibaba.common.logging.Logger;
import com.alibaba.common.logging.LoggerFactory;
import com.laiwang.pp.reg.service.PubUserRegService;
import com.laiwang.pp.reg.service.enums.PubUserSite;
import com.laiwang.pp.reg.service.enums.PubUserStatus;

/**
 * 类LwAuditTask.java的实现描述：来往审核临时任务
 * 
 * <pre>
 *  处理的文件的数据格式：infoId|pubType|status|reason
 * </pre>
 * 
 * @author huiran.wenghr@alibaba-inc.com 2014-2-19 上午9:55:40
 */
public class LwAuditTask implements BaseAO {

    private static final Logger     LOGGER        = LoggerFactory.getLogger("lwAuditTaskLogger");
    private static final String     RUN_PROPERTY  = "handler.file";
    private static final String     FILE_ENCODING = "utf8";
    private static final String     SEP_CHAR      = "\\|";
    private static final String     PERSON        = "p";

    private PubUserRegService       pubUserRegService;
    private IAuthLwAuditReasonCache authLwAuditReasonCache;
    private AuthTargetService       authTargetService;
    private IAuthLwPNACache         authLwPNACache;

    private AuthLwPnaDAO            authLwPnaDAO;
    private AuthLwOrgDAO            authLwOrgDAO;

    @Override
    public void execute() {
        String filepath = System.getProperty(RUN_PROPERTY);
        InputStream in = null;
        try {
            in = new FileInputStream(filepath);
            @SuppressWarnings("unchecked")
            List<String> auditResults = IOUtils.readLines(in, FILE_ENCODING);
            for (String auditResult : auditResults) {
                handler(auditResult);
            }
        } catch (Exception e) {
            LOGGER.error("[LwAuditTask.execute] cann't open file, path=" + filepath, e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * 处理审核记录<br/>
     * example: "infoId|pubType|status|reason"<br/>
     * status:failure、success
     * 
     * @param auditResult
     */
    private void handler(String auditResult) {
        LOGGER.info("handler start [auditResult]:" + auditResult);
        try {
            String[] auditDetail = auditResult.split(SEP_CHAR);
            if (auditDetail != null && auditDetail.length > 2) {
                // 更新信息表状态，如果失败需要将失败原因写入缓存

                // 通知来往
                String id = auditDetail[0];
                String pubType = auditDetail[1];
                String status = auditDetail[2];
                String laiwangId = null;
                String authType = null;

                if (StringUtil.isBlank(status)) {
                    return;
                }

                Long targetId = null;
                // 个人
                if (StringUtil.equals(pubType, PERSON)) {
                    authType = LwAuthType.PERSONAL_PUB_ACCOUNT.toString();
                    AuthLwPnaDO authLwPnaDO = authLwPnaDAO.findById(Long.valueOf(id.trim()));
                    if (authLwPnaDO != null) {
                        targetId = authLwPnaDO.getTargetId();
                        // 更新状态
                        authLwPnaDAO.updateStatus(Long.valueOf(id.trim()), status);
                        LOGGER.info("authLwPnaDAO.updateStatus,id=" + id + ",stauts=" + status);
                    }
                } else {
                    // 企业，组织
                    AuthLwOrgDO authLwOrgDO = authLwOrgDAO.getAuthLwOrgDOById(Long.valueOf(id.trim()));
                    if (authLwOrgDO != null) {
                        targetId = authLwOrgDO.getTargetId();
                        // 更新状态
                        authLwOrgDAO.updateStatus(Long.valueOf(id.trim()), status);
                        authType = authLwOrgDO.getLwAuthType();
                        LOGGER.info("authLwOrgDAO.updateStatus,id=" + id + ",stauts=" + status);
                    }
                }

                if (targetId != null) {
                    AuthTarget authTarget = authTargetService.getAuthTarget(targetId);
                    if (authTarget != null) {
                        laiwangId = authTarget.getEntityId();
                    }
                }

                String reason = null;
                if (auditDetail.length > 3) {
                    reason = auditDetail[3];
                }

                if (StringUtil.equals(status, LwAuthStatus.FAILURE.toString())) {
                    // 写缓存
                    String key = laiwangId + "_" + authType;
                    authLwAuditReasonCache.saveAuditReason(key, reason);
                    LOGGER.info("authLwAuditReasonCache.saveAuditReason,key=" + key);
                }

                // 通知来往
                notifyLw(laiwangId, status);
                // remove cache
                removeCache(laiwangId);
            }
        } catch (Exception e) {
            LOGGER.error("[handler] handler error! auditResult=" + auditResult, e);
        }
        LOGGER.info("handler end ");
    }

    /**
     * @param laiwangId
     */
    private void removeCache(String laiwangId) {
        String cacheKey = "lw_pna" + laiwangId;
        authLwPNACache.removeAuthFailureTimes(cacheKey);
    }

    /**
     * 通知来往
     * 
     * @param string
     */
    private void notifyLw(String laiwangId, String status) {
        PubUserStatus lwAuthStatus = null;
        try {
            if (StringUtil.equals(status, "init")) {
                lwAuthStatus = PubUserStatus.NEW;
            } else if (StringUtil.equals(status, "wait")) {
                lwAuthStatus = PubUserStatus.AUTHING;
            } else if (StringUtil.equals(status, "success")) {
                lwAuthStatus = PubUserStatus.AUTH_PASS;
            }
            if (lwAuthStatus != null) {
                boolean result = pubUserRegService.updateStatus(PubUserSite.LAIWANG, laiwangId, lwAuthStatus);
                LOGGER.info("notify laiwang result=" + result + ",laiwangId=" + laiwangId + ",status=" + status);
            }
        } catch (Exception e) {
            LOGGER.error("[notifyLw] notify laiwang error, laiwangId=" + laiwangId + ",lwAuthStatus=" + lwAuthStatus, e);
        }
    }

    public void setPubUserRegService(PubUserRegService pubUserRegService) {
        this.pubUserRegService = pubUserRegService;
    }

    public void setAuthLwAuditReasonCache(IAuthLwAuditReasonCache authLwAuditReasonCache) {
        this.authLwAuditReasonCache = authLwAuditReasonCache;
    }

    public void setAuthTargetService(AuthTargetService authTargetService) {
        this.authTargetService = authTargetService;
    }

    public void setAuthLwPnaDAO(AuthLwPnaDAO authLwPnaDAO) {
        this.authLwPnaDAO = authLwPnaDAO;
    }

    public void setAuthLwOrgDAO(AuthLwOrgDAO authLwOrgDAO) {
        this.authLwOrgDAO = authLwOrgDAO;
    }

    public void setAuthLwPNACache(IAuthLwPNACache authLwPNACache) {
        this.authLwPNACache = authLwPNACache;
    }

}
