package com.welocally.geodb.services.spatial;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class) 
@ContextConfiguration(locations="classpath:geodb-applicationContext.xml")
public class GeoSpatialTooTest {
	static Logger logger = 
		Logger.getLogger(GeoSpatialTooTest.class);
	
	@Autowired SpatialDocumentFactory spatialDocumentFactory;
	
	@Autowired SpatialConversionUtils spatialConversionUtils;
	
	@Autowired SpatialSearchService spatialSearchService;
	
	
	/**
	 * {
    "geometry": {
        "type": "Point",
        "coordinates": [
            131.041473389,
            -12.523059845
        ]
    },
    "type": "Feature",
    "id": "SG_2uPWew1lZQQWByErSOUo95_-12.523060_131.041473@1303236331",
    "properties": {
        "province": "NT",
        "city": "Coolalinga",
        "name": "Litchfield Vet Hospital",
        "tags": [
            "surgeon"
        ],
        "country": "AU",
        "classifiers": [
            {
                "category": "Retail",
                "type": "Services",
                "subcategory": "Veterinary Services"
            }
        ],
        "phone": "+61 8 8983 2838",
        "href": "http://api.simplegeo.com/1.0/features/SG_2uPWew1lZQQWByErSOUo95_-12.523060_131.041473@1303236331.json",
        "address": "Shop 1-2 Coolalinga Shopping Village",
        "owner": "simplegeo",
        "postcode": "0835"
    }
}
	 */
	@Test
	public void testSpatialSearch() {
		logger.debug("testSpatialSearch");

		try {
			
			Directory dir = new RAMDirectory();
			IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_33,
					new StandardAnalyzer(Version.LUCENE_33));
			IndexWriter writer = new IndexWriter(dir, config);
			createExampleLocationsGeo(writer,"/data/placesAU.geojson");
			writer.commit();
			writer.close(true);
			
			//search
			//IndexSearcher searcher = new IndexSearcher(dir, true);
			double testDistance = getMiles(1.0D);
			Point p = new Point(-12.523060,131.041473);
			JSONArray locations = 
				spatialSearchService.find(p, testDistance, "Litchfield Vet Hospital", 0, 25,"http://localhost:8983/solr/select/");
			for (int i = 0; i < locations.length(); i++) {
				JSONObject placeFound = locations.getJSONObject(i);
				JSONObject properties = placeFound.getJSONObject("properties");
				logger.debug("location found: " + placeFound.toString());
			}
			
			Assert.assertEquals(1, locations.length());
					
			
			
		} catch (CorruptIndexException e) {
			logger.error("error",e);
			Assert.fail(e.getMessage());
		} catch (LockObtainFailedException e) {
			logger.error("error",e);
			Assert.fail(e.getMessage());
		} catch (IOException e) {
			logger.error("error",e);
			Assert.fail(e.getMessage());
		} catch (Exception e) {
			logger.error("error",e);
			Assert.fail(e.getMessage());
		}

	}
	

	
	public double getMiles( double km) {
        return km / spatialConversionUtils.MILE;
    }
 
    public double getKm( double miles) {
        return miles * spatialConversionUtils.MILE;
    }
    
	public void createExampleLocationsGeo(IndexWriter writer, String examplesFile)
	throws Exception {
		try {
			logger.debug(examplesFile);
		
			InputStream i4 = GeoSpatialTooTest.class.getResourceAsStream(examplesFile);		
			
			InputStreamReader reader = 
				new InputStreamReader(i4);
			
			BufferedReader br = new BufferedReader(reader); 
			String str = null; 
			while((str = br.readLine()) != null) { 
				
				JSONObject place = 
					new JSONObject(str);
						
				writer.addDocument(
						spatialDocumentFactory.makePlaceDocument(place));

				
			} 
			reader.close(); 
				
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
	}
	

	

}
