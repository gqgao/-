/**
 * 文件名:MyTest.java
 * Copyright 2017，北京福富软件技术股份有限公司
 * All Rights Reserved.
 * 文件编号:
 * 创建人: LinQj
 * 日期: 2017年11月9日下午5:26:10
 * 修改人:
 * 日期:
 * 摘要:
 * 版本号:
 * 原 作 者: LinQj
 * 完成日期:
 */
package com.ffcs.oss.cap.alarm;

public class MyTest {

	public static void main(String[] args) {
		String s = "#S#jjjjjjj#E#";
		System.out.println(s.indexOf("#S#"));
		System.out.println(s.indexOf("#E#"));
		System.out.println(s.substring(s.indexOf("#S#") + "#S#".length(), s.indexOf("#E#")));
	}
}
