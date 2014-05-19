package com.alibaba.china.talos.file.interrupt;

import java.util.List;

import sun.misc.Signal;
import sun.misc.SignalHandler;

import com.alibaba.common.lang.StringUtil;
import com.alibaba.common.logging.Logger;
import com.alibaba.common.logging.LoggerFactory;

public class CommonSignalHandle implements SignalHandler {

    private static Logger       logger     = LoggerFactory.getLogger(CommonSignalHandle.class);

    private static final String SPEED_UP   = "USR1";

    private static final String SPEED_DOWN = "USR2";

    private SignalHandler oldHandler;

    private FileTaskHolder      fHolder;

    private FileTaskAO          fAO;

    public CommonSignalHandle(FileTaskHolder fHolder, FileTaskAO fAO){
        this.fHolder = fHolder;
        this.fAO = fAO;
    }

    public void start() {
        install(SPEED_UP);
        install(SPEED_DOWN);
    }

    public SignalHandler install(String signalName) {
        Signal diagSignal = new Signal(signalName);
        this.oldHandler = Signal.handle(diagSignal, this);
        return this;
    }

    public void handle(Signal signal) {
        logger.info("Signal handler called for signal " + signal);
        try {
            signalAction(signal);

            if (oldHandler != SIG_DFL && oldHandler != SIG_IGN) {
                oldHandler.handle(signal);
            }

        } catch (Exception e) {
            logger.error("handle|Signal handler failed, reason " + e.getMessage(), e);
        }
    }

    public void signalAction(Signal signal) {
        logger.info("Handling " + signal.getName());
        List<InterruptAbleFileTask> interruptAbleFileTasks = fAO.assembleInterruptAbleFileTasks();
        if (StringUtil.equals(signal.getName(), SPEED_UP)) {
            logger.info("Handling speed up" + signal.getName());
            fHolder.switch2MaxMode(interruptAbleFileTasks);
        } else if (StringUtil.equals(signal.getName(), SPEED_DOWN)) {
            logger.info("Handling speed down" + signal.getName());
            fHolder.switch2MinMode(interruptAbleFileTasks);
        } else {

        }
    }


    public static void main(String[] args) throws InterruptedException {
        CommonSignalHandle signalHandle = new CommonSignalHandle(null, null);
        signalHandle.start();
        System.out.println("Signal handling example.");
        Thread.sleep(1000000);

    }
}
