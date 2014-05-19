package com.alibaba.china.talos.service.impl;

import java.util.Date;
import java.util.List;

import com.alibaba.china.shared.talos.platform.constants.AuthStatusEnum;
import com.alibaba.china.shared.talos.platform.constants.ResultStatusEnum;
import com.alibaba.china.shared.talos.platform.model.AtomAuthInstance;
import com.alibaba.china.shared.talos.platform.model.AuthOrder;
import com.alibaba.china.talos.daemon.BaseAO;
import com.alibaba.china.talos.dal.param.platform.AuthOrderParam;
import com.alibaba.china.talos.platform.order.AuthOrderService;
import com.alibaba.china.talos.platform.product.atom.AtomAuthService;
import com.alibaba.common.lang.StringUtil;
import com.alibaba.common.logging.Logger;
import com.alibaba.common.logging.LoggerFactory;

public class AtomAuthInstanceDataTranfer implements BaseAO {

    private static final Logger logger = LoggerFactory.getLogger(AtomAuthInstanceDataTranfer.class);

    private AuthOrderService    authOrderService;

    private AtomAuthService     atomAuthService;

    @Override
    public void execute() {

        logger.info("task start");
        AuthOrderParam param = new AuthOrderParam();
        param.setPackageCode("V1001");
        String[] statuses = { AuthStatusEnum.init.name(), AuthStatusEnum.begin.name(), AuthStatusEnum.process.name() };
        param.setStatuses(statuses);
        List<AuthOrder> orders = authOrderService.findBy(param);
        if (orders == null || orders.isEmpty()) {
            logger.info("no record for AtomAuthInstanceDataTranfer task");
            return;
        }

        long startTime = System.nanoTime();
        int recordNum = 0;
        int executeNum = 0;
        for (AuthOrder order : orders) {

            try {
                if (StringUtil.equalsIgnoreCase(order.getExtraInfo("ENT_ALIPAY_BIND"), "y")) {

                    AtomAuthInstance atomAuthInstance = new AtomAuthInstance();
                    atomAuthInstance.setOrderId(order.getId());
                    List<AtomAuthInstance> instanceList = atomAuthService.findAll(atomAuthInstance);
                    logger.info("instance num:" + instanceList.size() + " for orderId:" + order.getId());
                    if (instanceList.size() > 1) {
                        continue;
                    }

                    recordNum++;

                    // 两证
                    atomAuthInstance.setTargetId(order.getTargetId());
                    atomAuthInstance.setOrderId(order.getId());
                    atomAuthInstance.setPackageCode("MULTICERTS");
                    atomAuthInstance.setStatus(AuthStatusEnum.end);
                    atomAuthInstance.setGmtFinish(order.getGmtCreate());
                    atomAuthInstance.setGmtCreate(order.getGmtCreate());
                    atomAuthInstance.setGmtModified(new Date());
                    atomAuthInstance.setResultStatus(ResultStatusEnum.success);
                    atomAuthInstance.setIsFree("y");
                    atomAuthService.create(atomAuthInstance);

                    // 银行打款
                    atomAuthInstance.setPackageCode("BANKREMIT");
                    atomAuthInstance.setId(null);
                    atomAuthService.create(atomAuthInstance);

                    logger.info("success to add MULTICERTS and BANKREMIT for V1001 to order:" + order.getId());
                    executeNum++;

                }
            } catch (Exception e) {
                logger.error("fail to add MULTICERTS and BANKREMIT for V1001 to order:" + order.getId(), e);
            }
        }

        logger.info("total records:" + recordNum + ",execute records:" + executeNum + ",execute time:"
                    + (System.nanoTime() - startTime));

    }

    public void setAtomAuthService(AtomAuthService atomAuthService) {
        this.atomAuthService = atomAuthService;
    }

    public void setAuthOrderService(AuthOrderService authOrderService) {
        this.authOrderService = authOrderService;
    }

}
