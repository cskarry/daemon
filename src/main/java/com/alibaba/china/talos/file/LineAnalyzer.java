package com.alibaba.china.talos.file;

/**
 * @description �����ݽ�����
 * @author karry
 * @date 2014-1-14 ����5:56:07
 */
public interface LineAnalyzer<T> {
    
    T analyse(String lineText);

}
