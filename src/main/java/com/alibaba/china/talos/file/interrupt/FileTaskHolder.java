package com.alibaba.china.talos.file.interrupt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.alibaba.china.talos.file.interrupt.InterruptAbleFileTask;
import com.alibaba.common.logging.Logger;
import com.alibaba.common.logging.LoggerFactory;

public class FileTaskHolder {

    int                         minThreadCount;

    int                         maxThreadCount;

    ExecutorService             exec;

    Mode                        mode                   = Mode.MAX;

    List<InterruptAbleFileTask> interruptAbleFileTasks = new ArrayList<InterruptAbleFileTask>();

    private static Logger       logger                 = LoggerFactory.getLogger(FileTaskHolder.class);

    public FileTaskHolder(int minThreadCount, int maxThreadCount){
        this.minThreadCount = minThreadCount;
        this.maxThreadCount = maxThreadCount;
    }

    public void executeTask4Min(List<InterruptAbleFileTask> interruptAbleFileTasks) {
        this.interruptAbleFileTasks = interruptAbleFileTasks;
        this.exec = Executors.newFixedThreadPool(minThreadCount);
        for (InterruptAbleFileTask interruptAbleFileTask : interruptAbleFileTasks) {
            exec.execute(new Worker(interruptAbleFileTask));
        }
        exec.shutdown();
        mode = Mode.MIN;
    }

    public void executeTask4Max(List<InterruptAbleFileTask> interruptAbleFileTasks) {
        this.interruptAbleFileTasks = interruptAbleFileTasks;
        this.exec = Executors.newFixedThreadPool(maxThreadCount);
        for (InterruptAbleFileTask interruptAbleFileTask : interruptAbleFileTasks) {
            exec.execute(new Worker(interruptAbleFileTask));
        }
        exec.shutdown();
        mode = Mode.MAX;
    }

    public void switch2MaxMode(List<InterruptAbleFileTask> interruptAbleFileTasks) {
        if (mode.equals(Mode.MAX)) {
            return;
        } else {
            removeAllTask();

            executeTask4Max(interruptAbleFileTasks);
        }
    }

    public void switch2MinMode(List<InterruptAbleFileTask> interruptAbleFileTasks) {
        if (mode.equals(Mode.MIN)) {
            return;
        } else {
            removeAllTask();

            executeTask4Min(interruptAbleFileTasks);
        }
    }

    public void removeAllTask() {
        for (InterruptAbleFileTask interruptAbleFileTask : interruptAbleFileTasks) {
            interruptAbleFileTask.triggerInterrupt();
        }
        interruptAbleFileTasks.clear();
        try {
            exec.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("remove all tasks occured error ", e);
        }

    }

    class Worker implements Runnable {

        InterruptAbleFileTask interruptAbleFileTask;

        public Worker(InterruptAbleFileTask interruptAbleFileTask){
            this.interruptAbleFileTask = interruptAbleFileTask;
        }

        @Override
        public void run() {
            try {
                logger.info("begin to analyze file " + interruptAbleFileTask.getFile().getName());
                interruptAbleFileTask.execute();
            } catch (Exception e) {
                logger.error("analyze file occured error where file is " + interruptAbleFileTask.getFile().getName(), e);
            } finally {
                logger.info("end to analyze file " + interruptAbleFileTask.getFile().getName());
            }

        }

    }

    enum Mode {
        MIN, MAX;
    }

}
