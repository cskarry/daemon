package com.alibaba.china.talos.file.interrupt;

import java.io.File;

import sun.nio.ch.Interruptible;

/**
 * @description �߳��жϵ�ʵ�֣��Զ����жϻص�����
 * @author karry
 * @date 2014-1-27 ����3:52:22
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
            // ִ��ҵ�����
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