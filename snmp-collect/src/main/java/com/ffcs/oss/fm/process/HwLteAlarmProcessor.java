/**
 * 文件名:HwLteAlarmProcessor.java
 * Copyright  2017，北京福富软件技术股份有限公司 
 * All Rights Reserved. 
 * 文件编号:
 * 创建人: LinQj
 * 日期: 2017年11月10日上午11:35:50
 * 修改人:
 * 日期:
 * 摘要:
 * 版本号: 
 * 原 作 者: LinQj
 * 完成日期:
 */
package com.ffcs.oss.fm.process;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import com.ffcs.oss.fm.util.FormatUtil;

import lombok.Data;
import net.sf.json.JSONObject;

/*@Service
@Data
@ConfigurationProperties(prefix = "hwlte.alarm")*/
public class HwLteAlarmProcessor implements AlarmProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(HwLteAlarmProcessor.class);
	
	private String[] oids;

	@Override
	public String processAlarm(String srcAlarm) {

		Map<String, Object> alarmData = new HashMap<>();
		String oid;
		String fieldName;

		try {
			// 解析OID对应字段
			for (int i = 0; i < oids.length; i++) {
				oid = oids[i].split(":")[0].replaceFirst(".", "");
				fieldName = oids[i].split(":")[1];
				alarmData.put(fieldName, getRegexValue(srcAlarm, oid + "=(.*)"));
			}

			alarmData.put("REGION", extractRegion("" + alarmData.get("EQUIPMENTNAME")));

			if ("301".equals(alarmData.get("ALARMINFO"))) {
				String neName = getRegexValue("" + alarmData.get("LOCATIONINFO"), "网元名称=([^,^，]+)[,，]");
				alarmData.put("REGION", extractRegion(neName));
			}

			// 需要特殊解析字段
			// 本地机架信息
			String locationInfo = String.valueOf(alarmData.get("LOCATIONINFO"));
			Map<String, Object> infos = new HashMap<>();
			infos.put("RACK", "柜号=(\\d+),");
			infos.put("SHELF", "框号=(\\d+),");
			infos.put("SLOT", "槽号=(\\d+),");
			infos.put("BOARDLOCATION", "单板类型=([a-zA-Z]+),");
			infos.put("PORTLOCATION", "端口号=端口(\\d+)");
			alarmData.putAll(convertValues(infos, locationInfo));
			if ("0".equals(alarmData.get("SHELF"))) {
				alarmData.put("SHELF", "");
			}
			// 本地小区信息
			Map<String, Object> privateFieldMap = new HashMap<>();
			privateFieldMap.put("BSCNAME", "");
			privateFieldMap.put("CELLID", ".*本地小区标识=([ \\d]*)");
			privateFieldMap.put("CELLNAME", ".*小区名称=([^,]*)");
			privateFieldMap.putAll(convertValues(privateFieldMap, locationInfo));
			String appendInfo = String.valueOf(alarmData.get("APPENDINFO"));
			privateFieldMap.put("BTSID", getRegexValue(appendInfo, "eNodeBId=(\\d+)"));
			privateFieldMap.put("RRUNAME", getRegexValue(appendInfo, "射频单元名称=([\\s\\S&&[^,]]+),"));
			
			if(alarmData.get("EQUIPMENTNAME").equals("OSS")){
				if(getRegexValue(locationInfo,"网元名称=([^,]*),") != ""){
					alarmData.put("EQUIPMENTNAME", getRegexValue(locationInfo,"网元名称=([^,]*),"));
					String btsidInfo = getRegexValue(locationInfo,"网元名称=([^,]*),");
					privateFieldMap.put("BTSID", getRegexValue(btsidInfo, "-(\\d+)"));
				}
			}
			
			if (!"".equals(privateFieldMap.get("CELLNAME")) && privateFieldMap.get("CELLNAME") != null) {
				alarmData.put("EQUIPMENTNAME", privateFieldMap.get("CELLNAME"));
			}

			alarmData.put("PERCEIVEDSEVERITY", convertAlarmSeverity("" + alarmData.get("PERCEIVEDSEVERITY")));

			alarmData.put("PRIVATEFIELDJSON", privateFieldMap);

			alarmData.put("CREATIONTIMESTAMP", FormatUtil.getStandardTime(new Date()));
			alarmData.put("EVENTTIME", FormatUtil.getStandardTime(String.valueOf(alarmData.get("EVENTTIME"))));
			alarmData.put("ADDITIONALTEXT", srcAlarm);
			LOGGER.info("告警处理完成");
			return JSONObject.fromObject(alarmData).toString();
		} catch (Exception e) {
			LOGGER.info("处理告警失败", e);
			return "";
		}
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
				return "1";
			case "2":
				return "2";
			case "3":
				return "3";
			case "4":
				return "4";
			case "5":
				return "0";
			case "6":
				return "5";
				default:
					return "0";
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

		Iterator<Entry<String, Object>> regexs = srcMapRegex.entrySet().iterator();
		Map<String, Object> values = new HashMap<>();
		while (regexs.hasNext()) {
			Entry<String, Object> map = regexs.next();
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
