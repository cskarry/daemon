package com.alibaba.china.talos.small;

import org.apache.commons.lang.StringUtils;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.alibaba.china.talos.daemon.RunMain;

public class Configer {

    private static final SmallLogger logger       = new SmallLogger("/configer.log");

    private static volatile boolean  isReadonly   = true;
    private static volatile boolean  isGroovyMode = false;
    private static volatile boolean  isWithData   = false;
    private static volatile boolean  isAutowired  = false;
    private static volatile boolean  isDaoOnly    = true;

    static {
        String groovyMode = System.getProperty("small.tasker.groovymode");
        if (!StringUtils.isBlank(groovyMode)) {
            try {
                setGroovyMode(Boolean.valueOf(groovyMode));
            } catch (Throwable e) {
                logger.error("[CONFIG] failed to set groovyMode : " + e.getMessage(), e);
            }
        }
        String smallReadonly = System.getProperty("small.tasker.readonly");
        if (!StringUtils.isBlank(smallReadonly)) {
            try {
                setReadonly(Boolean.valueOf(smallReadonly));
            } catch (Throwable e) {
                logger.error("[CONFIG] failed to set readonly : " + e.getMessage(), e);
            }
        }
        String withData = System.getProperty("small.tasker.withdata");
        if (!StringUtils.isBlank(withData)) {
            setWithData(true);
        }
        String autowired = System.getProperty("small.tasker.autowired");
        if (!StringUtils.isBlank(autowired)) {
            try {
                setAutowired(Boolean.valueOf(autowired));
            } catch (Throwable e) {
                logger.error("[CONFIG] failed to set autowired : " + e.getMessage(), e);
            }
        }
        String daoonly = System.getProperty("small.tasker.daoonly");
        if (!StringUtils.isBlank(daoonly)) {
            try {
                setDaoOnly(Boolean.valueOf(daoonly));
            } catch (Throwable e) {
                logger.error("[CONFIG] failed to set daoonly : " + e.getMessage(), e);
            }
        }
        logger.info(printConfig());
        if (!isDaoOnly) {
            logger.info("[CONFIG] init whole context with hsf ...");
            try {
                Daoer.wholeContext = (ClassPathXmlApplicationContext) new RunMain().getContext();
                logger.info("[CONFIG] successed to init  whole context with hsf ...");
            } catch (Throwable e) {
                logger.error("[CONFIG] failed to init whole context with hsf: " + e.getMessage(), e);
            }
        }
    }

    public static boolean isDaoOnly() {
        return isDaoOnly;
    }

    public static void setDaoOnly(boolean isDaoOnly) {
        Configer.isDaoOnly = isDaoOnly;
    }

    public static boolean isReadonly() {
        return isReadonly;
    }

    public static void setReadonly(boolean isReadonly) {
        Configer.isReadonly = isReadonly;
    }

    public static boolean isGroovyMode() {
        return isGroovyMode;
    }

    public static void setGroovyMode(boolean isGroovyMode) {
        Configer.isGroovyMode = isGroovyMode;
    }

    public static boolean isWithData() {
        return isWithData;
    }

    public static void setWithData(boolean isWithData) {
        Configer.isWithData = isWithData;
    }

    public static boolean isAutowired() {
        return isAutowired;
    }

    public static void setAutowired(boolean isAutowired) {
        Configer.isAutowired = isAutowired;
    }

    public static String printConfig() {
        return "[CONFIG] isReadonly<" + isReadonly + "> isGroovyMode<" + isGroovyMode + "> isWithData<" + isWithData
               + "> isAutowired<" + isAutowired + "> isDaoOnly<" + isDaoOnly + ">.";
    }

}
