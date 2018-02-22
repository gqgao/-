/**
 * 文件名:WlanAlarmProcess.java
 * Copyright 2017，北京福富软件技术股份有限公司
 * All Rights Reserved.
 * 文件编号:
 * 创建人: LinQj
 * 日期: 2017年11月17日上午11:31:02
 * 修改人:
 * 日期:
 * 摘要:
 * 版本号:
 * 原 作 者: LinQj
 * 完成日期:
 */
package com.ffcs.oss.fm.process;

import com.ffcs.oss.fm.util.FormatUtil;
import lombok.Data;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

//@Service
//@Data
//@ConfigurationProperties(prefix = "wlan")
public class WlanAlarmProcess implements AlarmProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(WlanAlarmProcess.class);

	private Map<String, String> fields = new HashMap<>();

	@Override
	public String processAlarm(String srcAlarm) {
		if (StringUtils.isEmpty(srcAlarm)) {
			return null;
		}

		try {
			Map<String, Object> alarmData = new HashMap<>();
			Document document = DocumentHelper.parseText(srcAlarm);
			Iterator<Element> iterator = document.getRootElement().elements().iterator();
			while (iterator.hasNext()) {
				Element element = iterator.next();
				String name = element.getName().trim();
				String value = element.getTextTrim();
				if (fields.containsKey(name)) {
					alarmData.put(fields.get(name), value);
				}
			}
			alarmData.put("EVENTTIME", FormatUtil.getStandardTime("" + alarmData.get("EVENTTIME")));
			alarmData.put("CLEARANCEREPORTFLAG", "0");

			alarmData.put("PERCEIVEDSEVERITY", convertAlarmSeverity("" + alarmData.get("PERCEIVEDSEVERITY")));
			if ("5".equals(alarmData.get("PERCEIVEDSEVERITY"))) {
				alarmData.put("CLEARANCETIMESTAMP", alarmData.get("EVENTTIME"));
				alarmData.put("CLEARANCEREPORTFLAG", "1");
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
		// TODO Auto-generated method stub

	}

	/**
	 *  告警等级转换
	 *  注：该方法不通用
	 * @param srcAlarmSeverity
	 * @return
	 */
	private String convertAlarmSeverity(String srcAlarmSeverity) {
		switch (srcAlarmSeverity) {
			case "1":
				return "1";
			case "2":
				return "2";
			case "3":
				return "3";
			case "4":
				return "4";
			case "5":
				return "5";
			case "6":
				return "4";
			case "7":
				return "0";
			default:
				return "0";
		}
	}

}
