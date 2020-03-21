package org.obiba.mica.study.rest;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestImportMain {
	
	private static final Logger log = LoggerFactory.getLogger(TestImportMain.class);
	
	public static void main(String[] args) {
		
		StudiesImportResource sir = new StudiesImportResource();
		
		//log.info("Response \"/ws/draft/study-states\" : " +	sir.listRemoteSourceIndividualStudies("https://recap-test.inesctec.pt/pub", "gfcg", "password", "harmonization-study").getEntity());
		
		List<String> idsToInclude = new ArrayList<>();
		idsToInclude.add("epice");
		
		log.info("Response \"/individual-study/{id}\" : " + 
				sir.includeStudies("https://recap-preterm.inesctec.pt/pub", "administrator", "correctduckpretermsubject", "individual-study", idsToInclude).getEntity());

	
		List<String> idsToUpdate = new ArrayList<>();
		idsToUpdate.add("action");
		
	}
}		
