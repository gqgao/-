/**
 * 文件名:DataProcessHandler.java
 * Copyright 2016，北京福富软件技术股份有限公司
 * All Rights Reserved.
 * 文件编号:
 * 创建人: LinQj
 * 日期: 2016年10月25日下午3:46:38
 * 修改人:
 * 日期:
 * 摘要:
 * 版本号:
 * 原 作 者: LinQj
 * 完成日期:
 */
package com.ffcs.oss.fm.session.source.server;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

import com.ffcs.oss.fm.session.source.SocketConn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataProcessHandler extends IoHandlerAdapter {

	private static final Logger LOGGER = LoggerFactory.getLogger(DataProcessHandler.class);

	public BlockingQueue<String> alarmQueue = new ArrayBlockingQueue<>(1000, true);

	// 服务端连接信息
	private SocketConn conn;

	public DataProcessHandler(SocketConn conn) {
		this.conn = conn;
	}

	@Override
	public void sessionCreated(IoSession session) {
		LOGGER.info("客户端[" + session.getRemoteAddress() + "]的连接请求，创建会话成功");
		LOGGER.info("创建告警缓存容器");
		session.setAttribute("ALARM_BUFFER", new StringBuilder());
	}

	@Override
	public void sessionIdle(IoSession session, IdleStatus status) {

		if (session.isReaderIdle()) {
			LOGGER.info("超过规定" + conn.getTimeout() + "秒未收到客户端报文主动断开客户端连接");
			session.closeOnFlush();
		}

	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		LOGGER.error("接收会话错误", cause);
	}

	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {

		StringBuilder buffer = (StringBuilder) session.getAttribute("ALARM_BUFFER");
		String alarmBegin = conn.getPacketBeginExp();
		String alarmEnd = conn.getPacketEndExp();
		IoBuffer ioBuffer = (IoBuffer) message;
		byte[] bytes = new byte[ioBuffer.limit()];
		ioBuffer.get(bytes);
		String content = buffer.toString() + new String(bytes, conn.getCharEncoding());
		LOGGER.info("接收到告警:" + content);
		if (content.contains(alarmBegin) && content.contains(alarmEnd)) {
			while (content.contains(alarmBegin) && content.contains(alarmEnd)) {
				String alarm = content.substring(content.indexOf(alarmBegin), content.indexOf(alarmEnd)).replace(alarmBegin, "");
				// 处理缓存信息
				LOGGER.info("接收到客户端发送的完整告警,内容如下:[" + alarm + "]");
				addAlarm(alarm);
				content = content.replace(alarmBegin + alarm + alarmEnd, "");
			}
			// 删除缓存中记录，添加剩余的告警内容
			buffer.delete(0, buffer.length());
			buffer.append(content);
		} else if (content.equals(conn.getHeatBeat())) {
			LOGGER.info("接收到心跳报文:" + content);
		} else {
			LOGGER.info("接收到的客户端告警内容:" + content);
			buffer.append(content);
		}

		// 服务端发送响应
		byte[] resBytes = conn.getResponsePck().getBytes(conn.getCharEncoding());
		IoBuffer buff = IoBuffer.allocate(resBytes.length);
		buff.setAutoExpand(true);
		buff.put(resBytes);
		buff.flip();
		session.write(buff);
	}

	public synchronized String getAlarm() {
		String alarm = "";
		if (!alarmQueue.isEmpty()) {
			try {
				alarm = alarmQueue.poll(5, TimeUnit.SECONDS);
			} catch (Exception e) {
				LOGGER.info("获取告警失败", e);
			}
		}
		return alarm;
	}

	public synchronized String addAlarm(String alarm) {
		if (alarmQueue.offer(alarm)) {
			LOGGER.info("成功添加告警到元消息队列");
			return "SUCCESS";
		} else {
			LOGGER.error("元消息队列已满，当前队列大小：" + alarmQueue.size());
			LOGGER.error("丢弃消息：" + alarm);
			return "FAILD";
		}
	}

	@Override
	public void sessionClosed(IoSession session) {
		LOGGER.info("客户端[" + session.getRemoteAddress() + "]的连接中断，会话关闭");
	}

}
