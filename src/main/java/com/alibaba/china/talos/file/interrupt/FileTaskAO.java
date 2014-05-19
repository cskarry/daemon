package com.alibaba.china.talos.file.interrupt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.china.talos.daemon.BaseAO;
import com.alibaba.china.talos.file.LineAnalyzer;
import com.alibaba.china.talos.file.LineHandle;
import com.alibaba.china.talos.file.interrupt.DefaultInterruptFileTask;
import com.alibaba.china.talos.file.interrupt.FileRecordContext.ComparatorModel;
import com.alibaba.china.talos.file.interrupt.InterruptAbleFileTask;
import com.alibaba.common.logging.Logger;
import com.alibaba.common.logging.LoggerFactory;


public abstract class FileTaskAO<T> implements BaseAO {

    private static final Logger log = LoggerFactory.getLogger(FileTaskAO.class);

    String                      path;
    int                         maxThreadNum;
    int                         minThreadNum;
    FileRecordContext           fContext;
    FileTaskHolder              fHolder;
    boolean                     init;

    private void init() {
        // 读取系统参数
        path = System.getProperty("filePath");
        maxThreadNum = Integer.parseInt(System.getProperty("maxThreadNum"));
        minThreadNum = Integer.parseInt(System.getProperty("minThreadNum"));
        // 初始化context
        fContext = new FileRecordContext(path);
        // 初始化TaskHolder
        fHolder = new FileTaskHolder(minThreadNum, maxThreadNum);
        // 初始化SignalHandle
        CommonSignalHandle signalHandle = new CommonSignalHandle(fHolder, this);
        signalHandle.start();
        init = true;
    }
    
    public abstract LineAnalyzer<T> getLineAnalyzer();

    public abstract LineHandle<T> getLineHandle();

    @Override
    public void execute() {
        if (!init) {
            init();
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                log.info("ShutdownHook triggered!");
                try {
                    fHolder.removeAllTask();
                    fContext.dump();
                } catch (IOException e) {
                    log.error("dump occured io error!", e);
                }
            }
        });

        List<InterruptAbleFileTask> interruptAbleFileTasks = assembleInterruptAbleFileTasks();
        fHolder.executeTask4Max(interruptAbleFileTasks);

        try {
            Thread.sleep(15552000000L);
        } catch (InterruptedException e) {
            log.error("main thread sleep error", e);
        }
    }

    public List<InterruptAbleFileTask> assembleInterruptAbleFileTasks() {
        List<ComparatorModel> ComparatorModels = fContext.fetchTaskFiles();
        List<InterruptAbleFileTask> interruptAbleFileTasks = new ArrayList<InterruptAbleFileTask>();
        for (ComparatorModel comparatorModel : ComparatorModels) {
            File f = comparatorModel.getF();
            DefaultInterruptFileTask<T> fTask = new DefaultInterruptFileTask<T>(f, "GBK");
            fTask.refreshHandle(getLineAnalyzer(), getLineHandle());
            interruptAbleFileTasks.add(fTask);
        }
        return interruptAbleFileTasks;
    }

}
