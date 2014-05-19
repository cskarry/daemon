package com.alibaba.china.talos.service.impl;

import java.util.List;

import com.alibaba.china.auth.service.enums.AuthTypeEnum;
import com.alibaba.china.shared.talos.platform.constants.AuthStatusEnum;
import com.alibaba.china.shared.talos.platform.constants.ResultStatusEnum;
import com.alibaba.china.shared.talos.platform.model.AtomAuthInstance;
import com.alibaba.china.shared.talos.platform.model.AuthOrder;
import com.alibaba.china.shared.talos.result.model.AtomResult;
import com.alibaba.china.shared.talos.result.model.AuthTarget;
import com.alibaba.china.shared.talos.result.service.AuthTargetService;
import com.alibaba.china.shared.talos.result.service.ResultReadService;
import com.alibaba.china.talos.daemon.BaseAO;
import com.alibaba.china.talos.dal.param.platform.AuthOrderParam;
import com.alibaba.china.talos.platform.order.AuthOrderService;
import com.alibaba.china.talos.platform.product.atom.AtomAuthService;
import com.alibaba.china.talos.platform.result.service.AuthCacheService;
import com.alibaba.china.talos.platform.result.service.InnerAuthResultWriteService;
import com.alibaba.common.logging.Logger;
import com.alibaba.common.logging.LoggerFactory;

public class ResultAVClearTask implements BaseAO {

    private static final Logger         logger = LoggerFactory.getLogger(ResultAVClearTask.class);

    private AuthOrderService            authOrderService;

    private AuthTargetService           authTargetService;

    private InnerAuthResultWriteService innerAuthResultWriteService;

    private AuthCacheService            authCacheService;

    private AtomAuthService             atomAuthService;

    private ResultReadService           resultReadService;

    @Override
    public void execute() {
        AuthOrderParam param = new AuthOrderParam();
        param.setStatus(AuthStatusEnum.end.name());
        param.setResultStatus(ResultStatusEnum.failure.name());
        List<AuthOrder> orders = authOrderService.findBy(param);
        if (orders == null || orders.isEmpty()) {
            logger.info("no record for ResultAVClearTask task");
            return;
        }

        long startTime = System.nanoTime();
        int recordNum = 0;
        int executeNum = 0;
        for (AuthOrder order : orders) {

            AtomAuthInstance atomAuthInstance = new AtomAuthInstance();
            atomAuthInstance.setOrderId(order.getId());
            atomAuthInstance.setTargetId(order.getTargetId());
            atomAuthInstance.setPackageCode(AuthTypeEnum.AV.name());
            atomAuthInstance.setStatus(AuthStatusEnum.end);
            atomAuthInstance.setResultStatus(ResultStatusEnum.success);
            List<AtomAuthInstance> instanceList = atomAuthService.findAll(atomAuthInstance);
            if (instanceList == null || instanceList.isEmpty()) {
                // logger.info("not suit,orderId:" + order.getId());
                continue;
            }

            // for (AtomAuthInstance instance : instanceList) {
            // if (!StringUtil.equalsIgnoreCase(instance.getPackageCode(), AuthTypeEnum.AV.name())
            // || instance.getResultStatus() != ResultStatusEnum.success) {
            // logger.error("query exception for orderId:" + order.getId());
            // break;
            // }
            //
            // }

            AuthOrderParam successParam = new AuthOrderParam();
            successParam.setTargetId(order.getTargetId());
            successParam.setStatus(AuthStatusEnum.end.name());
            successParam.setResultStatus(ResultStatusEnum.success.name());

            // 一个用户有失败的申请单且也有认证成功的，则忽略。（基于目前所有的认证包都有AV。不然就要去判断原子认证中是否包含AV）
            List<AuthOrder> successOrders = authOrderService.findBy(successParam);
            if (successOrders != null && !successOrders.isEmpty()) {
                // logger.info("not suit,orderId:" + order.getId());
                continue;
            }

            recordNum++;
            try {

                AtomResult atom = resultReadService.findAtomResult(order.getTargetId(), AuthTypeEnum.AV.name());
                if (atom == null) {
                    logger.error("dirty result data for orderId:" + order.getId());
                    continue;
                }

                // 清除数据库的记录
                boolean datebaseflag = innerAuthResultWriteService.deleteAllResult(order.getTargetId(),
                                                                                   AuthTypeEnum.AV.name());
                if (datebaseflag) {
                    logger.info("success to clear av record in database for targetId:" + order.getTargetId()
                                + ",orderId:" + order.getId());
                } else {
                    logger.error("fail to clear av record in database for targetId:" + order.getTargetId()
                                 + ",orderId:" + order.getId());
                }

                // 清除Cache记录
                AuthTarget target = authTargetService.getAuthTarget(order.getTargetId());
                boolean cacheFlag = authCacheService.removeAuth(target, AuthTypeEnum.AV.name());

                if (cacheFlag) {
                    logger.info("success to clear av record in cache for targetId:" + order.getTargetId() + ",orderId:"
                                + order.getId());
                } else {
                    logger.error("fail to clear av record in cache for targetId:" + order.getTargetId() + ",orderId:"
                                 + order.getId());
                }

                executeNum++;
            } catch (Exception e) {
                logger.error("fail to clear av record for targetId:" + order.getTargetId() + ",orderId:"
                                     + order.getId(), e);
            }

        }

        logger.info("total records:" + recordNum + ",execute records:" + executeNum + ",execute time:"
                    + (System.nanoTime() - startTime));

    }

    public void setInnerAuthResultWriteService(InnerAuthResultWriteService innerAuthResultWriteService) {
        this.innerAuthResultWriteService = innerAuthResultWriteService;
    }

    public void setAuthCacheService(AuthCacheService authCacheService) {
        this.authCacheService = authCacheService;
    }

    public void setAuthOrderService(AuthOrderService authOrderService) {
        this.authOrderService = authOrderService;
    }

    public void setAuthTargetService(AuthTargetService authTargetService) {
        this.authTargetService = authTargetService;
    }

    public void setAtomAuthService(AtomAuthService atomAuthService) {
        this.atomAuthService = atomAuthService;
    }

    public void setResultReadService(ResultReadService resultReadService) {
        this.resultReadService = resultReadService;
    }

}
