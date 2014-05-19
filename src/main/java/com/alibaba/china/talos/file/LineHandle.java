package com.alibaba.china.talos.file;


/**
 * @description 处理类的接口
 * @author karry
 * @date 2014-1-14 下午8:41:37
 * @param <T>
 */
public interface LineHandle<T> {

    public boolean handle(T model);

}
