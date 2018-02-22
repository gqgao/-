/**
 * 文件名:KafkaAlarmSourceSession.java
 * Copyright  2017，北京福富软件技术股份有限公司 
 * All Rights Reserved. 
 * 文件编号:
 * 创建人: LinQj
 * 日期: 2017年11月9日下午6:47:31
 * 修改人:
 * 日期:
 * 摘要:
 * 版本号: 
 * 原 作 者: LinQj
 * 完成日期:
 */
package com.ffcs.oss.fm.session.source.listener;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.stereotype.Service;

import com.ffcs.oss.fm.session.source.AbstractAlarmSourceSession;

@ConfigurationProperties(prefix = "client.kafka")
public class KafkaListenerAlarmSourceSession extends AbstractAlarmSourceSession {

	private static final Logger LOGGER = LoggerFactory.getLogger(KafkaListenerAlarmSourceSession.class);

	@Autowired
	private ListenerConsumerCfg config;

	@Autowired
	MessageListener listener;

	@PostConstruct
	private void init() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				isValid = false;
				KafkaListenerAlarmSourceSession.this.destroy();
			}
		});
	}

//	@Bean
//	public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> kafkaListenerContainerFactory() {
//		ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
//		factory.setConsumerFactory(consumerFactory());
//		factory.setConcurrency(3);
//		factory.getContainerProperties().setPollTimeout(1500);
//		return factory;
//	}
//
//	public ConsumerFactory<String, String> consumerFactory() {
//		return new DefaultKafkaConsumerFactory<>(config.getPropsMap());
//	}

	@Override
	public void ready() throws Exception {
		LOGGER.info("开始监听告警采集");
		while (isValid) {
			String alarm = listener.getAlarm();
			if (!alarm.isEmpty()) {
				addAlarm(alarm);
				Thread.sleep(1000);
			}
		}
	}

	@Override
	public void destroy() {

	}

}
