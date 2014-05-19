package com.alibaba.china.talos.service.impl;

import java.text.ParseException;
import java.util.Date;

import org.apache.commons.lang.time.DateUtils;

import com.alibaba.china.talos.daemon.BaseAO;
import com.alibaba.china.talos.file.LineAnalyzer;
import com.alibaba.china.talos.file.LineHandle;
import com.alibaba.china.talos.file.interrupt.FileTaskAO;
import com.alibaba.china.talos.model.PersonalModel;
import com.alibaba.common.logging.Logger;
import com.alibaba.common.logging.LoggerFactory;


/**
 * @description 可中断可伸缩的任务实现
 * @author karry
 * @date 2014-2-13 下午4:41:42
 */
public class InitPersonalAuthData4InterruptFileTaskAO extends FileTaskAO<PersonalModel> implements BaseAO {

    private InitPeronalAuthDataHandle initPeronalAuthDataHandle;

    private static final Logger       log = LoggerFactory.getLogger(InitPersonalAuthData4InterruptFileTaskAO.class);

    public void setInitPeronalAuthDataHandle(InitPeronalAuthDataHandle initPeronalAuthDataHandle) {
        this.initPeronalAuthDataHandle = initPeronalAuthDataHandle;
    }

    @Override
    public LineAnalyzer<PersonalModel> getLineAnalyzer() {
        return new LineAnalyzer<PersonalModel>() {

            @Override
            public PersonalModel analyse(String lineText) {
                // 有四个参数需要解析，userId、source、gmtModitified、certStatus
                String[] lineSep = lineText.split(",");
                if (null == lineSep || lineSep.length != 4) {
                    throw new RuntimeException("line text is invalid! where line is " + lineText);
                }
                Long userId = Long.parseLong(getMetaData(lineSep[0]));
                String source = getMetaData(lineSep[1]);
                String passTime = getMetaData(lineSep[2]);
                String certStatus = getMetaData(lineSep[3]);
                Date passDate = new Date();
                try {
                    passDate = DateUtils.parseDate(passTime, new String[] { "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss" });
                } catch (ParseException e) {
                    log.warn("parse date error where passDate:" + passTime);
                }
                PersonalModel personalModel = new PersonalModel();
                personalModel.setUserId(userId);
                personalModel.setSource(source);
                personalModel.setPassDate(passDate);
                personalModel.setSuccess(certStatus.equals("1"));
                return personalModel;
            }

            private String getMetaData(String data) {
                return data.substring(1, data.length() - 1);
            }

        };
    }

    @Override
    public LineHandle<PersonalModel> getLineHandle() {
        return initPeronalAuthDataHandle;
    }

}
