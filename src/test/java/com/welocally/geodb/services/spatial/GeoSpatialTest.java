package com.welocally.geodb.services.spatial;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.spatial.tier.DistanceQueryBuilder;
import org.apache.lucene.spatial.tier.projections.CartesianTierPlotter;
import org.apache.lucene.spatial.tier.projections.IProjector;
import org.apache.lucene.spatial.tier.projections.SinusoidalProjector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.NumericUtils;
import org.apache.lucene.util.Version;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class) 
@ContextConfiguration(locations="classpath:geodb-applicationContext.xml")
public class GeoSpatialTest {
	static Logger logger = 
		Logger.getLogger(GeoSpatialTest.class);
	
    public static String LAT_FIELD = "lat";
    public static String LON_FIELD = "lng";
	
    public static final double MILE = 1.609344;
    
	
	private double maxMiles = 250, minMiles = 1;
	private IProjector projector = new SinusoidalProjector();
	private CartesianTierPlotter ctp = new CartesianTierPlotter(0, projector, CartesianTierPlotter.DEFALT_FIELD_PREFIX);
	// startTier is 14 for 25 miles, 15 for 1 miles in lucene 3.0
	private int startTier = ctp.bestFit(maxMiles), endTier = ctp.bestFit(minMiles);
	
	
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
			IndexSearcher searcher = new IndexSearcher(dir, true);
			double testDistance = getMiles(1.0D);
			Point p = new Point(-12.523060,131.041473);
			List<String> locations = find(searcher, p, testDistance, "Litchfield Vet Hospital");
			for ( String location : locations) {
				logger.debug("location found: " + location);
			}
			
			Assert.assertEquals(1, locations.size());
					
			
			
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
        return km / MILE;
    }
 
    public double getKm( double miles) {
        return miles * MILE;
    }
    
	public void createExampleLocationsGeo(IndexWriter writer, String examplesFile)
	throws Exception {
		try {
			logger.debug(examplesFile);
		
			InputStream i4 = GeoSpatialTest.class.getResourceAsStream(examplesFile);		
			
			InputStreamReader reader = 
				new InputStreamReader(i4);
			
			BufferedReader br = new BufferedReader(reader); 
			String str = null; 
			while((str = br.readLine()) != null) { 
				
				JSONObject place = 
					new JSONObject(str);
				
				JSONObject properties = place.getJSONObject("properties");
				JSONObject geom = place.getJSONObject("geometry");
				JSONArray coords = geom.getJSONArray("coordinates");
				Point coord = 
					new Point(
							Double.parseDouble(coords.getString(1)), 
							Double.parseDouble(coords.getString(0)));
				addLoc(writer, place, coord);
				
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
	
	public void addLoc(IndexWriter writer, JSONObject place, Point coord) 
		throws CorruptIndexException, IOException, JSONException {
		Document doc = new Document();
		JSONObject properties = place.getJSONObject("properties");
		doc.add(new Field("name", properties.getString("name"), Field.Store.YES, Index.ANALYZED));
		doc.add(new Field("metafile", "doc", Store.YES, Index.ANALYZED));
		addSpatialLcnFields(coord, doc);
		writer.addDocument(doc);
	}
	
	private void addSpatialLcnFields(Point coord, Document document) {
		document.add(new Field(LAT_FIELD, NumericUtils
				.doubleToPrefixCoded(coord.getLat()), Field.Store.YES,
				Field.Index.NOT_ANALYZED));
		document.add(new Field(LON_FIELD, NumericUtils
				.doubleToPrefixCoded(coord.getLon()), Field.Store.YES,
				Field.Index.NOT_ANALYZED));
		addCartesianTiers(coord, document);
	}



	private void addCartesianTiers(Point coord, Document document) {
	   for (int tier = startTier; tier <= endTier; tier++) {
	      ctp = new CartesianTierPlotter(tier, projector, CartesianTierPlotter.DEFALT_FIELD_PREFIX);
	      double boxId = ctp.getTierBoxId(coord.getLat(), coord.getLon());
	      document.add(new Field(ctp.getTierFieldName(), NumericUtils
	         .doubleToPrefixCoded(boxId), Field.Store.YES,
	         Field.Index.NOT_ANALYZED_NO_NORMS));
	   }
	}
	
	private List<String> find(IndexSearcher searcher, Point start, double miles, String queryString) throws IOException, ParseException  {

		      List<String> result = new ArrayList<String>();		      	      
		      
		      final DistanceQueryBuilder dq = new DistanceQueryBuilder(
						start.getLat(), start.getLon(), 
						miles, 
						LAT_FIELD,
						LON_FIELD,
						CartesianTierPlotter.DEFALT_FIELD_PREFIX, 
						true, 
						startTier,
						
						endTier);

		      Query query = new TermQuery(new Term("metafile", "doc"));
		     		      
		      BooleanQuery bq = new BooleanQuery();

		      Query queryName = new QueryParser(Version.LUCENE_33, "name",
						new StandardAnalyzer(Version.LUCENE_33)).parse(queryString);
		      
		      bq.add(query, BooleanClause.Occur.MUST);
		      bq.add(queryName, BooleanClause.Occur.MUST);

		      TopDocs hits = searcher.search(dq.getQuery(bq), 10);
		      		      
		      logger.debug("total hits:"+ hits.totalHits);
		      for (int i = 0; i < hits.totalHits && i<hits.scoreDocs.length ; i++) {
		         Document doc = searcher.doc(hits.scoreDocs[i].doc);
		         result.add(doc.get("name"));
		      }
		      return result;
	}
}
