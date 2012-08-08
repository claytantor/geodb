package com.welocally.geodb.services.spatial;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.spatial.tier.DistanceFieldComparatorSource;
import org.apache.lucene.spatial.tier.DistanceQueryBuilder;
import org.apache.lucene.spatial.tier.projections.CartesianTierPlotter;
import org.apache.lucene.util.Version;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.welocally.geodb.services.index.DirectoryException;
import com.welocally.geodb.services.index.PlaceDirectory;

@Component
@Deprecated
public class LuceneSpatialSearchService implements SpatialSearchService {

	static Logger logger = Logger.getLogger(LuceneSpatialSearchService.class);
	


	@Autowired PlaceDirectory placeDirectory;
	
	@Autowired SpatialConversionUtils spatialConversionUtils;
	
	public IndexSearcher getPlaceSearcher() throws SpatialSearchException{
		//searcher
		try {
			return new IndexSearcher(placeDirectory.getDirectory(), true);
		} catch (CorruptIndexException e) {
			logger.error("problem with search",e);
			throw new SpatialSearchException(e);
		} catch (IOException e) {
			logger.error("problem with search",e);
			throw new SpatialSearchException(e);
		} catch (DirectoryException e) {
			logger.error("problem with search",e);
			throw new SpatialSearchException(e);
		}
	}
	


    public JSONArray find(Point point, double km, String queryString, int start, int rows, String endpoint)
            throws SpatialSearchException {
	    //return find(getPlaceSearcher(), point, km, queryString);
	    throw new RuntimeException("NO IMPL");
    }


	private JSONArray find(IndexSearcher searcher, Point start, double km, String queryString) 
		throws SpatialSearchException  {

		  JSONArray result = new JSONArray();		      	      
	      
	      try {
			final DistanceQueryBuilder dq = new DistanceQueryBuilder(
						start.getLat(), start.getLon(), 
						getMiles(km), 
						spatialConversionUtils.LAT_FIELD,
						spatialConversionUtils.LON_FIELD,
						CartesianTierPlotter.DEFALT_FIELD_PREFIX, 
						true, 
						spatialConversionUtils.getStartTier(),					
						spatialConversionUtils.getEndTier());
			
			// Create a distance sort
	        // As the radius filter has performed the distance calculations
	        // already, pass in the filter to reuse the results.
	        final DistanceFieldComparatorSource dsort = new DistanceFieldComparatorSource(dq.getDistanceFilter());
	        final Sort sort = new Sort(new SortField("geo_distance", dsort));

	        Query query = new TermQuery(new Term("metafile", "doc"));

			BooleanQuery bq = new BooleanQuery();

			Query queryName = new QueryParser(Version.LUCENE_33, "search",
					new StandardAnalyzer(Version.LUCENE_33)).parse(queryString);

			bq.add(query, BooleanClause.Occur.MUST);
			bq.add(queryName, BooleanClause.Occur.MUST);

			TopDocs hits = searcher.search(dq.getQuery(bq), 10, sort);
			Map distances = dq.getDistanceFilter().getDistances();

			logger.debug("total hits:" + hits.totalHits);
			for (int i = 0; i < hits.totalHits && i < hits.scoreDocs.length; i++) {
				int docID = hits.scoreDocs[i].doc;
				Document doc = searcher.doc(docID);
				JSONObject placeResult = new JSONObject(doc.get("place"));
				placeResult.put("geo_distance", getKm(Double.parseDouble(distances.get(docID).toString())));
				result.put(placeResult);
			}
			
		} catch (CorruptIndexException e) {
			logger.error("problem with search",e);
			throw new SpatialSearchException(e);
		} catch (ParseException e) {
			logger.error("problem with search",e);
			throw new SpatialSearchException(e);
		} catch (IOException e) {
			logger.error("problem with search",e);
			throw new SpatialSearchException(e);
		} catch (JSONException e) {
			logger.error("problem with search",e);
			throw new SpatialSearchException(e);
		}
	    return result;
	}


	public double getMiles(final double km) {
		return km / spatialConversionUtils.MILE;
	}

	public double getKm(final double miles) {
		return miles * spatialConversionUtils.MILE;
	}


	
}
