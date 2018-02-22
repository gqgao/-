/**
 * 文件名:BeanConfig.java
 * Copyright  2017，北京福富软件技术股份有限公司 
 * All Rights Reserved. 
 * 文件编号:
 * 创建人: LinQj
 * 日期: 2017年11月9日下午5:42:00
 * 修改人:
 * 日期:
 * 摘要:
 * 版本号: 
 * 原 作 者: LinQj
 * 完成日期:
 */
package com.ffcs.oss.fm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ffcs.oss.fm.session.source.SocketConn;

@Configuration
public class BeanConfig {
	
	@Bean(name="conn")
	@ConfigurationProperties(prefix = "socket")
	public SocketConn getDbConn() {
		return new SocketConn();	
	}
}
