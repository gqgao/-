package com.ffcs.oss.cap.alarm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class AlarmCollectorApplication {

	private static final Logger LOGGER = LoggerFactory.getLogger(AlarmCollectorApplication.class);
	public static void main(String[] args) {
		SpringApplication.run(AlarmCollectorApplication.class, args);
	}
}
