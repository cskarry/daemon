/*
 * Copyright 1999-2012 Alibaba.com All right reserved. This software is the confidential and proprietary information of
 * Alibaba.com ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with Alibaba.com.
 */
package com.alibaba.china.talos.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import com.alibaba.china.shared.talos.tools.AreaInfoObj;
import com.alibaba.china.shared.talos.tools.AreaQueryService;
import com.alibaba.china.talos.daemon.BaseAO;
import com.alibaba.china.talos.dal.dao.platform.dispatcher.AuthDispatcherRuleDAO;
import com.alibaba.china.talos.dal.dao.platform.provider.AuthProviderAtomAuthDAO;
import com.alibaba.china.talos.dal.dao.platform.provider.AuthProviderDAO;
import com.alibaba.china.talos.dal.dataobject.platform.dispatcher.AuthDispatcherRuleDO;
import com.alibaba.china.talos.dal.dataobject.platform.provider.AuthProviderAtomAuthDO;
import com.alibaba.china.talos.dal.dataobject.platform.provider.AuthProviderDO;
import com.alibaba.china.talos.dal.param.platform.dispatcher.AuthDispatcherRuleParam;

/**
 * @description TODO 类实现描述
 * @author haiou.chenho
 * @date 2014-2-12 下午3:16:00
 */
public class InitAuthProvider implements BaseAO {

    private static final Logger     logger     = Logger.getLogger(InitAuthProvider.class);

    private static final String     CHINA_CODE = "CN";

    @Autowired
    private AreaQueryService        areaQueryService;

    @Autowired
    private AuthProviderDAO         authProviderDAO;

    @Autowired
    private AuthProviderAtomAuthDAO authProviderAtomAuthDAO;

    @Autowired
    private AuthDispatcherRuleDAO   authDispatcherRuleDAO;

    @Override
    public void execute() {

        AuthProviderDO authProviderDO = new AuthProviderDO();
        authProviderDO.setProviderCode("zhongchengxin");
        authProviderDO.setName("中诚信");
        authProviderDO.setTelephone("0571-88052088");

        AuthProviderDO oldAuthProviderDO = authProviderDAO.getByCode("zhongchengxin");

        if (oldAuthProviderDO == null) {
            authProviderDAO.insert(authProviderDO);
        }

        AuthProviderAtomAuthDO authProviderAtomAuthDO = new AuthProviderAtomAuthDO();
        authProviderAtomAuthDO.setPackageCode("AV");
        authProviderAtomAuthDO.setProviderId(authProviderDAO.getByCode("zhongchengxin").getId());

        AuthProviderAtomAuthDO oldAuthProviderAtomAuthDO = authProviderAtomAuthDAO.getByProviderIdAndPackageCode(authProviderDAO.getByCode("zhongchengxin").getId(),
                                                                                                                 "AV");
        if (oldAuthProviderAtomAuthDO == null) {
            authProviderAtomAuthDAO.insert(authProviderAtomAuthDO);
        }

        List<AreaInfoObj> areaInfoObjList = areaQueryService.getProvinces(CHINA_CODE, true);

        if (CollectionUtils.isEmpty(areaInfoObjList)) {
            logger.error("invoke InitAuthProvider,the areaInfoObjList is empty.");
            return;
        }

        Long providerAtomAuthId = authProviderAtomAuthDAO.getByProviderIdAndPackageCode(authProviderDAO.getByCode("zhongchengxin").getId(),
                                                                                        "AV").getId();
        for (AreaInfoObj areaInfoObj : areaInfoObjList) {
            try {
                AuthDispatcherRuleDO authDispatcherRuleDO = new AuthDispatcherRuleDO();
                authDispatcherRuleDO.setProviderAtomAuthId(providerAtomAuthId);
                authDispatcherRuleDO.setProvince(areaInfoObj.getValue());
                authDispatcherRuleDO.setCalCount(0);
                authDispatcherRuleDO.setWeight(1.0);

                AuthDispatcherRuleParam authDispatcherRuleParam = new AuthDispatcherRuleParam();
                authDispatcherRuleParam.setProvince(areaInfoObj.getValue());
                List<Long> providerAtomAuthIdList = new ArrayList<Long>();
                providerAtomAuthIdList.add(providerAtomAuthId);
                authDispatcherRuleParam.setProviderAtomAuthIdList(providerAtomAuthIdList);
                List<AuthDispatcherRuleDO> authDispatcherRuleDOList = authDispatcherRuleDAO.getListByParam(authDispatcherRuleParam);
                if (CollectionUtils.isEmpty(authDispatcherRuleDOList)) {
                    authDispatcherRuleDAO.insert(authDispatcherRuleDO);
                }
                logger.info("InitAuthProvider success,the province=" + areaInfoObj.getName());
            } catch (Exception e) {
                logger.error("InitAuthProvider error,the province=" + areaInfoObj.getName() + ",exception message="
                             + e.getMessage());
            }
        }

    }
}
