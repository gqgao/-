package com.ffcs.oss.fm.session.source;

/**
 * Created by lenovo on 2017/3/20.
 */
public interface AlarmSourceSession {
    /**
     * 初始会话
     * @throws Exception
     */
    void ready() throws Exception;

    /**
     * 获取告警
     * @return 告警内容
     */
    String getAlarm() throws Exception;

    /**
     * 销毁会话，释放资源
     */
    void destroy();
}
