package com.alibaba.china.talos.small;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

public class SmallLaucher {

    private static final SmallLogger logger = new SmallLogger("small_laucher.log");

    public static void main(String[] args) {
        try {
            if (Configer.isGroovyMode()) {
                if (Configer.isWithData()) {
                    if (args.length < 2) {
                        logger.error("[ARGS] args.length should (>=2), args[0]=groovyFilePath, args[1]=dataFilePath .");
                        return;
                    }
                    GroovyTasker.fireTaskWithData(args[0], args[1]);
                } else {
                    if (args.length < 1) {
                        logger.error("[ARGS] args.length should (>=1), args[0]=groovyFilePath .");
                        return;
                    }
                    GroovyTasker.fireTask(args[0]);
                }
            } else {
                if (Configer.isWithData()) {
                    if (args.length < 2) {
                        logger.error("[ARGS] args.length should (>=2), args[0]=taskClassName , args[2]=dataFilePath .");
                        return;
                    }
                    fireByClassName(args[0], args[1]);
                } else {
                    if (args.length < 1) {
                        logger.error("[ARGS] args.length should (>=1), args[0]=taskClassName .");
                        return;
                    }
                    fireByClassName(args[0], null);
                }
            }
        } catch (Throwable e) {
            logger.error("[EXECUTE] failed to fire task : " + e.getMessage(), e);
        } finally {
            System.exit(0);
        }
    }

    static void fireByClassName(String taskClassName, String dataFileUrl) {
        try {
            Class<?> clazz = Class.forName(taskClassName);
            Object intance = clazz.newInstance();
            if (Configer.isAutowired()) {
                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    field.setAccessible(true);
                    Object value = Daoer.getBean(field.getName());
                    if (value != null) {
                        field.set(intance, value);
                    } else {
                        logger.error("[AUTOWIRED] could not found match bean for field<" + field.getName() + ">");
                    }
                }
            }
            if (Configer.isWithData()) {
                Method targetMethod = clazz.getMethod("execute", new Class<?>[] { List.class });
                targetMethod.invoke(intance, new Object[] { Dataer.getData(dataFileUrl) });
            } else {
                Method targetMethod = clazz.getMethod("execute", new Class<?>[0]);
                targetMethod.invoke(intance, new Object[0]);
            }
            logger.info("[RESULT] done for taskClassName<" + taskClassName + "> dataFileUrl<" + dataFileUrl + ">.");
        } catch (Throwable e) {
            logger.error("[EXECUTE] failed to fire by ClassName<" + taskClassName + "> : " + e.getMessage(), e);
        }
    }
}
