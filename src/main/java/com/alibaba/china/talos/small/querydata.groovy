package com.alibaba.china.talos.small;

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
 * 类querydata.groovy的实现描述：查询数据
 * @author guokai Feb 14, 2014 7:09:57 PM
 */
public class QueryData {

    private static final Logger       log = LoggerFactory.getLogger(QueryData.class);

    private AuthDataItemDAO           authDataItemDAO = Daoer.getBean("authDataItemDAO");
    private AuthDataItemRelationDAO   authDataItemRelationDAO = Daoer.getBean("authDataItemRelationDAO");
    private AuthDataItemStatusDAO     authDataItemStatusDAO = Daoer.getBean("authDataItemStatusDAO");
    private JdbcTemplate              jdbcTemplate = Daoer.getBean("jdbcTemplate");
    /**
     * 分表分库后的jdbcTemplate
     */
    private JdbcTemplate              seperateDbJdbcTemplate = Daoer.getBean("seperateDbJdbcTemplate");

    private AuthDataDefinitionService authDataDefinitionService = Daoer.getBean("authDataDefinitionService");

    private AvProcessInfoDAO          avProcessInfoDAO = Daoer.getBean("avProcessInfoDAO");
    private AvResponseResultDAO       avResponseResultDAO = Daoer.getBean("avResponseResultDAO");

    private AtomAuthInstanceDAO       atomAuthInstanceDAO = Daoer.getBean("atomAuthInstanceDAO");

    static final int STEP = 300;

    public void execute(List<String> authOrderIdList) {
    }

    def outputListData(rowName, rowList) {
        if(rowName!=null) {
            println ">> >> >> "+rowName;
        }
        if(rowList!=null && rowList.size()>0) {
            println rowList[0].keySet().collect{it -> StringUtils.rightPad(it, 30, ' ')}.join(",");
        }
        rowList.each {
            rowData ->
                outputRowData(null, rowData, true);
        };
    }

    def outputRowData(rowName, rowData, noColumn) {
        if(rowName!=null) {
            println ">> >> >> >> >> >> "+rowName;
        }
        if(noColumn==null) {
            println rowData.keySet().collect{it -> StringUtils.rightPad(it, 30, ' ')}.join(",");
        }
        println rowData.values().collect{it -> StringUtils.rightPad(it==null?"":it.toString(), 30, ' ')}.join(",");
    }

    public void execute() {
        log.info("[AuthDataItemTransportAO.execute()] start");

//            auth_data_item_history
//            auth_data_item_pool
//            auth_data_item_relation
//            auth_data_item
//            auth_data_item_status_history
//            auth_data_item_status
//            auth_detail

        def tableNames = """atom_auth_instance
            atom_auth_provider_relation
            auth_audit_dispatch
            auth_audit_info
            auth_audit_record
            auth_audit_request
            auth_audit_result
            auth_bank_remit_apply
auth_dispatcher_rule
            auth_grant
            auth_his
            auth_order
            auth_personal
            auth_provider_atom_auth
            auth_provider
            auth_result
            auth_stage_info_key
            auth_target
            av_process_status_ext
            av_process_status
            av_response_result_ext
            av_response_result
            auth_identity_info
            auth_identity_info_query_record
            """;
//        println tableNames;
            
        tableNames.split("\n").each {
            it ->
                try {
                    def maxId = jdbcTemplate.queryForLong("select max(id) from "+it);
                    println "tableName="+it+", maxId="+maxId;
                }catch(e) {
                    println "tableName="+it+", "+e;
                }
        };

        "2086016,2086018,2104001".split(",").each {
//        "2086005,2086009,2086010".split(",").each {
            orderId ->
                println "\n\norderId: " + orderId;

                def authOrderList = seperateDbJdbcTemplate
                                            .queryForList("select * from auth_order where id=?",
                                                [orderId] as String[]);
                outputListData("authOrder: ", authOrderList);

                def atomAuthInstanceList = seperateDbJdbcTemplate
                                            .queryForList("select * from atom_auth_instance where order_id=?",
                                                [orderId] as String[]);
                outputListData("atomAuthInstance: ", atomAuthInstanceList);

                def authDataRelationList = seperateDbJdbcTemplate
                                            .queryForList("select * from "
                                                            +getAuthDataItemRelationTableName(orderId)
                                                            +" where related_biz_id=?",
                                                [orderId] as String[]);
                if(authDataRelationList==null||authDataRelationList.isEmpty()) {
                    return;
                }

                authDataRelationList.each {
                    authDataRelation ->
                        outputRowData("authDataRelation:", authDataRelation, null);

                        def dataPoolId = authDataRelation["data_pool_id"] as Long;
                        println "dataPoolId: " + dataPoolId;
                        def authDataItemStatusList = seperateDbJdbcTemplate
                                                .queryForList("select * from "
                                                            +getAuthDataItemTableName(dataPoolId)
                                                            +" where data_pool_id=?",
                                                [dataPoolId] as Long[]);


                        def authDataItemList = seperateDbJdbcTemplate
                                                .queryForList("select * from "
                                                            +getAuthDataItemTableName(dataPoolId)
                                                            +" where data_pool_id=?",
                                                [dataPoolId] as Long[]);
                        outputListData("authDataItem: ", authDataItemList);
                };
        };

        log.info("[AuthDataItemTransportAO.execute()] end");
    }

    /**
     * @param dataPoolId
     * @return
     */
    private String getAuthDataItemStatusTableName(Long dataPoolId) {
        return "auth_data_item_status_" + StringUtils.leftPad("" + (dataPoolId % 64), 4, "0");
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
}
