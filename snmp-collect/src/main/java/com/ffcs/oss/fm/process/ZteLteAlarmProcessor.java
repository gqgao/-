/**
 * 文件名:ZteLteSnmpAlarmProcessor.java
 * Copyright  2017，北京福富软件技术股份有限公司
 * All Rights Reserved.
 * 文件编号:
 * 创建人: LinQj
 * 日期: 2017年11月8日下午3:48:58
 * 修改人:
 * 日期:
 * 摘要:
 * 版本号:
 * 原 作 者: LinQj
 * 完成日期:
 */
package com.ffcs.oss.fm.process;

import com.ffcs.oss.fm.AlarmParams;
import com.ffcs.oss.fm.util.FormatUtil;

import net.sf.json.JSONObject;

import org.apache.commons.lang3.StringUtils;
import org.omg.CosNaming.NamingContextPackage.AlreadyBound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*@Service*/
public class ZteLteAlarmProcessor implements AlarmProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(ZteLteAlarmProcessor.class);

	@Autowired
	private AlarmParams alarmParams;

	private Map<String, String> defaultAlarmMap = new HashMap<>();

	private Map<String, String> defaultPrivateFieldMap = new HashMap<>();

	@PostConstruct
	private void init() {
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

		Map<String, Object> alarmData = new HashMap<>(defaultAlarmMap);
		Map<String, Object> privateFieldMap = new HashMap<>(defaultPrivateFieldMap);

		List<String> alarm = new ArrayList<>(Arrays.asList(srcAlarm.split("ARG\\[\\d+\\]")));
		alarm.remove(0);
		boolean isClearAlarm = false;
		String rruName = "";
		String neName = "";
		for (int i = 0; i < alarm.size(); i++) {
			String value = alarm.get(i).substring(alarm.get(i).indexOf("=") + 1).trim();
			switch (i) {
				case 1:
					if ("1.3.6.1.4.1.3902.4101.1.4.1.2".equals(value))
						isClearAlarm = true;
					break;
				case 2:
					alarmData.put("EVENTTIME", FormatUtil.getStandardTime(value));
					if (isClearAlarm)
						alarmData.put("CLEARANCETIMESTAMP", FormatUtil.getStandardTime(value));
					break;
				case 5:
					privateFieldMap.put("SYSTEM_DN", value);
					break;
				case 6:
					privateFieldMap.put("SPECIFIC_PROBLEM", value);
					alarmData.put("ALARMINFO", value);
					break;
				case 7:
					privateFieldMap.put("NODEME", getRegexValue(value, ".*NodeMe=([ \\d]*)"));
					alarmData.put("RACK", ".*rack=([ \\d]*)");
					break;
				case 10:
					alarmData.put("PERCEIVEDSEVERITY", value);
					break;
				case 11:
					alarmData.put("ALARMDETAIL", value);
					break;
				case 12:
					Map<String, Object> cellInfos = new HashMap<>();
//					cellInfos.put("BTSID", "eNBId:([ \\d]+)");
					cellInfos.put("CELLID", "小区标识:([ \\d]+)");
					cellInfos.put("CELLNAME", "小区用户标识:([^,]*),");
					privateFieldMap.putAll(convertValues(cellInfos, value));
					break;
				case 13:
					alarmData.put("NECLASS", value);
					break;
				case 16:
					alarmData.put("ALARMTITLE", value);
					break;
				case 17:
					alarmData.put("LOCATIONINFO", value);
					Map<String, Object> infos = new HashMap<>();
					infos.put("RACK", "机架\\(MO SDR\\)=([ \\d]+)");
					infos.put("SHELF", "机框\\(MO SDR\\)=([ \\d]+)");
					infos.put("BOARDLOCATION", "单板\\(MO SDR\\)=([ \\d]+)");
					alarmData.putAll(convertValues(infos, value));
					break;
				case 19:
					alarmData.put("IPADDRESS", value);
					break;
				case 26:
					alarmData.put("VENDOR_ALARMID", value);
					break;
				case 27:
					alarmData.put("EQUIPMENTNAME", value);
					alarmData.put("REGION", extractRegion(value));
					break;
				case 33:
					alarmData.put("BTSID", value);
					break;
				case 35:
					rruName = value;
					break;
				case 36:
					if ("RU".equals(rruName))
						privateFieldMap.put("RACKID", value);
					break;
				case 37:
					if ("RU".equals(rruName))
						privateFieldMap.put("RRUNAME", value);
					neName = value;
					break;
			}
		}

		if (!"".equals(privateFieldMap.get("CELLNAME")) && null != privateFieldMap.get("CELLNAME")) {
			alarmData.put("EQUIPMENTNAME", privateFieldMap.get("CELLNAME"));
		}

		if ("".equals(alarmData.get("EQUIPMENTNAME"))) {
			alarmData.put("EQUIPMENTNAME", neName);
		}

		alarmData.put("PERCEIVEDSEVERITY", convertAlarmSeverity("" + alarmData.get("PERCEIVEDSEVERITY")));

		if (isClearAlarm) {
			alarmData.put("PERCEIVEDSEVERITY", "5");
		}

		alarmData.put("ADDITIONALTEXT", srcAlarm);
		alarmData.put("CREATIONTIMESTAMP", FormatUtil.getStandardTime(new Date()));
		alarmData.put("PRIVATEFIELDJSON", privateFieldMap);

		return JSONObject.fromObject(alarmData).toString();
	}

	@Override
	public void destroy() {
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
				return "0";
			case "2":
				return "1";
			case "3":
				return "2";
			case "4":
				return "3";
			case "5":
				return "4";
			case "6":
				return "5";
			default:
				return "0";
		}
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
	 * 将从文本中截取正则表达内容
	 *
	 * @param srcMapRegex
	 * @param content
	 * @return
	 */
	private Map<String, Object> convertValues(Map<String, Object> srcMapRegex, String content) {

		Iterator<Map.Entry<String, Object>> regexs = srcMapRegex.entrySet().iterator();
		Map<String, Object> values = new HashMap<>();
		while (regexs.hasNext()) {
			Map.Entry<String, Object> map = regexs.next();
			String fieldName = map.getKey();
			String rgx = String.valueOf(map.getValue());
			values.put(fieldName, getRegexValue(content, rgx));
		}
		return values;
	}

	private String getRegexValue(String content, String regex) {
		Matcher matcher = Pattern.compile(regex).matcher(content);
		if (matcher.find() && !regex.isEmpty() && !content.isEmpty()) {
			return matcher.group(1).trim();
		}
		return "";
	}

	private String extractRegion(String content) {
		if (content == null || "".equals(content)) {
			return "";
		}
		if (content.startsWith("XY-")) {
			return getRegexValue(content, "XY-([^-]*)");
		} else {
			return getRegexValue(content, "([^-]*)");
		}
	}
}
