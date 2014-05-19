package com.alibaba.china.app;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class Test {

    class Work implements Runnable {

        int i;
        Test test;

        public Work(int i, Test test){
            this.i = i;
            this.test = test;
        }

        @Override
        public void run() {
            System.out.println("work:" + i);
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
            }
        }
    }

    class Work1 implements Runnable {

        int i;
        Test test;

        public Work1(int i, Test test){
            this.i = i;
            this.test = test;
        }

        @Override
        public void run() {
            System.out.println("work1:" + i);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
            }
            synchronized (Test.this) {
                try {
                    test.wait();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            System.out.println("work1~:" + i);

        }
    }
    
    class Work2 implements Runnable {

        int  i;
        Test test;

        public Work2(int i, Test test){
            this.i = i;
            this.test = test;
        }

        @Override
        public void run() {
            System.out.println("work2:" + i);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
            }
            synchronized (Test.this) {
                try {
                    test.notifyAll();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            System.out.println("work2~:" + i);

        }
    }
    
    static class Work3 extends Thread {

        @Override
        public void run() {
            try {
                int i = 0;
                while (!isInterrupted()) {
                    System.out.println(getName());
                }
            } catch (Exception consumed) {
                System.out.println("intrept!");
            }

        }

        public void cancle() {
            System.out.println(getName());
            interrupt();
        }

    }

    static class Work4 implements Runnable {

        Thread currentThread;

        @Override
        public void run() {
            currentThread = Thread.currentThread();
            try {
                int i = 0;
                System.out.println(currentThread.getName());
                while (!currentThread.isInterrupted()) {
                    System.out.println(Thread.currentThread().getName());
                }
            } catch (Exception consumed) {
                System.out.println("intrept!");
            }

        }

        public void cancle() {
            System.out.println(currentThread.getName());
            currentThread.interrupt();
        }

    }

    static class Work5 implements Runnable {

        @Override
        public void run() {
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            System.out.println("work5 end");
        }

    }

    /**
     * @throws InterruptedException
     * @description TODO
     * @param args
     * @throws
     */
    public static void main(String[] args) throws InterruptedException {
        Test test = new Test();
        ExecutorService executor = Executors.newFixedThreadPool(5);
        /*
        executor.execute(test.new Work(1, test));
        executor.execute(test.new Work(2, test));
        executor.execute(test.new Work1(1, test));
        executor.execute(test.new Work1(2, test));
        executor.execute(test.new Work2(1, test));
        executor.execute(test.new Work1(3, test));
        executor.execute(test.new Work(3, test));
        executor.execute(test.new Work(4, test));
        executor.execute(test.new Work(5, test));
        */
        // Work3 work3 = new Work3();
        Work5 work5 = new Work5();
        System.out.println(Thread.currentThread().getName());
        // work3.start();
        executor.execute(work5);
        // executor.shutdown();
        executor.awaitTermination(80000, TimeUnit.MILLISECONDS);
        System.out.println("end");


    }

}
