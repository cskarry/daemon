package com.alibaba.china.talos.file;


/**
 * @description ������Ľӿ�
 * @author karry
 * @date 2014-1-14 ����8:41:37
 * @param <T>
 */
public interface LineHandle<T> {

    public boolean handle(T model);

}
