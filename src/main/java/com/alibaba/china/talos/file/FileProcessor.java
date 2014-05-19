package com.alibaba.china.talos.file;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.alibaba.common.logging.Logger;
import com.alibaba.common.logging.LoggerFactory;

public class FileProcessor {

    private FileProcessParam fp;

    private FileAnalyzer<?>  analyzer;

    private CountDownLatch   countDownLatch;

    private static Logger    logger = LoggerFactory.getLogger(FileProcessor.class);

    public FileProcessor(FileProcessParam fp, FileAnalyzer<?> analyzer){
        this.fp = fp;
        this.analyzer = analyzer;
        this.countDownLatch = new CountDownLatch(fp.getMaxThreadCount());
    }

    public void process() {
        try {
            int threadCount = fp.getMaxThreadCount();
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            for (File f : fp.getFiles()) {
                executor.execute(new Worker(f));

            }
            logger.info(String.format("the main thread which has distributed [%s] subthread,is waiting for the termination of all subthread.",
                                         threadCount));
            countDownLatch.await();

            logger.info(String.format("all [%s] threads have completed.", threadCount));
        } catch (Exception e) {
            logger.error(getClass().getSimpleName(), e);
        }

    }


    class Worker implements Runnable {

        File f;

        Worker(File f){
            this.f = f;
        }

        @Override
        public void run() {
            try {
                logger.info(String.format("begin to analyze file[%s]", f.getAbsolutePath()));
                analyzer.analyse(f);
                logger.info(String.format("end to analyze file[%s]", f.getAbsolutePath()));
            } catch (Exception e) {
                logger.error(String.format("the thread which processed file =[%s] occred an exception.", f.getPath()),
                                e);
            } finally {
                countDownLatch.countDown();
                logger.info(String.format("the thread which processed file [%s] has terminated,count of remained thread is [%s] ",
                                          f.getAbsolutePath(), countDownLatch.getCount()));
            }
        }
    }

}
