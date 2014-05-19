package com.alibaba.china.app;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import sun.nio.ch.Interruptible;

interface InterruptAble {

    public void interrupt() throws InterruptedException;
}

abstract class InterruptSupport implements InterruptAble {

    private volatile boolean interrupted = false;
    private Interruptible    interruptor = new Interruptible() {

                                             public void interrupt() {
                                                 interrupted = true;
                                                 InterruptSupport.this.interrupt();
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

    static void blockedOn(Interruptible intr) {
        sun.misc.SharedSecrets.getJavaLangAccess().blockedOn(Thread.currentThread(), intr);
    }
}

class TestInterruptSupport extends InterruptSupport {

    @Override
    public void bussiness() {
        System.out.println(Thread.currentThread().getName() + " start!");
        while (true) {
            // System.out.println(Thread.currentThread().getName() + " running!");
        }

    }

    @Override
    public void interrupt() {
        System.out.println(Thread.currentThread().getName() + " end!");

    }

}

public class TestHook {

    public static void testThread() throws InterruptedException {
        final TestInterruptSupport testInterruptSupport = new TestInterruptSupport();
        Thread t = new Thread() {

            @Override
            public void run() {
                long start = System.currentTimeMillis();
                try {
                    System.out.println("InterruptRead start!");
                    testInterruptSupport.execute();
                } catch (InterruptedException e) {
                    System.out.println("InterruptRead end! cost time : " + (System.currentTimeMillis() - start));
                    e.printStackTrace();
                }
            }
        };
        t.start();
        Thread.sleep(3000);
        // System.exit(0);
        t.interrupt();
    }

    public void testRunnable() throws InterruptedException {
        final TestInterruptSupport testInterruptSupport = new TestInterruptSupport();

        ExecutorService executor = Executors.newFixedThreadPool(5);
        executor.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    testInterruptSupport.execute();
                } catch (InterruptedException e) {

                }

            }

        });
        Thread.sleep(3000);
    }

    /**
     * @throws InterruptedException
     * @description TODO
     * @param args
     * @throws
     */
    public static void main(String[] args) throws InterruptedException {
        testThread();
    }
        

}
