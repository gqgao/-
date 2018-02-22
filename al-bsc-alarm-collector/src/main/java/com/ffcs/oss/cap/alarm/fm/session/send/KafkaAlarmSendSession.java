package com.ffcs.oss.cap.alarm.fm.session.send;

import lombok.Setter;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * Created by ${gujj} on 2017-08-24.
 */
@Service
public class KafkaAlarmSendSession implements AlarmSendSession {

	private final static Logger LOGGER = LoggerFactory.getLogger(KafkaAlarmSendSession.class);

	@Autowired
	private KafkaTemplate kafkaTemplate;

	@Override
	public void destroy() {}

	@Override
	public void sendAlarm(String alarm) {
		try {
			String key = "" + JSONObject.fromObject(alarm).get("EQUIPMENTNAME");
			LOGGER.info("开始发送告警至kafka：【key】" + key + "【alarm】" + alarm);
			ListenableFuture future = kafkaTemplate.sendDefault(key, alarm);
			future.get();
			LOGGER.info("成功发送告警至kafka");
		} catch (Exception e) {
			LOGGER.error("发送告警至kafka失败：", e);
		}
	}
}
