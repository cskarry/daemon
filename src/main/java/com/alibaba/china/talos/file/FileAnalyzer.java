package com.alibaba.china.talos.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

import com.alibaba.common.lang.StringUtil;
import com.alibaba.common.logging.Logger;
import com.alibaba.common.logging.LoggerFactory;
import com.alibaba.fastjson.JSON;


public class FileAnalyzer<T> {

    private static Logger                     logger       = LoggerFactory.getLogger(FileAnalyzer.class);

    private static final String               ENCODING     = "GBK";

    private LineAnalyzer<T>     lineAnalyzer;

    private LineHandle<T>       lineHandle;

    public FileAnalyzer(LineAnalyzer<T> lineAnalyzer, LineHandle<T> lineHandle){
        this.lineAnalyzer = lineAnalyzer;
        this.lineHandle = lineHandle;
    }

    public void analyse(File f) {
        InputStream stream = null;
        try {
            stream = new FileInputStream(f);
        } catch (FileNotFoundException e) {
            logger.error("file is not exist! where filePath is " + f.getPath());
            return;
        }
        LineIterator lineIterator = null;
        try {
            lineIterator = IOUtils.lineIterator(stream, ENCODING);
        } catch (IOException e) {
            logger.error("stream read failed where filePath is " + f.getPath());
            return;
        }
        try {
            process(lineIterator);
        } catch (Exception e) {
            logger.error("process step occured some error! where filePath is " + f.getPath(), e);
        } finally {
            LineIterator.closeQuietly(lineIterator);
            if (stream != null) try {
                stream.close();
            } catch (IOException e) {
                logger.error("stream close failed!", e);
            }
        }
    }

    public void process(LineIterator lineIterator) {
        if (null == lineIterator) return;
        int line = 0;
        String lineContext = "";
        while (lineIterator.hasNext()) {
            line++;
            String content = lineIterator.nextLine();
            if (StringUtil.isBlank(content)) {
                logger.error(String.format("This line is blank,Line number = [%s]", line));
                continue;
            }
            lineContext = content.trim();
            T model = null;
            boolean handleRes = false;            
            try {
                model = lineAnalyzer.analyse(lineContext);
                // 你一定很想知道究竟有没有解析错误，会不是该死的dba给你了一份xls文件，而你一直以为是文本文件
                logger.info("analyser ok and result model is:" + JSON.toJSONString(model));
                handleRes = lineHandle.handle(model);
            } catch (Exception e) {
                logger.error("handle lineContext error where lineContext:" + lineContext, e);
            }
            if (handleRes) {
                logger.info("process succeed,lineContext=" + lineContext);
            } else {
                logger.error("process failed,lineContext=" + lineContext);
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                logger.error("sleep error!", e);
            }
        }
    }

}
