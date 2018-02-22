package com.ffcs.oss.fm;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ${gujj} on 2017-08-30.
 */
@ConfigurationProperties(prefix = "alarm")
@Data
@Component
public class AlarmParams {

	private String columns;

	private String privateColumns;

	public Map<String, String> addFixedField = new HashMap<>();

	private String includeRegex;

	private String excludeRegex;
}
