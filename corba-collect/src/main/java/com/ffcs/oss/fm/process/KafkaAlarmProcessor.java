package com.ffcs.oss.fm.process;

import net.sf.json.JSONObject;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.lookup.MainMapLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ffcs.oss.fm.AlarmParams;

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

	private Map<String, String> defaultAlarmMap;

	@PostConstruct
	private void init() {
		setDefaultAlarmMap();
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
			Map<String, Object> privateFieldMap = new HashMap<>();
			privateFieldMap.put("BSCNAME", "");
			privateFieldMap.put("BTSID", "");
			privateFieldMap.put("CELLID", "");
			privateFieldMap.put("CELLNAME", "");
			privateFieldMap.put("RRUNAME", "");

			JSONObject jsonObject = JSONObject.fromObject(srcAlarm);
			Iterator<String> it = jsonObject.keys();
			String alarmSeverity = "";
			String eventTime = "";
			String equipmentName = "";
			while (it.hasNext()) {
				String key = it.next().trim();
				String value = ("" + jsonObject.get(key)).trim();
				switch (key) {
					case "h":
						alarmData.put("PERCEIVEDSEVERITY", value);
						if (value.equals("6")) {
							alarmSeverity = value;
						}
						break;
					case "event_name":
						if (!"".equals(value)) {
							alarmData.put("ALARMTYPE", Integer.parseInt(value.substring(1, 2)));
						}
						break;
					case "b":
						alarmData.put("EVENTTIME", getStandardTime(value));
						eventTime = value;
						break;
					case "c":
						alarmData.put("SYSTEM_DN", value);
						break;
					case "remainder_of_body":
						JSONObject remainJsonObj = JSONObject.fromObject(value);
						Iterator<String> remainIt = remainJsonObj.keys();
						while (remainIt.hasNext()) {
							String remainKey = remainIt.next().trim();
							String remainValue = ("" + remainJsonObj.get(remainKey)).trim();
							if (remainKey.equals("a")) {
								alarmData.put("NOTIFICATIONID", remainValue);
							} else if (remainKey.equals("f")) {
								alarmData.put("VENDOR_ALARMID", remainValue);
							} else if (remainKey.equals("i")) {
								alarmData.put("SPECIFIC_PROBLEM", remainValue);
							} else {
								if (remainValue.contains("|")) {
									for (String additional : StringUtils.split(remainValue, "|")) {
										String left = StringUtils.substring(additional, 0, additional.indexOf(":")).trim();
										String right = StringUtils.substring(additional, additional.indexOf(":") + 1).trim();
										if (left.equals("NeLocation") && right.contains("_")) {
											alarmData.put("REGION", StringUtils.split(right, "_")[0]);
											alarmData.put("REGIONALISM", StringUtils.split(right, "_")[1]);
										} else if (left.equals("NeType")) {
											alarmData.put("NECLASS", right);
										} else if (left.equals("neName")) {
											alarmData.put("EQUIPMENTNAME", right);
										} else if (left.equals("alarmName")) {
											alarmData.put("ALARMTITLE", right);
										} else if (left.equals("alarmLocation")) {
											alarmData.put("LOCATIONINFO", right);
											if (right.contains(",")) {
												for (String alarmLocation : StringUtils.split(right, ",")) {
													String locationName = StringUtils.substring(alarmLocation, 0, alarmLocation.indexOf("=")).trim();
													String locationValue = StringUtils.substring(alarmLocation, alarmLocation.indexOf("=") + 1).trim();
													if (locationName.equals("柜号")) {
														alarmData.put("RACK", locationValue);
													} else if (locationName.equals("框号")) {
														alarmData.put("SHELF", locationValue);
													} else if (locationName.equals("槽号")) {
														alarmData.put("SLOT", locationValue);
													} else if (locationName.equals("端口号")) {
														alarmData.put("PORTLOCATION", locationValue);
													} else if (locationName.equals("单板类型")) {
														alarmData.put("BOARDLOCATION", locationValue);
													} else if (locationName.equals("本地小区标识")) {
														privateFieldMap.put("CELLID", locationValue);
													} else if (locationName.equals("小区名称")) {
														privateFieldMap.put("CELLNAME", locationValue);
														equipmentName = locationValue;
													}
												}
											}
										} else if (left.equals("appendInfo")) {
											alarmData.put("APPENDINFO", right);
											if (right.contains(",")) {
												for (String appendInfo : StringUtils.split(right, ",")) {
													String infoName = StringUtils.substring(appendInfo, 0, appendInfo.indexOf("=")).trim();
													String infoValue = StringUtils.substring(appendInfo, appendInfo.indexOf("=") + 1).trim();
													if (infoName.equals("射频单元名称")) {
														privateFieldMap.put("RRUNAME", infoValue);
														equipmentName = infoValue;
													} else if (infoName.equals("eNodeBId")) {
														privateFieldMap.put("BTSID", infoValue);
													}
												}
											}
										} else if (left.equals("clearType")) {
											alarmData.put("CLEARTYPE", right);
										}
									}
								}
							}
						}
						break;
				}
			}

			if (alarmSeverity.equals("6") && !"".equals(eventTime)) {
				alarmData.put("CLEARANCETIMESTAMP", getStandardTime(eventTime));
			}

			if (!"".equals(equipmentName)) {
				alarmData.put("EQUIPMENTNAME", equipmentName);
			}

			alarmData.put("ADDITIONALTEXT", srcAlarm);
			alarmData.put("CREATIONTIMESTAMP", getStandardTime(new Date()));
			alarmData.put("PRIVATEFIELDJSON", privateFieldMap);

			return JSONObject.fromObject(alarmData).toString();
		} catch (Exception e) {
			LOGGER.error("处理告警失败，将返回null：", e);
			return null;
		}
		
	}
	public static void main(String[] args) {
		String srcAlarm = "{\"domain_name\":\"Alarm IRP V3.1\",\"type_name\":\"x5\",\"event_name\":\"x5\",\"e\":\"SubNetwork=hf1omcran,ManagedElement=BTS-350,BtsFunction=MOD1BTS-350\",\"a\":\"730990\",\"b\":\"2018-02-01 14:42:27.252\",\"g\":\"340\",\"h\":\"6\",\"f\":\"Orca=1,Mod1bts=350,Alarm=24732089\",\"c\":\"SubNetwork=hf1omcran,ManagementNode=hf1omcran,IRPAgent=1\",\"d\":\"BtsFunction\",\"remainder_of_body\":\"[ParseUnknownTCKind=0]\"}";
		
	}
	/**
	 * 默认将每个字段的值置为“”
	 */
	private void setDefaultAlarmMap() {
		defaultAlarmMap = new HashMap<>();
		Pattern pattern = Pattern.compile("\\w+");
		Matcher matcher = pattern.matcher(alarmParams.getColumns());
		while (matcher.find()) {
			defaultAlarmMap.put(matcher.group(), "");
		}
	}

	/**
	 * 添加额外的固定字段
	 */
	private void addFixedFiled() {
		Map<String, String> addFixedField = alarmParams.getAddFixedField();
		if (addFixedField != null && !addFixedField.isEmpty()) {
			defaultAlarmMap.putAll(addFixedField);
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
}
