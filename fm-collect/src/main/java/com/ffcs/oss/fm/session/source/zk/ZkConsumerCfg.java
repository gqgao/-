/**
 * 文件名:Config.java
 * Copyright  2017，北京福富软件技术股份有限公司 
 * All Rights Reserved. 
 * 文件编号:
 * 创建人: LinQj
 * 日期: 2017年11月13日下午5:42:32
 * 修改人:
 * 日期:
 * 摘要:
 * 版本号: 
 * 原 作 者: LinQj
 * 完成日期:
 */
package com.ffcs.oss.fm.session.source.zk;

import java.util.Properties;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;


@Data
@ConfigurationProperties(prefix="client.kafka")
@Component
public class ZkConsumerCfg {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ZkConsumerCfg.class);
	// kafka配置
	private String zookeepers;
	private String groupId;
	private String zkTimeout;
	private String zksynctime;
	private String autoCommitInterval;
	private String offSetRest;
	private String serializerClass;
	private String topic;
	private Properties props;
	
	@PostConstruct
	public void init() {
		LOGGER.info("开始加载kafka消费端配置参数");
		props = new Properties();
		props.put("zookeeper.connect",zookeepers);
		// group 代表一个消费组
		props.put("group.id", groupId);
		// zk连接超时
		props.put("zookeeper.session.timeout.ms", zkTimeout);
		props.put("zookeeper.sync.time.ms", zksynctime);
		props.put("auto.commit.interval.ms", autoCommitInterval);
		props.put("auto.offset.reset", offSetRest);
		// 序列化类
		props.put("serializer.class", serializerClass);
	}
}
