/**
 * 文件名:DTLteAlarmProcessor.java
 * Copyright 2017，北京福富软件技术股份有限公司
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

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.json.JSONObject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ffcs.oss.fm.util.FormatUtil;

@Service
public class NokiaLteAlarmProcessor implements AlarmProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(NokiaLteAlarmProcessor.class);

	private static LinkedHashMap<String, String> fieldInfo = new LinkedHashMap<>();
	
	private static LinkedHashMap<String, String> regionInfo = new LinkedHashMap<>();

	static {
		fieldInfo.put("alarminfo", "0,0,9");
		fieldInfo.put("severity", "0,10,14");
		fieldInfo.put("region", "0,52,70");
		fieldInfo.put("equipmentname", "1,0,60");
		fieldInfo.put("IPADDRESS", "1,61,91");
		fieldInfo.put("dn", "2,0,75");
		fieldInfo.put("ALARMDETAIL", "3,0,75");
		fieldInfo.put("EVENTTIME", "4,11,35");
		fieldInfo.put("cancelOrUpdateTag", "4,36,49");
		fieldInfo.put("CLEARANCETIMESTAMP", "4,50,75");
		fieldInfo.put("intId", "5,11,36");
		fieldInfo.put("NOTIFICATIONID", "5,50,60");
		fieldInfo.put("APPENDINFO", "7,11,171");
		fieldInfo.put("diagInfo", "8,11,171");
		fieldInfo.put("userAddInfo", "9,11,75");
		fieldInfo.put("ALARMTITLE", "10,11,71");
		fieldInfo.put("ALARMTYPE", "10,82,102");
		
		regionInfo.put("HEFEI", "合肥");
		regionInfo.put("WUHU", "芜湖");
		regionInfo.put("BENGBU", "蚌埠");
		regionInfo.put("HUAINAN", "淮南");
		regionInfo.put("MAANSHAN", "马鞍山");
		regionInfo.put("HUAIBEI", "淮北");
		regionInfo.put("TONGLING", "铜陵");
		regionInfo.put("ANQING", "安庆");
		regionInfo.put("SUZHOU", "宿州");
		regionInfo.put("CHUZHOU", "滁州");
		regionInfo.put("LUAN", "六安");
		regionInfo.put("XUANCHENG", "宣城");
		regionInfo.put("CHIZHOU", "池州");
		regionInfo.put("FUYANG", "阜阳");
		regionInfo.put("BOZHOU", "亳州");
		regionInfo.put("HUANGSHAN", "黄山");
	}

	@Override
	public String processAlarm(String srcAlarm) {

		if (StringUtils.isEmpty(srcAlarm)) {
			return null;
		}

		try {
			LOGGER.info("接收到原始告警:" + srcAlarm);
			// 告警前4位为告警体长度
			srcAlarm = srcAlarm.substring(4);
			Map<String, String> alarmValues = new HashMap<>();
			Map<String, String> reportData = new HashMap<>();
			reportData.put("CREATIONTIMESTAMP", FormatUtil.getStandardTime(new Date()));
			reportData.put("ADDITIONALTEXT", srcAlarm);

			// 获取原始字段
			Iterator<Entry<String, String>> iterator = fieldInfo.entrySet().iterator();
			String[] indexLine = srcAlarm.split("\r\n|\n");
			while (iterator.hasNext()) {
				Entry<String, String> entry = iterator.next();
				String[] indexStr = entry.getValue().split(",");
				int indexRow = Integer.parseInt(indexStr[0].trim());
				int beginIndex = Integer.parseInt(indexStr[1].trim());
				int endIndex = Integer.parseInt(indexStr[2].trim());
				
				if(indexRow == 10 && beginIndex == 82){
					alarmValues.put(entry.getKey().toUpperCase(), indexLine[indexRow].substring(beginIndex, indexLine[indexRow].length()-1).trim());
				}else{
					alarmValues.put(entry.getKey().toUpperCase(), indexLine[indexRow].substring(beginIndex, endIndex).trim());
				}
			}
			
			for (String region : regionInfo.keySet()) {
				String regionKey = region;
				String regionValue = regionInfo.get(regionKey);
				if(regionKey.equals(alarmValues.get("REGION"))){
					alarmValues.put("REGION", regionValue);
				}
			}

			if (!"-".equals(alarmValues.get("EVENTTIME"))) {
				alarmValues.put("EVENTTIME", FormatUtil.getStandardTime(alarmValues.get("EVENTTIME")));
			}
			
			if("-".equals(alarmValues.get("EVENTTIME"))){
				alarmValues.put("EVENTTIME",FormatUtil.getStandardTime(alarmValues.get("CLEARANCETIMESTAMP")));
			}

			if (!"-".equals(alarmValues.get("CLEARANCETIMESTAMP"))) {
				alarmValues.put("CLEARANCETIMESTAMP", FormatUtil.getStandardTime(alarmValues.get("CLEARANCETIMESTAMP")));
			}
			
			if ("-".equals(alarmValues.get("CLEARANCETIMESTAMP"))) {
				alarmValues.put("CLEARANCETIMESTAMP", "");
			}
			
			// 解析原始字段
			boolean cancelFlag = "CANCELLED".equalsIgnoreCase(alarmValues.remove("CANCELORUPDATETAG"));
			if (!cancelFlag) {
				reportData.put("EVENTTIME", alarmValues.get("EVENTTIME"));
			} else {
				reportData.put("CLEARANCETIMESTAMP", alarmValues.get("CLEARANCETIMESTAMP"));
			} 
			String dn = alarmValues.remove("DN");
			String pattern = "\\d{6}";
			Matcher m = Pattern.compile(pattern).matcher(dn); 
			if(m.find()){
				reportData.put("BTSID", m.group());
			}		
			
			String severity = alarmValues.remove("SEVERITY");
			Map<String, String> severitys = new HashMap<>();
			severitys.put("W", "4");
			severitys.put("*", "3");
			severitys.put("**", "2");
			severitys.put("***", "1");
			severitys.put("C", "5");
			severitys.put("4", "4");
			severitys.put("3", "3");
			severitys.put("2", "2");
			severitys.put("1", "1");
			severitys.put("5", "5");
			if (severitys.get(severity) != null) {
				reportData.put("PERCEIVEDSEVERITY", severitys.get(severity));
			} else {
				reportData.put("PERCEIVEDSEVERITY", "0");
			}
			
			if(cancelFlag){
				reportData.put("PERCEIVEDSEVERITY", "5");
			}
			
			if (alarmValues.get("ALARMINFO").equals("7653")
					|| alarmValues.get("ALARMINFO").equals("7654")
					|| alarmValues.get("ALARMINFO").equals("7655")) {
				int index = alarmValues.get("EQUIPMENTNAME").lastIndexOf("-");
				if (index != -1) {
					reportData.put("CELL_ID", alarmValues.get("EQUIPMENTNAME").substring(index + 1));
				}
			}
			
			reportData.putAll(alarmValues);
			return JSONObject.fromObject(reportData).toString();
		} catch (Exception e) {
			LOGGER.error("告警解析失败，将返回null", e);
			return null;
		}
	}
	@Override
	public void destroy() {}
}
