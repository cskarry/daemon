package com.alibaba.china.talos.daemon;

import com.alibaba.china.talos.daemon.BaseAO;
import com.alibaba.common.logging.Logger;
import com.alibaba.common.logging.LoggerFactory;

/**
 * @description TODO
 * @author karry
 * @date 2014-1-12 обнГ4:45:02
 */
public class RunClient {

    private static final Logger  log     = LoggerFactory.getLogger(RunClient.class);

    private static final RunMain runMain = new RunMain();

    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String taskBeanName = args[i];
            BaseAO taskAO;
            try {
                taskAO = (BaseAO) runMain.getContext().getBean(taskBeanName);
            } catch (Exception e) {
                log.fatal("Cannot initialize [" + taskBeanName + "] properly, and the task cannot run !!!");
                continue;
            }

            try {
                taskAO.execute();
            } catch (Throwable e) {
                log.error("Error occurred when running task [" + taskBeanName + "] ", e);
                System.exit(1);
            }
        }
        System.exit(0);
    }

}
