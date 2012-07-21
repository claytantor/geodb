package com.welocally.geodb.services.db;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodb.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.ComparisonOperator;
import com.amazonaws.services.dynamodb.model.Condition;
import com.amazonaws.services.dynamodb.model.QueryRequest;
import com.amazonaws.services.dynamodb.model.QueryResult;
import com.amazonaws.services.dynamodb.model.ScanRequest;
import com.amazonaws.services.dynamodb.model.ScanResult;
import com.welocally.geodb.services.util.JsonObjectSerializer;



@RunWith(SpringJUnit4ClassRunner.class) 
@ContextConfiguration(locations="classpath:geodb-applicationContext.xml")
public class DynamoDbTest {
	
	static Logger logger = 
		Logger.getLogger(DynamoDbTest.class);
	
	@Autowired IdGen idGen;
	
	@Autowired 
	@Qualifier("dynamoJsonDatabase")
	JsonDatabase jsonDatabase;
	
	@Autowired JsonObjectSerializer jsonObjectSerializer;
	
	@Value("${placesDatabase.collectionName:places.published}")
	String placesCollection;
	
	@Autowired
	private DynamoJsonObjectFactory dynamoJsonObjectFactory;
	
	private AmazonDynamoDBClient dynamoDB;
	
	@Value("${AWSCredentials.userKey:foo}")
	private String awsUserKey;
	
	@Value("${AWSCredentials.secretKey:bar}")
	private String awsSecretKey;
	
	
	@PostConstruct
	private void initDynamo() {
		 AWSCredentials credentials = new BasicAWSCredentials(awsUserKey,awsSecretKey);
	     dynamoDB = new AmazonDynamoDBClient(credentials);
	}
	
	
	@Test
	public void testFindPlaceByPrimaryKey() {
		//WL_3eQVMxQckdATyUVvLjulxo_64.881077_-147.798379@1293731153
		try {
	        JSONObject place = 
	        	jsonDatabase.findById(placesCollection, "WL_3eQVMxQckdATyUVvLjulxo_64.881077_-147.798379@1293731153");
	        Assert.assertEquals("WL_3eQVMxQckdATyUVvLjulxo_64.881077_-147.798379@1293731153", place.getString("_id"));
	        
		} catch (DbException e) {
	        Assert.fail("problem finding object");
        } catch (JSONException e) {
        	Assert.fail("problem parsing object");
        } 
	}
	
	@Test
	public void testFindAll() {

       
        ScanRequest scanRequest = new ScanRequest("classifiers");
        ScanResult scanResult = dynamoDB.scan(scanRequest);        
        logger.debug("Result: " + scanResult);
        Assert.assertEquals(true, scanResult.getItems().size()>0);	
		
	}
	
	@Test
	public void testScanClassifierTypeByName() {

        HashMap<String, Condition> scanFilter = new HashMap<String, Condition>();
        Condition condition = new Condition()
            .withComparisonOperator(ComparisonOperator.EQ.toString())
            .withAttributeValueList(new AttributeValue("Manufacturing & Wholesale Goods"));
        scanFilter.put("type", condition);
        ScanRequest scanRequest = new ScanRequest("classifiers").withScanFilter(scanFilter);
        ScanResult scanResult = dynamoDB.scan(scanRequest);
        logger.debug("Result: " + scanResult);
        Assert.assertEquals(true, scanResult.getItems().size()>0);	
		
	}
	
	@Test
	public void testScanClassifierType() {

		try {
			HashMap<String, Condition> scanFilter = new HashMap<String, Condition>();
	        Condition condition = new Condition()
	            .withComparisonOperator(ComparisonOperator.NOT_NULL.toString());
	        scanFilter.put("type", condition);
	        ScanRequest scanRequest = new ScanRequest("classifiers")
	        	.withScanFilter(scanFilter)
	        	.withAttributesToGet(new String[] {"type"});
	        ScanResult scanResult = dynamoDB.scan(scanRequest);
	        Set<String> types = new HashSet<String>();
	        	        
	        for (Map<String, AttributeValue> items : scanResult.getItems()) {
	            types.add(items.get("type").getS().toString());
            }
	        
	        logger.debug("Result: " + jsonObjectSerializer.serialize(types).toString());
	        	        
	        Assert.assertEquals(true, scanResult.getItems().size()>0);	
	        
	        
        } catch (AmazonServiceException e) {
	        logger.error("problem", e);
	        Assert.fail("problem with db");
        } catch (AmazonClientException e) {
        	logger.error("problem", e);
        	Assert.fail("problem with client");
        } catch (IOException e) {
        	logger.error("problem", e);
        	Assert.fail("problem with json");
        }
		
	}
	
	@Test
	public void testScanClassifierCatgeories() {

		try {
			HashMap<String, Condition> scanFilter = new HashMap<String, Condition>();
	        
			Condition condition = new Condition()
	            .withComparisonOperator(ComparisonOperator.EQ.toString())
	            .withAttributeValueList(new AttributeValue[] { new AttributeValue("Manufacturing & Wholesale Goods")});
	        scanFilter.put("type", condition);
	        	        
	        ScanRequest scanRequest = new ScanRequest("classifiers")
	        	.withScanFilter(scanFilter)
	        	.withAttributesToGet(new String[] {"category"});
	        ScanResult scanResult = dynamoDB.scan(scanRequest);
	        Set<String> catgeories = new HashSet<String>();
	        	        
	        for (Map<String, AttributeValue> items : scanResult.getItems()) {
	        	catgeories.add(items.get("category").getS().toString());
            }
	        
	        logger.debug("Result: " + jsonObjectSerializer.serialize(catgeories).toString());
	        	        
	        Assert.assertEquals(true, scanResult.getItems().size()>0);	
	        
	        
        } catch (AmazonServiceException e) {
	        logger.error("problem", e);
	        Assert.fail("problem with db");
        } catch (AmazonClientException e) {
        	logger.error("problem", e);
        	Assert.fail("problem with client");
        } catch (IOException e) {
        	logger.error("problem", e);
        	Assert.fail("problem with json");
        } catch (Exception e) {
        	logger.error("problem", e);
        	Assert.fail("problem with json");
        }
		
	}
	
	@Test
	public void testQueryClassifierTypeByName() {

		try {
	        Condition condition = new Condition()
	            .withComparisonOperator(ComparisonOperator.EQ.toString())
	            .withAttributeValueList(new AttributeValue("Manufacturing & Wholesale Goods"));
	        QueryRequest qRequest = new QueryRequest()
	        	.withHashKeyValue(new AttributeValue("type"))
	        	.withTableName("classifiers")
	        	.withRangeKeyCondition(condition);
	        QueryResult qResult = dynamoDB.query(qRequest);	        
	        logger.debug("Result: " + qResult);

		} catch (AmazonServiceException e) {
	        logger.error("problem", e);
	        Assert.fail("problem with db");
        } catch (AmazonClientException e) {
        	logger.error("problem", e);
        	Assert.fail("problem with client");
        } 
		
	}
	
	 
	
	

}
