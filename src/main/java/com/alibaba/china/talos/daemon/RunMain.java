package com.alibaba.china.talos.daemon;

import java.io.InputStream;
import java.util.Properties;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.alibaba.common.lang.ClassLoaderUtil;
import com.alibaba.common.logging.Logger;
import com.alibaba.common.logging.LoggerFactory;
import com.alibaba.common.logging.spi.log4j.DOMConfigurator;

import com.taobao.hsf.standalone.HSFEasyStarter;
import com.alibaba.common.lang.StringUtil;

/**
 * @description TODO
 * @author karry
 * @date 2014-1-12 下午4:33:03
 */
public class RunMain {

    protected static final Logger logger           = LoggerFactory.getLogger(RunMain.class);
    protected static final String HSF_CLIENT_FILE    = "hsf-client.properties";
    protected static String       taskSpringConfig   = "applicationContext.xml";
    private static final String   hsfClientSarAdress = "hsfclient.sar.adress";
    private static final String   hsfClientVersion   = "hsfclient.version";
    protected ApplicationContext  context;
    private boolean               isInited      = false;


    public RunMain(){
        if (StringUtil.isNotBlank(System.getProperty("springConfig"))) {
            taskSpringConfig = System.getProperty("springConfig");
        }
        initialize(taskSpringConfig);
    }

    public RunMain(String serviceFile){
        initialize(serviceFile);
    }

    public void initialize() {
        initialize(taskSpringConfig);
    }

    public void initialize(String serviceFile) {

        if (isInited) {
            return;
        }
        try {

            Properties props = new Properties();
            InputStream ins = ClassLoaderUtil.getResourceAsStream(HSF_CLIENT_FILE);
            props.load(ins);
            String adress = props.getProperty(hsfClientSarAdress);
            String version = props.getProperty(hsfClientVersion);
            ins.close();
            HSFEasyStarter.start(adress, version);
            // 等待服务地址
            Thread.sleep(1000);
            // DOMConfigurator.configure(ClassLoaderUtil.getResource(HSF_CLIENT_FILE), props);

            logger.info("begin to init context...");

            // DefaultServiceManager manager = new DefaultServiceManager("", serviceFile, true);
            // context = (BeanFactoryService) manager.getService(BeanFactoryService.SERVICE_NAME);
            context = new ClassPathXmlApplicationContext(new String[] { taskSpringConfig });
            logger.info("succeed to initialize bean factory!");
            isInited = true;

        } catch (Exception e) {
            logger.error("failed to initialize beanfactory will exit!", e);
            System.exit(-1);
        }

    }

    public ApplicationContext getContext() {
        return context;
    }


}
