package com.ffcs.oss.fm.process;

/**
 * Created by lenovo on 2017/7/5.
 * 告警处理器接口
 */
public interface AlarmProcessor {

    /**
     * 处理告警
     * @param srcAlarm 原始告警
     * @return 处理后的告警
     */
    String processAlarm(String srcAlarm);

    void destroy();
}
