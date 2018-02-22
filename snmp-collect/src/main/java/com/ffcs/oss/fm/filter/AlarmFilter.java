package com.ffcs.oss.fm.filter;

/**
 * Created by lenovo on 2017/7/13.
 * 告警过滤器
 */
public interface AlarmFilter {

    /**
     * 过滤告警
     * @param alarm 告警内容
     * @return 返回true表示告警被过滤，false表示告警未被过滤
     */
    boolean filterAlarm(String alarm);
}
