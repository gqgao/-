/**
 * 文件名:KafkaListener.java
 * Copyright  2017，北京福富软件技术股份有限公司 
 * All Rights Reserved. 
 * 文件编号:
 * 创建人: LinQj
 * 日期: 2017年11月9日下午7:37:23
 * 修改人:
 * 日期:
 * 摘要:
 * 版本号: 
 * 原 作 者: LinQj
 * 完成日期:
 */
package com.ffcs.oss.fm.session.source.listener;

import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

public class MessageListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(MessageListener.class);

	private BlockingQueue<String> alarmQueue = new ArrayBlockingQueue<>(1000, true);

	@KafkaListener(topics = { "AHDX_ITOWER_30633_ALARM" })
	public void listen(ConsumerRecord<?, ?> record) {
		Optional<?> kafkaMessage = Optional.ofNullable(record.value());
		if (kafkaMessage.isPresent()) {
			Object message = kafkaMessage.get();
			addAlarm((String) message);
		}
	}

	public synchronized String getAlarm() {
		String alarm = "";
		if (!alarmQueue.isEmpty()) {
			try {
				alarm = alarmQueue.poll(5, TimeUnit.SECONDS);
			} catch (Exception e) {
				LOGGER.info("获取告警失败", e);
			}
		}
		return alarm;
	}

	public synchronized String addAlarm(String alarm) {
		if (alarmQueue.offer(alarm)) {
			return "SUCCESS";
		}
		return "FAILD";
	}
}
