package opennlp.tools.chatbot.search_results_blender;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import opennlp.tools.chatbot.ChatIterationResult;
import opennlp.tools.chatbot.TopicExtractorFromSearchResult;
import opennlp.tools.parse_thicket.opinion_processor.EntityExtractionResult;
import opennlp.tools.similarity.apps.HitBase;
import opennlp.tools.similarity.apps.utils.PageFetcher;

public class BloombergScraper {
	private static final String sourceURL = "https://www.bloomberg.com/search?query=";
	private static final String SEARCH_RESULTS_SEPARATOR = "search-result-story__headline\">";
	protected PageFetcher pFetcher = new PageFetcher();
	protected SearchResultsScorer scorer = new SearchResultsScorer();
	
	
	private List<HitBase> searchScrapeSpecificSite(String queryOrig){
		 List<HitBase> results = new ArrayList<HitBase>();
		
		String encodedQuery = URLEncoder.encode(queryOrig);
		String content = pFetcher.fetchOrigHTML(sourceURL + encodedQuery);
		String[] areas = content.split(SEARCH_RESULTS_SEPARATOR);
		for(int i=1; i<areas.length; i++ ){ // skip first area
			HitBase hit = new HitBase();
			String searchResArea = areas[i];
			String url = StringUtils.substringBetween(searchResArea,"href=\"" , "\">");
			String title = StringUtils.substringBetween(searchResArea, "\">", "</a>");
			String abstr = StringUtils.substringBetween(searchResArea, "body\">", "</div>");
			if (url == null || title == null || abstr == null)
				continue;
			hit.setUrl(url);
			hit.setTitle(title);
			hit.setAbstractText(abstr);
			results.add(hit);
			
		}
		return results;
	}
	
	public List<ChatIterationResult> searchCustomQuery(String queryOrig, TopicExtractorFromSearchResult extractor){

		List<ChatIterationResult> chatIterationResults= new ArrayList<ChatIterationResult>();

		List<HitBase> resultsToFilter = searchScrapeSpecificSite(queryOrig);
		List<HitBase> results=new ArrayList<HitBase>();
        try {
	        results = scorer.filterHitsByLinguisticScore(queryOrig, resultsToFilter);
        } catch (Exception e1) {
	        // TODO Auto-generated catch block
	        e1.printStackTrace();
        }

		// extract phrases and entities
		for(HitBase currSearchRes: results){
			try {
				String text = null;
				if (currSearchRes.getOriginalSentences()==null || currSearchRes.getOriginalSentences().isEmpty() ||
						currSearchRes.getOriginalSentences().get(0).length()<40)
					text = currSearchRes.getAbstractText();
				else
					text = combineSentences(currSearchRes.getOriginalSentences());

				EntityExtractionResult eeRes = extractor.extractEntitiesSubtractOrigQuery(text, queryOrig);
				
				chatIterationResults.add(new ChatIterationResult(currSearchRes, eeRes, text));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return chatIterationResults;
	}
	
	protected String combineSentences(List<String> originalSentences) {
		StringBuffer res = new StringBuffer();
		for(String s: originalSentences){
			res.append(s+" ");
		}
		return res.toString();
	}
	
	public static void main(String[] args){
		List<ChatIterationResult> res = new BloombergScraper().
				searchCustomQuery("post election rally", new  TopicExtractorFromSearchResult());
		System.out.println(res);		
	}
}

/*

<h1 class="search-result-story__headline"> <a data-resource-type="" data-resource-id="" 
href="https://www.bloomberg.com/news/articles/2017-03-29/goldman-favors-emerging-markets-on-growing-u-s-value-gap-chart">

Goldman Favors Emerging Markets on Growing U.S. Value Gap: Chart</a> </h1><div class="search-result-story__body"> 
Sachs Group Inc. sees a buying opportunity in developing nations after the S&amp;P 500 Index’s <em>post</em>-<em>election</em> <em>rally</em> pushed its valuation to more than a decade high relative to the emerging-market benchmark... </div> </div>  
<div class="search-result-story__thumbnail"> <a class="search-result-story__thumbnail__link" 
href="https://www.bloomberg.com/news/articles/2017-03-29/goldman-favors-emerging-markets-on-growing-u-s-value-gap-chart"> <img class="search-result-story__thumbnail__image" src="https://assets.bwbx.io/images/users/iqjWHBFdfxIU/iWx7FywJ.GO0/v0/-1x-1.jpg"> </a> </div>  </article></div><div class="search-result" data-view-uid="1|0_1_3_3"><article class="search-result-story type-video"> <div class="search-result-story__container"> <div class="search-result-story__metadata">  <span class="metadata-timestamp"><time class="published-at" datetime="2017-03-27T15:19:58+00:00"> Mar 27, 2017 </time></span> </div

Newspapers
Text style & genre recognition dataset
Fact and Feeling
Argument annotated essays

*
*/