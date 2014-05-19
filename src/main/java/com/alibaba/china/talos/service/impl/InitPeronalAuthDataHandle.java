package com.alibaba.china.talos.service.impl;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.china.shared.talos.platform.api.AuthStatusReadService;
import com.alibaba.china.shared.talos.platform.constants.AuthStatusEnum;
import com.alibaba.china.shared.talos.platform.constants.ResultStatusEnum;
import com.alibaba.china.shared.talos.platform.model.AuthExportResult;
import com.alibaba.china.shared.talos.platform.model.AuthOrder;
import com.alibaba.china.shared.talos.result.model.AtomResult;
import com.alibaba.china.shared.talos.result.model.AuthTarget;
import com.alibaba.china.shared.talos.result.model.BundleResult;
import com.alibaba.china.shared.talos.result.service.AuthTargetService;
import com.alibaba.china.shared.talos.result.service.enums.AuthExportStatus;
import com.alibaba.china.talos.file.LineHandle;
import com.alibaba.china.talos.model.PersonalModel;
import com.alibaba.china.talos.platform.order.AuthOrderService;
import com.alibaba.china.talos.platform.result.service.AuthCacheService;
import com.alibaba.china.talos.platform.result.service.InnerAuthResultWriteService;
import com.alibaba.common.lang.StringUtil;
import com.alibaba.common.logging.Logger;
import com.alibaba.common.logging.LoggerFactory;
import com.alibaba.fastjson.JSON;
import com.taobao.uic.common.domain.BaseUserDO;
import com.taobao.uic.common.domain.ResultDO;
import com.taobao.uic.common.service.userinfo.client.UicReadServiceClient;

public class InitPeronalAuthDataHandle implements LineHandle<PersonalModel> {

    private static final String           PACKAGE_CODE = "V1006";

    private static final String           ATOM_PID     = "PERSONAL";

    private AuthTargetService             authTargetService;

    private InnerAuthResultWriteService   innerAuthResultWriteService;

    private AuthCacheService              authCacheService;

    private UicReadServiceClient          uicReadServiceClient;

    private AuthOrderService              authOrderService;

    private AuthStatusReadService         authStatusReadService;

    private static final Logger           log          = LoggerFactory.getLogger(InitPeronalAuthDataHandle.class);

    public void setAuthTargetService(AuthTargetService authTargetService) {
        this.authTargetService = authTargetService;
    }

    private boolean validParam(PersonalModel model) {
        return true;
    }

    /**
     * @description 填充姓名、身份证、身份证有效期并对身份证加密
     * @param model
     * @throws
     */
    private void processParam(PersonalModel model) {
        long userId = model.getUserId();
        if (userId < 1) {
            log.error("miss userId where " + JSON.toJSONString(model));
            throw new RuntimeException("miss userId!");
        }
        ResultDO<BaseUserDO> baseUserDORes = uicReadServiceClient.getBaseUserByUserId(userId);
        if (null == baseUserDORes || !baseUserDORes.isSuccess() || null == baseUserDORes.getModule()) {
            log.error("get baseUserDO error where userId:" + userId);
            throw new RuntimeException("get baseUserDO error where userId:" + userId);
        }
        BaseUserDO baseUserDO = baseUserDORes.getModule();
        String idCardNumber = baseUserDO.getIdCardNumber();
        String fullName = baseUserDO.getFullname();
        /*
        if (StringUtil.isBlank(fullName) || StringUtil.isBlank(idCardNumber)) {
            log.error("fullName or idCardNumber is null where userId:" + userId);
            throw new RuntimeException("fullName or idCardNumber is null where userId:" + userId);
        }
        */
        // set
        model.setMemberName(fullName);
        model.setIdCardNumber(idCardNumber);

        // 有效期默认长期有效
        Calendar c = Calendar.getInstance();
        c.set(2099, 1, 1);
        model.setIdCardExpireDate(c.getTime());
    }

    /**
     * @description 生成AtomResult
     * @param model
     * @param itemMap
     * @return
     * @throws
     */
    private AtomResult generateAtomResult(PersonalModel model, Map<String, Object> itemMap) {
        AtomResult atomResult = new AtomResult();
        atomResult.setAuthType(ATOM_PID);
        // 以udc数据的修改时间作为通过时间来计算
        atomResult.setPassDate(model.getPassDate());
        // 有效期默认长期有效
        Calendar c = Calendar.getInstance();
        c.set(2099, 1, 1);
        atomResult.setExpireDate(c.getTime());
        atomResult.setItemMap(itemMap);
        return atomResult;
    }

    /**
     * @description 生成BundleResult
     * @param model
     * @return
     * @throws
     */
    private BundleResult generateBundleResult(PersonalModel model) {
        BundleResult bundleResult = new BundleResult();
        bundleResult.setAuthType(PACKAGE_CODE);
        bundleResult.setPassDate(model.getPassDate());
        // 有效期默认长期有效
        Calendar c = Calendar.getInstance();
        c.set(2099, 1, 1);
        bundleResult.setExpireDate(c.getTime());
        return bundleResult;
    }

    /**
     * @description 生成itemMap
     * @param model
     * @return
     * @throws
     */
    private Map<String, Object> filterParam(PersonalModel model) {
        Map<String, Object> itemMap = new HashMap<String, Object>();
        itemMap.put("memberName", model.getMemberName());
        itemMap.put("idCardNumber", model.getIdCardNumber());
        itemMap.put("idCardExpireDate", model.getIdCardExpireDate());
        itemMap.put("source", model.getSource());
        return itemMap;
    }

