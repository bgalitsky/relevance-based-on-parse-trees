package opennlp.tools.chatbot.search_results_blender;

import java.util.List;

import opennlp.tools.similarity.apps.BingQueryRunner;
import opennlp.tools.similarity.apps.HitBase;
import opennlp.tools.similarity.apps.utils.PageFetcher;

public class WebDomainManager {
	protected PageFetcher pFetcher = new PageFetcher();
	protected BingQueryRunner bSearcher = new BingQueryRunner();
	protected String webDomain = null;
	
	public void setWebDomain(String dom){
		webDomain=dom;
	}

	public String formYelpRequestURL(String query) {
		String yelpQuery = "site:"+webDomain + " " + query;
	    List<HitBase> sResults = bSearcher.runSearch(yelpQuery);
	    if (sResults!=null &&  sResults.size()>0)
	    	return sResults.get(0).getUrl();
	    else
	    	return null;
    }

}


