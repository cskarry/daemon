package com.alibaba.china.talos.small;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;

public class Dataer {

    private static final SmallLogger logger = new SmallLogger("/dataer.log");

    @SuppressWarnings("unchecked")
    static List<String> getData(String dataFileUrl) {
        FileInputStream dataFileStream = null;
        List<String> lines = null;
        try {
            dataFileStream = new FileInputStream(new File(dataFileUrl));
            lines = IOUtils.readLines(dataFileStream);
        } catch (Throwable e) {
            logger.error("[EXECUTE] failed to read data from " + dataFileUrl + " : " + e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(dataFileStream);
        }
        return lines;
    }
}
