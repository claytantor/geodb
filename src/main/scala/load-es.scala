package com.welocally.geodb.services.utils
import scala.io.Source
import scala.util.parsing.json.JSON._

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.json.JSONObject
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.common.xcontent.XContentFactory
import org.elasticsearch.common.settings.ImmutableSettings
import com.welocally.geodb.services.util.WelocallyJSONUtils

import java.util.Date
object ESLoader {
  @Component
  class Wiring
  {
    @Autowired var welocallyJSONUtils : WelocallyJSONUtils = null
  }
  var wiredBean:Wiring = _

  def readFile(filename :String) = {
    println("Reading file " + filename)
    Source.fromFile(filename).getLines
  }

  def parseFile(filename :String) = 
    for(l <- readFile(filename)) yield new JSONObject(l)

  def makePlaceIndex(place :JSONObject) : JSONObject = {
    wiredBean.welocallyJSONUtils.updatePlaceToWelocallyFormat(place)
    wiredBean.welocallyJSONUtils.makeIndexablePlace(place)
  }

  def loadBatch(client :TransportClient, places:Iterable[JSONObject]) = {
      val bulkRequest = client.prepareBulk();
      for (place <- places) {
        val placeIndex = makePlaceIndex(place)
      
        val id:String = placeIndex.get("if").asInstanceOf[String]
        bulkRequest.add(client.prepareIndex("geodb","place",id)
           .setSource(XContentFactory.jsonBuilder()
	  	.startObject()
		.field("search", placeIndex.get("search"))
		.startArray("location")
		.value(placeIndex.get("location_1_coordinate").asInstanceOf[Double])
		.value(placeIndex.get("location_0_coordinate").asInstanceOf[Double])
		.endArray()
		.endObject()))
      }
    val bulkResponse = bulkRequest.execute().actionGet();
//    println("bulkResponse = " + bulkResponse)
    if(bulkResponse.hasFailures()) {
        println(bulkResponse.buildFailureMessage())
        throw new Exception("ES Bulk Response failures!!");
    }

  }

  def main(args: Array[String])  {
    val ctx = new ClassPathXmlApplicationContext("geodb-applicationContext.xml", "signpost4j-context.xml");
    wiredBean = ctx.getBean(classOf[Wiring]);
    println("welocallyJSONUtils = " + wiredBean.welocallyJSONUtils)
    val settings = ImmutableSettings.settingsBuilder()
             .put("cluster.name", "es-welocally").build();
    val client = new TransportClient(settings)
        .addTransportAddress(new InetSocketTransportAddress("ec2-107-22-80-168.compute-1.amazonaws.com", 9300))
        .addTransportAddress(new InetSocketTransportAddress("ec2-184-72-135-198.compute-1.amazonaws.com", 9300))
        .addTransportAddress(new InetSocketTransportAddress("ec2-23-22-45-184.compute-1.amazonaws.com", 9300))
        .addTransportAddress(new InetSocketTransportAddress("ec2-23-22-31-108.compute-1.amazonaws.com", 9300))

    val parsedFile = parseFile(args(0))
    var totalLoaded = 0
    for(batch <- parsedFile.grouped(10000))
    { 
      loadBatch(client, batch)
      totalLoaded += batch.size
      if(totalLoaded % 10000 == 0) {
        println( new Date() + " total loaded: " + totalLoaded)
      }	
    }

    client.close()
    System.exit(0)
  }
}

object HelloWorld {
  def main(args: Array[String])  {
      println("Hi from Scala!");
  }
}