/**************************************************************
 * Copyright @ 2016，北京福富软件技术股份有限公司
 * All Rights Reserved.
 * 文件名称：SocketAlarmSourceSession.java
 * 摘 要：
 * 作 者： yew
 * 创建时间：2016年8月26日 下午12:29:30
 **************************************************************/
package com.ffcs.oss.fm.session.source.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.ffcs.oss.fm.session.source.AbstractAlarmSourceSession;
import com.ffcs.oss.fm.session.source.SocketConn;
import org.springframework.stereotype.Service;

/*************************************
 * 类名称：Socket告警源会话
 * 类功能：适用于普通字符流方式的Socket告警采集接口
 *************************************/
//@Service
public class SocketAlarmSourceSession extends AbstractAlarmSourceSession {

	private static final Logger LOGGER = LoggerFactory.getLogger(SocketAlarmSourceSession.class);

	// 告警接口连接信息
	@Autowired
	private SocketConn conn;

	// Temip Socket连接
	private Socket socket;

	// Socket输入流
	private InputStream reader;

	// Socket输出流
	private OutputStream writer;

	@PostConstruct
	private void init() {
		isValid = true;
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				SocketAlarmSourceSession.this.destroy();
				isValid = false;
			}
		});
	}

	@Override
	public void ready() throws Exception {
		// 消除会话
		destroy();

		// 建立连接
		LOGGER.info("开始建立Socket连接:" + conn.toString());
		if (conn.getLocalPort() == 0) {
			socket = new Socket(conn.getIpAddress(), conn.getPort());
		} else {
			//socket = new Socket(conn.getIpAddress(), conn.getPort(), InetAddress.getLocalHost(), conn.getLocalPort());
			if (StringUtils.isEmpty(conn.getLocalHost())) {
				socket = new Socket(conn.getIpAddress(), conn.getPort(), InetAddress.getByName("0.0.0.0"), conn.getLocalPort());
			} else {
				socket = new Socket(conn.getIpAddress(), conn.getPort(), InetAddress.getByName(conn.getLocalHost()), conn.getLocalPort());
			}
		}
		socket.setSoTimeout(conn.getTimeout() * 1000);
		socket.setTcpNoDelay(true);
		socket.setKeepAlive(true);
		reader = socket.getInputStream();
		writer = socket.getOutputStream();
		LOGGER.info("建立socket连接成功,开始采集告警数据");

		// 采集告警
		StringBuilder builder = new StringBuilder();
		String endFlag = conn.getPacketEndExp();
		String startFlag = conn.getPacketBeginExp();
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.currentThread().setName("AlarmSourceThread");
					while (isValid) {
						writer.write("realtime_alarm".getBytes(conn.getCharEncoding()));
						// 读取表示告警体长度的四个字节
						byte[] alarmByte = new byte[1024];
						String content = new String(alarmByte, 0, reader.read(alarmByte), conn.getCharEncoding());
						builder.append(content);
						while (builder.indexOf(conn.getPacketEndExp()) != -1) {
							addAlarm(builder.substring(builder.indexOf(conn.getPacketBeginExp()), builder.indexOf(conn.getPacketEndExp()) + conn.getPacketEndExp().length()));
							builder.delete(0, builder.indexOf(conn.getPacketEndExp()) + conn.getPacketEndExp().length());
						}
					}
				} catch (Exception e) {
					LOGGER.error("添加告警错误", e);
				}
			}
		}).start();
	}

	@Override
	public void destroy() {
		LOGGER.info("断开Socket连接");
		if (reader != null) {
			try {
				reader.close();
			} catch (IOException e) {
				LOGGER.info("关闭输入流错误", e);
			}
		}
		if (writer != null) {
			try {
				writer.close();
			} catch (IOException e) {
				LOGGER.info("关闭输出流错误", e);
			}
		}
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				LOGGER.info("关闭socket流错误", e);
			}
		}
		reader = null;
		writer = null;
		socket = null;
	}

	//public int getByteToInt(byte[] b)
	//{
	//	int result = b[3] & 0xFF;
	//	result = (result << 8) + (b[2] & 0xFF);
	//	result = (result << 8) + (b[1] & 0xFF);
	//	result = (result << 8) + (b[0] & 0xFF);
	//	return result;
	//}

	/**
	 * byte数组中取int数值，本方法适用于(低位在前，高位在后)的顺序，和和intToBytes（）配套使用
	 *
	 * @param src
	 *            byte数组
	 * @param offset
	 *            从数组的第offset位开始
	 * @return int数值
	 */
	public static int bytesToInt(byte[] src, int offset) {
		int value;
		value = (int) ((src[offset] & 0xFF)
				| ((src[offset+1] & 0xFF)<<8)
				| ((src[offset+2] & 0xFF)<<16)
				| ((src[offset+3] & 0xFF)<<24));
		return value;
	}

	/**
	 * byte数组中取int数值，本方法适用于(低位在后，高位在前)的顺序。和intToBytes2（）配套使用
	 */
	public static int bytesToInt2(byte[] src, int offset) {
		int value;
		value = (int) ( ((src[offset] & 0xFF)<<24)
				|((src[offset+1] & 0xFF)<<16)
				|((src[offset+2] & 0xFF)<<8)
				|(src[offset+3] & 0xFF));
		return value;
	}
}
