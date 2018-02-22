package com.ffcs.oss.fm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AlarmCollectorApplication {

	@Autowired
	private AlarmParams params;

	//@Bean
	//public PropertiesFactoryBean getPropertiesFactoryBean() {
	//	PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
	//	propertiesFactoryBean.setFileEncoding("UTF-8");
	//	return propertiesFactoryBean;
	//}

	@Bean
	public CommandLineRunner init() {
		return args -> {
			System.out.println("---->" + params.getExcludeRegex());
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(AlarmCollectorApplication.class, args);
	}
}