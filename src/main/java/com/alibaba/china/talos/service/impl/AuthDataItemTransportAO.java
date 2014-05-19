package com.alibaba.china.talos.service.impl;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.alibaba.china.shared.talos.authdata.model.AuthDataItemMappingResult;
import com.alibaba.china.shared.talos.authdata.model.AuthDataItemPool;
import com.alibaba.china.shared.talos.authdata.util.AuthDataItemMappingUtil;
import com.alibaba.china.shared.talos.authdata.util.DefaultNameMapper;
import com.alibaba.china.shared.talos.authdata.util.NameMapper;
import com.alibaba.china.shared.talos.platform.constants.AtomAuthEnum;
import com.alibaba.china.shared.talos.platform.constants.AuthStatusEnum;
import com.alibaba.china.shared.talos.platform.model.AuthOrder;
import com.alibaba.china.shared.talos.product.av.enums.CompanyType;
import com.alibaba.china.shared.talos.product.brand.model.BrandAuditInfoObject;
import com.alibaba.china.shared.talos.product.common.util.AuditInfoJsonUtil;
import com.alibaba.china.shared.talos.product.multicerts.model.BusinessLicenseAuditInfoObject;
import com.alibaba.china.shared.talos.product.multicerts.model.IdCardAuditInfoObject;
import com.alibaba.china.shared.talos.product.multicerts.model.ImageAuditInfoObject;
import com.alibaba.china.talos.dal.dao.av.AvProcessInfoDAO;
import com.alibaba.china.talos.dal.dao.av.AvResponseResultDAO;
import com.alibaba.china.talos.dal.dao.platform.AtomAuthInstanceDAO;
import com.alibaba.china.talos.dal.dao.platform.AuthDataItemDAO;
import com.alibaba.china.talos.dal.dao.platform.AuthDataItemRelationDAO;
import com.alibaba.china.talos.dal.dao.platform.AuthDataItemStatusDAO;
import com.alibaba.china.talos.dal.dataobject.av.AvProcessInfoDO;
import com.alibaba.china.talos.dal.dataobject.av.AvResponseResultDO;
import com.alibaba.china.talos.dal.dataobject.bankremit.AuthBankRemitApplyDO;
import com.alibaba.china.talos.dal.dataobject.platform.AtomAuthInstanceDO;
import com.alibaba.china.talos.dal.dataobject.platform.AuthDataItemDO;
import com.alibaba.china.talos.dal.dataobject.platform.AuthDataItemRelationDO;
import com.alibaba.china.talos.dal.dataobject.platform.AuthDataItemStatusDO;
import com.alibaba.china.talos.dal.param.av.AvProcessInfoParam;
import com.alibaba.china.talos.dal.param.av.AvResponseResultParam;
import com.alibaba.china.talos.dal.param.platform.AtomAuthInstanceParam;
import com.alibaba.china.talos.platform.dataitem.constants.AuthDataItemErrorCodeEnum;
import com.alibaba.china.talos.platform.dataitem.constants.AuthDataItemStatusEnum;
import com.alibaba.china.talos.platform.dataitem.exceptions.AuthDataItemException;
import com.alibaba.china.talos.platform.dataitem.service.inner.AuthDataDefinitionService;
import com.alibaba.china.talos.platform.product.item.AuthItem;
import com.alibaba.common.logging.Logger;
import com.alibaba.common.logging.LoggerFactory;
import com.taobao.tddl.client.sequence.exception.SequenceException;

/**
 * 类AuthDataItemTransportAO.java的实现描述：auth_data_item, auth_data_item_relation, auth_data_item_status 数据迁移任务
 *
 * @author guokai Feb 11, 2014 3:16:50 PM
 */
public class AuthDataItemTransportAO {

    private static final Logger       log = LoggerFactory.getLogger(AuthDataItemTransportAO.class);

    private AuthDataItemDAO           authDataItemDAO;
    private AuthDataItemRelationDAO   authDataItemRelationDAO;
    private AuthDataItemStatusDAO     authDataItemStatusDAO;
    private JdbcTemplate              jdbcTemplate;
    /**
     * 分表分库后的jdbcTemplate
     */
    private JdbcTemplate              seperateDbJdbcTemplate;

    private AuthDataDefinitionService authDataDefinitionService;

    @SuppressWarnings("deprecation")
    private AvProcessInfoDAO          avProcessInfoDAO;
    private AvResponseResultDAO       avResponseResultDAO;

    private AtomAuthInstanceDAO       atomAuthInstanceDAO;

    public void setAuthDataItemDAO(AuthDataItemDAO authDataItemDAO) {
        this.authDataItemDAO = authDataItemDAO;
    }

    public void setAuthDataItemRelationDAO(AuthDataItemRelationDAO authDataItemRelationDAO) {
        this.authDataItemRelationDAO = authDataItemRelationDAO;
    }

