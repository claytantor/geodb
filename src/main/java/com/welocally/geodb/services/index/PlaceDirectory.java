package com.welocally.geodb.services.index;

import java.io.File;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PlaceDirectory  {
	
	
	private SimpleFSDirectory directoryPlaces;
	
	@Value("${placeDirectory.fs.fileName:/Users/claygraham/data/tmp/places}")
	private String directoryFileNamePlaces;

	
	@PostConstruct
	private void initDirectory()  throws IOException{
		directoryPlaces = new SimpleFSDirectory(new File(directoryFileNamePlaces));
	}
	
	public Directory getDirectory() throws DirectoryException{
		
		return directoryPlaces;
		
						
	}
}
