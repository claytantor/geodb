package com.welocally.geodb.services.spatial;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
import org.springframework.stereotype.Component;

import com.welocally.geodb.services.app.CommandException;
import com.welocally.geodb.services.app.CommandSupport;
import com.welocally.geodb.services.db.DbException;
import com.welocally.geodb.services.index.DirectoryException;
import com.welocally.geodb.services.index.DocumentContentException;
import com.welocally.geodb.services.index.PlaceDirectory;
import com.welocally.geodb.services.jmx.IndexMonitor;
import com.welocally.geodb.services.util.WelocallyJSONUtils;

@Component
public class LuceneNFSSpatialIndexService implements SpatialIndexService,CommandSupport  {

	static Logger logger = Logger.getLogger(LuceneNFSSpatialIndexService.class);
	
	@Autowired PlaceDirectory placeDirectory;
	
	@Autowired SpatialConversionUtils spatialConversionUtils;
	
	@Autowired SpatialDocumentFactory spatialDocumentFactory;
	
	@Autowired IndexMonitor indexMonitor;
	
	@Autowired WelocallyJSONUtils welocallyJSONUtils;


	@Override
	public void doCommand(JSONObject command) throws CommandException {
		try {
			indexMonitor.reset();
			index(command.getString("source") , command.getInt("maxDocs") );
		} catch (SpatialIndexException e ) {
			throw new CommandException(e);
		} catch (JSONException e) {
			throw new CommandException(e);
		}
	}

	/* (non-Javadoc)
	 * @see com.welocally.geodb.services.spatial.SpatialIndexService#index()
	 */
	public void index( String collectionName,int maxdocs)  throws SpatialIndexException {
		logger.debug("initializing");
				
		try {
												
				String[] files=null;
				try {
					//files = placeDirectory.getDirectory(collectionName).listAll();
					files = placeDirectory.getDirectory().listAll();
					
					boolean indexExists = false;
					for (String file : files) {
						if(file.equals("segments.gen")){
							indexExists=true;
							break;
						}			
					}
					
					if(!indexExists){
						generateIndex(collectionName, maxdocs);
					}
					
					
				} catch (NoSuchDirectoryException e) {
					generateIndex(collectionName, maxdocs);
				}
				
		} 
		catch (DbException e) {
			logger.error("cant get documents for indexing", e);
			throw new SpatialIndexException(e);
		} 
		catch (CorruptIndexException e) {
			logger.error("cant get documents for indexing", e);
			throw new SpatialIndexException(e);
		} catch (IOException e) {
			logger.error("cant get documents for indexing", e);
			throw new SpatialIndexException(e);
		} catch (Exception e) {
			logger.error("cant get documents for indexing", e);
			throw new SpatialIndexException(e);
		}

	}
	
	private void generateIndex(String collectionName, int maxDocs) 
		throws CorruptIndexException, LockObtainFailedException, IOException, 
		DirectoryException, DbException, JSONException, DocumentContentException {

		
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_33,
				new StandardAnalyzer(Version.LUCENE_33));
		
		IndexWriter writer = 
			new IndexWriter(placeDirectory.getDirectory(), 
					config);
		
		InputStream i4 = new FileInputStream(new File(collectionName));		
		
		InputStreamReader reader = 
			new InputStreamReader(i4);
		
		BufferedReader br = new BufferedReader(reader); 
		String str = null; 
		while((str = br.readLine()) != null) { 
			
			JSONObject place = 
				new JSONObject(str);
			
			welocallyJSONUtils.updatePlaceToWelocally(place);
			
			writer.addDocument(
					spatialDocumentFactory.makePlaceDocument(place));
			indexMonitor.increment();

			
		} 
		reader.close(); 
		writer.commit();
		writer.close(true);

		
		logger.debug("finished indexing");
	}

	@Override
	public void indexPlace(JSONObject place) throws SpatialIndexException {
		throw new SpatialIndexException("NO IMPL");
		
	}
	
	

	
}
