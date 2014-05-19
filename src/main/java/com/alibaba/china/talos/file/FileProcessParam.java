package com.alibaba.china.talos.file;

import java.io.File;


public class FileProcessParam {
    
    private String path;

    private int    minThreadCount;

    private int    maxThreadCount;

    public FileProcessParam(String path, int minThreadCount, int maxThreadCount){
        this.path = path;
        this.minThreadCount = minThreadCount;
        this.maxThreadCount = maxThreadCount;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getMinThreadCount() {
        return minThreadCount;
    }

    public void setMinThreadCount(int minThreadCount) {
        this.minThreadCount = minThreadCount;
    }

    public int getMaxThreadCount() {
        return maxThreadCount;
    }

    public void setMaxThreadCount(int maxThreadCount) {
        this.maxThreadCount = maxThreadCount;
    }

    public File[] getFiles() {
        File f = new File(path);
        if (f.isFile()) {
            return null;
        }
        return f.listFiles();
    }

}
