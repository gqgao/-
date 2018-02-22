/**
 * 文件名:ConsumerConfig.java
 * Copyright  2017，北京福富软件技术股份有限公司 
 * All Rights Reserved. 
 * 文件编号:
 * 创建人: LinQj
 * 日期: 2017年11月9日下午7:04:06
 * 修改人:
 * 日期:
 * 摘要:
 * 版本号: 
 * 原 作 者: LinQj
 * 完成日期:
 */
package com.ffcs.oss.fm.session.source.listener;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "client.kafka")
public class ListenerConsumerCfg {

	private static final Logger LOGGER = LoggerFactory.getLogger(ListenerConsumerCfg.class);
	private String servers;
	private boolean enableAutoCommit;
	private String sessionTimeout;
	private String autoCommitInterval;
	private String groupId;
	private String keydeserializer;
	private String valuedeserializer;
	private int concurrency;
	private String topic;
	private Map<String, Object> propsMap;

	@PostConstruct
	public void init() {
		LOGGER.info("开始加载kafka消费端配置参数");
		propsMap = new HashMap<>();
		propsMap.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
		propsMap.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, enableAutoCommit);
		propsMap.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, autoCommitInterval);
		propsMap.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, sessionTimeout);
		propsMap.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keydeserializer);
		propsMap.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valuedeserializer);
		propsMap.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		propsMap.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 1000);
	}
}
