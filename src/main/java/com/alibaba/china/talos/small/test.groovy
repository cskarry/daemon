package com.alibaba.china.talos.small;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;
import java.util.Map;

import java.sql.ResultSet;

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

import org.springframework.jdbc.core.RowMapper;
import com.alibaba.china.talos.dal.dataobject.platform.AtomAuthInstanceDO;

import com.alibaba.china.shared.talos.authdata.model.AuthDataItemMappingResult;
import com.alibaba.china.shared.talos.authdata.model.AuthDataItemPool;
import com.alibaba.china.shared.talos.authdata.util.AuthDataItemMappingUtil;
import com.alibaba.china.shared.talos.authdata.util.DefaultNameMapper;
import com.alibaba.china.shared.talos.authdata.util.NameMapper;
import com.alibaba.china.shared.talos.platform.constants.AuthStatusEnum;
import com.alibaba.china.shared.talos.platform.model.AuthOrder;
import com.alibaba.china.shared.talos.product.av.enums.CompanyType;
import com.alibaba.china.shared.talos.product.common.util.AuditInfoJsonUtil;
import com.alibaba.china.shared.talos.product.multicerts.model.BusinessLicenseAuditInfoObject;
import com.alibaba.china.shared.talos.product.multicerts.model.IdCardAuditInfoObject;
import com.alibaba.china.shared.talos.product.multicerts.model.ImageAuditInfoObject;
import com.alibaba.china.shared.talos.result.service.AuthTargetService;
import com.alibaba.china.talos.daemon.BaseAO;
import com.alibaba.china.talos.dal.dao.platform.AuthDataItemDAO;
import com.alibaba.china.talos.dal.dao.platform.AuthDataItemRelationDAO;
import com.alibaba.china.talos.dal.dao.platform.AuthDataItemStatusDAO;
import com.alibaba.china.talos.dal.dataobject.av.AvProcessInfoDO;
import com.alibaba.china.talos.dal.dataobject.platform.AtomAuthInstanceDO;
import com.alibaba.china.talos.dal.dataobject.platform.AuthDataItemDO;
import com.alibaba.china.talos.dal.dataobject.platform.AuthDataItemRelationDO;
import com.alibaba.china.talos.dal.dataobject.platform.AuthDataItemStatusDO;
import com.alibaba.common.logging.Logger;
import com.alibaba.common.logging.LoggerFactory;
import com.taobao.tddl.client.sequence.exception.SequenceException;

public class Test {

    private static final SmallLogger logger              = new SmallLogger("auth_data_item_transfer.log");

    AtomAuthInstanceDAO              atomAuthInstanceDAO = Daoer.getDAO(AtomAuthInstanceDAO.class);
    AuthAuditRequestDAO              authAuditRequestDAO = Daoer.getDAO(AuthAuditRequestDAO.class);
    StageInfoDAO                     stageInfoDAO        = Daoer.getDAO(StageInfoDAO.class);
    JdbcTemplate              		 jdbcTemplate 		 = Daoer.getDAO(JdbcTemplate.class);

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

    void handleForAudit(String packageCode, Long atomAuthId, boolean readonly) {
        AuthAuditRequestDO auditRequestDO = authAuditRequestDAO.findByAtomAuthId(atomAuthId);
        if (auditRequestDO != null) {
            Date auditTime = auditRequestDO.getAuditTime();
            String auditStatus = auditRequestDO.getStatus();
            if ((!StringUtils.equals(auditStatus, "SUCCESS")) && (!StringUtils.equals(auditStatus, "FAIL"))) {
                if (auditTime != null) {
                    logger.info("[" + packageCode + "] atomAuthId<" + atomAuthId + "> readonly<" + readonly
                                + "> transfter AuthStageInfo SecondAudit.");
                    if (!readonly) {
                        handleStageInfo(packageCode, atomAuthId, "SecondAudit");
                    }
                } else {
                    logger.info("[" + packageCode + "] atomAuthId<" + atomAuthId + "> readonly<" + readonly
                                + "> transfter AuthStageInfo FirstAudit.");
                    if (!readonly) {
                        handleStageInfo(packageCode, atomAuthId, "FirstAudit");
                    }
                }
            } else {
                logger.error("[" + packageCode + "] atomAuthId<" + atomAuthId + "> readonly<" + readonly
                             + "> auditStatus<" + auditStatus + "> failed to transfter .");
            }
        }
    }

