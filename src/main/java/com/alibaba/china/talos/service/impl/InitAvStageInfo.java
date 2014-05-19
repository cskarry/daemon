/*
 * Copyright 1999-2012 Alibaba.com All right reserved. This software is the confidential and proprietary information of
 * Alibaba.com ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with Alibaba.com.
 */
package com.alibaba.china.talos.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import com.alibaba.china.shared.talos.platform.constants.AuthStatusEnum;
import com.alibaba.china.talos.av.model.AvProcessStatus;
import com.alibaba.china.talos.av.service.inner.AvProcessStatusService;
import com.alibaba.china.talos.daemon.BaseAO;
import com.alibaba.china.talos.dal.dao.platform.AtomAuthInstanceDAO;
import com.alibaba.china.talos.dal.dao.platform.flow.StageInfoDAO;
import com.alibaba.china.talos.dal.dataobject.platform.AtomAuthInstanceDO;
import com.alibaba.china.talos.dal.dataobject.platform.flow.StageInfoDO;
import com.alibaba.china.talos.dal.param.av.AvProcessStatusParam;
import com.alibaba.china.talos.dal.param.platform.AtomAuthInstanceParam;
import com.alibaba.china.talos.platform.flow.stage.StageStatus;
import com.alibaba.common.logging.Logger;
import com.alibaba.common.logging.LoggerFactory;

/**
 * @description TODO 类实现描述
 * @author haiou.chenho
 * @date 2014-2-12 上午11:12:52
 */
public class InitAvStageInfo implements BaseAO {

    private static final Logger    logger = LoggerFactory.getLogger(InitAvStageInfo.class);

    @Autowired
    private StageInfoDAO           stageInfoDAO;

    @Autowired
    private AtomAuthInstanceDAO    atomAuthInstanceDAO;

    @Autowired
    private AvProcessStatusService avProcessStatusService;

    @Override
    public void execute() {
        AtomAuthInstanceParam atomAuthInstanceParam = new AtomAuthInstanceParam();
        atomAuthInstanceParam.setPackageCode("AV");
        atomAuthInstanceParam.setStatus(AuthStatusEnum.process.name());
        List<AtomAuthInstanceDO> atomAuthInstanceList = atomAuthInstanceDAO.findAtomAuthInstanceDOByParam(atomAuthInstanceParam);
        if (!CollectionUtils.isEmpty(atomAuthInstanceList)) {
            List<Long> atomAuthIds = new ArrayList<Long>();
            for (AtomAuthInstanceDO atomAuth : atomAuthInstanceList) {
                atomAuthIds.add(atomAuth.getId());
            }
            AvProcessStatusParam param = new AvProcessStatusParam();
            param.setAtomAuthIds(atomAuthIds);
            param.setAvStatus("auth");
            List<AvProcessStatus> avProcessStatusList = avProcessStatusService.findBy(param);
            if (!CollectionUtils.isEmpty(avProcessStatusList)) {
                for (AvProcessStatus avProcessStatus : avProcessStatusList) {
                    try {
                        StageInfoDO stageInfoDO = new StageInfoDO();
                        stageInfoDO.setAtomAuthId(avProcessStatus.getAtomAuthId());
                        stageInfoDO.setAuthFlowName("AV");
                        stageInfoDO.setGmtCreate(new Date());
                        stageInfoDO.setGmtModified(new Date());
                        stageInfoDO.setStageName("processing");
                        stageInfoDO.setStageStatus(StageStatus.RUNNING.getValue());
                        stageInfoDAO.insert(stageInfoDO);
                        logger.info("initAvStageInfo success,the atomAuthId=" + avProcessStatus.getAtomAuthId()
                                    + ",stage=process");
                    } catch (Exception e) {
                        logger.error("initAvStageInfo error,the atomAuthId=" + avProcessStatus.getAtomAuthId()
                                     + ",stage=process,exception message=" + e.getMessage());
                    }
                }
            }

            param.setAtomAuthIds(atomAuthIds);
            param.setAvStatus("pass");
            avProcessStatusList = avProcessStatusService.findBy(param);

            if (!CollectionUtils.isEmpty(avProcessStatusList)) {
                for (AvProcessStatus avProcessStatus : avProcessStatusList) {
                    try {
                        StageInfoDO stageInfoDO = new StageInfoDO();
                        stageInfoDO.setAtomAuthId(avProcessStatus.getAtomAuthId());
                        stageInfoDO.setAuthFlowName("AV");
                        stageInfoDO.setGmtCreate(new Date());
                        stageInfoDO.setGmtModified(new Date());
                        stageInfoDO.setStageName("waitForSend");
                        stageInfoDO.setStageStatus(StageStatus.RUNNING.getValue());
                        stageInfoDAO.insert(stageInfoDO);
                        logger.info("initAvStageInfo success,the atomAuthId=" + avProcessStatus.getAtomAuthId()
                                    + ",stage=waitForSend");
                    } catch (Exception e) {
                        logger.error("initAvStageInfo error,the atomAuthId=" + avProcessStatus.getAtomAuthId()
                                     + ",stage=waitForSend,exception message=" + e.getMessage());
                    }
                }
            }

        }
    }
}
