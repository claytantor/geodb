package com.welocally.geodb.services.util;

import org.springframework.stereotype.Component;

@Component
public class MongoPlaceCategoryLoader {
	
//	
//
//	static Logger logger = 
//		Logger.getLogger(MongoPlaceCategoryLoader.class);
//	
//	@Autowired 
//	@Qualifier("mongoJsonDatabase")
//	JsonDatabase jsonDatabase;
//	
//	@Autowired IdGen idGen;
//	
//
//	@Override
//	public void doCommand(JSONObject command) throws CommandException {
//		try {
//			logger.debug("doCommand");
//
//			
//			DbPage dbpage =
//				jsonDatabase.findAll(command.getString("source"), 1);
//			loadPage(dbpage, command.getString("target"));
//						
//			int resultCount = 0;
//			int maxDocs = Integer.parseInt(command.getString("maxDocs"));
//			if (dbpage.getCount() > dbpage.getObjects().length()) {
//				int pages = dbpage.getCount() / dbpage.getPageSize();
//				if (dbpage.getCount() % dbpage.getPageSize() > 0)
//					pages = pages + 1;
//				for (int pageNumIndex = 2; pageNumIndex <= pages && resultCount<maxDocs; pageNumIndex++) {
//					dbpage = jsonDatabase.findAll(command.getString("source"), pageNumIndex);
//					resultCount = resultCount + dbpage.getObjects().length();
//					loadPage(dbpage, command.getString("target"));
//				}
//			}
//						
//		} catch (DbException e) {
//			logger.error(e);
//			throw new CommandException(e);
//		} catch (JSONException e) {
//			logger.error(e);
//			throw new CommandException(e);
//		}
//	
//	}
//	
//
//	
//	private void loadPage(DbPage dbpage, String targetCollection) throws JSONException, DbException{
//		
//		for (int i = 0; i < dbpage.getObjects().length(); i++) {
//			//allCats.put(dbpage.getObjects().get(i));
//			JSONObject propClassifiers = dbpage.getObjects().getJSONObject(i).getJSONObject("properties");
//			JSONArray classifiers = propClassifiers.getJSONArray("classifiers");
//			loadClassifiers(
//					 classifiers, 
//					 targetCollection) ;
//		}
//		
//	}
//	
//	private void loadClassifiers(
//			JSONArray classifiers, 
//			String targetCollection) 
//	throws DbException, JSONException{
//		//for each classifier
//		for (int i = 0; i < classifiers.length(); i++) {
//			JSONObject classifier = classifiers.getJSONObject(i);
//			//try to look the classifier up by the three fields
//			DbPage dbpage =
//				jsonDatabase.findByExample(targetCollection, 1,  classifier );
//			if(dbpage.getCount()==0){
//				//not found? add it
//				loadClassifierIntoDb(classifier, targetCollection);
//			}
//	
//		}	
//
//	}
//	
//	public String loadClassifierIntoDb(JSONObject classifier, String targetCollection) 
//	throws JSONException, DbException{
//		String id = idGen.genBasic(10);	
//		jsonDatabase.put(classifier, targetCollection, id, JsonDatabase.EntityType.CLASSIFER);
//		return id;
//	}


}
