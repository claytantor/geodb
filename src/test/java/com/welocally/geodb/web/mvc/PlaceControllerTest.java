package com.welocally.geodb.web.mvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.welocally.geodb.services.db.DbException;
import com.welocally.geodb.services.db.IdGen;
import com.welocally.geodb.services.db.JsonDatabase;
import com.welocally.geodb.services.spatial.SpatialConversionUtils;

@RunWith(SpringJUnit4ClassRunner.class) 
@ContextConfiguration(locations="classpath:geodb-applicationContext.xml")
public class PlaceControllerTest {
	
	static Logger logger = 
		Logger.getLogger(PlaceControllerTest.class);
	
	private JsonDatabase mockedDB;
	
	@Autowired IdGen idGen; 
	
	@Autowired SpatialConversionUtils spatialConversionUtils; 
	
	private PlaceControllerV1 placeControllerV1;
	
	private String requestJSON = "{\"geometry\": {\"type\": \"Point\", \"coordinates\": [131.041473389, -12.523059845]}, \"type\": \"Feature\", \"id\": \"SG_2uPWew1lZQQWByErSOUo95_-12.523060_131.041473@1303236331\", \"properties\": {\"province\": \"NT\", \"city\": \"Coolalinga\", \"name\": \"Litchfield Vet Hospital\", \"tags\": [\"surgeon\"], \"country\": \"AU\", \"classifiers\": [{\"category\": \"Retail\", \"type\": \"Services\", \"subcategory\": \"Veterinary Services\"}], \"phone\": \"+61 8 8983 2838\", \"href\": \"http://api.simplegeo.com/1.0/features/SG_2uPWew1lZQQWByErSOUo95_-12.523060_131.041473@1303236331.json\", \"address\": \"Shop 1-2 Coolalinga Shopping Village\", \"owner\": \"simplegeo\", \"postcode\": \"0835\"}}";
		
	@PostConstruct
	public void stub(){
				
		placeControllerV1 = new PlaceControllerV1();
		placeControllerV1.setSpatialConversionUtils(spatialConversionUtils);
		placeControllerV1.setIdGen(idGen);
				
		mockedDB = mock(JsonDatabase.class);
		
		try {
			when(mockedDB.findById("places", "WL_2uPWew1lZQQWByErSOUo95_-12.523060_131.041473@1303236331")).thenReturn(new JSONObject(requestJSON));
		} catch (DbException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		placeControllerV1.setJsonDatabase(mockedDB);
		

	}
	
//	@Test
//	public void testPutPlace() {
//		logger.debug("testPutPlace");
//		
//		placeControllerV1.saveJ(
//				requestJSON,null);		
//
//	}
	

}
