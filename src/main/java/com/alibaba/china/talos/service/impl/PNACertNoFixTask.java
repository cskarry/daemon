package com.alibaba.china.talos.service.impl;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.china.auth.dal.dao.AuthRelDAO;
import com.alibaba.china.auth.dal.dao.PNAInfoDAO;
import com.alibaba.china.auth.dal.dataobject.AuthRelDO;
import com.alibaba.china.auth.dal.dataobject.PNAInfoDO;
import com.alibaba.china.auth.service.AuthReadService;
import com.alibaba.china.auth.service.enums.AuthTypeEnum;
import com.alibaba.china.auth.service.models.Owner;
import com.alibaba.china.auth.service.models.UnitedAuthModel;
import com.alibaba.china.auth.service.models.read.pna.PNAInfoModel;
import com.alibaba.china.auth.service.enums.CertTypeEnum;
import com.alibaba.china.member.service.MemberReadService;
import com.alibaba.china.member.service.models.MemberModel;
import com.alibaba.china.talos.alipay.model.AlipayUserModel;
import com.alibaba.china.talos.alipay.service.AlipayUserQueryService;
import com.alibaba.china.talos.daemon.AbstractFileTaskAO;
import com.alibaba.china.talos.file.LineAnalyzer;
import com.alibaba.china.talos.file.LineHandle;

import com.alibaba.china.talos.model.BindRelDO;
import com.alibaba.china.talos.util.UICUtilService;
import com.alibaba.china.talos.util.UICUtilService.AlipayAuthStatus;
import com.alibaba.common.lang.StringUtil;
import com.taobao.uic.common.domain.BaseUserDO;
import com.taobao.uic.common.domain.ExtraUserDO;


/**
 * @description 15位身份证refresh
 * 
 * <li>大致逻辑像这样子的：先判断是否需要刷新，其次刷新pna记录，最后刷新uic记录</li>
 * <li>而哪些用户是需要刷新pna的呢？下述的条件是&的逻辑</li>
 * <li>1、通过了pna认证</li>
 * <li>2、通过方式为支付宝快捷认证</li>
 * <li>3、PNA证件类型为身份证</li>
 * <li>4、PNA记录的证件号码非18位</li>
 * <li>5、支付宝账户为个人账户</li>
 * <li>6、支付宝通过了身份证认证</li>
 * <li>7、支付宝姓名与网站姓名一致</li>
 * <li>8、支付宝证件号类型为身份证</li>
 * <li>9、支付宝证件号为18位</li>
 * <li>接下来需要刷新uic了，下述条件依然是&的逻辑</li>
 * <li>1、未通过支付宝实名认证</li>
 * <li>2、无talos、talosing、pna的标 || 有pna标且realName一致且idCard为15位</li>
 * <li>呐，我已经怀着故障的风险把UIC刷新完了，这个时候我不禁要想了为什么每天都要处理这么多乱七八糟的脏数据</li>
 * <li>瞧瞧光订正的逻辑就这么恶心，突然想到淘宝的xfile预发布都挂了一个月了都没人管，线上发布需要PE一台一台看，本地压根起不来应用</li>
 * <li>我们是要做102年的公司啊，逻辑都乱成这样了</li>
 * <li>而我当前仅要做的就是华丽丽的写代码，如果可以的话尽量保证没有故障，今天有点小雨，注释有点多了，亦注释心情</li>
 * 
 * 
 * @author karry
 * @date 2014-4-3 下午3:17:03
 *
 */
public class PNACertNoFixTask extends AbstractFileTaskAO<BindRelDO> {

    @Autowired
    private PNAInfoDAO pnaInfoDAO;

    @Autowired
    private AuthRelDAO             authRelDAO;

    @Autowired
    private UICUtilService uicUtilService;

    @Autowired
    private AuthReadService   authReadService;

    @Autowired
    private MemberReadService memberReadService;

    @Autowired
    private AlipayUserQueryService alipayUserQueryService;

    private static final String    ALIPAY_PROVIDER = "alipay";

    private static final String    PERSON_ALIPAY_USER = "2";

    private static final String    CERT_NUM_TYPE      = "0";

