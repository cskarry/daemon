package com.alibaba.china.talos.service.impl;

import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.china.shared.talos.order.service.AuthOrderReadService;
import com.alibaba.china.shared.talos.platform.constants.AuthStatusEnum;
import com.alibaba.china.shared.talos.platform.model.AuthOrder;
import com.alibaba.china.shared.talos.result.model.AuthTarget;
import com.alibaba.china.shared.talos.result.service.AuthTargetService;
import com.alibaba.china.talos.daemon.AbstractFileTaskAO;
import com.alibaba.china.talos.file.LineAnalyzer;
import com.alibaba.china.talos.file.LineHandle;
import com.alibaba.china.talos.model.PairsId;
import com.alibaba.china.talos.util.UICUtilService;
import com.alibaba.china.shared.talos.result.service.enums.SiteEnum;

/**
 * @description �������ϵ��ҵ��֤�д��talosing���
 * <li>�Ҹо�����������������Ϊ����ϵ��ҵ��֤���ߵ�ʱ���������û������������Ҫ��������</li>
 * <li>�ѵ�����ΪPDû�а��������</li>
 * <li>��ΪPD��ԭ�����Ƶ�Ͷ�ߺ͹���������Ѿ��кö�����</li>
 * <li>PD���ǵ����ػ�����©�����Լ�Ϊʲô��û�п��ǹ��������������Ҫ����Щ����</li>
 * <li>����ܱ�������������ô���ҿ���Ҫ�����£��������Դ�</li>
 * @author karry
 * @date 2014-4-21 ����10:15:19
 */
public class RemoveTalosingTag4Taobao extends AbstractFileTaskAO<PairsId> {

    @Autowired
    private AuthOrderReadService authOrderReadService;

    @Autowired
    private AuthTargetService    authTargetService;

    @Autowired
    private UICUtilService       uicUtilService;

    @Override
    public LineAnalyzer<PairsId> getLineAnalyzer() {
        return new LineAnalyzer<PairsId>() {

            @Override
            public PairsId analyse(String lineText) {

                String[] datas = lineText.split(",");
                long orderId = Long.parseLong(getMetaData(datas[0], 1, 0));
                long targetId = Long.parseLong(getMetaData(datas[1], 2, 3));

                return new PairsId(orderId, targetId);
            }

            private String getMetaData(String data, int begin, int end) {
                return data.substring(begin, data.length() - end);
            }
        };
    }

    @Override
    public LineHandle<PairsId> getLineHandle() {
        return newLineHandle4LogicDeleteAuthOrder();
    }

    public LineHandle<PairsId> newLineHandle() {
        return new LineHandle<PairsId>() {

            @Override
            public boolean handle(PairsId model) {

                long targetId = model.getTargetId();
                long orderId = model.getOrderId();

                // step1 ����Ѿ���ᣬ��care
                AuthOrder authOrder = authOrderReadService.findAuthOrder(orderId);
                AuthStatusEnum authStatus = authOrder.getStatus();
                if (AuthStatusEnum.end.equals(authStatus)) {
                    return true;
                }

                // step2 ����������˺Ż��߰������˺ţ�Ҳ��care
                AuthTarget authTarget = authTargetService.getAuthTarget(targetId);
                SiteEnum site = authTarget.getSite();
                if (SiteEnum.LAIWANG.equals(site) || SiteEnum.ALIYUN.equals(site)) {
                    return true;
                }

                // step3 Removing talosing tag
                long entityId = Long.parseLong(authTarget.getEntityId());
                boolean unTagRes = uicUtilService.unTagAlipayBindFlag(entityId);
                if (unTagRes) {
                    getLogger().info("do unTag success where entityId:" + entityId);
                }
                return unTagRes;
            }

        };
    }

    public LineHandle<PairsId> newLineHandle4LogicDeleteAuthOrder() {
        return new LineHandle<PairsId>() {

            @Override
            public boolean handle(PairsId model) {

                long targetId = model.getTargetId();
                long orderId = model.getOrderId();

                AuthTarget authTarget = authTargetService.getAuthTarget(targetId);
                long entityId = Long.parseLong(authTarget.getEntityId());

                AuthOrder authOrder = authOrderReadService.findAuthOrder(orderId);
                if (null == authOrder) {
                    boolean unTagRes = uicUtilService.unTagAlipayBindFlag(entityId);
                    if (unTagRes) {
                        getLogger().info("logicdel authorder do unTag success where entityId:" + entityId);
                    } else {
                        getLogger().error("logicdel authorder do unTag failed where entityId:" + entityId);
                    }
                    return unTagRes;
                }
                return true;
            }
            
        };
    }

}
