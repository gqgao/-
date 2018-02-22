/**
 * 文件名:StationAlarmProcessor.java
 * Copyright 2017，北京福富软件技术股份有限公司
 * All Rights Reserved.
 * 文件编号:
 * 创建人: LinQj
 * 日期: 2017年12月21日下午2:09:38
 * 修改人:
 * 日期:
 * 摘要:
 * 版本号:
 * 原 作 者: LinQj
 * 完成日期:
 */
package com.ffcs.oss.fm.process;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.ffcs.oss.fm.util.FormatUtil;
import lombok.Data;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.swing.text.html.parser.Entity;

//@Service
//@Data
//@ConfigurationProperties(prefix = "station")
public class StationAlarmProcessor implements AlarmProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(StationAlarmProcessor.class);

	private Map<String, String> fields = new HashMap<>();

	@Override
	public String processAlarm(String srcAlarm) {
		if (StringUtils.isEmpty(srcAlarm)) {
			return null;
		}

		try {
			Map<String, Object> alarmData = new HashMap<>();
			// 添加字段
			Iterator<Map.Entry<String, String>> iterator = fields.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry entry = iterator.next();
				alarmData.put(entry.getKey().toString().trim(), getRegexValue(srcAlarm, entry.getValue().toString().trim()));
			}

			Matcher matcher = Pattern.compile("ALARM\\s*(\\S*)\\s*(\\S*)\\s*(\\S*)\\s*(\\S*)\\s*(\\S*)").matcher(srcAlarm);
			if (matcher.find()) {
				if ("恢复".equals(matcher.group(1))) {
					alarmData.put("CLEARANCEREPORTFLAG", "1");
					alarmData.put("PERCEIVEDSEVERITY", "5");
				} else {
					alarmData.put("CLEARANCEREPORTFLAG", "0");
					// 告警等级默认放0 
					//alarmData.put("PERCEIVEDSEVERITY", "0");
				}

				if ("严重告警".equals(matcher.group(2))) {
					alarmData.put("PERCEIVEDSEVERITY", "1");
				} else if ("重要告警".equals(matcher.group(2))) {
					alarmData.put("PERCEIVEDSEVERITY", "2");
				} else if ("一般告警".equals(matcher.group(2))) {
					alarmData.put("PERCEIVEDSEVERITY", "3");
				}
				alarmData.put("LOCATIONINFO", matcher.group(3));
				alarmData.put("ALARMINFO", matcher.group(4));
				alarmData.put("ADDKEYWORD1", matcher.group(5));
			}

			alarmData.put("EVENTTIME", FormatUtil.getStandardTime("" + alarmData.get("EVENTTIME")));
			if ("1".equals(alarmData.get("CLEARANCEREPORTFLAG"))) {
				alarmData.put("CLEARANCETIMESTAMP", alarmData.get("EVENTTIME"));
			}

			// 告警等级默认放0
			if (alarmData.get("PERCEIVEDSEVERITY") == null || "".equals(alarmData.get("PERCEIVEDSEVERITY"))) {
				alarmData.put("PERCEIVEDSEVERITY", "0");
			}

			alarmData.put("PRIVATEFIELDJSON", new HashMap<String, String>());
			alarmData.put("ADDITIONALTEXT", srcAlarm);
			alarmData.put("CREATIONTIMESTAMP", FormatUtil.getStandardTime(new Date()));
			return JSONObject.fromObject(alarmData).toString();
		} catch (Exception e) {
			LOGGER.error("告警解析失败，将返回null", e);
			return null;
		}
	}

	@Override
	public void destroy() {

	}

	private String getRegexValue(String content, String regex) {
		Matcher matcher = Pattern.compile(regex).matcher(content);
		if (matcher.find() && !regex.isEmpty() && !content.isEmpty()) {
			return matcher.group(1).trim();
		}
		return "";
	}
}
