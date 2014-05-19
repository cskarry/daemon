daemon
======

支持可中断、动态编译的任务框架

五种任务执行模式：
一、普通任务模式
参考类：InitAvStageInfo
执行脚本：initAvStageInfo.sh

二、有文本参数解析的任务模式
参考类：DaemonTask
执行脚本：sh daemon.sh -b daemonTask -f /home/admin -s

三、有文本参数可中断可控制速度的任务模式，适用于有大量数据需要处理的场景
参考类：InitPersonalAuthData4InterruptFileTaskAO
执行脚本 sh daemon.sh -b initPersonalAuthData4InterruptFileTaskAO -f /home/admin -s

四、动态编译Groovy任务模式，无需发布，但是不支持hsf client
参考类：querydata.groovy
执行脚本：small-laucher.sh

五、动态编译Java任务模式，无需发布，支持hsf client
参考类：ITaskDemo
执行脚本：quickTasker.sh
