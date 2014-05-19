package com.alibaba.china.talos.small;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.alibaba.china.auth.process.cna.constants.CNAProcessStatusEnum;
import com.alibaba.china.talos.dal.dao.audit.AuthAuditRequestDAO;
import com.alibaba.china.talos.dal.dao.platform.AtomAuthInstanceDAO;
import com.alibaba.china.talos.dal.dao.platform.flow.StageInfoDAO;
import com.alibaba.china.talos.dal.dataobject.audit.AuthAuditRequestDO;
import com.alibaba.china.talos.dal.dataobject.platform.AtomAuthInstanceDO;
import com.alibaba.china.talos.dal.dataobject.platform.flow.StageInfoDO;
import com.alibaba.china.talos.platform.flow.stage.StageStatus;
import com.alibaba.fastjson.JSON;

/**
 * stageinfo transfer's sample
 */
public class StageInfoTransfer {

    private static final SmallLogger logger              = new SmallLogger("stage_info_transfer.log");

    AtomAuthInstanceDAO              atomAuthInstanceDAO = Daoer.getDAO(AtomAuthInstanceDAO.class);
    AuthAuditRequestDAO              authAuditRequestDAO = Daoer.getDAO(AuthAuditRequestDAO.class);
    StageInfoDAO                     stageInfoDAO        = Daoer.getDAO(StageInfoDAO.class);
    TransactionTemplate              transactionTemplate = Daoer.getTransactionTemplate();

    void handleStageInfo(String packageCode, Long atomAuthId, String stageName) {
        if (stageInfoDAO.getByAtomAuthId(atomAuthId) == null) {
            StageInfoDO stageInfoDO = new StageInfoDO();
            stageInfoDO.setAtomAuthId(atomAuthId);
            stageInfoDO.setAuthFlowName(packageCode);
            stageInfoDO.setIsDeleted("n");
            stageInfoDO.setStageName(stageName);
            stageInfoDO.setStageStatus(StageStatus.RUNNING.name());
            stageInfoDAO.insert(stageInfoDO);
        } else {
            logger.error("[" + packageCode + "] stageInfo for atomAuthId<" + atomAuthId + "> already existed, skipped.");
        }
    }

    void handleForAudit(String packageCode, Long atomAuthId) {
        AuthAuditRequestDO auditRequestDO = authAuditRequestDAO.findByAtomAuthId(atomAuthId);
        if (auditRequestDO != null) {
            Date auditTime = auditRequestDO.getAuditTime();
            String auditStatus = auditRequestDO.getStatus();
            if ((!StringUtils.equals(auditStatus, "SUCCESS")) && (!StringUtils.equals(auditStatus, "FAIL"))) {
                if (auditTime != null) {
                    logger.info("[" + packageCode + "] atomAuthId<" + atomAuthId + "> readonly<"
                                + Configer.isReadonly() + "> transfter AuthStageInfo SecondAudit.");
                    if (!Configer.isReadonly()) {
                        handleStageInfo(packageCode, atomAuthId, "SecondAudit");
                    }
                } else {
                    logger.info("[" + packageCode + "] atomAuthId<" + atomAuthId + "> readonly<"
                                + Configer.isReadonly() + "> transfter AuthStageInfo FirstAudit.");
                    if (!Configer.isReadonly()) {
                        handleStageInfo(packageCode, atomAuthId, "FirstAudit");
                    }
                }
            } else {
                logger.error("[" + packageCode + "] atomAuthId<" + atomAuthId + "> readonly<" + Configer.isReadonly()
                             + "> auditStatus<" + auditStatus + "> failed to transfter .");
            }
        }
    }

    static final String REMIT_STATUS_URL = "http://10.125.192.217:7001/auth/bankRemitStatus/status.jsonp?atomAuthId=";

