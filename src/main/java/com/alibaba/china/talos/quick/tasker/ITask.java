package com.alibaba.china.talos.quick.tasker;

import java.lang.reflect.Field;

import com.alibaba.china.talos.daemon.AbstractFileTaskAO;

import com.alibaba.common.logging.Logger;
import com.alibaba.common.logging.LoggerFactory;


public abstract class ITask<T> extends AbstractFileTaskAO<T> {

    private static final Logger logger = LoggerFactory.getLogger(ITask.class);


    @SuppressWarnings("rawtypes")
    @Override
    public void execute() {
        Class<? extends ITask> selfClass = this.getClass();
        Field[] fields = selfClass.getDeclaredFields();
        if (null != fields && fields.length > 0) {
            for (Field field : fields) {
                if (field.isAnnotationPresent(ResourceTasker.class)) {
                    ResourceTasker resourceTasker = field.getAnnotation(ResourceTasker.class);
                    String beanName = resourceTasker.name();
                    Object beanObj = ITaskContext.getBean(beanName);
                    if (null == beanObj) {
                        logger.error("spring has no bean named:" + beanName);
                        return;
                    }
                    field.setAccessible(true);
                    try {
                        field.set(this, beanObj);
                    } catch (IllegalArgumentException e) {
                        logger.error("filed set error!",e);
                        return;
                    } catch (IllegalAccessException e) {
                        logger.error("filed set error!",e);
                        return;
                    }
                }
            }
        }
        super.execute();

    }

}
