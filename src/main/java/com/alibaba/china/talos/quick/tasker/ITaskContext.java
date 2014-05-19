package com.alibaba.china.talos.quick.tasker;

import org.springframework.context.ApplicationContext;

import com.alibaba.china.talos.daemon.RunMain;

public class ITaskContext {

    private static ApplicationContext context;
    
    static {
        RunMain runMain = new RunMain();
        context = runMain.getContext();
    }

    public static Object getBean(String beanName) {
        return context.getBean(beanName);
    }

}
