package com.ffcs.oss.cap.alarm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AlarmCollectorApplicationTests {
	private static final Logger LOGGER = LoggerFactory.getLogger(AlarmCollectorApplicationTests.class);

	@Test
	public void contextLoads() {
		try {
			Matcher m = Pattern.compile("").matcher("1234");
			if(m.find()){
				System.out.println(m.group(1));
			}
		} catch (Exception e) {
			LOGGER.info("发送失败：", e);
		}
	}

}
