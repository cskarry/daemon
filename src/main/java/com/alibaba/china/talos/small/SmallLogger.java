package com.alibaba.china.talos.small;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.commons.lang.StringUtils;

public class SmallLogger {

    final boolean        printDetail;
    final String         rawFileName;
    volatile File        file;
    volatile boolean     useFile = false;
    volatile PrintWriter pw;
    volatile String      lastDatePath;

    public SmallLogger(){
        this(null, false);
    }

    public SmallLogger(boolean printDetail){
        this(null, printDetail);
    }

    public SmallLogger(String file){
        this(file, false);
    }

    public SmallLogger(String file, boolean printDetail){
        rawFileName = file;
        lastDatePath = getCurrentDatePath();
        this.file = getLogFile(file, getRoot(), lastDatePath);
        this.printDetail = printDetail;
        if (this.file != null) {
            if (!createFileIfNotExist()) {
                throw new IllegalArgumentException(this.file + " could not be created.");
            } else {
                try {
                    pw = new PrintWriter(new FileWriter(this.file, false));
                    this.useFile = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    void tryToRefresh() {
        String currentDatePath = getCurrentDatePath();
        if (!StringUtils.equals(currentDatePath, lastDatePath)) {
            lastDatePath = currentDatePath;
            this.file = getLogFile(rawFileName, getRoot(), lastDatePath);
            if (this.file != null) {
                if (!createFileIfNotExist()) {
                    throw new IllegalArgumentException(this.file + " could not be created.");
                } else {
                    try {
                        pw = new PrintWriter(new FileWriter(this.file, false));
                        this.useFile = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void detail(String message) {
        tryToRefresh();
        if (!printDetail) {
            return;
        }
        if (this.useFile) {
            pw.write(message + "\n");
            pw.flush();
        } else {
            System.out.println(message);
        }
    }

    public void error(String message) {
        error(message, null);
    }

    public void error(String message, Throwable e) {
        tryToRefresh();
        if (this.useFile) {
            pw.write(message + "\n");
            pw.flush();
        } else {
            System.err.println(message);
        }
        if (e != null) {
            if (this.useFile) {
                e.printStackTrace(pw);
                pw.flush();
            } else {
                e.printStackTrace();
            }
        }
    }

    public void info(String message) {
        tryToRefresh();
        if (this.useFile) {
            pw.write(message + "\n");
            pw.flush();
        } else {
            System.out.println(message);
        }
    }

    public void println(String message) {
        tryToRefresh();
        if (this.useFile) {
            pw.write(message + "\n");
            pw.flush();
        } else {
            System.out.println(message);
        }
    }

    private boolean createFileIfNotExist() {
        if (!file.exists()) {
            int lastDot = file.getAbsolutePath().lastIndexOf('/');

            if (lastDot > -1) {
                String dir = file.getAbsolutePath().substring(0, lastDot);
                new File(dir).mkdirs();
            }
            try {
                return file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }

    File getLogFile(String file, String root, String datePath) {
        if (file == null) {
            return null;
        }
        String wholeFileName = file.startsWith("/") ? root + "/" + datePath + file : root + "/" + datePath + "/" + file;
        return new File(wholeFileName);
    }

    static String getLogPath() {
        return getRoot() + getCurrentDatePath();
    }

    static String getCurrentDatePath() {
        return new SimpleDateFormat("yyyy_MM_dd").format(Calendar.getInstance().getTime());
    }

    static String getRoot() {
        String home = System.getenv("SMALL_OUTPUT");
        if (home == null) {
            home = System.getenv("HOME");
        }
        if (home == null) {
            throw new IllegalArgumentException("init log root failed, please offer -Dlitter.output ");
        }
        return home;
    }

    public static void main(String[] args) {
        SmallLogger alogger = new SmallLogger("/a.log");
        SmallLogger blogger = new SmallLogger("b.log");
        alogger.println("hello a!");
        blogger.println("hello b!");
    }
}
