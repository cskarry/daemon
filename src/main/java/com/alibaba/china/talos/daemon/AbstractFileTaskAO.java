package com.alibaba.china.talos.daemon;

import com.alibaba.china.talos.file.FileAnalyzer;
import com.alibaba.china.talos.file.FileProcessParam;
import com.alibaba.china.talos.file.FileProcessor;
import com.alibaba.china.talos.file.LineAnalyzer;
import com.alibaba.china.talos.file.LineHandle;
import com.alibaba.common.logging.Logger;
import com.alibaba.common.logging.LoggerFactory;


public abstract class AbstractFileTaskAO<T> implements BaseAO {

    private static final String K_FILE_PATH      = "filePath";

    private static final String K_MAX_THREAD_NUM = "maxThreadNum";

    private static final String K_MIN_THREAD_NUM = "minThreadNum";

    private final Logger        logger           = LoggerFactory.getLogger(this.getClass());

    @Override
    public void execute() {
        LineAnalyzer<T> lineAnalyzer = getLineAnalyzer();
        LineHandle<T> lineHandle = getLineHandle();
        FileAnalyzer<T> fileAnalyzer = new FileAnalyzer<T>(lineAnalyzer, lineHandle);
        FileProcessParam fpp = getFileProcessParam();
        FileProcessor fp = new FileProcessor(fpp, fileAnalyzer);
        fp.process();
    }
    
    public abstract LineAnalyzer<T> getLineAnalyzer();

    public abstract LineHandle<T> getLineHandle();

    private FileProcessParam getFileProcessParam() {
        String path = System.getProperty(K_FILE_PATH);
        String maxThreadNum = System.getProperty(K_MAX_THREAD_NUM);
        String minThreadNum = System.getProperty(K_MIN_THREAD_NUM);
        return new FileProcessParam(path, Integer.parseInt(minThreadNum), Integer.parseInt(maxThreadNum));
    }

    public Logger getLogger() {
        return logger;
    }

}
