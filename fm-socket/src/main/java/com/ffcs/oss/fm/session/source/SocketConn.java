/**************************************************************
 * Copyright @ 2016，北京福富软件技术股份有限公司
 * All Rights Reserved.
 * 文件名称：SocketConn.java
 * 摘 要：
 * 作 者： yew
 * 创建时间：2016年8月26日 下午12:31:06
 **************************************************************/
package com.ffcs.oss.fm.session.source;

import lombok.Data;

/*************************************
 * 类名称：Socket连接信息 类功能：
 *************************************/
@Data
public class SocketConn {

	// IP地址
	private String ipAddress;

	// 端口
	private int port;

	// 字符编码
	private String charEncoding;

	// 超时时间
	private int timeout;

	// 测试连接命令（握手或心跳命令）
	private String heatBeat;

	// 报文结束标识
	private String packetEndExp;

	// 报文正常标识
	private String packetBeginExp;

	private String responsePck;

	// Socket类型
	private int socketType;

	// 本地IP
	private String localHost;

	// 本机端口
	private int localPort;

	private String testPkg;

	@Override
	public String toString() {
		return "SocketConn [ipAddress=" + ipAddress + ", port=" + port + ", charEncoding=" + charEncoding + ", timeout=" + timeout + ", heatBeat=" + heatBeat + ", packetEndExp="
				+ packetEndExp + ", packetBeginExp=" + packetBeginExp + "]";
	}
}