    void handleForBankRemit(Long atomAuthId) {
        HttpClient httpClient = new HttpClient();
        PostMethod method = new PostMethod(REMIT_STATUS_URL + atomAuthId);
        try {
            int status = httpClient.executeMethod(method);
            if (status >= 300 || status < 200) {
                throw new RuntimeException("[BANKREMIT] failed to get remitStatus for atomAuthId<" + atomAuthId
                                           + "> and response<" + method.getResponseBodyAsString() + ">.");
            }
            StringBuffer contentBuffer = new StringBuffer();
            InputStream inputStream = method.getResponseBodyAsStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, method.getResponseCharSet()));
            String inputLine = null;
            while ((inputLine = reader.readLine()) != null) {
                contentBuffer.append(inputLine);
                contentBuffer.append("\n");
            }
            inputStream.close();
            String result = contentBuffer.toString();
            int firstIndex = result.indexOf('{');
            int lastIndex = result.lastIndexOf('}');
            if (firstIndex != -1 && lastIndex != -1 && firstIndex < lastIndex) {
                result = result.substring(firstIndex, lastIndex + 1);
                String remitStatus = (String) JSON.parseObject(result, Map.class).get("remitStatus");
                logger.info("[BANKREMIT] atomAuthId<" + atomAuthId + ">'s remitStatus<" + remitStatus + ">.");
                if (!Configer.isReadonly()) {
                    CNAProcessStatusEnum processStatus = CNAProcessStatusEnum.valueOf(remitStatus);
                    String stageName = null;
                    switch (processStatus) {
                        case CNAProcessStatusEnum.INIT:
                        case CNAProcessStatusEnum.WAIT_REMIT:
                            stageName = "WaitRemit";
                            break;
                        case CNAProcessStatusEnum.REMIT_FAIL:
                        case CNAProcessStatusEnum.SUCCESS:
                            logger.error("[BANKREMIT] atomAuthId<" + atomAuthId
                                         + ">'s status is process but remitStatus<" + remitStatus + ">.");
                            break;
                        case CNAProcessStatusEnum.WAIT_1ST_CONFIRM:
                            stageName = "WaitFirstConfirm";
                            break;
                        case CNAProcessStatusEnum.WAIT_2ND_CONFIRM:
                            stageName = "WaitSecondConfirm";
                            break;
                        case CNAProcessStatusEnum.CONFIRM_FAIL:
                            stageName = "RemitTbd";
                            break;
                    }
                    if (stageName != null) {
                        handleStageInfo("BANKREMIT", atomAuthId, stageName);
                    }
                }
            }
        } catch (Throwable e) {
            logger.error("[BANKREMIT] failed to get remitStatus for atomAuthId<" + atomAuthId + "> : " + e.getMessage(),
                         e);
            throw new RuntimeException("[BANKREMIT] failed to get remitStatus for atomAuthId<" + atomAuthId + ">",
                                       e.getCause());
        }
    }

    void handle(Long atomAuthId) {
        AtomAuthInstanceDO atomAuthInstanceDO = atomAuthInstanceDAO.findAtomAuthInstanceDOById(atomAuthId);
        if (atomAuthInstanceDO == null) {
            logger.error("[EXECUTE] could not found atomAuthInstance for atomAuthId<" + atomAuthId + ">, continue ");
            return;
        }
        String status = atomAuthInstanceDO.getStatus();
        String resultStatus = atomAuthInstanceDO.getResultStatus();
        String packageCode = atomAuthInstanceDO.getPackageCode();
        if (StringUtils.equals(status, "process") && StringUtils.isBlank(resultStatus)) {
            if (StringUtils.equals(packageCode, "BRAND") || StringUtils.equals(packageCode, "MULTICERTS")) {
                try {
                    handleForAudit(packageCode, atomAuthId);
                } catch (Throwable e) {
                    logger.error("[" + packageCode + "] failed to handleForAudit for atomAuthId<" + atomAuthId
                                 + "> readonly<" + Configer.isReadonly() + "> : " + e.getMessage(), e);
                    throw new RuntimeException("[" + packageCode + "] failed to handleForAudit for atomAuthId<"
                                               + atomAuthId + "> readonly<" + Configer.isReadonly() + ">", e.getCause());
                }
            } else if (StringUtils.equals(packageCode, "BANKREMIT")) {
                try {
                    handleForBankRemit(atomAuthId);
                } catch (Throwable e) {
                    logger.error("[" + packageCode + "] failed to handleForBankRemit for atomAuthId<" + atomAuthId
                                 + "> readonly<" + Configer.isReadonly() + "> : " + e.getMessage(), e);
                    throw new RuntimeException("[" + packageCode + "] failed to handleForBankRemit for atomAuthId<"
                                               + atomAuthId + "> readonly<" + Configer.isReadonly() + ">", e.getCause());
                }
            }
        } else {
            logger.info("[" + packageCode + "] atomAuthId<" + atomAuthId + "> readonly<" + Configer.isReadonly()
                        + "> status<" + status + "> resultStatus<" + resultStatus + ">, skipped.");
        }
    }

    public void execute(final List<String> atomAuthIds) {
        if (atomAuthIds == null || atomAuthIds.isEmpty()) {
            return;
        }
        transactionTemplate.execute([
            doInTransaction : { TransactionStatus status ->
                for (String atomAuthId : atomAuthIds) {
                    if (StringUtils.isBlank(atomAuthId)) {
                        continue;
                    }
                    try {
                        handle(Long.valueOf(atomAuthId));
                    } catch (Throwable e) {
                        logger.error("[EXECUTE] failed to handle for atomAuthId<" + atomAuthId + "> readonly<"
                                     + Configer.isReadonly() + "> : " + e.getMessage(), e);
                        throw new RuntimeException("[EXECUTE] failed to handle for atomAuthId<" + atomAuthId
                                                   + "> readonly<" + Configer.isReadonly() + ">", e.getCause());
                    }
                }
                return null;
            }
        ] as TransactionCallback)
    }
}
