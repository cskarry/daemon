package com.alibaba.china.talos.file.interrupt;

import java.io.File;

import sun.nio.ch.Interruptible;

/**
 * @description 线程中断的实现，自定义中断回调函数
 * @author karry
 * @date 2014-1-27 下午3:52:22
 */
abstract class AbstractInterruptTask implements InterruptAbleFileTask {

    private volatile boolean interrupted = false;
    private Interruptible    interruptor = new Interruptible() {

                                             @Override
                                             public void interrupt() {
                                                 interrupted = true;
                                                 AbstractInterruptTask.this.interrupt();
                                             }
                                         };

    public final boolean execute() throws InterruptedException {
        try {
            blockedOn(interruptor);
            if (Thread.currentThread().isInterrupted()) {
                interruptor.interrupt();
            }
            // 执行业务代码
            bussiness();
        } finally {
            blockedOn(null);
        }
        return interrupted;
    }

    public abstract void bussiness();

    public abstract void interrupt();

    public abstract File getFile();

    @Override
    public boolean isInterrupt() {
        return interrupted;
    }

    @Override
    public void triggerInterrupt() {
        interruptor.interrupt();
    }

    static void blockedOn(Interruptible intr) {
        sun.misc.SharedSecrets.getJavaLangAccess().blockedOn(Thread.currentThread(), intr);
    }
}