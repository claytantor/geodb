package com.welocally.geodb.services.index;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;
import org.springframework.stereotype.Component;

@Component
public class DirectoryAnalyzer  {
	private StandardAnalyzer analyzer;

	public DirectoryAnalyzer() {
		super();
		analyzer = new StandardAnalyzer(Version.LUCENE_33);
	}

	public StandardAnalyzer getAnalyzer() {
		return analyzer;
	}

	public void setAnalyzer(StandardAnalyzer analyzer) {
		this.analyzer = analyzer;
	}
	

}
