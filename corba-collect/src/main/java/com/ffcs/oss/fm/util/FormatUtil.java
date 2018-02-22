/**
 * 文件名:FormatUtil.java
 * Copyright  2017，北京福富软件技术股份有限公司 
 * All Rights Reserved. 
 * 文件编号:
 * 创建人: LinQj
 * 日期: 2017年11月10日下午1:19:08
 * 修改人:
 * 日期:
 * 摘要:
 * 版本号: 
 * 原 作 者: LinQj
 * 完成日期:
 */
package com.ffcs.oss.fm.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FormatUtil {

	/**
	 * 获取标准格式时间
	 *
	 * @param time
	 *            String
	 * @return String
	 */
	public static String getStandardTime(String time) {
		
		String parsedTime = "";
		Matcher matcher = Pattern.compile("\\d+").matcher(time);
		while (matcher.find()) {
			String group = matcher.group();
			parsedTime = parsedTime + (group.length() == 1 ? "0" + group : group);
		}

		parsedTime += "000000000";
		if (parsedTime.length() < 17) {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
		}
		try {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
					.format(new SimpleDateFormat("yyyyMMddHHmmssSSS").parse(parsedTime.substring(0, 17)));
		} catch (ParseException e) {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
		}
	}
}