    @Override
    public LineAnalyzer<BindRelDO> getLineAnalyzer() {
        return new LineAnalyzer<BindRelDO>() {

            @Override
            public BindRelDO analyse(String lineText) {

                String segP = "\t";

                String[] data = lineText.trim().split(segP);

                return new BindRelDO(data[0], data[data.length - 1]);
            }

        };
    }

    @Override
    public LineHandle<BindRelDO> getLineHandle() {
        return new LineHandle<BindRelDO>() {

            @Override
            public boolean handle(BindRelDO bindRelDO) {
                if (canSyncPNA(bindRelDO)) {
                    doSyncPNA(bindRelDO);
                    if (canSyncUIC(bindRelDO)) {
                        doSyncUIC(bindRelDO);
                    }
                }
                return true;
            }

            /**
             * @description 是否需要同步PNA
             * @param bindRelDO
             * @return
             * @throws
             */
            private boolean canSyncPNA(BindRelDO bindRelDO) {
                String memberId = bindRelDO.getMemberId();
                String alipayUserId = bindRelDO.getAlipayUserId();
                // 1、是否通过PNA认证
                Owner owner = new Owner(memberId);
                UnitedAuthModel uam = authReadService.findAuthByOwner(owner, AuthTypeEnum.PNA);
                if (null == uam || !uam.hasPNA() || null == uam.getPNAInfoModel()) {
                    // getLogger().warn("has not pass pna where memberId:" + memberId);
                    return false;
                }

                PNAInfoModel pnaInfoModel = uam.getPNAInfoModel();
                String pnaName = StringUtil.trimToEmpty(pnaInfoModel.getName());
                String pnaCertNum = StringUtil.trimToEmpty(pnaInfoModel.getCertNum());

                // 保存pna过程数据
                bindRelDO.setPnaName(pnaName);
                bindRelDO.setPnaIdcard(pnaCertNum);

                // 2、是否通过支付宝快捷认证
                if (!StringUtil.equals(ALIPAY_PROVIDER, pnaInfoModel.getAuthProvider().getProviderId())) {
                    // getLogger().warn("pna provider is not alipay where memberId:" + memberId);
                    return false;
                }

                // 3、pna证件类型为身份证
                if (!CertTypeEnum.ID_CARD.equals(pnaInfoModel.getCertType())) {
                    // getLogger().warn("pna cert type is not shenfengzheng where memberId:" + memberId);
                    return false;
                }
                // 4、身份证号码非18位
                if (StringUtil.isNotBlank(pnaCertNum) && pnaCertNum.length() == 18) {
                    // getLogger().warn("pna cerNum is 18 bit where memberId:" + memberId);
                    return false;
                }

                AlipayUserModel alipayUser = alipayUserQueryService.query(alipayUserId);
                if (null == alipayUser) {
                    // getLogger().warn("get alipay user is null where alipayUserId:" + alipayUserId);
                    return false;
                }
                String alipayName = StringUtil.trimToEmpty(alipayUser.getReal_name());
                String alipayCertNum = StringUtil.trimToEmpty(alipayUser.getCert_no());

                // 保存支付宝过程数据
                bindRelDO.setAlipayName(alipayName);
                bindRelDO.setAlipayIdcard(alipayCertNum);

                // 5、支付宝账户为个人账户
                if (!StringUtil.equals(PERSON_ALIPAY_USER, alipayUser.getUser_type())) {
                    // getLogger().warn("alipay type is not personal where memberId:" + memberId);
                    return false;
                }
                // 6、支付宝通过了身份证认证
                if (!alipayUser.isIs_id_auth()) {
                    // getLogger().warn("alipay is not is authed where memberId:" + memberId);
                    return false;
                }
                // 7、支付宝姓名是否与网站一致
                MemberModel memberModel = memberReadService.findMember(memberId);
                String alibabaName = StringUtil.trimToEmpty(memberModel.getAdminPerson().getFirstName());
                if (!StringUtil.equalsIgnoreCase(alipayName, alibabaName)
                    || !StringUtil.equalsIgnoreCase(pnaName, alipayName)) {
                    getLogger().warn("userName is diff where uicName:" + alibabaName + ",alipayName:" + alipayName
                                             + ",pnaName:" + pnaName + ",memberId:" + memberId);
                    return false;
                }

                // 8、支付宝证件类型为身份证
                if (!CERT_NUM_TYPE.equals(alipayUser.getCert_type())) {
                    // getLogger().warn("alipay cert type is not shenfengzheng where memberId:" + memberId);
                    return false;
                }
                // 9、支付宝证件号18位
                if (alipayCertNum.length() != 18) {
                    // getLogger().warn("alipay certnum is not 18 bit where memberId:" + memberId);
                    return false;
                }
                return true;
            }

            // 开始华丽丽的同步了
            private void doSyncPNA(BindRelDO bindRelDO) {
                String memberId = bindRelDO.getMemberId();

                AuthRelDO authRelDO = authRelDAO.findAuthRel("PNA", "ALIBABA_CN", memberId, "M");
                if (null == authRelDO) {
                    getLogger().error("sync2pna where authrelDO is null,memberId:" + memberId);
                    throw new RuntimeException("sync2pna where authrelDO is null,memberId:" + memberId);
                }
                long authId = authRelDO.getAuthId();
                PNAInfoDO pnaInfoDO = new PNAInfoDO();
                pnaInfoDO.setCertNum(bindRelDO.getAlipayIdcard());
                pnaInfoDO.setId(authId);
                Set<String> changeProperties = new HashSet<String>();
                changeProperties.add("certNum");
                boolean updateRes = pnaInfoDAO.update(pnaInfoDO, changeProperties);
                if (!updateRes) {
                    getLogger().error("update pna error where memberId:" + memberId);
                    throw new RuntimeException("update pna error where memberId:" + memberId);
                }
                getLogger().info("update pna  success where memberId:" + memberId + ",oldCert:"
                                         + bindRelDO.getPnaIdcard() + ",newCert:" + bindRelDO.getAlipayIdcard());
            }

            /**
             * @description 是否需要同步UIC
             * @param bindRelDO
             * @return
             * @throws
             */
            private boolean canSyncUIC(BindRelDO bindRelDO) {
                long userId = memberReadService.convertMemberIdToUserId(bindRelDO.getMemberId());
                bindRelDO.setUserId(userId);
                // 1、未通过支付宝实名认证
                AlipayAuthStatus alipayAuthStatus = uicUtilService.getAlipayAuthStatus(userId);
                if (AlipayAuthStatus.ENTERPRISE_AUTH.equals(alipayAuthStatus)
                    || AlipayAuthStatus.PERSONAL_AUTH.equals(alipayAuthStatus)) {
                    return false;
                }
                // 2、无talos、talosing、pna标 或者 有pna标且realName一致且idCard为15位
                ExtraUserDO extraUserDO = uicUtilService.findExtraUserDO(userId);
                BaseUserDO baseUserDO = uicUtilService.getUser(userId);
                String uicName = StringUtil.trimToEmpty(baseUserDO.getFullname());
                String uicCertNum = StringUtil.trimToEmpty(baseUserDO.getIdCardNumber());
                // 保存UIC过程数据
                bindRelDO.setUicName(uicName);
                bindRelDO.setUicIdcard(uicCertNum);

                if (isTalos(extraUserDO.getUserTag6()) || isTalosing(extraUserDO.getUserTag6())) {
                    return false;
                }
                if (isPNA(extraUserDO.getUserTag7())) {
                    if (!StringUtil.equalsIgnoreCase(uicName, bindRelDO.getPnaName()) || uicCertNum.length() == 18) {
                        return false;
                    }
                }
                return true;
            }

            private void doSyncUIC(BindRelDO bindRelDO) {
                boolean updateRes = uicUtilService.updateUser(bindRelDO.getUserId(), bindRelDO.getUicName(),
                                                              bindRelDO.getAlipayIdcard());
                if (!updateRes) {
                    getLogger().error("update uic error where memberId:" + bindRelDO.getMemberId());
                    throw new RuntimeException("update uic error where memberId:" + bindRelDO.getMemberId());
                }
                getLogger().info("update uic success where memberId:" + bindRelDO.getMemberId() + ",oldCert:"
                                         + bindRelDO.getUicIdcard() + ",newCert:" + bindRelDO.getAlipayIdcard());
            }

            private boolean isPNA(long userTag7) {
                return (userTag7 >> 52 & 1) == 1;
            }

            private boolean isTalos(long userTag6) {
                return (userTag6 >> 57 & 1) == 1;
            }

            private boolean isTalosing(long userTag6) {
                return (userTag6 >> 55 & 1) == 1;
            }

        };
    }


}
