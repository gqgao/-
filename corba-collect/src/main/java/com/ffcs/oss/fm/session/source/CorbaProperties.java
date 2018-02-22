package com.ffcs.oss.fm.session.source;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ${gujj} on 2017-09-05.
 */
@ConfigurationProperties(prefix = "corba")
@Data
@Component
public class CorbaProperties {

	private String ior;

	private String irpTypes;

	private Map<String, String> props = new HashMap<>();
}
