package com.welocally.geodb.services.spatial;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.NoSuchDirectoryException;
import org.apache.lucene.util.Version;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.welocally.geodb.services.app.CommandException;
import com.welocally.geodb.services.app.CommandSupport;
import com.welocally.geodb.services.db.DbException;
import com.welocally.geodb.services.db.DbPage;
import com.welocally.geodb.services.db.JsonDatabase;
import com.welocally.geodb.services.index.DirectoryException;
import com.welocally.geodb.services.index.DocumentContentException;
import com.welocally.geodb.services.index.PlaceDirectory;
import com.welocally.geodb.services.jmx.IndexMonitor;

@Component
public class LuceneMongoSpatialIndexService implements SpatialIndexService,CommandSupport  {

    @Override
    public void doCommand(JSONObject command) throws CommandException {
        // TODO Auto-generated method stub
        throw new RuntimeException("NO IMPL");
    }

    @Override
    public void index(String collectionName, int maxdocs)
            throws SpatialIndexException {
        // TODO Auto-generated method stub
        throw new RuntimeException("NO IMPL");
    }

    @Override
    public void indexPlace(JSONObject place) throws SpatialIndexException {
        // TODO Auto-generated method stub
        throw new RuntimeException("NO IMPL");
    }

//	static Logger logger = Logger.getLogger(LuceneMongoSpatialIndexService.class);
//	
//	@Autowired PlaceDirectory placeDirectory;
//	
//	@Autowired SpatialConversionUtils spatialConversionUtils;
//	
//	@Autowired SpatialDocumentFactory spatialDocumentFactory;
//	
//	@Autowired IndexMonitor indexMonitor;
//
//	@Autowired
//	@Qualifier("mongoJsonDatabase")
//	private JsonDatabase jsonDatabase;
//
//
//
//	@Override
//	public void doCommand(JSONObject command) throws CommandException {
//		try {
//			indexMonitor.reset();
//			index(command.getString("source") , command.getInt("maxDocs") );
//		} catch (SpatialIndexException e ) {
//			throw new CommandException(e);
//		} catch (JSONException e) {
//			throw new CommandException(e);
//		}
//	}
//
//	/* (non-Javadoc)
//	 * @see com.welocally.geodb.services.spatial.SpatialIndexService#index()
//	 */
//	public void index( String collectionName,int maxdocs)  throws SpatialIndexException {
//		logger.debug("initializing");
//				
//		try {
//												
//				String[] files=null;
//				try {
//					//files = placeDirectory.getDirectory(collectionName).listAll();
//					files = placeDirectory.getDirectory().listAll();
//					
//					boolean indexExists = false;
//					for (String file : files) {
//						if(file.equals("segments.gen")){
//							indexExists=true;
//							break;
//						}			
//					}
//					
//					if(!indexExists){
//						generateIndex(collectionName, maxdocs);
//					}
//					
//					
//				} catch (NoSuchDirectoryException e) {
//					generateIndex(collectionName, maxdocs);
//				}
//				
//		} 
//		catch (DbException e) {
//			logger.error("cant get documents for indexing", e);
//			throw new SpatialIndexException(e);
//		} 
//		catch (CorruptIndexException e) {
//			logger.error("cant get documents for indexing", e);
//			throw new SpatialIndexException(e);
//		} catch (IOException e) {
//			logger.error("cant get documents for indexing", e);
//			throw new SpatialIndexException(e);
//		} catch (Exception e) {
//			logger.error("cant get documents for indexing", e);
//			throw new SpatialIndexException(e);
//		}
//
//	}
//	
//	private void generateIndex(String collectionName, int maxDocs) throws CorruptIndexException, LockObtainFailedException, IOException, DirectoryException, DbException {
//
//		
//		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_33,
//				new StandardAnalyzer(Version.LUCENE_33));
//		
//		IndexWriter writer = 
//			new IndexWriter(placeDirectory.getDirectory(), 
//					config);
//
//		// get the first page and add
//		int resultCount = 0;
//		DbPage dbpage = jsonDatabase.findAll(collectionName, 1);
//		addPage(dbpage, writer);
//
//		if (dbpage.getCount() > dbpage.getObjects().length()) {
//			int pages = dbpage.getCount() / dbpage.getPageSize();
//			if (dbpage.getCount() % dbpage.getPageSize() > 0)
//				pages = pages + 1;
//			for (int pageNumIndex = 2; pageNumIndex <= pages && resultCount<maxDocs; pageNumIndex++) {
//				dbpage = jsonDatabase.findAll(collectionName, pageNumIndex);
//				resultCount = resultCount + dbpage.getObjects().length();
//				addPage(dbpage, writer);
//			}
//		}
//		writer.commit();
//		writer.close(true);
//		
//		logger.debug("finished indexing");
//	}
//	
//	
//
//	public void addPage(DbPage page, IndexWriter writer) {
//		// this is only getting first page, we need
//		// to iterate through all pages
//		for (int i = 0; i < page.getObjects().length(); i++) {
//
//			// don't fail on a specific doc
//			try {
//				JSONObject place = page.getObjects().getJSONObject(i);
//				logger.debug("writing place:"+place.getString("_id"));
//				writer.addDocument(
//						spatialDocumentFactory.makePlaceDocument(place));	
//				
//				indexMonitor.increment();
//
//			} catch (JSONException e) {
//				logger.warn("cant index a documenent", e);
//			} catch (CorruptIndexException e) {
//				logger.warn("cant index a documenent", e);
//			} catch (IOException e) {
//				logger.warn("cant index a documenent", e);
//			} catch (DocumentContentException e) {
//				logger.warn("cant index a documenent", e);
//			}
//		}
//	}
//
//	public void indexPlace(JSONObject place) throws SpatialIndexException{
//		
//		try {
//			IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_33,
//					new StandardAnalyzer(Version.LUCENE_33));
//			
//			IndexWriter writer = 
//				new IndexWriter(placeDirectory.getDirectory(),config);
//			
//			writer.addDocument(
//					spatialDocumentFactory.makePlaceDocument(place));
//			
//			
//			writer.commit();
//			writer.close(true);
//			
//		} catch (CorruptIndexException e) {
//			logger.warn("cant index a documenent", e);
//			throw new SpatialIndexException(e);
//		} catch (IOException e) {
//			logger.warn("cant index a documenent", e);
//			throw new SpatialIndexException(e);
//		} catch (Exception e) {
//			logger.warn("cant index a documenent", e);
//			throw new SpatialIndexException(e);
//		}
//		
//	}

	
}
