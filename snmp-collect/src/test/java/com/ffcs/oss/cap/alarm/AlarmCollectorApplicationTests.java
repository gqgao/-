package com.ffcs.oss.cap.alarm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ffcs.oss.fm.AlarmParams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/*@RunWith(SpringRunner.class)
@SpringBootTest*/
public class AlarmCollectorApplicationTests {
	/*private static final Logger LOGGER = LoggerFactory.getLogger(AlarmCollectorApplicationTests.class);

	@Autowired(required=false)
	private AlarmParams params;

	@Test
	public void contextLoads() {
		Matcher matcher = Pattern.compile("eNodeBId=(\\d+)").matcher(
				"1.3.6.1.4.1.2011.2.15.2.4.3.3.51.0=基站制式=L, 影响制式=L, 部署标识=NULL, 射频单元名称=BZ-涡阳-涡阳王集-HFTA-439583-52@F-BZ-16-0296-O-RW-TT, 累计时长(s)=90, eNodeBId=439583");
		if (matcher.find()) {
			System.out.println(matcher.group(1));
		}
	}

	@Test
	public void test (){
		LOGGER.info("332432432");
		System.out.println("param---->" + params);
		//LOGGER.info("daf------>" + params.getExcludeRegex());
	}
*/
}
