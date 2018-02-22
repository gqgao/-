/**************************************************************
 * Copyright @ 2016，北京福富软件技术股份有限公司
 * All Rights Reserved.
 *
 * 文件名称：AbstractAlarmSourceSession.java
 * 摘       要：
 * 作       者： yew
 * 创建时间：2016年8月26日 下午12:45:06
 *
 **************************************************************/
package com.ffcs.oss.fm.session.source;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;


/*************************************
 * 类名称：抽象告警源会话
 * 类功能：
 *************************************/
public abstract class AbstractAlarmSourceSession implements AlarmSourceSession {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAlarmSourceSession.class);
	
	// 告警队列大小
	private static final int QUEUE_SIZE = 100000;
	
	// 告警采集队列
	protected BlockingQueue<String> alarmQueue = new ArrayBlockingQueue<String>(QUEUE_SIZE, true);

	protected int idleTimeout = 1800;

	@Override
	public synchronized String getAlarm() throws Exception {
		if (idleTimeout == 0) {
		    // take():检索并移除此队列的头部，如果此队列不存在任何元素，则一直等待。
			return alarmQueue.take();
		}
		String alarm = alarmQueue.poll(idleTimeout, TimeUnit.SECONDS);
		if (alarm == null) {
			throw new Exception("超过" + idleTimeout + "秒未收到告警或心跳");
		}
		return alarm;
	}
	
	/**
	 * 添加告警到告警采集队列
	 * @param alarm String
	 * @return
	 */
	public void addAlarm(String alarm) {
		if (alarmQueue.offer(alarm)) {
			LOGGER.info("告警成功添加到队列：" + alarm);
		} else {
			LOGGER.error("消息队列已满，当前队列大小：" + alarmQueue.size());
			LOGGER.error("丢弃消息：" + alarm);
		}
	}

	public void setIdleTimeout(int idleTimeout) {
		this.idleTimeout = idleTimeout;
	}
}
