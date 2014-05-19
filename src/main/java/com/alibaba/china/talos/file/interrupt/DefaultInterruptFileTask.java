package com.alibaba.china.talos.file.interrupt;

import java.io.File;

import com.alibaba.china.talos.file.LineAnalyzer;
import com.alibaba.china.talos.file.LineHandle;

public class DefaultInterruptFileTask<T> extends AbstractInterruptFileTask {

    LineAnalyzer<T> lineAnalyzer;

    LineHandle<T>   lineHandle;

    public DefaultInterruptFileTask(File taskMetaFile, String encoding){
        super(taskMetaFile, encoding);
    }

    public DefaultInterruptFileTask(File taskMetaFile, long processedLines, String encoding){
        super(taskMetaFile, processedLines, encoding);
    }

    public void refreshHandle(LineAnalyzer<T> lineAnalyzer, LineHandle<T> lineHandle) {
        this.lineAnalyzer = lineAnalyzer;
        this.lineHandle = lineHandle;
    }


    @Override
    void doProcess(String lineContext) {
        if (null == lineAnalyzer || null == lineHandle) {
            throw new RuntimeException("miss lineAnalyzer or lineHandle where file is " + this.taskMetaFile.getName());
        }
        T model = null;
        boolean handleRes = false;
        try {
            model = lineAnalyzer.analyse(lineContext);
            handleRes = lineHandle.handle(model);
        } catch (Exception e) {
            getLogger().error("handle lineContext error where lineContext:" + lineContext, e);
        }
        if (handleRes) {
            getLogger().info("process succeed,lineContext=" + lineContext);
        } else {
            getLogger().error("process failed,lineContext=" + lineContext);
        }

    }


}
