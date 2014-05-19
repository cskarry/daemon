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
 * @description 15λ���֤refresh
 * 
 * <li>�����߼��������ӵģ����ж��Ƿ���Ҫˢ�£����ˢ��pna��¼�����ˢ��uic��¼</li>
 * <li>����Щ�û�����Ҫˢ��pna���أ�������������&���߼�</li>
 * <li>1��ͨ����pna��֤</li>
 * <li>2��ͨ����ʽΪ֧���������֤</li>
 * <li>3��PNA֤������Ϊ���֤</li>
 * <li>4��PNA��¼��֤�������18λ</li>
 * <li>5��֧�����˻�Ϊ�����˻�</li>
 * <li>6��֧����ͨ�������֤��֤</li>
 * <li>7��֧������������վ����һ��</li>
 * <li>8��֧����֤��������Ϊ���֤</li>
 * <li>9��֧����֤����Ϊ18λ</li>
 * <li>��������Ҫˢ��uic�ˣ�����������Ȼ��&���߼�</li>
 * <li>1��δͨ��֧����ʵ����֤</li>
 * <li>2����talos��talosing��pna�ı� || ��pna����realNameһ����idCardΪ15λ</li>
 * <li>�ţ����Ѿ����Ź��ϵķ��հ�UICˢ�����ˣ����ʱ���Ҳ���Ҫ����Ϊʲôÿ�춼Ҫ������ô�����߰����������</li>
 * <li>���ƹⶩ�����߼�����ô���ģ�ͻȻ�뵽�Ա���xfileԤ����������һ�����˶�û�˹ܣ����Ϸ�����ҪPEһ̨һ̨��������ѹ������Ӧ��</li>
 * <li>������Ҫ��102��Ĺ�˾�����߼����ҳ�������</li>
 * <li>���ҵ�ǰ��Ҫ���ľ��ǻ�������д���룬������ԵĻ�������֤û�й��ϣ������е�С�꣬ע���е���ˣ���ע������</li>
 * 
 * 
 * @author karry
 * @date 2014-4-3 ����3:17:03
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
             * @description �Ƿ���Ҫͬ��PNA
             * @param bindRelDO
             * @return
             * @throws
             */
            private boolean canSyncPNA(BindRelDO bindRelDO) {
                String memberId = bindRelDO.getMemberId();
                String alipayUserId = bindRelDO.getAlipayUserId();
                // 1���Ƿ�ͨ��PNA��֤
                Owner owner = new Owner(memberId);
                UnitedAuthModel uam = authReadService.findAuthByOwner(owner, AuthTypeEnum.PNA);
                if (null == uam || !uam.hasPNA() || null == uam.getPNAInfoModel()) {
                    // getLogger().warn("has not pass pna where memberId:" + memberId);
                    return false;
                }

                PNAInfoModel pnaInfoModel = uam.getPNAInfoModel();
                String pnaName = StringUtil.trimToEmpty(pnaInfoModel.getName());
                String pnaCertNum = StringUtil.trimToEmpty(pnaInfoModel.getCertNum());

                // ����pna��������
                bindRelDO.setPnaName(pnaName);
                bindRelDO.setPnaIdcard(pnaCertNum);

                // 2���Ƿ�ͨ��֧���������֤
                if (!StringUtil.equals(ALIPAY_PROVIDER, pnaInfoModel.getAuthProvider().getProviderId())) {
                    // getLogger().warn("pna provider is not alipay where memberId:" + memberId);
                    return false;
                }

                // 3��pna֤������Ϊ���֤
                if (!CertTypeEnum.ID_CARD.equals(pnaInfoModel.getCertType())) {
                    // getLogger().warn("pna cert type is not shenfengzheng where memberId:" + memberId);
                    return false;
                }
                // 4�����֤�����18λ
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

                // ����֧������������
                bindRelDO.setAlipayName(alipayName);
                bindRelDO.setAlipayIdcard(alipayCertNum);

                // 5��֧�����˻�Ϊ�����˻�
                if (!StringUtil.equals(PERSON_ALIPAY_USER, alipayUser.getUser_type())) {
                    // getLogger().warn("alipay type is not personal where memberId:" + memberId);
                    return false;
                }
                // 6��֧����ͨ�������֤��֤
                if (!alipayUser.isIs_id_auth()) {
                    // getLogger().warn("alipay is not is authed where memberId:" + memberId);
                    return false;
                }
                // 7��֧���������Ƿ�����վһ��
                MemberModel memberModel = memberReadService.findMember(memberId);
                String alibabaName = StringUtil.trimToEmpty(memberModel.getAdminPerson().getFirstName());
                if (!StringUtil.equalsIgnoreCase(alipayName, alibabaName)
                    || !StringUtil.equalsIgnoreCase(pnaName, alipayName)) {
                    getLogger().warn("userName is diff where uicName:" + alibabaName + ",alipayName:" + alipayName
                                             + ",pnaName:" + pnaName + ",memberId:" + memberId);
                    return false;
                }

                // 8��֧����֤������Ϊ���֤
                if (!CERT_NUM_TYPE.equals(alipayUser.getCert_type())) {
                    // getLogger().warn("alipay cert type is not shenfengzheng where memberId:" + memberId);
                    return false;
                }
                // 9��֧����֤����18λ
                if (alipayCertNum.length() != 18) {
                    // getLogger().warn("alipay certnum is not 18 bit where memberId:" + memberId);
                    return false;
                }
                return true;
            }

            // ��ʼ��������ͬ����
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
             * @description �Ƿ���Ҫͬ��UIC
             * @param bindRelDO
             * @return
             * @throws
             */
            private boolean canSyncUIC(BindRelDO bindRelDO) {
                long userId = memberReadService.convertMemberIdToUserId(bindRelDO.getMemberId());
                bindRelDO.setUserId(userId);
                // 1��δͨ��֧����ʵ����֤
                AlipayAuthStatus alipayAuthStatus = uicUtilService.getAlipayAuthStatus(userId);
                if (AlipayAuthStatus.ENTERPRISE_AUTH.equals(alipayAuthStatus)
                    || AlipayAuthStatus.PERSONAL_AUTH.equals(alipayAuthStatus)) {
                    return false;
                }
                // 2����talos��talosing��pna�� ���� ��pna����realNameһ����idCardΪ15λ
                ExtraUserDO extraUserDO = uicUtilService.findExtraUserDO(userId);
                BaseUserDO baseUserDO = uicUtilService.getUser(userId);
                String uicName = StringUtil.trimToEmpty(baseUserDO.getFullname());
                String uicCertNum = StringUtil.trimToEmpty(baseUserDO.getIdCardNumber());
                // ����UIC��������
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
