package com.welocally.geodb.services.db;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.welocally.geodb.services.spatial.Point;


@RunWith(SpringJUnit4ClassRunner.class) 
@ContextConfiguration(locations="classpath:geodb-applicationContext.xml")
public class IdGenTest {
	
	static Logger logger = 
		Logger.getLogger(IdGenTest.class);
	
	@Autowired IdGen idGen;
	
	@Test
	public void testIdGenBasic() {
		logger.debug("testIdGenBasic");
		String id = idGen.genBasic("2uPWew1lZQQWByErSOUo95".length());
		logger.debug(id);
		Assert.assertEquals("2uPWew1lZQQWByErSOUo95".length(), id.length());
	}
	
	@Test
	public void testIdGenPlace() {
		logger.debug("testIdGenPlace");
		Point p = new Point(-12.4962596893, 131.045791626);
		String id = idGen.genPoint("WL_",p);
		logger.debug(id);
		//WL_2uPWew1lZQQWByErSOUo95_-12.523060_131.041473@1303236331
		Assert.assertEquals("WL_2uPWew1lZQQWByErSOUo95_-12.523060_131.041473@1303236331".length(), id.length());
	}

}
