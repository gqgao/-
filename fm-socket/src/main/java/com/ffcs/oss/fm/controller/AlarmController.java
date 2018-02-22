package com.ffcs.oss.fm.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.ffcs.oss.fm.filter.AlarmFilter;
import com.ffcs.oss.fm.process.AlarmProcessor;
import com.ffcs.oss.fm.session.send.AlarmSendSession;
import com.ffcs.oss.fm.session.source.AlarmSourceSession;

import io.swagger.annotations.ApiOperation;

/**
 * Created by lenovo on 2017/7/5.
 * 告警接口
 */
@RestController
@RequestMapping(value = "alarm", method = RequestMethod.POST)
public class AlarmController {

	private static final Logger LOGGER = LoggerFactory.getLogger(AlarmController.class);

	@Autowired
	private AlarmFilter alarmFilter;

	@Autowired
	private AlarmProcessor alarmProcessor;

	@Autowired
	private AlarmSourceSession alarmSourceSession;

	@Autowired
	private AlarmSendSession alarmSendSession;

	@Bean
	public CommandLineRunner startCollectAlarm() {
		return args -> {
			LOGGER.info("告警任务采集启动");
			try {
				alarmSourceSession.ready();
				while (true) {
					dealAlarm();
				}
			} catch (Exception e) {
				LOGGER.error("告警采集任务执行出错", e);
			}
			LOGGER.info("告警任务终止");
		};
	}

	private void dealAlarm() {
		try {
			String srcAlarm = alarmSourceSession.getAlarm();
			LOGGER.info("原始告警：" + srcAlarm);

			if (alarmFilter != null && alarmFilter.filterAlarm(srcAlarm)) {
				LOGGER.info("告警被过滤：" + srcAlarm);
				return;
			}

			String alarm = srcAlarm;
			if (alarmProcessor != null) {
				alarm = alarmProcessor.processAlarm(srcAlarm);
				if (alarm == null) {
					LOGGER.info("告警处理异常，不发送:" + alarm);
					return;
				}
				LOGGER.info(srcAlarm + "\n转换后告警：\n" + alarm);
			}

			if (alarmSendSession != null) {
				alarmSendSession.sendAlarm(alarm);
			}
		} catch (Exception e) {
			LOGGER.error("告警采集会话出错，准备重新建立会话", e);
			try {
				Thread.sleep(30 * 1000);
				alarmSourceSession.ready();
			} catch (Exception e1) {
				LOGGER.error("重新建立告警源会话出错", e1);
			}
		} catch (Throwable e) {
			LOGGER.error("告警采集出错", e);
		}
	}

	@RequestMapping("notify")
	@ApiOperation("接收告警通知")
	public void notify(@RequestBody String alarmContent) {
		LOGGER.info("收到告警通知：" + alarmContent);

		if (alarmFilter.filterAlarm(alarmContent)) {
			LOGGER.info("告警被过滤：" + alarmContent);
			return;
		}

		try {
			String alarm = alarmProcessor.processAlarm(alarmContent);
			LOGGER.info("处理后的告警：" + alarm);
			alarmSendSession.sendAlarm(alarm);
		} catch (Exception e) {
			LOGGER.error("告警解析处理出错：" + alarmContent, e);
		}
	}
}
