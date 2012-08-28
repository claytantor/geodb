package com.welocally.geodb.services.db;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudwatch.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodb.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.ComparisonOperator;
import com.amazonaws.services.dynamodb.model.Condition;
import com.amazonaws.services.dynamodb.model.DeleteItemRequest;
import com.amazonaws.services.dynamodb.model.DeleteItemResult;
import com.amazonaws.services.dynamodb.model.GetItemRequest;
import com.amazonaws.services.dynamodb.model.GetItemResult;
import com.amazonaws.services.dynamodb.model.Key;
import com.amazonaws.services.dynamodb.model.PutItemRequest;
import com.amazonaws.services.dynamodb.model.PutItemResult;
import com.amazonaws.services.dynamodb.model.ScanRequest;
import com.amazonaws.services.dynamodb.model.ScanResult;
import com.welocally.geodb.services.db.DbException.Type;

@Component("dynamoJsonDatabase")
public class DynamoJsonDatabase implements JsonDatabase {

	static Logger logger = Logger.getLogger(DynamoJsonDatabase.class);

	private AmazonDynamoDBClient dynamoDB;

	@Autowired
	private DynamoJsonObjectFactory dynamoJsonObjectFactory;

	@Value("${AWSCredentials.userKey:foo}")
	private String awsUserKey;

	@Value("${AWSCredentials.secretKey:bar}")
	private String awsSecretKey;
	
	private Map<String, String[]> classifiers;
	
	
	
	public void evict(){
		classifiers.clear();
	}
	
	private AmazonDynamoDBClient getDB(){
	    if(dynamoDB == null){
	        AWSCredentials credentials = 
	             new BasicAWSCredentials(awsUserKey, awsSecretKey);
	  
	        dynamoDB = new AmazonDynamoDBClient(credentials);
	        
	        
	    }
	    return dynamoDB;
	}
	
	public synchronized void load() {
	    
	    logger.debug("foo");
	        
		if(classifiers == null || classifiers.isEmpty()){
		    classifiers = new HashMap<String, String[]>();
    		ScanRequest scanRequest = new ScanRequest("classifiers");
    	    ScanResult scanResult = getDB().scan(scanRequest); 
    	     
    	    for (Map<String, AttributeValue> items : scanResult.getItems()) {
    	    	 String[] row = new String[] {
    	    			 items.get("type").getS().toString(),
    	    			 items.get("category").getS().toString(),
    	    			 items.get("subcategory").getS().toString(),
    	    			 
    	    	 };
    	    	 classifiers.put(items.get("id").getS().toString(), row);	      
            }
		}
	}
	

	public DbPage findAll(String collectionName, int pageNum)
	        throws DbException {
		// TODO Auto-generated method stub
		throw new RuntimeException("NO IMPL");
	}

	public DbPage findByExample(String collectionName, int pageNumber,
	        JSONObject example) throws DbException {
		// TODO Auto-generated method stub
		throw new RuntimeException("NO IMPL");
	}

	public DbPage findByExampleIncluding(String collectionName, int pageNumber,
	        JSONObject example, JSONObject inclusions) throws DbException {
	    
	    //ignore inclusions
	    
	    //Scan items for movies with a year attribute greater than 1985
        HashMap<String, Condition> scanFilter = new HashMap<String, Condition>();
        
        //example is equals
        String[] names = JSONObject.getNames(example);
        for (int i = 0; i < names.length; i++) {
            Condition condition = new Condition()
            .withComparisonOperator(ComparisonOperator.GT.toString())
            .withAttributeValueList(new AttributeValue().withN("1985"));
            
            scanFilter.put("year", condition);
            
        }

        ScanRequest scanRequest = new ScanRequest(collectionName).withScanFilter(scanFilter);
        ScanResult scanResult = dynamoDB.scan(scanRequest);
        logger.debug("Result: " + scanResult);

	    
		// TODO Auto-generated method stub
		throw new RuntimeException("NO IMPL");
	}

	public JSONObject findById(String collectionName, String id)
	        throws DbException {

		try {
			
			Key primaryKey = new Key()
			        .withHashKeyElement(new AttributeValue(id));
			GetItemRequest request = new GetItemRequest().withTableName(
			        collectionName).withKey(primaryKey);

			GetItemResult result = getDB().getItem(request);

			if (result.getItem() == null || result.getItem().isEmpty())
				throw new DbException(Type.OBJECT_NOT_FOUND,"collection: "+collectionName+" id:"+id);
			else {
				return new JSONObject(result.getItem().get("document").getS());
			}

		} catch (AmazonServiceException e) {
			throw new DbException(Type.DB_ERROR);
		} catch (AmazonClientException e) {
			throw new DbException(Type.DB_ERROR);
		} catch (JSONException e) {
			throw new DbException(Type.DB_ERROR);
		}

	}
	
	

