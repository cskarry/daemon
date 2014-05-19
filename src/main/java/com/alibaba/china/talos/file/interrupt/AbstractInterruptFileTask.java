package com.alibaba.china.talos.file.interrupt;

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

/**
 * @description 任务中断的实现,非单例
 * @author karry
 * @date 2014-1-27 下午4:02:08
 */
public abstract class AbstractInterruptFileTask extends AbstractInterruptTask {

    private static Logger       logger   = LoggerFactory.getLogger(AbstractInterruptFileTask.class);

    String                encoding = "GBK";

    File                        taskMetaFile;

    long                        processedLines;

    InputStream           stream;

    AbstractInterruptFileTask(File taskMetaFile, long processedLines, String encoding){
        this.taskMetaFile = taskMetaFile;
        this.processedLines = processedLines;
        this.encoding = encoding;
    }

    AbstractInterruptFileTask(File taskMetaFile, String encoding){
        this.taskMetaFile = taskMetaFile;
        this.encoding = encoding;
    }

    @Override
    public void bussiness() {
        this.processedLines = FileRecordContext.getProcessedLines(taskMetaFile);
        if (processedLines == FileRecordContext.END_FLAG) {
            return;
        }
        // get stream
        try {
            stream = new FileInputStream(taskMetaFile);
        } catch (FileNotFoundException e) {
            logger.error("file is not exist! where filePath is " + taskMetaFile.getPath());
            return;
        }
        // get lineIterator
        LineIterator lineIterator = null;
        try {
            lineIterator = IOUtils.lineIterator(stream, encoding);
        } catch (IOException e) {
            logger.error("stream read failed where filePath is " + taskMetaFile.getPath());
            return;
        }
        // process lineIterator
        try {
            process(lineIterator);
        } catch (Exception e) {
            logger.error("process step occured some error! where filePath is " + taskMetaFile.getPath(), e);
        } finally {
            statusRecord();
            LineIterator.closeQuietly(lineIterator);
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    logger.error("stream close failed!", e);
                }
            }
        }

    }

    private void process(LineIterator lineIterator) {
        if (null == lineIterator) {
            return;
        }
        // step to right line
        initLineIterator(lineIterator);
        String lineContext = "";
        while (lineIterator.hasNext() && !isInterrupt()) {
            if (isInterrupt()) {
                logger.info("has interrupted where file:" + taskMetaFile.getName() + " and lines:" + processedLines);
                return;
            }
            String content = lineIterator.nextLine();
            processedLines++;
            if (StringUtil.isBlank(content)) {
                logger.error(String.format("This line is blank,Line number = [%s]", processedLines));
                continue;
            }
            lineContext = content.trim();
            doProcess(lineContext);
        }
    }

    abstract void doProcess(String lineContext);

    private void statusRecord() {
        FileRecordContext.recordProcessedLines(this.taskMetaFile, this.processedLines);
    }

    private void initLineIterator(LineIterator lineIterator) {
        int i = 0;
        while (i < processedLines) {
            if (!lineIterator.hasNext()) {
                throw new RuntimeException("file to end but processedLine is:" + processedLines + ",file is "
                                           + taskMetaFile.getName());
            }
            lineIterator.nextLine();
            i++;
        }
    }

    @Override
    public void interrupt() {
        logger.info("begin to interrupt file:" + taskMetaFile.getName());
        /*
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                logger.error("stream close failed! where file is " + taskMetaFile.getName(), e);
            }
        }
        */
    }

    public static Logger getLogger() {
        return logger;
    }

    @Override
    public File getFile() {
        return taskMetaFile;
    }

}
