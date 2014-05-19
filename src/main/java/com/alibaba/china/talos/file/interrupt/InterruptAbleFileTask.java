package com.alibaba.china.talos.file.interrupt;

import java.io.File;

/**
 * @description 可中断的任务类，控制任务的执行速度和中断恢复任务功能
 * @author karry
 * @date 2014-2-12 下午7:52:18
 */
public interface InterruptAbleFileTask {

    public boolean execute() throws InterruptedException;

    public File getFile();

    public void interrupt();

    public void triggerInterrupt();

    public boolean isInterrupt();


}