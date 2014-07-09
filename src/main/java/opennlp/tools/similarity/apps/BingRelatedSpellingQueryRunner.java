package opennlp.tools.similarity.apps;

import java.util.ArrayList;
import java.util.List;

import net.billylieurance.azuresearch.AzureSearchRelatedSearchQuery;
import net.billylieurance.azuresearch.AzureSearchRelatedSearchResult;
import net.billylieurance.azuresearch.AzureSearchResultSet;
import net.billylieurance.azuresearch.AzureSearchSpellingSuggestionQuery;
import net.billylieurance.azuresearch.AzureSearchSpellingSuggestionResult;
import net.billylieurance.azuresearch.AzureSearchWebQuery;
import net.billylieurance.azuresearch.AzureSearchWebResult;

public class BingRelatedSpellingQueryRunner extends BingQueryRunner{
	private AzureSearchRelatedSearchQuery aq = new AzureSearchRelatedSearchQuery ();
	private AzureSearchSpellingSuggestionQuery  ssq = new AzureSearchSpellingSuggestionQuery ();
	
	
	public List<HitBase> runSearch(String query, int nRes) {
		aq.setAppid(BING_KEY);
		aq.setQuery(query);		
		aq.setPerPage(nRes);
		aq.doQuery();
		
		List<HitBase> results = new ArrayList<HitBase> ();
		AzureSearchResultSet<AzureSearchRelatedSearchResult> ars = aq.getQueryResult();
		
		for (AzureSearchRelatedSearchResult anr : ars){
		    HitBase h = new HitBase();
		    h.setTitle(anr.getTitle());
		    h.setUrl(anr.getBingUrl());
		    results.add(h);
		}
		return results;
	}
	
	public List<HitBase> runSSSearch(String query, int nRes) {
		ssq.setAppid(BING_KEY);
		ssq.setQuery(query);		
		ssq.setPerPage(nRes);
		ssq.doQuery();
		
		List<HitBase> results = new ArrayList<HitBase> ();
		AzureSearchResultSet<AzureSearchSpellingSuggestionResult> ars = ssq.getQueryResult();
		
		for ( AzureSearchSpellingSuggestionResult anr : ars){
		    HitBase h = new HitBase();
		    h.setTitle(anr.getTitle());
		    h.setAbstractText(anr.getValue());
		   
		    results.add(h);
		}
		return results;
	}
	
	public static void main(String[] args) {
		BingRelatedSpellingQueryRunner self = new BingRelatedSpellingQueryRunner();
	    try {
	    	self.setLang("es-MX");
	    	self.setKey(
	    			"e8ADxIjn9YyHx36EihdjH/tMqJJItUrrbPTUpKahiU0=");
	      List<HitBase> resp = self
	          .runSearch("clear Sess", 10);
	      System.out.print(resp.get(0));
	      
	      resp = self
		          .runSSSearch("clear Sess", 10);
		      System.out.print(resp.get(0));
	    } catch (Exception e) {
	      // TODO Auto-generated catch block
	      e.printStackTrace();
	    }
	}
}
