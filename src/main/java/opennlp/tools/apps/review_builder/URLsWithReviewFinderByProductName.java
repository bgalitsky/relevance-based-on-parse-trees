package opennlp.tools.apps.review_builder;

import java.util.ArrayList;
import java.util.List;

import opennlp.tools.similarity.apps.BingWebQueryRunner;
import opennlp.tools.similarity.apps.HitBase;

public class URLsWithReviewFinderByProductName {
BingWebQueryRunner search = new BingWebQueryRunner();
	
	public List<String> findFacebookURLByNameAndZip(String name){
		List<HitBase> foundFBPages = search.runSearch(name, 20);
		List<String> results = new ArrayList<String>();
		for(HitBase h: foundFBPages){
			results.add(h.getUrl());
		}
		return results;
	}
	
}
