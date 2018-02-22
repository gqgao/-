/**
 * 文件名:KafkaZkAlarmSourceSession.java
 * Copyright  2017，北京福富软件技术股份有限公司 
 * All Rights Reserved. 
 * 文件编号:
 * 创建人: LinQj
 * 日期: 2017年11月13日下午3:58:15
 * 修改人:
 * 日期:
 * 摘要:
 * 版本号: 
 * 原 作 者: LinQj
 * 完成日期:
 */
package com.ffcs.oss.fm.session.source.zk;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.ffcs.oss.fm.session.source.AbstractAlarmSourceSession;

/**
 * 通过Zookeeper获取kafka的broker
 */
@Service
public class KafkaZkAlarmSourceSession extends AbstractAlarmSourceSession {

	private static final Logger LOGGER = LoggerFactory.getLogger(KafkaZkAlarmSourceSession.class);

	@Autowired
	private ZkConsumerCfg consumerCfg;

	@Autowired
	private Environment env;

	private ConsumerConnector connector;

	@PostConstruct
	private void init() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				isValid = false;
				KafkaZkAlarmSourceSession.this.destroy();
			}
		});
	}

	@Override
	public void ready() throws Exception {

		LOGGER.info("开始从kafka服务器获取数据");
		ConsumerConfig config = new ConsumerConfig(consumerCfg.getProps());
		connector = kafka.consumer.Consumer.createJavaConsumerConnector(config);
		Map<String, Integer> topicCountMap = new HashMap<>();
		topicCountMap.put(consumerCfg.getTopic(), new Integer(1));
		Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = connector.createMessageStreams(topicCountMap);
		KafkaStream<byte[], byte[]> streams = consumerMap.get(consumerCfg.getTopic()).get(0);
		ConsumerIterator<byte[], byte[]> it = streams.iterator();
		DesUtil desUtil = new DesUtil("1234abcd", "CBC", "NoPadding", "00000000");
		new Thread(new Runnable() {
			@Override
			public void run() {
					while (isValid) {
						if (it.hasNext()) {
							try {
								LOGGER.info("接收告警时间[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "]");
								String alarm = desUtil.decrypt(new String(it.next().message(), env.getProperty("alarmEncode")));
								addAlarm(alarm);
								Thread.sleep(2000);
							} catch (Exception e) {
								LOGGER.error("告警采集异常：", e);
							}
						}
					}
			}
		}).start();
	}

	@Override
	public void destroy() {

	}

}
