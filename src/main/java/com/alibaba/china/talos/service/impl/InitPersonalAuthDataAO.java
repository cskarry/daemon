package com.alibaba.china.talos.service.impl;

import java.text.ParseException;
import java.util.Date;

import org.apache.commons.lang.time.DateUtils;

import com.alibaba.china.talos.daemon.BaseAO;
import com.alibaba.china.talos.file.FileAnalyzer;
import com.alibaba.china.talos.file.FileProcessParam;
import com.alibaba.china.talos.file.FileProcessor;
import com.alibaba.china.talos.file.LineAnalyzer;
import com.alibaba.china.talos.model.PersonalModel;
import com.alibaba.common.logging.Logger;
import com.alibaba.common.logging.LoggerFactory;


public class InitPersonalAuthDataAO implements BaseAO {

    private InitPeronalAuthDataHandle initPeronalAuthDataHandle;

    private static final Logger       log = LoggerFactory.getLogger(InitPersonalAuthDataAO.class);

    @Override
    public void execute() {
        LineAnalyzer<PersonalModel> lineAnalyzer = new LineAnalyzer<PersonalModel>() {

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
        FileAnalyzer<PersonalModel> fileAnalyzer = new FileAnalyzer<PersonalModel>(lineAnalyzer,
                                                                                   initPeronalAuthDataHandle);
        FileProcessParam fpp = getFileProcessParam();
        
        FileProcessor fp = new FileProcessor(fpp, fileAnalyzer);

        fp.process();

    }

    private FileProcessParam getFileProcessParam() {
        String path = System.getProperty("filePath");
        String maxThreadNum = System.getProperty("maxThreadNum");
        String minThreadNum = System.getProperty("minThreadNum");
        return new FileProcessParam(path, Integer.parseInt(minThreadNum), Integer.parseInt(maxThreadNum));
    }

    public void setInitPeronalAuthDataHandle(InitPeronalAuthDataHandle initPeronalAuthDataHandle) {
        this.initPeronalAuthDataHandle = initPeronalAuthDataHandle;
    }

}
