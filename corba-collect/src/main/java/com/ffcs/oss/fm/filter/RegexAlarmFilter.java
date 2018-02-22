package com.ffcs.oss.fm.filter;

import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ffcs.oss.fm.AlarmParams;

/**
 * Created by lenovo on 2017/7/13.
 * 正则表达式告警过滤器
 */
@Service
public class RegexAlarmFilter implements AlarmFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegexAlarmFilter.class);

    @Autowired
	private AlarmParams alarmParams;

    // 告警必须匹配的正则表达式模式
    private Pattern includePattern;

    // 告警不能匹配的正则表达式模式
    private Pattern excludePattern;

    @Override
    public boolean filterAlarm(String alarm) {
        if (includePattern != null) {
            if (!includePattern.matcher(alarm).find()) {
                LOGGER.info("告警不能匹配必须的模式[" + includePattern + "]");
                return true;
            }
        }

        if (excludePattern != null) {
            if (excludePattern.matcher(alarm).find()) {
                LOGGER.info("告警匹配上了过滤模式[" + excludePattern + "]" );
                return true;
            }
        }

        return false;
    }

    @PostConstruct
    public void init() throws Exception {
        if (StringUtils.isNotBlank(alarmParams.getIncludeRegex())) {
            try {
                includePattern = Pattern.compile(alarmParams.getIncludeRegex());
            } catch (Exception e) {
                LOGGER.error("includeRegex配置错误：" + alarmParams.getIncludeRegex(), e);
            }
        }
        if (StringUtils.isNotBlank(alarmParams.getExcludeRegex())) {
            try {
                excludePattern = Pattern.compile(alarmParams.getExcludeRegex());
            } catch (Exception e) {
                LOGGER.info("excludeRegex配置错误：" + alarmParams.getExcludeRegex(), e);
            }
        }
    }
}