    public void setAuthDataItemStatusDAO(AuthDataItemStatusDAO authDataItemStatusDAO) {
        this.authDataItemStatusDAO = authDataItemStatusDAO;
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setSeperateDbJdbcTemplate(JdbcTemplate seperateDbJdbcTemplate) {
        this.seperateDbJdbcTemplate = seperateDbJdbcTemplate;
    }

    public void setAuthDataDefinitionService(AuthDataDefinitionService authDataDefinitionService) {
        this.authDataDefinitionService = authDataDefinitionService;
    }

    @SuppressWarnings("deprecation")
    public void setAvProcessInfoDAO(AvProcessInfoDAO avProcessInfoDAO) {
        this.avProcessInfoDAO = avProcessInfoDAO;
    }

    public void setAvResponseResultDAO(AvResponseResultDAO avResponseResultDAO) {
        this.avResponseResultDAO = avResponseResultDAO;
    }

    public void setAtomAuthInstanceDAO(AtomAuthInstanceDAO atomAuthInstanceDAO) {
        this.atomAuthInstanceDAO = atomAuthInstanceDAO;
    }

    static final int STEP = 300;

    @SuppressWarnings("unchecked")
    public void execute(List<String> authOrderIdList) {
        log.info("[AuthDataItemTransportAO.execute(authOrderIdList)] start");
        if (authOrderIdList == null || authOrderIdList.isEmpty()) {
            log.warn("[AuthDataItemTransportAO.execute] authOrderIdList is empty");
        }

        List<AuthOrder> authOrderList = jdbcTemplate.query("select id, package_code, target_id, status from auth_order where id in ( "
                                                                   + StringUtils.join(authOrderIdList, ',') + ")",
                                                           buildAuthOrderRowMapper());
        if (authOrderList == null || authOrderList.isEmpty()) {
            log.warn("[AuthDataItemTransportAO.execute] authOrderIdRange: authOrderId is "
                     + StringUtils.join(authOrderIdList, ','));
            return;
        }

        Long lastAuthOrderIdInThisBatch = authOrderList.get(authOrderList.size() - 1).getId();
        log.info("[AuthDataItemTransportAO.execute] authOrderIdRange: begin=" + authOrderList.get(0).getId() + ", end="
                 + lastAuthOrderIdInThisBatch);

        dealWithAuthOrderList(authOrderList);
        log.info("[AuthDataItemTransportAO.execute(authOrderIdList)] end");
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void execute() {
        log.info("[AuthDataItemTransportAO.execute()] start");
        Map minAndMaxAuthOrderId = jdbcTemplate.queryForMap("select min(id) min_id, max(id) max_id from auth_order");
        Long minAuthOrderId = ((BigInteger) minAndMaxAuthOrderId.get("min_id")).longValue();
        Long maxAuthOrderId = ((BigInteger) minAndMaxAuthOrderId.get("max_id")).longValue();

        log.info("[AuthDataItemTransportAO.execute()] minAuthOrderId="+minAuthOrderId+", maxAuthOrderId="+maxAuthOrderId);

        long currentAuthOrderId = minAuthOrderId;

        while (currentAuthOrderId <= maxAuthOrderId) {
            List<AuthOrder> authOrderList = jdbcTemplate.query("select id, package_code, target_id, status from auth_order where id>=? order by id limit "
                                                                       + STEP, new Long[] { currentAuthOrderId },
                                                               buildAuthOrderRowMapper());
            if (authOrderList == null || authOrderList.isEmpty()) {
                log.warn("[AuthDataItemTransportAO.execute] authOrderIdRange: begin=" + currentAuthOrderId);
                continue;
            }

            Long lastAuthOrderIdInThisBatch = authOrderList.get(authOrderList.size() - 1).getId();
            log.info("[AuthDataItemTransportAO.execute] authOrderIdRange: begin=" + authOrderList.get(0).getId()
                     + ", end=" + lastAuthOrderIdInThisBatch);

            dealWithAuthOrderList(authOrderList);

            currentAuthOrderId = lastAuthOrderIdInThisBatch + 1;
        }
        log.info("[AuthDataItemTransportAO.execute()] end");
    }

    /**
     * @param authOrderList
     */
    @SuppressWarnings({ "deprecation" })
    private void dealWithAuthOrderList(List<AuthOrder> authOrderList) {
        for (AuthOrder authOrder : authOrderList) {
            log.info("[AuthDataItemTransportAO.execute] authOrderId: " + authOrder.getId());
            AuthDataItemRelationDO authDataItemRelationDO = dealWithAuthDataItemRelation(authOrder);
            AuthDataItemStatusDO insertedStatusDO = dealWithAuthDataItemStatus(authOrder, authDataItemRelationDO);
            if (insertedStatusDO == null) {
                log.warn("[AuthDataItemTransportAO.execute] insertedStatusDO is null, param: authOrder.getId()="
                         + authOrder.getId());
                continue;
            }

            AtomAuthInstanceParam atomAuthInstanceParam = new AtomAuthInstanceParam();
            atomAuthInstanceParam.setOrderId(authOrder.getId());
            List<AtomAuthInstanceDO> atomAuthInstanceList = atomAuthInstanceDAO.findAtomAuthInstanceDOByParam(atomAuthInstanceParam);

            // List<AtomAuthInstanceDO> atomAuthInstanceList =
            // jdbcTemplate.query("select id, package_code from atom_auth_instance "
            // + " where order_id=?",
            // new Long[] { authOrder.getId() },
            // buildAtomAuthInstanceRowMapper());

            AuthDataItemPool pool = new AuthDataItemPool();
            AuthDataItemPool poolBrand = new AuthDataItemPool();
            AuthDataItemPool poolMC = new AuthDataItemPool();
            AuthDataItemPool poolBR = new AuthDataItemPool();
            AuthDataItemPool poolAV = new AuthDataItemPool();
            for (AtomAuthInstanceDO atomAuthInstanceDO : atomAuthInstanceList) {
                if (atomAuthInstanceDO.getPackageCode() == null) {
                    log.warn("atomAuthId <" + atomAuthInstanceDO.getId() + "> has no packageCode!!");
                    continue;
                }
                AtomAuthEnum atomAuth = AtomAuthEnum.valueOf(atomAuthInstanceDO.getPackageCode());
                switch (atomAuth) {
                    case AV:
                        poolAV = dealWithAvProcessInfoAtomAuth(atomAuthInstanceDO, authDataItemRelationDO);
                        AuthDataItemPool poolAV2 = dealWithAvResponseResultAtomAuth(atomAuthInstanceDO,
                                                                                    authDataItemRelationDO);
                        poolAV = AuthDataItemMappingUtil.merge(poolAV, poolAV2);
                        break;
                    case BANKREMIT:
                        poolBR = dealWithBankRemitAtomAuth(atomAuthInstanceDO);
                        break;
                    case BRAND:
                        poolBrand = dealWithBrandAtomAuth(atomAuthInstanceDO, authDataItemRelationDO);
                        break;
                    case MULTICERTS:
                        poolMC = dealWithMultiCertsAtomAuth(atomAuthInstanceDO);
                        break;
                    case ORGANIC:
                        // 农场没有接入，所以暂时没有数据，不用考虑迁移
                        // AuthDataItemPool poolOrganic = dealWithOrganicAtomAuth(atomAuthInstanceDO);
                        // pool = AuthDataItemMappingUtil.merge(pool, poolOrganic);
                        break;
                }
            }
            pool = AuthDataItemMappingUtil.merge(pool, poolBrand);
            pool = AuthDataItemMappingUtil.merge(pool, poolMC);
            pool = AuthDataItemMappingUtil.merge(pool, poolBR);
            pool = AuthDataItemMappingUtil.merge(pool, poolAV);

            try {
                filterDataItemNotInDefinition(pool, authOrder.getPackageCode());
            } catch (AuthDataItemException e) {
                log.error("invoke filterDataItemNotInDefinition error, param: authOrder.getPackageCode()="
                                  + authOrder.getPackageCode(), e);
            }

            Integer insertRecordsInBatch = authDataItemDAO.insertRecordsInBatch(convertPoolToDataItemList(pool,
                                                                                                          authDataItemRelationDO));
            log.info("param: authDataItemRelationDO.relatedBizId=" + authDataItemRelationDO.getRelatedBizId()
                     + ", authDataItemRelationDO.poolId=" + authDataItemRelationDO.getDataPoolId()
                     + ", result: insertRecordsInBatch=" + insertRecordsInBatch);
        }
    }

    /**
     * @param mergedDataItemPool
     * @param authOrderId
     * @throws AuthDataItemException
     */
    private void filterDataItemNotInDefinition(AuthDataItemPool mergedDataItemPool, String authOrderPackageCode)
                                                                                                                throws AuthDataItemException {
        System.out.println("authOrderPackageCode=" + authOrderPackageCode);
        List<AuthItem> authItemDefList = authDataDefinitionService.getDataItemDefinitionListFromProductConfig(authOrderPackageCode);

        List<String> authItemNames = new ArrayList<String>();

        for (AuthItem authItem : authItemDefList) {
            authItemNames.add(authItem.getName());
        }

        Collections.sort(authItemNames);

        Set<Entry<String, String>> dataItems = mergedDataItemPool.getDataItems();
        for (Iterator<Entry<String, String>> iterator = dataItems.iterator(); iterator.hasNext();) {
            Entry<String, String> entry = (Entry<String, String>) iterator.next();
            if (authItemNames.contains(entry.getKey())) {
                continue;
            }

            System.out.println(entry.getKey());
            iterator.remove();
        }

        if (mergedDataItemPool.getDataItems() == null || mergedDataItemPool.getDataItems().size() == 0) {
            throw new AuthDataItemException(
                                            AuthDataItemErrorCodeEnum.POJO_HAS_NO_DATA_INCLUDE_IN_DEFINITION_ERROR.name());
        }
    }

    // /**
    // * @param authOrderList
    // * @return
    // */
    // private List<AuthDataItemRelationDO> batchAuthDataItemRelation(final List<AuthOrder> authOrderList) {
    // final IbatisAuthDataItemRelationDAO authDataItemRelationDAOImpl = ((IbatisAuthDataItemRelationDAO)
    // authDataItemRelationDAO);
    // authDataItemRelationDAOImpl.getSqlMapClientTemplate().execute(new SqlMapClientCallback() {
    //
    // @Override
    // public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
    // executor.startBatch();
    // for (AuthOrder authOrder : authOrderList) {
    // String relatedBizId = "" + authOrder.getId();
    //
    // AuthDataItemRelationDO authDataItemRelationParam = new AuthDataItemRelationDO();
    // authDataItemRelationParam.setRelatedBizId(relatedBizId);
    // List<AuthDataItemRelationDO> list =
    // authDataItemRelationDAO.getAuthDataItemRelationList(authDataItemRelationParam);
    // if (list != null && !list.isEmpty()) {
    // // Integer deleteCountByRelatedBizId =
    // // authDataItemRelationDAO.deleteByRelatedBizId(relatedBizId);
    // Integer deleteCountByRelatedBizId = executor.update("authDataItemRelation.delete", relatedBizId);
    // log.info("invoke authDataItemRelationDAO.deleteByRelatedBizId, param: relatedBizId="
    // + relatedBizId + ", result: deleteCountByRelatedBizId=" + deleteCountByRelatedBizId);
    // }
    //
    // AuthDataItemRelationDO authDataItemRelationDO = new AuthDataItemRelationDO();
    // authDataItemRelationDO.setRelatedBizId(relatedBizId);
    // try {
    // // authDataItemRelationDAO.insertAuthDataItemRelation(authDataItemRelationDO);
    //
    // if (authDataItemRelationDO.getId() == null) {
    // authDataItemRelationDO.setId(authDataItemRelationDAOImpl.getTalosSequence().next());
    // }
    // if (authDataItemRelationDO.getDataPoolId() == null) {
    // authDataItemRelationDO.setDataPoolId(authDataItemRelationDAOImpl.getAnotherTalosSequence(SequenceNames.AUTH_DATA_ITEM_POOL_SEQUENCE_KEY).next());
    // }
    // executor.insert("authDataItemRelation.insert", authDataItemRelationDO);
    //
    // return authDataItemRelationDO;
    // } catch (Throwable e) {
    // log.error("invoke authDataItemRelationDAO.insertAuthDataItemRelation error, param: authOrder.getId()="
    // + relatedBizId, e);
    // }
    // }
    //
    // return Integer.valueOf(executor.executeBatch());
    // }
    // });
    //
    // return null;
    // }

    /**
     * @param atomAuthInstanceDO
     * @param authDataItemRelationDO
     */
    @SuppressWarnings({ "deprecation" })
    private AuthDataItemPool dealWithAvProcessInfoAtomAuth(AtomAuthInstanceDO atomAuthInstanceDO,
                                                           AuthDataItemRelationDO authDataItemRelationDO) {
        if (!StringUtils.equals("AV", atomAuthInstanceDO.getPackageCode())) {
            return new AuthDataItemPool();
        }

        AvProcessInfoParam avProcessInfoParam = new AvProcessInfoParam();
        avProcessInfoParam.setAtomAuthId(atomAuthInstanceDO.getId());
        List<AvProcessInfoDO> list = avProcessInfoDAO.findAvProcessInfoDOByParam(avProcessInfoParam);

        // List<AvProcessInfoDO> avProcessInfoList =
        // jdbcTemplate.query("select * from av_process_info i where i.is_deleted='n' " //
        // + " and i.atom_auth_id=?", //
        // new Long[] { atomAuthInstanceDO.getId() },
        // buildAvProcessInfoRowMapper());

        if (list == null || list.isEmpty()) {
            log.warn("no data found, param: atomAuthInstanceDO.getId()=" + atomAuthInstanceDO.getId());
            return new AuthDataItemPool();
        }

        AvProcessInfoDO avProcessInfo = list.get(0);
        return mappingToAuthDataItemFromAvProcessInfo(avProcessInfo, authDataItemRelationDO);
    }

    /**
     * FIXME 对AV反馈的结果进行处理，如果在原子认证成功的时候
     *
     * @param atomAuthInstanceDO
     * @param authDataItemRelationDO
     */
    private AuthDataItemPool dealWithAvResponseResultAtomAuth(AtomAuthInstanceDO atomAuthInstanceDO,
                                                              AuthDataItemRelationDO authDataItemRelationDO) {
        if (!StringUtils.equals("AV", atomAuthInstanceDO.getPackageCode())
            || !StringUtils.equals("end", atomAuthInstanceDO.getStatus())
            || !StringUtils.equals("success", atomAuthInstanceDO.getResultStatus())) {
            return new AuthDataItemPool();
        }

        AvResponseResultParam param = new AvResponseResultParam();
        param.setAtomAuthId(atomAuthInstanceDO.getId());
        List<AvResponseResultDO> list = avResponseResultDAO.findBy(param);
        if (list == null || list.isEmpty()) {
            log.warn("no data found, param: atomAuthInstanceDO.getId()=" + atomAuthInstanceDO.getId());
            return new AuthDataItemPool();
        }

        AvResponseResultDO avResponseResultDO = list.get(0);
        AuthDataItemMappingResult<AuthDataItemPool> result = AuthDataItemMappingUtil.convertPojo2DataItem(avResponseResultDO);
        if (result.isFailed()) {
            log.warn("convertPojo2DataItem av_response_result failed, param: atomAuthInstanceDO.getId()="
                     + atomAuthInstanceDO.getId());
        }
        return result.getReturnValue();
    }

    /**
     * @param avProcessInfo
     * @param authDataItemRelationDO
     * @return
     */
    @SuppressWarnings("deprecation")
    private AuthDataItemPool mappingToAuthDataItemFromAvProcessInfo(AvProcessInfoDO avProcessInfo,
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

        return pool;
    }

    private List<AuthDataItemDO> convertPoolToDataItemList(AuthDataItemPool pool,
                                                           AuthDataItemRelationDO authDataItemRelationDO) {
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

    /**
     * @param isCompanyPrincipal
     * @return
     */
    private String convertIsCompanyPrinciple(String isCompanyPrincipal) {
        if (StringUtils.equals("y", isCompanyPrincipal)) {
            return Boolean.TRUE.toString();
        } else {
            return Boolean.FALSE.toString();
        }
    }

    /**
     * @param companyType
     * @return
     */
    private String convertCompanyType(String companyType) {
        if (StringUtils.equals("PNN", companyType)) {
            return CompanyType.IndividualBusinessNoneShopName.name();
        } else {
            return CompanyType.Normal.name();
        }
    }

    // /**
    // * @return
    // */
    // private RowMapper buildAvProcessInfoRowMapper() {
    // return new RowMapper() {
    //
    // @SuppressWarnings("deprecation")
    // @Override
    // public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
    // AvProcessInfoDO avProcessInfo = new AvProcessInfoDO();
    //
    // // avProcessInfo.setAvType(rs.getString("av_type"));
    // // avProcessInfo.setAvOrderId(rs.getLong("av_order_id"));
    // // avProcessInfo.setRemainSrvDays(rs.getInt("remain_srv_days"));
    // // avProcessInfo.setRemark(rs.getString("remark"));
    // // avProcessInfo.setApplicant(rs.getString("applicant"));
    // // avProcessInfo.setApplicantOrg(rs.getInt("applicant_org"));
    // // avProcessInfo.setApproverOrgId(rs.getInt("approver_org_id"));
    // avProcessInfo.setTargetId(rs.getLong("target_id"));
    // avProcessInfo.setAtomAuthId(rs.getLong("atom_auth_id"));
    // // avProcessInfo.setApprover(rs.getString("approver"));
    // // avProcessInfo.setGmtApproved(rs.getDate("gmt_approved"));
    // // avProcessInfo.setGmtContact(rs.getDate("gmt_contact"));
    // // avProcessInfo.setAvTimes(rs.getInt("av_times"));
    // avProcessInfo.setCompanyNameCn(rs.getString("company_name_cn"));
    // avProcessInfo.setCompanyNameEn(rs.getString("company_name_en"));
    // avProcessInfo.setCompanyType(rs.getString("company_type"));
    // avProcessInfo.setCompanyCountry(rs.getString("company_country"));
    // avProcessInfo.setCompanyProvince(rs.getString("company_province"));
    // avProcessInfo.setCompanyCityEn(rs.getString("company_city_en"));
    // avProcessInfo.setCompanyCityCn(rs.getString("company_city_cn"));
    // avProcessInfo.setCompanyAreaEn(rs.getString("company_area_en"));
    // avProcessInfo.setCompanyAreaCn(rs.getString("company_area_cn"));
    // avProcessInfo.setCompanyAddrEn(rs.getString("company_addr_en"));
    // avProcessInfo.setCompanyAddrCn(rs.getString("company_addr_cn"));
    // avProcessInfo.setCompanyZip(rs.getString("company_zip"));
    // avProcessInfo.setMemberSex(rs.getString("member_sex"));
    // avProcessInfo.setMemberNameCn(rs.getString("member_name_cn"));
    // avProcessInfo.setMemberFirstName(rs.getString("member_first_name"));
    // avProcessInfo.setMemberLastName(rs.getString("member_last_name"));
    // avProcessInfo.setMemberPhoneCountry(rs.getString("member_phone_country"));
    // avProcessInfo.setMemberPhoneArea(rs.getString("member_phone_area"));
    // avProcessInfo.setMemberPhoneNumber(rs.getString("member_phone_number"));
    // avProcessInfo.setMemberPhoneExt(rs.getString("member_phone_ext"));
    // avProcessInfo.setMemberFaxCountry(rs.getString("member_fax_country"));
    // avProcessInfo.setMemberFaxArea(rs.getString("member_fax_area"));
    // avProcessInfo.setMemberFaxNumber(rs.getString("member_fax_number"));
    // avProcessInfo.setMemberJobTitleEn(rs.getString("member_job_title_en"));
    // avProcessInfo.setMemberJobTitleCn(rs.getString("member_job_title_cn"));
    // avProcessInfo.setMemberDeptEn(rs.getString("member_dept_en"));
    // avProcessInfo.setMemberDeptCn(rs.getString("member_dept_cn"));
    // avProcessInfo.setMemberEmail(rs.getString("member_email"));
    // avProcessInfo.setMemberCardId(rs.getString("member_card_id"));
    // avProcessInfo.setMemberCardType(rs.getString("member_card_type"));
    // avProcessInfo.setMemberBankName(rs.getString("member_bank_name"));
    // avProcessInfo.setMemberName(rs.getString("member_name"));
    // avProcessInfo.setMemberBankProvince(rs.getString("member_bank_province"));
    // avProcessInfo.setMemberBankCity(rs.getString("member_bank_city"));
    // avProcessInfo.setMemberBankAccount(rs.getString("member_bank_account"));
    // avProcessInfo.setMemberAlipayAccount(rs.getString("member_alipay_account"));
    // avProcessInfo.setMemberMobileNumber(rs.getString("member_mobile_number"));
    // avProcessInfo.setMemberIsInternalStaff(rs.getString("member_is_internal_staff"));
    // // avProcessInfo.setIsDeleted(rs.getString("is_deleted"));
    // avProcessInfo.setRegCode(rs.getString("reg_code"));
    // avProcessInfo.setRegExpireDate(rs.getDate("reg_expire_date"));
    // avProcessInfo.setCompanyPrincipal(rs.getString("company_principal"));
    // avProcessInfo.setIsCompanyPrincipal(rs.getString("is_company_principal"));
    // // avProcessInfo.setIsFree(rs.getString("is_free"));
    // // avProcessInfo.setCompanyPrincipalCardId(rs.getString("company_principal_card_id"));
    // // avProcessInfo.setCompanyPrincipalCardType(rs.getString("company_principal_card_type"));
    // return avProcessInfo;
    // }
    // };
    // }

    /**
     * @param authOrder
     * @param authDataItemRelationDO
     */
    @SuppressWarnings("unchecked")
    private AuthDataItemStatusDO dealWithAuthDataItemStatus(AuthOrder authOrder,
                                                            AuthDataItemRelationDO authDataItemRelationDO) {
        List<Long> list = seperateDbJdbcTemplate.queryForList("select id from "
                                                                      + getAuthDataItemStatusTableName(authDataItemRelationDO.getDataPoolId())
                                                                      + " where data_pool_id=?",
                                                              new Long[] { authDataItemRelationDO.getDataPoolId() },
                                                              Long.class);
        if (list != null && !list.isEmpty()) {
            AuthDataItemStatusDO authDataItemStatusDeleteParam = new AuthDataItemStatusDO();
            authDataItemStatusDeleteParam.setDataPoolId(authDataItemRelationDO.getDataPoolId());
            Integer deleteCountByDataPoolId = seperateDbJdbcTemplate.update("delete from "
                                                                                    + getAuthDataItemStatusTableName(authDataItemRelationDO.getDataPoolId())
                                                                                    + " where data_pool_id=?",
                                                                            new Long[] { authDataItemRelationDO.getDataPoolId() });
            log.info("invoke authDataItemStatusDAO.deleteAuthDataItemStatus, param: DataPoolId="
                     + authDataItemRelationDO.getDataPoolId() + ", result: deleteCountByRelatedBizId="
                     + deleteCountByDataPoolId);
        }

        AuthDataItemStatusDO statusDO = new AuthDataItemStatusDO();
        statusDO.setDataPoolId(authDataItemRelationDO.getDataPoolId());
        statusDO.setPoolVersion(authDataItemRelationDO.getPoolVersion());
        statusDO.setModifier("sys_transport");

        if (authOrder.getStatus() == null) {
            log.info("authOrder.getStatus() is null, authOrder.getId()=" + authOrder.getId());
            return null;
        }

        // if (authOrder.getStatus() == AuthStatusEnum.init) {
        // // notConfirm;
        // statusDO.setStatus(AuthDataItemStatusEnum.DRAFT.name());
        // }
        // if (authOrder.getStatus() == AuthStatusEnum.process) {
        //
        // // confirm;
        // // syswrite from av_response_result;
        //
        // AtomAuthInstanceParam atomAuthInstanceParam = new AtomAuthInstanceParam();
        // atomAuthInstanceParam.setOrderId(authOrder.getId());
        // List<AtomAuthInstanceDO> atomAuthList =
        // atomAuthInstanceDAO.findAtomAuthInstanceDOByParam(atomAuthInstanceParam);
        // for (AtomAuthInstanceDO atomAuthInstanceDO : atomAuthList) {
        // if (atomAuthInstanceDO.getPackageCode().equals("AV")) {
        // if (AuthStatusEnum.end.name().equals(atomAuthInstanceDO.getStatus())) {
        // if (StringUtils.equalsIgnoreCase("y", atomAuthInstanceDO.getIsFree())) {
        // statusDO.setStatus(AuthDataItemStatusEnum.SUBMIT.name());
        // } else {
        // statusDO.setStatus(AuthDataItemStatusEnum.SYSWRITE.name());
        // }
        // } else {
        // statusDO.setStatus(AuthDataItemStatusEnum.SUBMIT.name());
        // }
        // break;
        // }
        // }
        //
        // }
        // if (authOrder.getStatus() == AuthStatusEnum.end) {
        // // confirm;
        // // syswrite from av_response_result;
        // statusDO.setStatus(AuthDataItemStatusEnum.SYSWRITE.name());
        // }

        statusDO.setStatus(AuthDataItemStatusEnum.SYSWRITE.name());

        Long statusPk = authDataItemStatusDAO.insertAuthDataItemStatus(statusDO);
        log.info("invoke authDataItemStatusDAO.insertAuthDataItemStatus ok, statusPk=" + statusPk);

        return authDataItemStatusDAO.getAuthDataItemStatus(statusPk, authDataItemRelationDO.getDataPoolId());
    }

    /**
     * @param dataPoolId
     * @return
     */
    private String getAuthDataItemStatusTableName(Long dataPoolId) {
        return "auth_data_item_status_" + StringUtils.leftPad("" + (dataPoolId % 64), 4, "0");
    }

    /**
     * @param authOrder
     */
    @SuppressWarnings("unchecked")
    private AuthDataItemRelationDO dealWithAuthDataItemRelation(AuthOrder authOrder) {
        String relatedBizId = "" + authOrder.getId();

        List<Long> dataPoolIdList = seperateDbJdbcTemplate.queryForList("select data_pool_id from "
                                                                                + getAuthDataItemRelationTableName(relatedBizId)
                                                                                + " where related_biz_id=?",
                                                                        new String[] { relatedBizId }, Long.class);
        if (dataPoolIdList != null && !dataPoolIdList.isEmpty()) {
            deteleAuthDataItemByDataPoolId(dataPoolIdList);

            deteleAuthDataItemStatusByDataPoolId(dataPoolIdList);

            Integer deleteCountByRelatedBizId = seperateDbJdbcTemplate.update("delete from "
                                                                                      + getAuthDataItemRelationTableName(relatedBizId)
                                                                                      + " where related_biz_id=?",
                                                                              new String[] { relatedBizId });
            log.info("invoke authDataItemRelationDAO.deleteByRelatedBizId, param: relatedBizId=" + relatedBizId
                     + ", result: deleteCountByRelatedBizId=" + deleteCountByRelatedBizId);
        }

        AuthDataItemRelationDO authDataItemRelationDO = new AuthDataItemRelationDO();
        authDataItemRelationDO.setRelatedBizId(relatedBizId);
        try {
            authDataItemRelationDAO.insertAuthDataItemRelation(authDataItemRelationDO);

            return authDataItemRelationDAO.getAuthDataItemRelation(relatedBizId);
        } catch (SequenceException e) {
            log.error("invoke authDataItemRelationDAO.insertAuthDataItemRelation error, param: authOrder.getId()="
                      + authOrder.getId(), e);
        }
        return null;
    }

    /**
     * 从分表分库的库表中删除之前插入的数据
     *
     * @param dataPoolIdList
     * @return
     */
    private void deteleAuthDataItemStatusByDataPoolId(List<Long> dataPoolIdList) {
        for (Long dataPoolId : dataPoolIdList) {
            Integer deleteCount = seperateDbJdbcTemplate.update("delete from "
                                                                + getAuthDataItemStatusTableName(dataPoolId)
                                                                + " where data_pool_id=?", new Long[] { dataPoolId });
            log.info("invoke deteleAuthDataItemStatusByDataPoolId, param: dataPoolId=" + dataPoolId
                     + ", result: deleteCount=" + deleteCount);
        }
    }

    /**
     * 从分表分库的库表中删除之前插入的数据
     *
     * @param dataPoolIdList
     * @return
     */
    private void deteleAuthDataItemByDataPoolId(List<Long> dataPoolIdList) {
        for (Long dataPoolId : dataPoolIdList) {
            Integer deleteCount = seperateDbJdbcTemplate.update("delete from " + getAuthDataItemTableName(dataPoolId)
                                                                + " where data_pool_id=?", new Long[] { dataPoolId });
            log.info("invoke deteleAuthDataItemByDataPoolId, param: dataPoolId=" + dataPoolId
                     + ", result: deleteCount=" + deleteCount);
        }
    }

    /**
     * @param dataPoolId
     * @return
     */
    private String getAuthDataItemTableName(Long dataPoolId) {
        return "auth_data_item_" + StringUtils.leftPad("" + (dataPoolId % 128), 4, "0");
    }

    /**
     * @param relatedBizId
     * @return
     */
    private String getAuthDataItemRelationTableName(String relatedBizId) {
        return "auth_data_item_relation_" + StringUtils.leftPad("" + (Math.abs(relatedBizId.hashCode()) % 64), 4, "0");
    }

    /**
     * @return
     * @see AuthOrder
     */
    private RowMapper buildAuthOrderRowMapper() {
        return new RowMapper() {

            @Override
            public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
                AuthOrder authOrder = new AuthOrder();
                authOrder.setId(rs.getLong("id"));
                authOrder.setPackageCode(rs.getString("package_code"));
                authOrder.setTargetId(rs.getLong("target_id"));
                String status = rs.getString("status");
                if (status != null) {
                    authOrder.setStatus(AuthStatusEnum.valueOf(status));
                }
                return authOrder;
            }
        };
    }

    /**
     * @param atomAuthInstanceDO
     * @return
     */
    @SuppressWarnings("unchecked")
    private AuthDataItemPool dealWithBankRemitAtomAuth(AtomAuthInstanceDO atomAuthInstanceDO) {
        AuthDataItemPool bankRemitPool = new AuthDataItemPool();
        if (!StringUtils.equals("BANKREMIT", atomAuthInstanceDO.getPackageCode())) {
            return bankRemitPool;
        }

        List<AuthBankRemitApplyDO> bankRemitList = jdbcTemplate.query("select * from auth_bank_remit_apply i where i.is_deleted='N' " //
                                                                              + " and i.atom_auth_id=?",
                                                                      new Long[] { atomAuthInstanceDO.getId() },
                                                                      buildAuthBankRemitApplyRowMapper());

        if (bankRemitList == null || bankRemitList.isEmpty()) {
            return bankRemitPool;
        }

        AuthBankRemitApplyDO authBankRemitApplyDO = bankRemitList.get(0);

        AuthDataItemMappingResult<AuthDataItemPool> result = AuthDataItemMappingUtil.convertPojo2DataItem(authBankRemitApplyDO);
        if (result.isFailed()) {
            log.warn("convert BANKREMIT dataItem failed! atomAuthId = " + atomAuthInstanceDO.getId());
            return bankRemitPool;
        }
        return result.getReturnValue();
    }

    /**
     * @return
     */
    private RowMapper buildAuthBankRemitApplyRowMapper() {
        return new RowMapper() {

            @Override
            public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
                String bank_account = rs.getString("bank_account");
                String bank_account_name = rs.getString("bank_account_name");
                String bank_name = rs.getString("bank_name");
                String bank_city = rs.getString("bank_city");
                String bank_province = rs.getString("bank_province");
                String bank_branch_name = rs.getString("bank_branch_name");

                AuthBankRemitApplyDO authBankRemitApplyDO = new AuthBankRemitApplyDO();
                authBankRemitApplyDO.setBankAccount(bank_account);
                authBankRemitApplyDO.setBankAccountName(bank_account_name);
                authBankRemitApplyDO.setBankBranchName(bank_branch_name);
                authBankRemitApplyDO.setBankCity(bank_city);
                authBankRemitApplyDO.setBankName(bank_name);
                authBankRemitApplyDO.setBankProvince(bank_province);
                return authBankRemitApplyDO;
            }
        };
    }

    // /**
    // * 农场
    // *
    // * @param atomAuthInstanceDO
    // * @return
    // */
    // private AuthDataItemPool dealWithOrganicAtomAuth(AtomAuthInstanceDO atomAuthInstanceDO) {
    // AuthDataItemPool pool = new AuthDataItemPool();
    // if (!StringUtils.equals("ORGANIC", atomAuthInstanceDO.getPackageCode())) {
    // return pool;
    // }
    // return pool;
    // }

    /**
     * 两证
     *
     * @param atomAuthInstanceDO
     * @return
     */
    @SuppressWarnings("unchecked")
    private AuthDataItemPool dealWithMultiCertsAtomAuth(AtomAuthInstanceDO atomAuthInstanceDO) {
        if (!StringUtils.equals("MULTICERTS", atomAuthInstanceDO.getPackageCode())) {
            return new AuthDataItemPool();
        }

        AuthDataItemPool mcPool = new AuthDataItemPool();

        List<ImageAuditInfoObject> auditInfoList = jdbcTemplate.query("select info from auth_audit_info i where i.is_deleted='N' " //
                                                                              + " and i.atom_auth_id=?",
                                                                      new Long[] { atomAuthInstanceDO.getId() },
                                                                      buildMultiCertsAuditInfoRowMapper());

        for (ImageAuditInfoObject auditInfoObject : auditInfoList) {
            if (auditInfoObject instanceof IdCardAuditInfoObject) {
                IdCardAuditInfoObject idCardAuditInfoObject = (IdCardAuditInfoObject) auditInfoObject;

                // idCardAuditInfoObject.getBackImageId();
                // idCardAuditInfoObject.getFrontImageId(); // "idCardFront"
                // idCardAuditInfoObject.getIdCardNum();
                // // idCardAuditInfoObject.getImageType(); // ImageTypeEnum.ID_CARD 这个字段只是通过class的类型来直接指定的，不进入认证资料
                // idCardAuditInfoObject.getIsMainland();
                // idCardAuditInfoObject.getName();
                // idCardAuditInfoObject.getPlusIdCardNum();
                // idCardAuditInfoObject.getPlusImageId();

                NameMapper nameMapper = new DefaultNameMapper().setPojoMaptoItem("frontImageId", "idCardFront") //
                .setPojoMaptoItem("backImageId", "idCardBack") //
                .setPojoMaptoItem("plusImageId", "otherCert") //
                .setPojoMaptoItem("idCardNum", "certNum") //
                .setPojoMaptoItem("plusIdCardNum", "otherCertNum") //
                .setPojoMaptoItem("isMainland", "ceoIsMainland");

                AuthDataItemMappingResult<AuthDataItemPool> result = AuthDataItemMappingUtil.convertPojo2DataItem(nameMapper,
                                                                                                                  idCardAuditInfoObject);
                AuthDataItemPool pool = result.getReturnValue();
                mcPool = AuthDataItemMappingUtil.merge(pool, mcPool);
            }
            if (auditInfoObject instanceof BusinessLicenseAuditInfoObject) {
                BusinessLicenseAuditInfoObject businessLicenseAuditInfoObject = (BusinessLicenseAuditInfoObject) auditInfoObject;
                // businessLicenseAuditInfoObject.getBusinessLicenseImageId();
                // // businessLicenseAuditInfoObject.getImageType(); // ImageTypeEnum.BUSINESS_LICENSE
                // 这个字段只是通过class的类型来直接指定的，不进入认证资料

                NameMapper nameMapper = new DefaultNameMapper().setPojoMaptoItem("businessLicenseImageId",
                                                                                 "businessLicense");

                AuthDataItemMappingResult<AuthDataItemPool> result = AuthDataItemMappingUtil.convertPojo2DataItem(nameMapper,
                                                                                                                  businessLicenseAuditInfoObject);
                AuthDataItemPool pool = result.getReturnValue();
                mcPool = AuthDataItemMappingUtil.merge(pool, mcPool);
            }
        }
        return mcPool;

    }

    /**
     * 品牌
     *
     * @param atomAuthInstanceDO
     * @param authDataItemRelationDO
     * @return
     */
    @SuppressWarnings("unchecked")
    private AuthDataItemPool dealWithBrandAtomAuth(AtomAuthInstanceDO atomAuthInstanceDO,
                                                   AuthDataItemRelationDO authDataItemRelationDO) {
        AuthDataItemPool pool = new AuthDataItemPool();
        if (!StringUtils.equals("BRAND", atomAuthInstanceDO.getPackageCode())) {
            return pool;
        }
        List<BrandAuditInfoObject> auditInfoList = jdbcTemplate.query("select info from auth_audit_info i, auth_audit_request r where i.is_deleted='N' and i.audit_request_id = r.id "
                                                                              + " and r.atom_auth_id=?",
                                                                      new Long[] { atomAuthInstanceDO.getId() },
                                                                      buildBrandAuditInfoRowMapper());
        for (BrandAuditInfoObject auditInfoObject : auditInfoList) {
            if (auditInfoObject instanceof BrandAuditInfoObject) {
                BrandAuditInfoObject brandAuditInfoObject = (BrandAuditInfoObject) auditInfoObject;
                // brandAuditInfoObject.getEndDate();
                // brandAuditInfoObject.getStartDate();
                // brandAuditInfoObject.getTrademarkCode();
                // brandAuditInfoObject.getBrandName();

                NameMapper nameMapper = new DefaultNameMapper() //
                .setPojoMaptoItem("endDate", "brandEndDate") //
                .setPojoMaptoItem("startDate", "brandStartDate");
                AuthDataItemMappingResult<AuthDataItemPool> result = AuthDataItemMappingUtil.convertPojo2DataItem(nameMapper,
                                                                                                                  brandAuditInfoObject);
                pool = result.getReturnValue();
            }
        }
        return pool;
    }

    /**
     * @return
     */
    private RowMapper buildBrandAuditInfoRowMapper() {
        return new RowMapper() {

            @Override
            public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
                String infoJson = rs.getString("info");
                return (BrandAuditInfoObject) AuditInfoJsonUtil.parseObject(infoJson);
            }
        };
    }

    /**
     * @return
     * @see ImageAuditInfoObject
     */
    private RowMapper buildMultiCertsAuditInfoRowMapper() {
        return new RowMapper() {

            @Override
            public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
                String infoJson = rs.getString("info");
                return (ImageAuditInfoObject) AuditInfoJsonUtil.parseObject(infoJson);
            }
        };
    }
}
