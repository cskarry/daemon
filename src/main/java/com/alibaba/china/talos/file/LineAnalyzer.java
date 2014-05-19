package com.alibaba.china.talos.file;

/**
 * @description 行数据解析器
 * @author karry
 * @date 2014-1-14 下午5:56:07
 */
public interface LineAnalyzer<T> {
    
    T analyse(String lineText);

}
