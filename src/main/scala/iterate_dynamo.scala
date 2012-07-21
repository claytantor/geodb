/*
 * Exploratory code to access DynamoDB
 * and start iterating through places
 */

import Stream._;
import collection.JavaConversions._

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
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

object DynamoConnection {
  val awsUserKey = "AKIAI7ES52LFCMFDHCCQ"
  val awsSecretKey = "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"
  val credentials = new BasicAWSCredentials(awsUserKey, awsSecretKey);
  val dynamoDB = new AmazonDynamoDBClient(credentials);

  def scanItems(table:String) = {
    val sr = new ScanRequest().withTableName(table)
    val result = dynamoDB.scan(sr)
    result.getItems()
  }

  def doScan(table:String, start:Option[Key], count:Int) = {
    val sr = new ScanRequest().withTableName(table)
    sr.setLimit(count)
    start match {
      case Some(key) => {
	sr.setExclusiveStartKey(key)
      }
      case None => ()
    }
    println(sr)
    dynamoDB.scan(sr)
  }

  def streamTable(table:String, start:Option[Key]) : Stream[java.util.Map[String,AttributeValue]] = {
      val result = doScan(table,start,1000)
      val lastKey = result.getLastEvaluatedKey()
      val items : java.util.List[java.util.Map[String,AttributeValue]] = result.getItems()
      (items toStream) append (if(lastKey == null) {
      	empty
      } else {
        streamTable(table,Some(lastKey))
     })
  }
}


