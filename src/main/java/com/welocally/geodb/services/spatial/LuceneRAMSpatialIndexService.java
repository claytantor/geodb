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
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.NoSuchDirectoryException;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
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
import com.welocally.geodb.services.jmx.IndexMonitor;
import com.welocally.geodb.services.util.WelocallyJSONUtils;

@Component
public class LuceneRAMSpatialIndexService implements SpatialIndexService,CommandSupport  {

	static Logger logger = Logger.getLogger(LuceneRAMSpatialIndexService.class);
	
	
	
	@Autowired SpatialConversionUtils spatialConversionUtils;
	
	@Autowired SpatialDocumentFactory spatialDocumentFactory;
	
	@Autowired IndexMonitor indexMonitor;
	
	@Autowired WelocallyJSONUtils welocallyJSONUtils;
	
	Directory placeDirectory = new RAMDirectory();
	
	private String target;
	

	public void doCommand(JSONObject command) throws CommandException {
		try {
			indexMonitor.reset();
			this.target = command.getString("target");
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
	public void index( String source, int maxdocs)  throws SpatialIndexException {
		logger.debug("initializing");
				
		try {
												
				String[] files=null;
				try {
					//files = placeDirectory.getDirectory(collectionName).listAll();
					files = placeDirectory.listAll();
					
					boolean indexExists = false;
					for (String file : files) {
						if(file.equals("segments.gen")){
							indexExists=true;
							break;
						}			
					}
					
					if(!indexExists){
						generateIndex(source, target, maxdocs);
					}
					
					
				} catch (NoSuchDirectoryException e) {
					generateIndex(source, target, maxdocs);
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
	
	private void generateIndex(String source, String target, int maxDocs) 
		throws CorruptIndexException, LockObtainFailedException, IOException, 
		DirectoryException, DbException, JSONException, DocumentContentException {

		
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_33,
				new StandardAnalyzer(Version.LUCENE_33));
		
		IndexWriter writer = 
			new IndexWriter(placeDirectory, 
					config);
		
		InputStream i4 = new FileInputStream(new File(source));		
		
		InputStreamReader reader = 
			new InputStreamReader(i4);
		
		BufferedReader br = new BufferedReader(reader); 
		String str = null; 
		while((str = br.readLine()) != null && indexMonitor.getCount()<maxDocs) { 
			
			JSONObject place = 
				new JSONObject(str);
			
			welocallyJSONUtils.updatePlaceToWelocallyFormat(place);
			
			writer.addDocument(
					spatialDocumentFactory.makePlaceDocument(place));
			indexMonitor.increment();

			
		} 
		reader.close(); 
		writer.commit();
		writer.close(true);
		
		//now copy the directory
		writeDirectoryFiles(placeDirectory, target);
		
		
		logger.debug("finished indexing");
	}


	public void indexPlace(JSONObject place) throws SpatialIndexException {
		throw new SpatialIndexException("NO IMPL");
		
	}
	
	private void writeDirectoryFiles(Directory sourceDir, String targetDir) throws IOException{

			Directory target = new SimpleFSDirectory(new File(targetDir));
			

			//Directory to; // the directory to copy to
			 for (String file : sourceDir.listAll()) {
				 sourceDir.copy(target, file, file); // newFile can be either file, or a new name
			 }

	}
	
	

	
}
