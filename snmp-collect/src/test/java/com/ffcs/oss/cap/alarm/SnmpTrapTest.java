/**
 * 文件名:SnmpTrapTest.java
 * Copyright  2017，北京福富软件技术股份有限公司 
 * All Rights Reserved. 
 * 文件编号:
 * 创建人: LinQj
 * 日期: 2017年11月8日下午8:06:23
 * 修改人:
 * 日期:
 * 摘要:
 * 版本号: 
 * 原 作 者: LinQj
 * 完成日期:
 */
package com.ffcs.oss.cap.alarm;

import com.ffcs.oss.fm.session.source.SnmpAlarmSourceSession;

public class SnmpTrapTest {

	public static void main(String[] args) {
		SnmpAlarmSourceSession session = new SnmpAlarmSourceSession();
		try {
			session.ready();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
