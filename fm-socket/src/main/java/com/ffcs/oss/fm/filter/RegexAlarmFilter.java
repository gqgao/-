package com.ffcs.oss.fm.filter;

import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import javafx.scene.chart.PieChart;
import org.apache.commons.lang3.CharEncoding;
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
    
    public static void main(String[] args) {
    	String pattern = "7650.*|7651.*|7652.*|7653.*|7654.*|7655.*|7656.*|7657.*|7665.*|71058";
    	String alarm = "11177666      C                                         HUAINAN            \n" +
				"HN-市区-联通新大楼-NFTA-440703-51                                                                  \n" +
				"PLMN-PLMN/MRBTS-440703/LNBTS-440703/LNCEL-1                                 \n" +
				"CELL OPERATION DEGRADED                                                     \n" +
				"Started    -                        Cancelled     2018-01-30 19:37:47       \n" +
				"IntId      0                        NotifId       17999982                  \n" +
				"PC TEXT    Indeterminate                                                               \n" +
				"Suppl Info shared:N;Failure in optical interface                                                                                                                            \n" +
				"Diag Info  100 100 100 2004Cablink 100 0 path=/SMOD-1/rf_ext2 additionalFaultId:3030;                                                                                       \n" +
				"User info                                                                   \n" +
				"Object     HN-市区-联通新大楼-NFTA-440703-51                                             QUALITY_OF_SERVICE                      \n";
    	Pattern includePattern = Pattern.compile(pattern); 
    	if (includePattern != null) {
            if (!includePattern.matcher(alarm).find()) {
                System.out.println("告警不能匹配必须的模式");
            }
            System.out.println("告警 ==能==匹配必须的模式");
        }
	}
}
