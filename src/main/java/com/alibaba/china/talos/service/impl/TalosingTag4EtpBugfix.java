package com.alibaba.china.talos.service.impl;

import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.china.auth.service.AuthReadService;
import com.alibaba.china.auth.service.enums.AVTypeEnum;
import com.alibaba.china.auth.service.enums.AuthTypeEnum;
import com.alibaba.china.auth.service.models.Owner;
import com.alibaba.china.auth.service.models.UnitedAuthModel;
import com.alibaba.china.member.service.MemberReadService;
import com.alibaba.china.shared.talos.order.service.AuthOrderReadService;
import com.alibaba.china.shared.talos.platform.model.AuthOrder;
import com.alibaba.china.shared.talos.result.model.AuthTarget;
import com.alibaba.china.shared.talos.result.service.AuthTargetService;
import com.alibaba.china.talos.daemon.AbstractFileTaskAO;
import com.alibaba.china.talos.file.LineAnalyzer;
import com.alibaba.china.talos.file.LineHandle;
import com.alibaba.china.talos.util.UICUtilService;


public class TalosingTag4EtpBugfix extends AbstractFileTaskAO<String> {

    @Autowired
    private AuthReadService   authReadService;

    @Autowired
    private UICUtilService    uicUtilService;

    @Autowired
    private MemberReadService memberReadService;

    @Autowired
    private AuthOrderReadService authOrderReadService;

    @Autowired
    private AuthTargetService    authTargetService;

    @Override
    public LineAnalyzer<String> getLineAnalyzer() {
        return new LineAnalyzer<String>() {

            @Override
            public String analyse(String lineText) {

                return getMetaData(lineText);
            }

            private String getMetaData(String data) {
                return data.substring(1, data.length() - 1);
            }
        };
    }

    @Override
    public LineHandle<String> getLineHandle() {
        return new LineHandle<String>() {

            @Override
            public boolean handle(String model) {
                // get id
                String memberId = model;
                long userId = memberReadService.convertMemberIdToUserId(memberId);
                if (needHandle(memberId, userId)) {
                    boolean unTagRes = uicUtilService.unTagAlipayBindFlag(userId);
                    if (!unTagRes) {
                        getLogger().error("process failed where memberId:" + memberId);
                        return false;
                    }
                }
                return true;
            }

            private boolean needHandle(String memberId, long userId) {
                UnitedAuthModel uam = authReadService.findAuthByOwner(new Owner(memberId), AuthTypeEnum.AV,
                                                                      AuthTypeEnum.PNA);
                if (null != uam && uam.hasAV()) {
                    AVTypeEnum avTypeEnum = uam.getAVInfoModel().getAVType();
                    if (AVTypeEnum.EnterpriseAV.equals(avTypeEnum)) {
                        // 如果是etp用户，继续判断是否正在进行talos认证
                        Long targetId = authTargetService.getAuthTargetId(new AuthTarget(String.valueOf(userId)));
                        if (null != targetId && targetId > 0) {
                            AuthOrder authOrder = authOrderReadService.findLatestUnFinishAuthOrder(targetId);
                            if (null == authOrder) {
                                return true;
                            }
                        } else {
                            return true;
                        }
                    }
                }
                return false;
            }

        };
    }

}
