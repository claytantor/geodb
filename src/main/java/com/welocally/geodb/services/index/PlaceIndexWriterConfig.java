package com.welocally.geodb.services.index;

import javax.annotation.PostConstruct;

import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.util.Version;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PlaceIndexWriterConfig {
	
	private IndexWriterConfig indexWriterConfig; 
	
	@Autowired 
	private DirectoryAnalyzer analyzer;
	
	@PostConstruct
	public void initConfig(){
		indexWriterConfig = new IndexWriterConfig(Version.LUCENE_33, 
				analyzer.getAnalyzer());
	}

	public IndexWriterConfig getIndexWriterConfig() {
		return indexWriterConfig;
	}

}
