package com.alibaba.china.app;

import com.alibaba.china.talos.file.LineAnalyzer;
import com.alibaba.china.talos.file.LineHandle;
import com.alibaba.china.talos.file.interrupt.FileTaskAO;


public class FileTaskAOTest extends FileTaskAO<Integer> {

    @Override
    public LineAnalyzer<Integer> getLineAnalyzer() {
        return new LineAnalyzer<Integer>() {

            @Override
            public Integer analyse(String lineText) {
                return Integer.parseInt(lineText);
            }

        };
    }

    @Override
    public LineHandle<Integer> getLineHandle() {
        return new LineHandle<Integer>() {

            @Override
            public boolean handle(Integer model) {
                try {
                    Thread.sleep(1000);
                    System.out.println(Thread.currentThread().getName() + ":" + model);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return true;
            }

        };
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("filePath", "/Users/apple/Downloads/test1");
        System.setProperty("maxThreadNum", "5");
        System.setProperty("minThreadNum", "1");
        FileTaskAOTest test = new FileTaskAOTest();
        test.execute();

    }

}
