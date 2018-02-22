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
			while (it.hasNext()) {
				String key = it.next().trim();
				String value = ("" + jsonObject.get(key)).trim();
				switch (key) {
					case "a":
						alarmData.put("NOTIFICATIONID", value);
						alarmData.put("VENDOR_ALARMID", value);
						break;
					case "e":
						privateFieldMap.put("MANAGED_OBJECT_INSTANCE", value);
						Matcher matcher = Pattern.compile("\\[([A-Z]+_[A-Z]+_([A-Z]+)\\w+)\\],ManagedElement").matcher(value);
						if (matcher.find()) {
							privateFieldMap.put("BSCNAME", matcher.group(1));
							alarmData.put("REGION", matcher.group(2));
						}

						if (value.contains("BtsFunction")) {
							managerObjectClass = "BtsFunction";
							alarmData.put("EQUIPMENTNAME", getRegexValue(value, "BtsFunction=.*\\[(.*)\\]"));
						}
						if (value.contains("BscFunction")) {
							managerObjectClass = "BscFunction";
							alarmData.put("EQUIPMENTNAME", getRegexValue(value, "BscFunction=.*\\[(.*)\\]"));
						}
						break;
					case "h":
						alarmData.put("PERCEIVEDSEVERITY", convertAlarmSeverity(value));
						break;
					case "event_name":
						if (!"".equals(value)) {
							alarmData.put("ALARMTYPE", Integer.parseInt(value.substring(1, 2)));
						}
						break;
					case "jj":
						alarmData.put("ALARMTITLE", value);
						break;
					case "c":
						privateFieldMap.put("SYSTEM_DN", value);
						break;
					case "ll":
						alarmData.put("CLEARANCETIMESTAMP", getStandardTime(value));
						break;
					case "j":
						for (String text : StringUtils.split(value, ";")) {
							String textValue = StringUtils.substring(text, text.indexOf("(") + 1, text.lastIndexOf(")")).trim();
							text = StringUtils.substring(text, 0, text.indexOf("(")).trim();
							if (text.equals("告警发生位置")) {
								alarmData.put("LOCATIONINFO", textValue);
								for (String location : StringUtils.split(textValue, ",")) {
									String locationValue = StringUtils.substringAfter(location, ":").trim();
									if (location.contains("Server")) {
										privateFieldMap.put("OMCID", locationValue);
									} else if (location.contains("System")) {
										privateFieldMap.put("BTSID", locationValue);
									} else if (location.contains("Rack")) {
										alarmData.put("RACK", locationValue);
									} else if (location.contains("Shelf")) {
										alarmData.put("SHELF", locationValue);
									} else if (location.contains("Slot")) {
										alarmData.put("SLOT", locationValue);
									}
								}
							} else if (text.equals("告警信息")) {
								alarmData.put("ALARMDETAIL", textValue);
								for (String alarmInfo : StringUtils.split(textValue, ",")) {
									if (alarmInfo.contains("Alias")) {
										privateFieldMap.put("RRUNAME", StringUtils.substringAfter(alarmInfo, "=").trim());
									} else if (alarmInfo.contains("Channel number")) {
										privateFieldMap.put("CHANNELNUMBER", StringUtils.substringAfter(alarmInfo, ":").trim());
									} else if (alarmInfo.contains("RRU ID")) {
										privateFieldMap.put("RRU_ID", StringUtils.substringAfterLast(alarmInfo, "=").trim());
										if("".equals(privateFieldMap.get("RRU_ID")) || privateFieldMap.get("RRU_ID") == null){
											privateFieldMap.put("RRU_ID", alarmData.get("SLOT"));
										}
									} else if (alarmInfo.contains("Type")) {
										type = StringUtils.substring(alarmInfo, alarmInfo.indexOf("=") + 1, alarmInfo.indexOf("]")).trim();
									}
								}
							} else if (text.equals("告警码")) {
								privateFieldMap.put("ALARMCODE", textValue);
							} else if (text.equals("告警原因码")) {
								privateFieldMap.put("ALARMREASONCODE", textValue);
							} else if (text.equals("告警码描述")) {
								if (textValue.contains("未探测到")) {
									if (textValue.endsWith("。")) {
										privateFieldMap.put("ALARM_CODE_DESCRIPTION",
												textValue.substring(textValue.indexOf("未探测到") + 4, textValue.indexOf("。")));
									} else {
										privateFieldMap.put("ALARM_CODE_DESCRIPTION",
												textValue.substring(textValue.indexOf("未探测到") + 4));
									}
								}
							} else if (text.equals("告警原因描述")) {
								if (textValue.contains("光口")) {
									if (textValue.endsWith("。")) {
										privateFieldMap.put("ALARM_REASON_DESCRIPTION",
												textValue.substring(textValue.indexOf("光口") + 2, textValue.indexOf("。")));
									} else {
										privateFieldMap.put("ALARM_REASON_DESCRIPTION",
												textValue.substring(textValue.indexOf("光口") + 2));
									}
								}
							}
						}
						break;
					case "b":
						alarmData.put("EVENTTIME", getStandardTime(value));
						break;
				}
			}
			if(alarmData.get("EVENTTIME").equals("") || alarmData.get("EVENTTIME") == null){
				alarmData.put("EVENTTIME", alarmData.get("CLEARANCETIMESTAMP"));
			}
			
			if (!"RSU".equals(type) && "BtsFunction".equals(managerObjectClass) && !"".equals(privateFieldMap.get("RRUNAME"))) {
				alarmData.put("EQUIPMENTNAME", privateFieldMap.get("RRUNAME"));
			}
			
			if ("".equals(alarmData.get("EQUIPMENTNAME")) || alarmData.get("EQUIPMENTNAME") == null) {
				alarmData.put("EQUIPMENTNAME", privateFieldMap.get("MANAGED_OBJECT_INSTANCE"));
			}

			if ("5".equals(alarmData.get("PERCEIVEDSEVERITY")) && "".equals(alarmData.get("CLEARANCETIMESTAMP"))) {
				alarmData.put("CLEARANCETIMESTAMP", alarmData.get("EVENTTIME"));
			}

			alarmData.put("ADDITIONALTEXT", srcAlarm);
			alarmData.put("CREATIONTIMESTAMP", getStandardTime(new Date()));
			alarmData.put("PRIVATEFIELDJSON", privateFieldMap);

			return spliceClearAlarm(alarmData, cacheManager.getCache("ZTE_CDMA_ALARM"), "5".equals(alarmData.get("PERCEIVEDSEVERITY")));
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
				alarm.put("EVENTTIME", alarmData.get("EVENTTIME"));
				alarm.put("ADDITIONALTEXT", alarmData.get("ADDITIONALTEXT"));

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
	
	private static String getRegexValue(String content, String regex) {
		Matcher matcher = Pattern.compile(regex).matcher(content);
		if (matcher.find() && !regex.isEmpty() && !content.isEmpty()) {
			return matcher.group(1).trim();
		}
		return "";
	}
}
