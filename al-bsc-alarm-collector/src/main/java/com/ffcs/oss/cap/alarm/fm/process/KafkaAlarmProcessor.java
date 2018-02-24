package com.ffcs.oss.cap.alarm.fm.process;

import com.ffcs.oss.cap.alarm.fm.AlarmParams;

import net.sf.json.JSONObject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ${gujj} on 2017-08-28.
 */
@Service
public class KafkaAlarmProcessor implements AlarmProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(KafkaAlarmProcessor.class);

	@Autowired
	private AlarmParams alarmParams;

	@Autowired
	private CacheManager cacheManager;

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
	public void destroy() {
	}

	@Override
	public String processAlarm(String srcAlarm) {
		if (srcAlarm == null || "".equals(srcAlarm)) {
			return null;
		}

		try {
			Map<String, Object> alarmData = new HashMap<>(defaultAlarmMap);
			Map<String, Object> privateFieldMap = new HashMap<>(defaultPrivateFieldMap);

			JSONObject jsonObject = JSONObject.fromObject(srcAlarm);
			Iterator<String> it = jsonObject.keys();
			String managerObjectClass = "";
			String type = "";
			String mod1btsTxAmp = "";
			String sector = "";
			while (it.hasNext()) {
				String key = it.next().trim();
				String value = ("" + jsonObject.get(key)).trim();
				switch (key) {
					case "a":
						alarmData.put("NOTIFICATIONID", value);
						break;
					case "f":
						alarmData.put("VENDOR_ALARMID", getRegexValue(value, "Alarm=([ \\d]+)"));
						alarmData.put("BTSID", getRegexValue(value, "Mod1bts=([ \\d]+)"));
						break;
					case "nn":
						alarmData.put("ALARMTITLE", value);
						break;
					case "i":
						mod1btsTxAmp = getRegexValue(value, "Mod1btsTxAmp=([\\d]+)");
						break;
					case "e":
						privateFieldMap.put("MANAGED_OBJECT_INSTANCE", value);
						alarmData.put("EQUIPMENTNAME", value);
						sector = getRegexValue(value, "Sector=([ \\d]+)");
						break;
					case "h":
						alarmData.put("PERCEIVEDSEVERITY", convertAlarmSeverity(value));
						break;
					case "event_name":
						if (!"".equals(value)) {
							alarmData.put("ALARMTYPE", Integer.parseInt(value.substring(1, 2)));
						}
						break;
					case "c":
						privateFieldMap.put("SYSTEM_DN", value);
						break;
					case "ll":
						alarmData.put("CLEARANCETIMESTAMP", getStandardTime(value));
						break;
					case "b":
						alarmData.put("EVENTTIME", getStandardTime(value));
						break;
				}
			}

			String alarmTitle = (String) alarmData.get("ALARMTITLE");
			if (mod1btsTxAmp.length() > 0) {
				if (("Mod1btsTxAmp_txAmplifierLossOfCommunication,Mod1btsTxAmp_txAmpIndeterminate,Mod1btsTxAmp_txAmplifierCircuit".contains(alarmTitle + ""))){
					alarmData.put("CELLID", convertAlarmCellid(mod1btsTxAmp));
				}
			}
			
			if (sector.length() > 1) {
				alarmData.put("BTSID", sector.substring(0, sector.length() - 1));
				alarmData.put("CELLID", sector.substring(sector.length() - 1, sector.length()));
			}
			
			if ("5".equals(alarmData.get("PERCEIVEDSEVERITY")) && "".equals(alarmData.get("CLEARANCETIMESTAMP"))) {
				alarmData.put("CLEARANCETIMESTAMP", alarmData.get("EVENTTIME"));
			}

			alarmData.put("ADDITIONALTEXT", srcAlarm);
			alarmData.put("CREATIONTIMESTAMP", getStandardTime(new Date()));
			alarmData.put("PRIVATEFIELDJSON", privateFieldMap);

			return JSONObject.fromObject(alarmData).toString();
			//return spliceClearAlarm(alarmData, cacheManager.getCache("ZTE_CDMA_ALARM"), "5".equals(alarmData.get("PERCEIVEDSEVERITY")));
		} catch (Exception e) {
			LOGGER.error("处理告警失败，将返回null：", e);
			return null;
		}
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
	 *  扇区id转换
	 *  注：该方法不通用
	 * @param srcAlarmCellid
	 * @return
	 */
	private String convertAlarmCellid(String srcAlarmCellid) {
		switch (srcAlarmCellid) {
			case "1":
				return "1";
			case "2":
				return "1";
			case "4":
				return "2";
			case "5":
				return "2";
			case "7":
				return "3";
			case "8":
				return "3";
			default:
				return "0";
		}
	}

	/**
	 *  1、若收到故障告警，放入缓存
	 *  2、若收到清除告警，根据VENDOR_ALARMID在缓存中找到对应的故障告警，进行拼接返回
	 * @param alarmData
	 * @param cache
	 * @param isClearAlarm
	 * @return
	 */
	private String spliceClearAlarm(Map<String, Object> alarmData, Cache cache, boolean isClearAlarm) {
		String vendorAlarmId = "" + alarmData.get("VENDOR_ALARMID");
		if (isClearAlarm) {
			if (cache.get(vendorAlarmId) == null) {
				LOGGER.info("缓存中没有消除通知【Id=" + vendorAlarmId + "】对应的故障通知，将原样返回");
			} else {
				Map<String, Object> alarm = new HashMap<>((Map<String, Object>)cache.get(vendorAlarmId).get());
				LOGGER.info("从缓存找到消除通知【Id=" + vendorAlarmId + "】对应的故障通知，进行拼接");
				alarm.put("CLEARANCETIMESTAMP", alarmData.get("CLEARANCETIMESTAMP"));
				alarm.put("PERCEIVEDSEVERITY", alarmData.get("PERCEIVEDSEVERITY"));
				alarm.put("CREATIONTIMESTAMP", alarmData.get("CREATIONTIMESTAMP"));

				LOGGER.info("从缓存删除故障通知【Id=" + vendorAlarmId + "】");
				cache.evict(vendorAlarmId);
				return JSONObject.fromObject(alarm).toString();
			}
		} else {
			LOGGER.info("将故障通知【Id=" + vendorAlarmId + "】放入缓存");
			cache.put(vendorAlarmId, alarmData);
		}

		return JSONObject.fromObject(alarmData).toString();
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

	private String getRegexValue(String content, String regex) {
		Matcher matcher = Pattern.compile(regex).matcher(content);
		if (matcher.find() && !regex.isEmpty() && !content.isEmpty()) {
			return matcher.group(1).trim();
		}
		return "";
	}
}
