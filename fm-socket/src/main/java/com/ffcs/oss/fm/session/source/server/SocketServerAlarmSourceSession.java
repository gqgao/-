/**
 * 文件名:SocketServerAlarmSourceSession.java
 * Copyright 2016，北京福富软件技术股份有限公司
 * All Rights Reserved.
 * 文件编号:
 * 创建人: LinQj
 * 日期: 2016年10月24日下午6:57:57
 * 修改人:
 * 日期:
 * 摘要:
 * 版本号:
 * 原 作 者: LinQj
 * 完成日期:
 */
package com.ffcs.oss.fm.session.source.server;

import java.net.InetSocketAddress;
import java.util.Iterator;

import javax.annotation.PostConstruct;

import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ffcs.oss.fm.session.source.AbstractAlarmSourceSession;
import com.ffcs.oss.fm.session.source.SocketConn;

/**
 * 类名称：Socket服务端告警会话
 */
@Service
public class SocketServerAlarmSourceSession extends AbstractAlarmSourceSession {

	private static final Logger LOGGER = LoggerFactory.getLogger(SocketServerAlarmSourceSession.class);

	// Socket服务端
	private IoAcceptor socketAcceptor;

	// 连接信息
	@Autowired
	private SocketConn conn;

	@PostConstruct
	private void init() {
		isValid = true;
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				SocketServerAlarmSourceSession.this.destroy();
				isValid = false;
			}
		});
	}

	@Override
	public void ready() throws Exception {
		destroy();
		LOGGER.info("Socket连接信息:" + conn);
		if (conn.getSocketType() == 0) {
			socketAcceptor = new NioSocketAcceptor();
		} else {
			socketAcceptor = new NioDatagramAcceptor();
		}
		socketAcceptor.getSessionConfig().setIdleTime(IdleStatus.READER_IDLE, conn.getTimeout());
		socketAcceptor.getSessionConfig().setReadBufferSize(100000);
		DataProcessHandler handler = new DataProcessHandler(conn);
		socketAcceptor.setHandler(handler);
		socketAcceptor.bind(new InetSocketAddress(conn.getIpAddress(), conn.getPort()));
		LOGGER.info("Socket服务端创建成功信息[" + conn + "],等待客户端连接...");
		new Thread(new Runnable() {
			@Override
			public void run() {
				Thread.currentThread().setName("AlarmSourceThread");
				while (isValid) {
					try {
						Iterator<IoSession> sessions = socketAcceptor.getManagedSessions().values().iterator();
						while (sessions.hasNext()) {
							IoSession session = sessions.next();
							if (session == null) {
								LOGGER.info("session---->null");
							}
							DataProcessHandler sessionHandler = (DataProcessHandler) session.getHandler();
							if (sessionHandler == null) {
								LOGGER.info("sessionHandler----->null");
							}
							String alarm = sessionHandler.getAlarm();
							if (!alarm.isEmpty()) {
								addAlarm(alarm);
								try {
									Thread.sleep(1000);
								} catch (Exception e) {
									LOGGER.error("线程中断", e);
								}
							}
						}
					} catch (Exception e) {
						LOGGER.error("告警源线程执行异常：", e);
					}
				}
				LOGGER.error("告警源线程退出");
			}
		}).start();
	}

	@Override
	public void destroy() {
		if (socketAcceptor != null) {
			socketAcceptor = null;
		}
	}

}
