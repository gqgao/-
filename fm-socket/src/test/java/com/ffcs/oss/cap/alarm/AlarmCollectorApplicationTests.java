package com.ffcs.oss.cap.alarm;

import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AlarmCollectorApplicationTests {
	private static final Logger LOGGER = LoggerFactory.getLogger(AlarmCollectorApplicationTests.class);

	@Test
	public void contextLoads() {
		try {
			String[] additionals = StringUtils.split("NeLocation: ", ":");
			LOGGER.info("lenth : " + additionals.length);
		} catch (Exception e) {
			LOGGER.info("发送失败：", e);
		}
	}

}