    public void setUicReadServiceClient(UicReadServiceClient uicReadServiceClient) {
        this.uicReadServiceClient = uicReadServiceClient;
    }


    public void setInnerAuthResultWriteService(InnerAuthResultWriteService innerAuthResultWriteService) {
        this.innerAuthResultWriteService = innerAuthResultWriteService;
    }

    public void setAuthStatusReadService(AuthStatusReadService authStatusReadService) {
        this.authStatusReadService = authStatusReadService;
    }

    public void setAuthCacheService(AuthCacheService authCacheService) {
        this.authCacheService = authCacheService;
    }

    public void setAuthOrderService(AuthOrderService authOrderService) {
        this.authOrderService = authOrderService;
    }

    @Override
    public boolean handle(PersonalModel model) {
        // 验证参数
        if (!validParam(model)) {
            log.warn("param is invalid! where " + JSON.toJSONString(model));
            return false;
        }
        // 处理参数
        processParam(model);
        // 过滤参数
        Map<String, Object> itemMap = filterParam(model);
        if (null == itemMap || itemMap.size() == 0) {
            log.warn("itemMap is empty where userId:" + model.getUserId());
            return false;
        }
        // 防止重复提交
        if (needTerm(model)) {
            return true;
        }
        // 取authTarget
        AuthTarget authTarget = new AuthTarget(model.getUserId().toString());
        Long targetId = authTargetService.getAuthTargetId(authTarget);
        if (null == targetId || targetId < 0) {
            targetId = authTargetService.saveAuthTarget(authTarget);
        }

        if (model.isSuccess()) {
            // step1 通过原子认证
            AtomResult atomResult = generateAtomResult(model, itemMap);
            boolean res = innerAuthResultWriteService.passAtomResult(targetId, atomResult);
            if (!res) {
                log.warn("pass atomResult failed where targetId:" + targetId);
                return false;
            }
            // 写缓存
            boolean cacheWriteRes = authCacheService.addAuth(authTarget, ATOM_PID, atomResult.getResultMap());
            if (!cacheWriteRes) {
                log.warn("cache atom write error where targetId:" + targetId);
                return false;
            }
            // step2 通过产品包
            BundleResult bundleResult = generateBundleResult(model);
            boolean bundleRes = innerAuthResultWriteService.passBundleResult(targetId, bundleResult);
            if (!bundleRes) {
                log.warn("pass bundleRes failed where targetId:" + targetId);
                return false;
            }
            // 写缓存
            boolean cacheWriteBundleRes = authCacheService.addAuth(authTarget, PACKAGE_CODE,
                                                                   bundleResult.getResultMap());
            if (!cacheWriteBundleRes) {
                log.warn("cache bundle write error where targetId:" + targetId);
                return false;
            }
        } else {
            // 认证失败只写auth_order
            AuthOrder authOrder = new AuthOrder();
            authOrder.setAuthOrigin("POS");
            authOrder.setGmtCreate(model.getPassDate());
            authOrder.setGmtFinish(model.getPassDate());
            authOrder.setGmtModified(model.getPassDate());
            authOrder.setGmtSubmit(model.getPassDate());
            authOrder.setPackageCode(PACKAGE_CODE);
            authOrder.setResultStatus(ResultStatusEnum.failure);
            authOrder.setStatus(AuthStatusEnum.end);
            authOrder.setTargetId(targetId);
            boolean orderCreateRes = authOrderService.create(authOrder);
            boolean updateRes = authOrderService.update(authOrder);
            if (!orderCreateRes || !updateRes) {
                log.error("auth order create failed where userId:" + model.getUserId());
                return false;
            }
        }
        if (verifyResult(model)) {
            return true;
        } else {
            log.error("verify result failed where userId:" + model.getUserId() + ",isSuccess:" + model.isSuccess());
            return false;
        }

    }

    private boolean verifyResult(PersonalModel model) {
        AuthExportResult authExportResult = authStatusReadService.getAuthStatus(model.getUserId(), PACKAGE_CODE);
        if (null == authExportResult || StringUtil.isBlank(authExportResult.getStatus())) {
            return false;
        }
        if (model.isSuccess()) {
            return StringUtil.equals(authExportResult.getStatus(), AuthExportStatus.success.name());
        } else {
            return StringUtil.equals(authExportResult.getStatus(), AuthExportStatus.fail.name());
        }
    }

    /**
     * @description 可以中断的场景
     * <i>已有成功的记录</i>
     * <i>重复的失败记录</i>
     * <i>有认证中的记录</i>
     * @param model
     * @return  
     *  
     * @throws
     *
     */
    private boolean needTerm(PersonalModel model) {
        AuthExportResult authExportResult = authStatusReadService.getAuthStatus(model.getUserId(), PACKAGE_CODE);
        boolean isSuccess = model.isSuccess();
        if (null == authExportResult || StringUtil.isBlank(authExportResult.getStatus())) {
            return false;
        }
        if (StringUtil.equals(authExportResult.getStatus(), AuthExportStatus.success.name())) {
            log.info("User has success record so need not sync data where " + JSON.toJSONString(model));
            return true;
        } else if (StringUtil.equals(authExportResult.getStatus(), AuthExportStatus.fail.name())) {
            if (!isSuccess) {
                log.info("multi submit where " + JSON.toJSONString(model));
                return true;
            }
        } else if (StringUtil.equals(authExportResult.getStatus(), AuthExportStatus.invalid.name())) {
            return false;
        } else {
            log.info("User has processing record so need not sync data where " + JSON.toJSONString(model));
            return true;
        }
        return false;

    }

}
