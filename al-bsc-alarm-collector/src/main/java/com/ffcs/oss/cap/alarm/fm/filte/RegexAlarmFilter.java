package com.ffcs.oss.cap.alarm.fm.filte;

import com.ffcs.oss.cap.alarm.fm.AlarmParams;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.regex.Pattern;

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
    
    public static void main(String[] args) {
		String str = "{\"domain_name\":\"Alarm IRP V3.1\",\"type_name\":\"x5\",\"event_name\":\"x5\",\"e\":\"SubNetwork=hf1omcran,ManagedElement=BTS-350,BtsFunction=-350\",\"a\":\"1047703\",\"b\":\"2018-02-07 11:23:00.239\",\"g\":\"340\",\"h\":\"6\",\"f\":\"Orca=1,=350,Alarm=24892417\",\"c\":\"SubNetwork=hf1omcran,ManagementNode=hf1omcran,IRPAgent=1\",\"d\":\"BtsFunction\",\"remainder_of_body\":\"[ParseUnknownTCKind=0]\"}";
		Pattern includePattern = Pattern.compile("Mod1bts");
		if (includePattern != null) {
            if (!includePattern.matcher(str).find()) {
                System.out.println(("告警不能匹配必须的模式[" + includePattern + "]"));
            }else{
            	System.out.println(("告警 能匹配必须的模式[" + includePattern + "]"));
            }
        }
    }
}
