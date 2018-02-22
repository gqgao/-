/**
 * 文件名:SnmpTest.java
 * Copyright  2017，北京福富软件技术股份有限公司 
 * All Rights Reserved. 
 * 文件编号:
 * 创建人: LinQj
 * 日期: 2017年11月8日下午7:39:07
 * 修改人:
 * 日期:
 * 摘要:
 * 版本号: 
 * 原 作 者: LinQj
 * 完成日期:
 */
package com.ffcs.oss.cap.alarm;

import java.io.IOException;
import java.util.Vector;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class SnmpTest {
	private Snmp snmp = null;

	private Address targetAddress = null;

	public void initComm() throws IOException {

		// 设置Agent方的IP和端口
		targetAddress = GenericAddress.parse("udp:localhost/5100");
		TransportMapping transport = new DefaultUdpTransportMapping();
		snmp = new Snmp(transport);
		transport.listen();

	}

	public void sendPDU() throws IOException {

		// 设置 target
		CommunityTarget target = new CommunityTarget();
		target.setCommunity(new OctetString("public"));
		target.setAddress(targetAddress);

		// 通信不成功时的重试次数
		target.setRetries(2);

		// 超时时间
		target.setTimeout(1500);
		target.setVersion(SnmpConstants.version2c);

		// 创建 PDU
		PDU pdu = new PDU();
		pdu.add(new VariableBinding(new OID(".1.3.6.1.4.1.2011.2.15.2.4.3.3.1.0"), new OctetString("112345")));
		pdu.add(new VariableBinding(new OID(".1.3.6.1.4.1.2011.2.15.2.4.3.3.3.0"), new OctetString("1")));
		pdu.add(new VariableBinding(new OID(".1.3.6.1.4.1.2011.2.15.2.4.3.3.4.0"),
				new OctetString("2017-11-10 01:54:03")));
		pdu.add(new VariableBinding(new OID(".1.3.6.1.4.1.2011.2.15.2.4.3.3.6.0"), new OctetString("81")));
		pdu.add(new VariableBinding(new OID(".1.3.6.1.4.1.2011.2.15.2.4.3.3.9.0"), new OctetString("26504")));
		pdu.add(new VariableBinding(new OID(".1.3.6.1.4.1.2011.2.15.2.4.3.3.10.0"), new OctetString("8")));
		pdu.add(new VariableBinding(new OID(".1.3.6.1.4.1.2011.2.15.2.4.3.3.11.0"), new OctetString("2")));
		pdu.add(new VariableBinding(new OID(".1.3.6.1.4.1.2011.2.15.2.4.3.3.15.0"), new OctetString("aaa")));
		pdu.add(new VariableBinding(new OID(".1.3.6.1.4.1.2011.2.15.2.4.3.3.27.0"),
				new OctetString("柜号=0, 框号=93, 端口号=端口0, 单板类型=MRRU")));
		pdu.add(new VariableBinding(new OID(".1.3.6.1.4.1.2011.2.15.2.4.3.3.28.0"), new OctetString("告警异常")));
		pdu.add(new VariableBinding(new OID(".1.3.6.1.4.1.2011.2.15.2.4.3.3.29.0"), new OctetString("bbb")));
		pdu.add(new VariableBinding(new OID(".1.3.6.1.4.1.2011.2.15.2.4.3.3.30.0"), new OctetString("bbb")));
		pdu.add(new VariableBinding(new OID(".1.3.6.1.4.1.2011.2.15.2.4.3.3.31.0"), new OctetString("ccc")));
		pdu.add(new VariableBinding(new OID(".1.3.6.1.4.1.2011.2.15.2.4.3.3.49.0"), new OctetString("ddd")));
		pdu.add(new VariableBinding(new OID(".1.3.6.1.4.1.2011.2.15.2.4.3.3.51.0"),
				new OctetString("射频单元名称=花园-二期, enNodeBId=9077877")));
		pdu.setType(PDU.TRAP);
		// 向Agent发送PDU，并接收Response
		snmp.send(pdu, target);

	}

	public static void main(String[] args) {

		try {

			SnmpTest util = new SnmpTest();
			util.initComm();
			util.sendPDU();

		} catch (IOException e) {

			e.printStackTrace();

		}

	}
}