    static final String REMIT_STATUS_URL = "http://r-test.1688.com:7001/auth/bankRemitStatus/status.jsonp?atomAuthId=";

    void handleForBankRemit(Long atomAuthId, boolean readonly) {
    }

    void handle(Long atomAuthId, boolean readonly) {
        AtomAuthInstanceDO atomAuthInstanceDO = atomAuthInstanceDAO.findAtomAuthInstanceDOById(atomAuthId);
        if(atomAuthInstanceDO == null) {
            logger.error("[EXECUTE] could not found atomAuthInstance for atomAuthId<" + atomAuthId + ">, continue ");
            return;
        }
        String status = atomAuthInstanceDO.getStatus();
        String resultStatus = atomAuthInstanceDO.getResultStatus();
        String packageCode = atomAuthInstanceDO.getPackageCode();
        if (StringUtils.equals(status, "process") && StringUtils.isBlank(resultStatus)) {
            if (StringUtils.equals(packageCode, "BRAND") || StringUtils.equals(packageCode, "MULTICERTS")) {
                try {
                    handleForAudit(packageCode, atomAuthId, readonly);
                } catch (Throwable e) {
                    logger.error("[" + packageCode + "] failed to handleForAudit for atomAuthId<" + atomAuthId
                                 + "> readonly<" + readonly + "> : " + e.getMessage(), e);
                    throw new RuntimeException("[" + packageCode + "] failed to handleForAudit for atomAuthId<"
                                               + atomAuthId + "> readonly<" + readonly + ">", e.getCause());
                }
            } else if (StringUtils.equals(packageCode, "BANKREMIT")) {
                try {
                    handleForBankRemit(atomAuthId, readonly);
                } catch (Throwable e) {
                    logger.error("[" + packageCode + "] failed to handleForBankRemit for atomAuthId<" + atomAuthId
                                 + "> readonly<" + readonly + "> : " + e.getMessage(), e);
                    throw new RuntimeException("[" + packageCode + "] failed to handleForBankRemit for atomAuthId<"
                                               + atomAuthId + "> readonly<" + readonly + ">", e.getCause());
                }
            }
        } else {
            logger.info("[" + packageCode + "] atomAuthId<" + atomAuthId + "> readonly<" + readonly + "> status<"
                        + status + "> resultStatus<" + resultStatus + ">, skipped.");
        }
    }

    public void execute(final List<String> authOrderIds, final boolean readonly) {
        if (authOrderIds == null || authOrderIds.isEmpty()) {
        	logger.error("[EXECUTE] authOrderIds is empty!!");
            return;
        }
        List<AtomAuthInstanceDO> atomAuthInstanceList = jdbcTemplate.query("select id, package_code from atom_auth_instance "
                                                                            + " where order_id=?",
                                                                            [ authOrder.getId() ],
                                                                            buildAtomAuthInstanceRowMapper());
        for (AtomAuthInstanceDO atomAuthInstanceDO : atomAuthInstanceList) {
        /*
            dealWithBrandAtomAuth(atomAuthInstanceDO, authDataItemRelationDO);
            dealWithMultiCertsAtomAuth(atomAuthInstanceDO);
            dealWithOrganicAtomAuth(atomAuthInstanceDO);
            dealWithBankRemitAtomAuth(atomAuthInstanceDO);
         */
            dealWithAvProcessInfoAtomAuth(atomAuthInstanceDO, authDataItemRelationDO);
        }
    }

