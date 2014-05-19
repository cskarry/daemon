package com.alibaba.china.talos.file.interrupt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.common.lang.StringUtil;
import com.alibaba.common.logging.Logger;
import com.alibaba.common.logging.LoggerFactory;

public class FileRecordContext {

    String path;

    private static final String                    RECORD_FILE_NAME   = "record";

    public static final long                       END_FLAG           = -1;

    private static Logger                          logger             = LoggerFactory.getLogger(FileRecordContext.class);

    private static ConcurrentHashMap<String, File> fileRepertory      = new ConcurrentHashMap<String, File>();

    private static ConcurrentHashMap<String, Long> fileTotalLines = new ConcurrentHashMap<String, Long>();

    private static ConcurrentHashMap<String, Long> fileProcessedLines = new ConcurrentHashMap<String, Long>();

    public FileRecordContext(String path){
        this.path = path;
        init();
    }

    /**
     * @throws FileNotFoundException
     * @description 计算文本行数
     * @throws
     */
    private void init() {
        try {
            File fd = new File(path);
            File fr = new File(path + File.separator + RECORD_FILE_NAME);
            Properties props = new Properties();
            InputStream fr_ins = null;
            if (fr.exists()) {
                fr_ins = new FileInputStream(fr);
                props.load(fr_ins);
            }
            for (File f : fd.listFiles()) {
                if (f.isFile() && !f.getName().equals(RECORD_FILE_NAME)) {
                    fileRepertory.put(getFileKey(f), f);
                    fileTotalLines.put(getFileKey(f), countTotalLines(f));
                    String processedLines = props.getProperty(getFileKey(f));
                    if (StringUtil.isBlank(processedLines)) {
                        fileProcessedLines.put(getFileKey(f), 0L);
                    } else {
                        fileProcessedLines.put(getFileKey(f), Long.parseLong(processedLines));
                    }
                }
            }
            if (null != fr_ins) {
                fr_ins.close();
            }

        } catch (IOException e) {
            logger.error("init path error where path:" + path, e);
        }

    }

    private long countTotalLines(File f) {
        long count = 0;
        BufferedReader bufR = null;
        try {
            bufR = new BufferedReader(new FileReader(f));
            while (bufR.ready()) {
                bufR.readLine();
                count++;
            }
        } catch (IOException e) {
            logger.error("read file error where file is " + f.getName(), e);
        } finally {
            if (null != bufR) {
                try {
                    bufR.close();
                } catch (IOException e) {
                    logger.error("close file error where file is " + f.getName(), e);
                }
            }
        }
        return count;
    }

    public static long getProcessedLines(File f) {
        return fileProcessedLines.get(getFileKey(f));
    }

    public static void recordProcessedLines(File f, long processedLines) {
        String key = getFileKey(f);
        if (fileTotalLines.get(key).equals(processedLines)) {
            processedLines = END_FLAG;
        }
        fileProcessedLines.put(key, processedLines);
        boolean term = true;
        for (Entry<String, Long> entry : fileProcessedLines.entrySet()) {
            if (entry.getValue() != END_FLAG) {
                term = false;
                break;
            }
        }

        // System.out.println(processedLines);
        if (term) {
            System.exit(0);
        }

    }

    public void dump() throws IOException {
        File f = new File(path + File.separator + RECORD_FILE_NAME);
        FileWriter fWriter = new FileWriter(f);
        for (Entry<String, Long> entry : fileProcessedLines.entrySet()) {
            String key = entry.getKey();
            long value = entry.getValue();
            if (fileTotalLines.get(key).equals(value)) {
                value = END_FLAG;
            }
            fWriter.write(key + "=" + value + "\n");
        }
        fWriter.close();
    }

    public static String getFileKey(File f) {
        return f.getName();
    }

    public static List<ComparatorModel> fetchTaskFiles(int num) {
        List<ComparatorModel> comparatorList = fetchTaskFiles();
        if (null == comparatorList || comparatorList.size() < 0) {
            return null;
        } else if (comparatorList.size() <= num) {
            return comparatorList;
        } else {
            return comparatorList.subList(0, num);
        }
    }

    public static List<ComparatorModel> fetchTaskFiles() {
        List<ComparatorModel> comparatorList = new ArrayList<ComparatorModel>();
        for (Entry<String, Long> entry : fileProcessedLines.entrySet()) {
            String key = entry.getKey();
            if (entry.getValue() != -1 && fileTotalLines.get(key) != 0) {
                ComparatorModel cm = new ComparatorModel();
                cm.f = fileRepertory.get(key);
                cm.surplusLines = fileTotalLines.get(key) - entry.getValue();
                comparatorList.add(cm);
            }
        }
        if (comparatorList.size() > 0) {
            Collections.sort(comparatorList, new Comparator<ComparatorModel>() {

                @Override
                public int compare(ComparatorModel arg0, ComparatorModel arg1) {
                    if (arg0.surplusLines < arg1.surplusLines) {
                        return 1;
                    } else if (arg0.surplusLines > arg1.surplusLines) {
                        return -1;
                    } else {
                        return 0;
                    }
                }

            });
        }
        if (comparatorList.size() < 0) {
            return null;
        } else {
            return comparatorList;
        }
    }

    public static void main(String[] args) throws IOException {

    }

    public static class ComparatorModel {

        File f;
        long   surplusLines;

        public File getF() {
            return f;
        }

        public void setF(File f) {
            this.f = f;
        }

        public long getSurplusLines() {
            return surplusLines;
        }

        public void setSurplusLines(long surplusLines) {
            this.surplusLines = surplusLines;
        }

    }



}
