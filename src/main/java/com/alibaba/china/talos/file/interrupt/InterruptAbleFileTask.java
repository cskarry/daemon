package com.alibaba.china.talos.file.interrupt;

import java.io.File;

/**
 * @description ���жϵ������࣬���������ִ���ٶȺ��жϻָ�������
 * @author karry
 * @date 2014-2-12 ����7:52:18
 */
public interface InterruptAbleFileTask {

    public boolean execute() throws InterruptedException;

    public File getFile();

    public void interrupt();

    public void triggerInterrupt();

    public boolean isInterrupt();


}