    /**
     * @param atomAuthInstanceDO
     * @param authDataItemRelationDO
     */
    private void dealWithAvProcessInfoAtomAuth(AtomAuthInstanceDO atomAuthInstanceDO,
                                               AuthDataItemRelationDO authDataItemRelationDO) {
        if (!StringUtils.equals("AV", atomAuthInstanceDO.getPackageCode())) {
            return;
        }
        List<AvProcessInfoDO> avProcessInfoList = jdbcTemplate.query("select * from av_process_info i where i.is_deleted='n' "
                                                                             + " and r.atom_auth_id=?",
                                                                     [ atomAuthInstanceDO.getId() ],
                                                                     buildAvProcessInfoRowMapper());

        if (avProcessInfoList == null || avProcessInfoList.size() > 0) {
            logger.warn("no data found, param: atomAuthInstanceDO.getId()=" + atomAuthInstanceDO.getId());
            return;
        }

        AvProcessInfoDO avProcessInfo = avProcessInfoList.get(0);
        List<AuthDataItemDO> authDataItemList = mappingToAuthDataItemFromAvProcessInfo(avProcessInfo,
                                                                                       authDataItemRelationDO);

        Integer insertRecordsInBatch = authDataItemDAO.insertRecordsInBatch(authDataItemList);
        logger.info("param: authDataItemRelationDO.relatedBizId=" + authDataItemRelationDO.getRelatedBizId()
                 + ", authDataItemRelationDO.poolId=" + authDataItemRelationDO.getDataPoolId()
                 + ", result: insertRecordsInBatch=" + insertRecordsInBatch);
    }

    /**
     * @param avProcessInfo
     * @param authDataItemRelationDO
     * @return
     */
    private List<AuthDataItemDO> mappingToAuthDataItemFromAvProcessInfo(AvProcessInfoDO avProcessInfo,
                                                                        AuthDataItemRelationDO authDataItemRelationDO) {
        NameMapper nameMapper = new DefaultNameMapper() //
        .setPojoMaptoItem("regCode", "licenseNumber").setPojoMaptoItem("regExpireDate", "licenseEndYear") //
        .setPojoMaptoItem("memberCardId", "memberCertNum").setPojoMaptoItem("isCompanyPrincipal",
                                                                            "memberIsCompanyPrinciple");
        AuthDataItemMappingResult<AuthDataItemPool> result = AuthDataItemMappingUtil.convertPojo2DataItem(nameMapper,
                                                                                                          avProcessInfo);
        AuthDataItemPool pool = result.getReturnValue();
        pool.addDataItem("companyType", convertCompanyType(avProcessInfo.getCompanyType()));
        pool.addDataItem("memberIsCompanyPrinciple", convertIsCompanyPrinciple(avProcessInfo.getIsCompanyPrincipal()));

        List<AuthDataItemDO> authDataItemDOList = new ArrayList<AuthDataItemDO>();
        Set<Entry<String, String>> set = pool.getDataItems();
        for (Entry<String, String> entry : set) {
            AuthDataItemDO authDataItemDO = new AuthDataItemDO();
            authDataItemDO.setDataPoolId(authDataItemRelationDO.getDataPoolId());
            authDataItemDO.setPoolVersion(authDataItemRelationDO.getPoolVersion());
            authDataItemDO.setItemName(entry.getKey());
            authDataItemDO.setItemValue(entry.getValue());
            authDataItemDOList.add(authDataItemDO);
        }

        return authDataItemDOList;
    }

    private String convertIsCompanyPrinciple(String isCompanyPrincipal) {
        if (StringUtils.equals("y", isCompanyPrincipal)) {
            return Boolean.TRUE.toString();
        } else {
            return Boolean.FALSE.toString();
        }
    }

    private String convertCompanyType(String companyType) {
        if (StringUtils.equals("PNN", companyType)) {
            return CompanyType.IndividualBusinessNoneShopName.name();
        } else {
            return CompanyType.Normal.name();
        }
    }

    private RowMapper buildAtomAuthInstanceRowMapper() {
        return new RowMapper() {

            public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
                AtomAuthInstanceDO atomAuthInstanceDO = new AtomAuthInstanceDO();
                atomAuthInstanceDO.setId(rs.getLong("id"));
                atomAuthInstanceDO.setPackageCode(rs.getString("package_code"));
                return atomAuthInstanceDO;
            }
        };
    }
}

