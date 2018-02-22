/**
 * 文件名:SnmpAlarmSourceSession.java
 * Copyright  2017，北京福富软件技术股份有限公司
 * All Rights Reserved.
 * 文件编号:
 * 创建人: LinQj
 * 日期: 2017年11月8日下午3:03:59
 * 修改人:
 * 日期:
 * 摘要:
 * 版本号:
 * 原 作 者: LinQj
 * 完成日期:
 */
package com.ffcs.oss.fm.session.source;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.annotation.PostConstruct;

import com.ffcs.oss.fm.AlarmParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TcpAddress;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.MultiThreadedMessageDispatcher;
import org.snmp4j.util.ThreadPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class SnmpAlarmSourceSession extends AbstractAlarmSourceSession {

	private static final Logger LOGGER = LoggerFactory.getLogger(SnmpAlarmSourceSession.class);

	@Autowired
	private Environment env;

	@Autowired
	private AlarmParams params;

	// 告警监听路径
	private String listenerAddr;

	private Snmp snmp;

	private ThreadPool threadPool;

	private MultiThreadedMessageDispatcher dispatcher;

	@PostConstruct
	public void init() {
		listenerAddr = env.getProperty("listenerAddr");
		LOGGER.info("加载TRAP监听地址:" + listenerAddr);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				SnmpAlarmSourceSession.this.destroy();
			}
		});
	}

	@Override
	public void ready() throws Exception {
		destroy();
		threadPool = ThreadPool.create("Trap", 2);
		dispatcher = new MultiThreadedMessageDispatcher(threadPool, new MessageDispatcherImpl());
		LOGGER.info(listenerAddr);
		Address listenerAddress = GenericAddress.parse(listenerAddr);
		TransportMapping transport;
		if (listenerAddress instanceof UdpAddress) {
			transport = new DefaultUdpTransportMapping((UdpAddress) listenerAddress);
		} else {
			transport = new DefaultTcpTransportMapping((TcpAddress) listenerAddress);
		}
		snmp = new Snmp(dispatcher, transport);
		snmp.getMessageDispatcher().addMessageProcessingModel(new MPv1());
		snmp.getMessageDispatcher().addMessageProcessingModel(new MPv2c());
		snmp.getMessageDispatcher().addMessageProcessingModel(new MPv3());
		USM usm = new USM(SecurityProtocols.getInstance(), new OctetString(MPv3.createLocalEngineID()), 0);
		SecurityModels.getInstance().addSecurityModel(usm);
		snmp.listen();
		snmp.addCommandResponder(new CommandResponder() {

			@Override
			public void processPdu(CommandResponderEvent e) {
				PDU command = e.getPDU();
				int argus = command.getVariableBindings().size();
				StringBuilder builder = new StringBuilder();
				for (int i = 0; i < argus; i++) {
					VariableBinding vb = command.get(i);
					Variable value = vb.getVariable();
					String varString = "";
					if (value instanceof OctetString) {
						try {
							varString = new String(((OctetString) value).getValue(), env.getProperty("alarm.encoding"));
						} catch (UnsupportedEncodingException e1) {
							LOGGER.info("不支持的编码格式", e1);
						}
					} else {
						varString = value.toString();
					}
					builder.append("ARG[" + i + "]" + vb.getOid().toString() + "=" + varString + "\n");
				}
				if (!builder.toString().isEmpty()) {
					LOGGER.info("接收到告警:\n" + builder.toString());
					addAlarm(builder.toString());
				} else {
					LOGGER.info("接收到告警数据为空");
				}
			}
		});
		LOGGER.info("开始监听地址:" + listenerAddr);
	}

	@Override
	public void destroy() {
		try {
			if (snmp != null) {
				snmp.close();
			}
		} catch (IOException e) {
			LOGGER.info("SNMP关闭错误", e);
		}
	}

}