    public void delete(String collectionName, String id) throws DbException {
	    try {
            Key primaryKey = new Key()
            .withHashKeyElement(new AttributeValue(id));
            DeleteItemRequest deleteItemRequest = new DeleteItemRequest(collectionName, primaryKey);
            DeleteItemResult result = getDB().deleteItem(deleteItemRequest);
        } catch (AmazonServiceException e) {
            throw new DbException(Type.DB_ERROR);
        } catch (AmazonClientException e) {
            throw new DbException(Type.DB_ERROR);
        }

    }
	

    public void deleteAll(String collectionName) throws DbException {
        
        
        // TODO Auto-generated method stub
        throw new RuntimeException("NO IMPL");
    }
    
    

	public JSONArray findDistinct(String collectionName, String key,
	        JSONObject query) throws DbException {
		// TODO Auto-generated method stub
		throw new RuntimeException("NO IMPL");
	}

	public JSONArray findTypes() throws DbException {
	    
	    logger.debug("find types");
		load();
		JSONArray typesArray = new JSONArray();
		Set<String> types = new HashSet<String>();
		for (String[] row : classifiers.values()) 
	        types.add(row[0]);
		for (String item : types) {
			typesArray.put(item);
        }
		
		return typesArray;
	}
	
	public JSONArray findCatgories(String type) throws DbException {
        load();
        JSONArray oArray = new JSONArray();
        Set<String> result = new HashSet<String>();
        for (String[] row : classifiers.values()) {
            if(row[0].equals(type))
                result.add(row[1]);
        }
        for (String item : result) {
            oArray.put(item);
        }
        return oArray;
    }
    
	public JSONArray findUserPlaces(String publisherKey, String collectionName, String status) throws DbException {
        JSONArray oArray = new JSONArray();
        
        ScanRequest scanRequest = new ScanRequest(collectionName);
        Map<String,Condition> scanFilter = new HashMap<String,Condition>();
        Condition conditionOwner = new Condition()
        .withComparisonOperator(ComparisonOperator.EQ)
        .withAttributeValueList(new AttributeValue().withS(publisherKey));
        
        Condition conditionStatus = new Condition()
        .withComparisonOperator(ComparisonOperator.EQ)
        .withAttributeValueList(new AttributeValue().withS(status));        
    
        scanFilter.put("owner", conditionOwner); 
        scanFilter.put("status", conditionStatus); 
        scanRequest.setScanFilter(scanFilter);
        
        ScanResult scanResult;
        try {
            scanResult = getDB().scan(scanRequest);
        } catch (Exception e) {
            if(e.getMessage().equals("Requested resource not found"))
                return oArray;
            else 
                throw new RuntimeException(e);
        } 
         
        for (Map<String, AttributeValue> item : scanResult.getItems()) { 
                JSONObject place;
                try {
                    place = new JSONObject(item.get("document").getS());
                    oArray.put(place);    
                } catch (Exception e) {
                    logger.error("problem getting place for owner:"+publisherKey);
                }
                
        }

        return oArray;
    }
    
    
	
	public JSONArray findSubcatgories(String type, String category) throws DbException {
		load();
		JSONArray oArray = new JSONArray();
		Set<String> result = new HashSet<String>();
		for (String[] row : classifiers.values()) {
			if(row[0].equals(type) && row[1].equals(category))
				result.add(row[2]);
		}
		for (String item : result) {
			oArray.put(item);
        }

		return oArray;
	}


	public void put(JSONObject doc, JSONObject schema, String collectionName, String id, EntityType type, StatusType status)
	        throws DbException {

		try {

			switch(type){
			case PLACE:{
			    putItem(dynamoJsonObjectFactory.makeRecord(doc, schema, id, status.toString().toLowerCase()),
                        collectionName);
			    break;
			}
			case USER_PLACE:{
			    JSONObject place = doc.getJSONObject("userPlace");
			    JSONObject data = doc.getJSONObject("userData");
               			    
                putItem(dynamoJsonObjectFactory.makeUserPlace(place, data, status.toString().toLowerCase()),
                        collectionName);
                break;
            }
            case CLASSIFER: {
                putItem(dynamoJsonObjectFactory.makeClassifier(doc),
                        collectionName);
                break;
            }
            case DEAL: {
                putItem(dynamoJsonObjectFactory.makeDeal(doc, status.toString().toLowerCase()),
                        collectionName);
                break;
            }
            case PUBLISHER: {
                putItem(dynamoJsonObjectFactory.makePublisher(doc, status.toString().toLowerCase()),
                        collectionName);
                break;
            }            
			}
            
			
			
			
		} catch (JSONException e) {
			logger.error("cant put object to dynamo store", e);
			throw new DbException(DbException.Type.DB_ERROR);
		}

	}

	private void putItem(Map<String, AttributeValue> item, String collectionName) {
		PutItemRequest putItemRequest = new PutItemRequest(collectionName, item);
		PutItemResult putItemResult = getDB().putItem(putItemRequest);
		logger.debug(putItemResult.getConsumedCapacityUnits()
		        + "units used, result:" + putItemResult.toString());
	}


}
