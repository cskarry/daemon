package com.alibaba.china.talos.quick.tasker;

import com.alibaba.common.logging.Logger;
import com.alibaba.common.logging.LoggerFactory;

public class ITaskClient {

    private static final Logger log = LoggerFactory.getLogger(ITaskClient.class);

    public static void main(String[] args) {
        try {
            String taskBeanName = args[0];
            String className = args[1];
            ITaskControl.compile(taskBeanName);
            ITaskControl.refreshClassPath(taskBeanName);
            ITaskControl.invoke(className);
        }catch (Exception e) {
            log.fatal("occured error", e);
            System.exit(-1);
        }


    }

}
