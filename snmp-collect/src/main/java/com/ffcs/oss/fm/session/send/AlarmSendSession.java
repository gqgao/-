package com.ffcs.oss.fm.session.send;

/**
 * Created by lenovo on 2017/3/16.
 */
public interface AlarmSendSession {

    /**
     * 发送告警
     * @param alarm 告警内容
     */
    void sendAlarm(String alarm) ;

    /**
     * 销毁会话，释放资源
     */
    void destroy();
}
