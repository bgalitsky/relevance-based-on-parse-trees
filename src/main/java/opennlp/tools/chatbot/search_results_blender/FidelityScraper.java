package opennlp.tools.chatbot.search_results_blender;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import opennlp.tools.chatbot.ChatIterationResult;
import opennlp.tools.chatbot.TopicExtractorFromSearchResult;
import opennlp.tools.jsmlearning.ProfileReaderWriter;
import opennlp.tools.parse_thicket.Pair;
import opennlp.tools.parse_thicket.opinion_processor.EntityExtractionResult;
import opennlp.tools.similarity.apps.BingQueryRunner;
import opennlp.tools.similarity.apps.HitBase;
import opennlp.tools.similarity.apps.utils.PageFetcher;

public class FidelityScraper {
	private static final String sourceURL = "https://www.bloomberg.com/search?query=";
	private static final String SEARCH_RESULTS_SEPARATOR = "search-result-story__headline\">";
	protected PageFetcher pFetcher = new PageFetcher();
	protected BingQueryRunner runner = new BingQueryRunner();
	
	
	public List<String> getAListOfFAQ_Questions(String topic, String domain){
		List<String> results = new ArrayList<String>();
		 List<HitBase> sresults = new ArrayList<HitBase>();
		 
		 sresults = runner.runSearch(topic + " site:"+domain);
		
		 String url = sresults.get(0).getUrl();
		 
		String content = pFetcher.fetchOrigHTML(url);
		
		String faqArea = StringUtils.substringBetween(content,  "<div class=\"promo-group-horizontal-promo--section", "/ul>");
		String[] faqItems =  faqArea.split("/a></li>");

		//<li><a href="#faq_about1">How does cash availability work in my account?</a></li>

		for(int i=1; i<faqItems.length; i++ ){ 
			String qArea = faqItems[i];
			String q = StringUtils.substringBetween(qArea,"\">" , "<");
			results.add(q);
			
		}
		return results;
	}
	
	public List<Pair<String, String>> getAListOfFAQ_QuestionsAndAnswers(String topic, String domain){
		List<Pair<String, String>> results = new ArrayList<Pair<String, String>>();
		 List<HitBase> sresults = new ArrayList<HitBase>();
		 
		 sresults = runner.runSearch(topic + " site:"+domain);
		
		 String url = sresults.get(0).getUrl();
		 
		String content = pFetcher.fetchOrigHTML(url);
		
		//<span id="faq_about8">What do the different account values mean?</span>
		String[] faqItems =  content.split("<span id=\"faq_about");
		for(int i=1; i< faqItems.length; i++){
			String portion = faqItems[i];
			String question = StringUtils.substringBetween(portion,"\">" , "<");
			String answer = StringUtils.substringBetween(portion,"</h3>", "</section>");
			answer = answer.replaceAll("( )+", " ").trim();
			if (question!=null && question.length()>10 &&
					answer!=null && answer.length()>60 )
				results.add(new Pair<String, String>(question, answer));
		}
		
		if (results.isEmpty()){
			faqItems =  content.split("class=\"expand-collapse--head");	
			for(int i=1; i< faqItems.length; i++){
				String portion = faqItems[i];
				String question = StringUtils.substringBetween(portion,"\" >" , "<");
				String answer = StringUtils.substringBetween(portion,"<p>", "</div>");
				
				if (question!=null && question.length()>10 &&
						answer!=null && answer.length()>60 )
					answer = answer.replaceAll("( )+", " ").trim();
					results.add(new Pair<String, String>(question, answer));
			}
		}
			
		return results;
	}
	
	
	public void runExtactedFAQ(String topic, String domain){
		List<String[]> report = new ArrayList<String[]>();
		
		List<String> qs = getAListOfFAQ_Questions(topic, domain);
		for(String q: qs){
			List<HitBase> sresults = new ArrayList<HitBase>();		 
			sresults = runner.runSearch(q + " site:"+domain);
			for(HitBase h: sresults){
				report.add(new String[] { q, h.getAbstractText(), h.getTitle(), h.getUrl()} );
			}
		}
		
		ProfileReaderWriter.writeReport(report, "FAQ_extractedAskedQuestions.csv");
	}
	
	
	public static void main(String[] args){
		FidelityScraper faqEvaluator = new FidelityScraper();
		//site:www.fidelity.com trading account faq
		//List<String> qs = 
		
		List<Pair<String, String>> res=
				faqEvaluator.getAListOfFAQ_QuestionsAndAnswers(//"placing orders "
						 "trading account faq", "www.fidelity.com");
				//getAListOfFAQ_Questions
				//runExtactedFAQ("trading account faq", "www.fidelity.com");
		//System.out.println(qs);		
	}
}

/*

<h1 class="search-result-story__headline"> <a data-resource-type="" data-resource-id="" 
href="https://www.bloomberg.com/news/articles/2017-03-29/goldman-favors-emerging-markets-on-growing-u-s-value-gap-chart">

Goldman Favors Emerging Markets on Growing U.S. Value Gap: Chart</a> </h1><div class="search-result-story__body"> 
Sachs Group Inc. sees a buying opportunity in developing nations after the S&amp;P 500 Index’s <em>post</em>-<em>election</em> <em>rally</em> pushed its valuation to more than a decade high relative to the emerging-market benchmark... </div> </div>  
<div class="search-result-story__thumbnail"> <a class="search-result-story__thumbnail__link" 
href="https://www.bloomberg.com/news/articles/2017-03-29/goldman-favors-emerging-markets-on-growing-u-s-value-gap-chart"> <img class="search-result-story__thumbnail__image" src="https://assets.bwbx.io/images/users/iqjWHBFdfxIU/iWx7FywJ.GO0/v0/-1x-1.jpg"> </a> </div>  </article></div><div class="search-result" data-view-uid="1|0_1_3_3"><article class="search-result-story type-video"> <div class="search-result-story__container"> <div class="search-result-story__metadata">  <span class="metadata-timestamp"><time class="published-at" datetime="2017-03-27T15:19:58+00:00"> Mar 27, 2017 </time></span> </div
*/