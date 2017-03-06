package opennlp.tools.chatbot;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;

import opennlp.tools.parse_thicket.opinion_processor.EntityExtractionResult;
import opennlp.tools.similarity.apps.BingQueryRunner;
import opennlp.tools.similarity.apps.HitBase;
import opennlp.tools.similarity.apps.utils.PageFetcher;



public class WebSearcherWrapper extends LongQueryWebSearcher{
	private static Logger LOG = Logger
			.getLogger("opennlp.tools.chatbot.WebSearcherWrapper");
	
	private static WebSearcherWrapper instance;
	public synchronized static WebSearcherWrapper getInstance() {
	    if (instance == null)
	      instance = new WebSearcherWrapper();

	    return instance;
	  }
	private WebSearcherWrapper(){
		try {
	        query_listOfSearchResults = (Map<String, List<ChatIterationResult>>) serializer.readObject();
	        if ( query_listOfSearchResults==null)
	        	query_listOfSearchResults = new HashMap<String, List<ChatIterationResult>>();
        } catch (Exception e) {
        	System.err.println("Going to create cache for chat bot search results");
        	query_listOfSearchResults = new HashMap<String, List<ChatIterationResult>>();
        }
	}
	
	private Map<String, List<ChatIterationResult>> query_listOfSearchResults = new HashMap<String, List<ChatIterationResult>>();
	private ChatBotCacheSerializer serializer = new ChatBotCacheSerializer();
	
	public List<ChatIterationResult> searchLongQuery(String queryOrig){
		queryOrig = queryOrig.trim();
		List<ChatIterationResult> foundInCache = query_listOfSearchResults.get(queryOrig);
		if (foundInCache != null){
			return foundInCache;
		}
		List<ChatIterationResult> addToCache = super.searchLongQuery(queryOrig);
		query_listOfSearchResults.put(queryOrig, addToCache );
		// each time new query comes => write the results
		serializer.writeObject(query_listOfSearchResults);
		return addToCache;
	}

	public String produceAlertForRandomTopic(){
		Set<String> keys = query_listOfSearchResults.keySet();
		List<String >keyList = new ArrayList<String>(keys);
		int randIndex = new Double(Math.random()*(double)keyList.size()).intValue();
		String currentQuery = keyList.get(randIndex );
		List<ChatIterationResult>  sResults = query_listOfSearchResults.get(currentQuery);
		for(ChatIterationResult res: sResults){
			if (res.firstClarificationPhrase != null){
				List<HitBase> hits = bSearcher.runNewsSearch(res.firstClarificationPhrase, 3);
				return "We found interesting stuff for your recent query:'"+ currentQuery +"' "+
				hits.get(0).getTitle() +	hits.get(0).getAbstractText() ;
			} 
		}
		return "Sorry I don't have anything for you at this time";
	}
	
	public List<ChatIterationResult> searchLongQuery(String queryOrig, int count){
		if (isProductQuery(queryOrig)){
			try {
				List<ChatIterationResult> res =  searchProductQuery(queryOrig);
				return res;
			} catch (Exception e) {
				LOG.info("failed product page detail extraction. Proceeding with regular extraction path.");
			}
		}

		List<ChatIterationResult> chatIterationResults= new ArrayList<ChatIterationResult>();

		List<HitBase> results = bSearcher.runSearch(queryOrig, count), augmResults = new ArrayList<HitBase>() ;
		// populate with orig text
		for(HitBase currSearchRes: results){
			try {
				HitBase augmRes = paraFormer.formTextFromOriginalPageGivenSnippet(currSearchRes);
				augmResults.add(augmRes);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// extract phrases and entities
		for(HitBase currSearchRes: augmResults){
			try {
				String text = null;
				if (currSearchRes.getOriginalSentences()==null || currSearchRes.getOriginalSentences().isEmpty() ||
						currSearchRes.getOriginalSentences().get(0).length()<40)
					text = currSearchRes.getAbstractText();
				else
					text = combineSentences(currSearchRes.getOriginalSentences());

				if (text.length()>350)
					text = form350CharSubstringWithProperEnd(text);
				EntityExtractionResult eeRes = extractor.extractEntitiesSubtractOrigQuery(text, queryOrig);

				chatIterationResults.add(new ChatIterationResult(currSearchRes, eeRes, text));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return chatIterationResults;
	}
	
	public static void main(String[] args){
		String queryOrig = //"can I pay with one credit card for another";
				//"Sony DSC RX100 Sensor Digital Camera";
				//"Acer V173 Djb 17-Inch LCD Monitor";
				//"CUK HP 15z White Silver Student Notebook";
				//"LG 24UD58-B 24-Inch 4K UHD IPS Monitor";
				//"Kindle E-reader - Black Glare-Free Touchscreen Display";
				//"HP OfficeJet Pro 6968 Wireless All-in-One Photo Printer ";
		//"Brother FAX-2840 High Speed Mono Laser Fax Machine";
			//	"Dell Inspiron 24 3000 Series All-In-One";
			//	"Crucial 8GB Single DDR3L  Laptop Memory"; 
			//	"Avera 32AER10N LED-LCD HDTV";
			//	"Escort Passport 9500IX Radar Detector"; 
			//	"Motorola Moto E Android Prepaid Phone";
			//	"PUBLIC Bikes Women C1 Dutch  City Bike";
			//	"Schwinn Women's Wayfare Hybrid Bike";
			//	"Outmate 6 pcs Aluminum D-ring Locking Carabiner";
			//	"Venture's Pal Lightweight Packable Durable Hiking Backpack";
			//"Triple Eight Helmet with Sweatsaver Liner 123";
				"Sony HD Video Recording HDRCX405";
		WebSearcherWrapper searcher = new WebSearcherWrapper();
		List<ChatIterationResult> res = searcher.searchLongQuery(queryOrig);
		System.out.println(res);
	}

	
}
