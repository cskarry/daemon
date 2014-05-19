package com.alibaba.china.talos.small;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

import java.io.File;
import java.util.List;

public class GroovyTasker {

    private static final SmallLogger logger       = new SmallLogger("/groovy_tasker.log");

    private static GroovyClassLoader groovyLoader = new GroovyClassLoader(
                                                                          Thread.currentThread().getContextClassLoader());
    public static final String       START_METHOD = "execute";

    public static void fireTask(String taskFileUrl) {
        GroovyObject tasker = getTasker(taskFileUrl);
        if (tasker == null) {
            logger.error("[EXECUTE] failed to get tasker for taskFileUrl<" + taskFileUrl + ">.");
        }
        fireByGroovyObject(tasker);
        logger.info("[RESULT] done for taskFileUrl<" + taskFileUrl + "> " + Configer.printConfig());
    }

    public static void fireTaskWithData(String taskFileUrl, String dataFileUrl) {
        List<String> data = Dataer.getData(dataFileUrl);
        if (data == null || data.isEmpty()) {
            logger.error("[EXECUTE] failed to get data from dataFileUrl<" + dataFileUrl + ">.");
            return;
        }
        GroovyObject tasker = getTasker(taskFileUrl);
        if (tasker == null) {
            logger.error("[EXECUTE] failed to get tasker for taskFileUrl<" + taskFileUrl + ">.");
        }
        fireByGroovyObjectWithData(tasker, data);
        logger.info("[RESULT] done for taskFileUrl<" + taskFileUrl + "> dataFileUrl<" + dataFileUrl + "> " + Configer.printConfig());
    }

    static GroovyObject getTasker(String taskFileUrl) {
        File groovyFile = new File(taskFileUrl);
        if (groovyFile.exists()) {
            try {
                Class<?> scriptClass = groovyLoader.parseClass(groovyFile);
                if (!scriptClass.isInterface()) {
                    return (GroovyObject) scriptClass.newInstance();
                } else {
                    logger.error("[EXECUTE] groovy file [" + taskFileUrl + "] should not be interface.");
                }
            } catch (Throwable e) {
                logger.error("[EXECUTE] failed to parse groovy file [" + taskFileUrl + "] : " + e.getMessage(), e);
            }
        } else {
            logger.error("[EXECUTE] " + taskFileUrl + " not existed for TaskParser attach.");
        }
        return null;
    }

    static void fireByGroovyObjectWithData(GroovyObject tasker, List<String> data) {
        try {
            tasker.invokeMethod(START_METHOD, new Object[] { data });
        } catch (Throwable e) {
            logger.error("[EXECUTE] failed to fireByGroovyObjectWithData : " + e.getMessage(), e);
        }
    }

    static void fireByGroovyObject(GroovyObject tasker) {
        try {
            tasker.invokeMethod(START_METHOD, new Object[] {});
        } catch (Throwable e) {
            logger.error("[EXECUTE] failed to fireByGroovyObject : " + e.getMessage(), e);
        }
    }
}
