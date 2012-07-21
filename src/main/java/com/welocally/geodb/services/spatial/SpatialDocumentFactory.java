package com.welocally.geodb.services.spatial;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.spatial.tier.projections.CartesianTierPlotter;
import org.apache.lucene.util.NumericUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.welocally.geodb.services.index.DocumentContentException;

@Component
public class SpatialDocumentFactory {
	
	
	@Autowired SpatialConversionUtils spatialConversionUtils;
	

	/**
	 * 
	 * @param placeObject
	 * @return
	 * @throws JSONException
	 */
	public Document makePlaceDocument(JSONObject placeObject) throws JSONException, DocumentContentException
	{
				
		JSONObject properties = placeObject.getJSONObject("properties");
		JSONObject geom = placeObject.getJSONObject("geometry");
		JSONArray coords = geom.getJSONArray("coordinates");
		Point coord = 
			new Point(
					Double.parseDouble(coords.getString(1)), 
					Double.parseDouble(coords.getString(0)));
		
		Document doc = new Document();
		
		if(!placeObject.isNull("_id")){
			doc.add(new Field("_id", placeObject.getString("_id"), Field.Store.YES,
					Field.Index.NOT_ANALYZED));
		}
			
		doc.add(new Field("name", properties.getString("name"), Field.Store.YES,
				Field.Index.NOT_ANALYZED));
		
		doc.add(new Field("place", placeObject.toString(), Field.Store.YES,
				Field.Index.NOT_ANALYZED));
		
		doc.add(new Field("search", makeSearchablePlaceContent(properties), Field.Store.YES,
				Field.Index.ANALYZED));
		
		doc.add(new Field("metafile", "doc", Store.YES, Index.ANALYZED));
		addSpatialLcnFields(coord, doc);
								
		
		return doc;
		
	}
	
	
	public Document makePlaceDocumentForIndex(JSONObject placeObject) throws JSONException, DocumentContentException
	{
				
		JSONObject properties = placeObject.getJSONObject("properties");
		JSONObject geom = placeObject.getJSONObject("geometry");
		JSONArray coords = geom.getJSONArray("coordinates");
		Point coord = 
			new Point(
					Double.parseDouble(coords.getString(1)), 
					Double.parseDouble(coords.getString(0)));
		
		Document doc = new Document();
		
		if(!placeObject.isNull("_id")){
			doc.add(new Field("_id", placeObject.getString("_id"), Field.Store.YES,
					Field.Index.NOT_ANALYZED));
		}
			
		
		doc.add(new Field("search", makeSearchablePlaceContent(properties), Field.Store.YES,
				Field.Index.ANALYZED));
		
		doc.add(new Field("metafile", "doc", Store.YES, Index.ANALYZED));
		addSpatialLcnFields(coord, doc);
								
		
		return doc;
		
	}
	
	public String makeSearchableDealContent(JSONObject deal) throws JSONException{
        StringBuffer buf = new StringBuffer();
        buf.append(deal.getString("title")+" ");
        buf.append(deal.getString("details")+" ");
        
        if(deal.has("location")){
            JSONObject location = deal.getJSONObject("location");
            buf.append(location.getString("city")+" ");
        }
           
        return buf.toString();  
    }
	
	public String makeSearchablePlaceContent(JSONObject placeProperties) throws JSONException{
		StringBuffer buf = new StringBuffer();
		buf.append(placeProperties.getString("name")+" ");
		
		//classifiers
		if(placeProperties.has("classifiers")){
			JSONArray categories = placeProperties.getJSONArray("classifiers");
			for (int i = 0; i < categories.length(); i++) {
				//buf.append(tags.getString(i)+" ");
				JSONObject cat = categories.getJSONObject(i);
				buf.append(cat.getString("category")+" ");
				buf.append(cat.getString("subcategory")+" ");
				buf.append(cat.getString("type")+" ");
			}
		}
		
		//tags
		if(placeProperties.has("tags")){
			JSONArray tags = placeProperties.getJSONArray("tags");
			for (int i = 0; i < tags.length(); i++) {
				buf.append(tags.getString(i)+" ");
			}
		}
		
		return buf.toString().toLowerCase().trim();	
	}
	
	public String makeSearchableUserDataContent(JSONObject placeProperties, JSONObject userdata) throws JSONException{
        StringBuffer buf = new StringBuffer();
        buf.append(makeSearchablePlaceContent(placeProperties)+" ");
        /*
         * {
         *  "data": [
         *      {"name":"foo", "value":"bar"},
         *      {"name":"roo", "value":"tar"}
         *  ]
         * }
         * 
         */
        JSONArray data = userdata.getJSONArray("data");
        for (int i = 0; i < data.length(); i++) {
            JSONObject nv = data.getJSONObject(i);
            buf.append(nv.getString("value")+" ");
        }
        
        return buf.toString().toLowerCase().trim();  
    }
		
	private void addSpatialLcnFields(Point coord, Document document) {
		document.add(new Field(spatialConversionUtils.LAT_FIELD, NumericUtils
				.doubleToPrefixCoded(coord.getLat()), Field.Store.YES,
				Field.Index.NOT_ANALYZED));
		document.add(new Field(spatialConversionUtils.LON_FIELD, NumericUtils
				.doubleToPrefixCoded(coord.getLon()), Field.Store.YES,
				Field.Index.NOT_ANALYZED));
		addCartesianTiers(coord, document);
	}
	
	private void addCartesianTiers(Point coord, Document document) {
	   for (int tier = spatialConversionUtils.getStartTier(); tier <= spatialConversionUtils.getEndTier(); tier++) {
		  CartesianTierPlotter ctp = new CartesianTierPlotter(tier, spatialConversionUtils.getProjector(), CartesianTierPlotter.DEFALT_FIELD_PREFIX);
	      double boxId = ctp.getTierBoxId(coord.getLat(), coord.getLon());
	      document.add(new Field(ctp.getTierFieldName(), NumericUtils
	         .doubleToPrefixCoded(boxId), Field.Store.YES,
	         Field.Index.NOT_ANALYZED_NO_NORMS));
	   }
	}
	

}
