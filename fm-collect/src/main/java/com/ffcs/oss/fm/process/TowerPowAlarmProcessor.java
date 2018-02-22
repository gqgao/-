/**
 * 文件名:TowerPowAlarmProcessor.java
 * Copyright  2017，北京福富软件技术股份有限公司 
 * All Rights Reserved. 
 * 文件编号:
 * 创建人: LinQj
 * 日期: 2017年11月17日下午2:51:18
 * 修改人:
 * 日期:
 * 摘要:
 * 版本号: 
 * 原 作 者: LinQj
 * 完成日期:
 */
package com.ffcs.oss.fm.process;

import com.ffcs.oss.fm.AlarmParams;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TowerPowAlarmProcessor  implements AlarmProcessor{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TowerPowAlarmProcessor.class);

	private Map<String, String> defaultAlarmMap = new HashMap<>();

	private Map<String, String> defaultPrivateFieldMap = new HashMap<>();

	@Autowired
	AlarmParams alarmParams;

	@PostConstruct
	public void init() {
		// 将通用字段的值设置为""
		setDefaultMap(defaultAlarmMap, alarmParams.getColumns());
		// 将厂家特有字段的值设置为""
		setDefaultMap(defaultPrivateFieldMap, alarmParams.getPrivateColumns());
		// 添加固定字段到厂家特有字段
		addFixedFiled();
	}
	
	@Override
	public String processAlarm(String srcAlarm) {
		if (StringUtils.isEmpty(srcAlarm)) {
			return null;
		}

		try {
			Document document = DocumentHelper.parseText(srcAlarm);
			Element alarmElement = document.getRootElement().element("Alarm");
			if (alarmElement == null) {
				return null;
			}

			Map<String, Object> alarmData = new HashMap<>(defaultAlarmMap);
			Map<String, Object> privateFieldMap = new HashMap<>(defaultPrivateFieldMap);
			for (Object object : alarmElement.elements()) {
				Element field = (Element)object;
				String name = field.getName().trim();
				String value = field.getTextTrim();
				switch (name) {
					case "ProvinceName":
						privateFieldMap.put("PROVINCENAME", value);
						break;
					case "CityName":
						alarmData.put("REGION", value);
						break;
					case "NeName":
						alarmData.put("SITE_NAME", value);
						break;
					case "NeID":
						alarmData.put("ADDKEYWORD3", value);
						break;
					case "ResID":
						alarmData.put("RSCNAME", value);
						break;
					case "RoomId":
						alarmData.put("ADDKEYWORD4", value);
						break;
					case "ResRoomId":
						alarmData.put("ADDKEYWORD5", value);
						break;
					case "AlarmUniqueId":
						//alarmData.put("VENDOR_ALARMID", value);
						alarmData.put("SORALMLOGO", value);
						break;
					case "SignalId":
						alarmData.put("ALARMINFO", value);
						break;
					case "DeviceType":
						alarmData.put("NECLASS", value);
						break;
					case "DeviceName":
						alarmData.put("EQUIPMENTNAME", value);
						break;
					case "DeviceId":
						privateFieldMap.put("DEVICEID", value);
						break;
					case "AlarmTitle":
						alarmData.put("ALARMTITLE", value);
						break;
					case "EventTime":
						alarmData.put("EVENTTIME", getStandardTime(value));
						break;
					case "ClearTime":
						alarmData.put("CLEARANCETIMESTAMP", getStandardTime(value));
						break;
					case "AlarmCancelStatus":
						alarmData.put("CLEARANCEREPORTFLAG", value);
						if ("1".equals(value)) {
							alarmData.put("PERCEIVEDSEVERITY", "5");
						} else {
							alarmData.put("PERCEIVEDSEVERITY", "3");
						}
						break;
					case "AlarmSendStatus":
						alarmData.put("ADDKEYWORD1", value);
						break;
					case "AlarmEngineerStatus":
						alarmData.put("ADDKEYWORD2", value);
						break;
				}
			}

			if ("".equals(alarmData.get("EVENTTIME")) || alarmData.get("EVENTTIME") == null) {
				alarmData.put("EVENTTIME", alarmData.get("CLEARANCETIMESTAMP"));
			}

			// 告警等级默认放0
			if (alarmData.get("PERCEIVEDSEVERITY") == null || "".equals(alarmData.get("PERCEIVEDSEVERITY"))) {
				alarmData.put("PERCEIVEDSEVERITY", "0");
			}

			alarmData.put("ADDITIONALTEXT", srcAlarm);
			alarmData.put("CREATIONTIMESTAMP", getStandardTime(new Date()));
			alarmData.put("PRIVATEFIELDJSON", privateFieldMap);

			return JSONObject.fromObject(alarmData).toString();
		} catch (Exception e) {
			LOGGER.info("处理告警失败，将返回null：", e);
			return null;
		}
	}	

	@Override
	public void destroy() {
		
	}

	/**
	 * 默认将每个字段的值置为“”
	 */
	private void setDefaultMap(Map<String, String> defaultMap, String columns) {
		Matcher matcher = Pattern.compile("\\w+").matcher(columns);
		while (matcher.find()) {
			defaultMap.put(matcher.group(), "");
		}
	}

	/**
	 * 添加额外的固定字段
	 */
	private void addFixedFiled() {
		Map<String, String> addFixedField = alarmParams.getAddFixedField();
		if (addFixedField != null && !addFixedField.isEmpty()) {
			defaultPrivateFieldMap.putAll(addFixedField);
		}
	}

	/**
	 * 获取标准格式时间
	 *
	 * @param time String
	 * @return String
	 */
	private String getStandardTime(String time) {
		if (StringUtils.isEmpty(time)) {
			return time;
		}
		String parsedTime = "";

		Matcher matcher = Pattern.compile("\\d+").matcher(time);
		while (matcher.find()) {
			String group = matcher.group();
			parsedTime = parsedTime + (group.length() == 1 ? "0" + group : group);
		}

		parsedTime += "000000000";
		if (parsedTime.length() < 17) {
			return getStandardTime(new Date());
		}
		try {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new SimpleDateFormat("yyyyMMddHHmmssSSS").parse(parsedTime.substring(0, 17)));
		} catch (ParseException e) {
			return getStandardTime(new Date());
		}
	}

	/**
	 * 获取标准格式时间
	 *
	 * @param date Date
	 * @return String
	 */
	private String getStandardTime(Date date) {
		if (date == null) {
			return "";
		}

		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(date);
	}

}
