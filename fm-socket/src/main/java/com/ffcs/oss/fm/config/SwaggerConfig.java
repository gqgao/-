package com.ffcs.oss.fm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import static springfox.documentation.builders.PathSelectors.regex;

/**
 * Created by lenovo on 2017/5/20.
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket restApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .genericModelSubstitutes(ResponseEntity.class)
                .useDefaultResponseMessages(false)
                .pathMapping("/")// base，最终调用接口后会和paths拼接在一起
                .select()
                .paths(regex("/.*"))//过滤的接口
                .apis(RequestHandlerSelectors.basePackage("com.ffcs.oss.cap.alarm.controller"))
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("福富综合告警系统告警接口使用文档")//大标题
                .description("用于告警通知上报到综合告警系统")//详细描述
                .version("1.0")//版本
                .termsOfServiceUrl("http://www.ffcs.cn/")
                .build();
    }
}