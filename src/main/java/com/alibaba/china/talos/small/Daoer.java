package com.alibaba.china.talos.small;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

public class Daoer {

    static List<String>                          CONFIGURATIONS = Arrays.asList("classpath*:talos/bean/common/biz-data-source-mysql.xml",
                                                                                "classpath*:talos/bean/common/biz*-dao.xml",
                                                                                "classpath*:talos/bean/common/biz-dao.xml",
                                                                                "classpath*:daemon-datasource.xml");
    public static ClassPathXmlApplicationContext daoContext     = new ClassPathXmlApplicationContext(
                                                                                                     CONFIGURATIONS.toArray(new String[CONFIGURATIONS.size()]));

    public static ApplicationContext             wholeContext   = null;

    @SuppressWarnings("unchecked")
    public static <T> T getDAO(Class<T> daoClass) {
        String beanName = daoClass.getSimpleName();
        return (T) daoContext.getBean(StringUtils.uncapitalize(beanName), daoClass);
    }

    public static TransactionTemplate getTransactionTemplate() {
        return (TransactionTemplate) daoContext.getBean("companyTransactionTemplate");
    }

    public static JdbcTemplate getJdbcTemplate() {
        return (JdbcTemplate) daoContext.getBean("jdbcTemplate");
    }

    public static Object getBean(String beanName) {
        return daoContext.getBean(beanName);
    }

    public static Object getBeanFromWholeContext(String beanName) {
        return wholeContext.getBean(beanName);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getBean(Class<T> targetClass) {
        String beanName = targetClass.getSimpleName();
        return (T) daoContext.getBean(StringUtils.uncapitalize(beanName), targetClass);
    }
}
