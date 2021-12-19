package opennlp.tools.chatbot.qna_pairs_adapter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;

import opennlp.tools.chatbot.ChatBotCacheSerializer;
import opennlp.tools.chatbot.ChatIterationResult;
import opennlp.tools.chatbot.LongQueryWebSearcher;
import opennlp.tools.chatbot.TopicExtractorFromSearchResult;
import opennlp.tools.parse_thicket.opinion_processor.EntityExtractionResult;
import opennlp.tools.similarity.apps.BingQueryRunner;
import opennlp.tools.similarity.apps.HitBase;
import opennlp.tools.similarity.apps.utils.PageFetcher;

public class QnAPairsSearcherBlender extends LongQueryWebSearcher{
	private static Logger LOG = Logger
			.getLogger("opennlp.tools.chatbot.WebCustomSearcherBlender");

	private QnAPairsIndexSearcher luceneSearcher = new QnAPairsIndexSearcher ();
	
	private static Map<String, List<ChatIterationResult>> query_listOfSearchResults = new HashMap<String, List<ChatIterationResult>>();
	private static ChatBotCacheSerializer serializer = new ChatBotCacheSerializer();
	//protected SearchResultsScorer scorer = new SearchResultsScorer();
	
	private static QnAPairsSearcherBlender   instance;
	public synchronized static QnAPairsSearcherBlender   getInstance() {
	    if (instance == null)
	      instance = new QnAPairsSearcherBlender();
	    query_listOfSearchResults = (Map<String, List<ChatIterationResult>>)serializer.readObject();
	    if (query_listOfSearchResults==null)
	    	query_listOfSearchResults = new HashMap<String, List<ChatIterationResult>>();

	    return instance;
	  }
	
	public List<ChatIterationResult> searchLongQuery(String queryOrig){
		queryOrig = queryOrig.trim();
		List<ChatIterationResult> foundInCache = query_listOfSearchResults.get(queryOrig);
		// temporarily disable cache
	/*	if (foundInCache != null){
			return foundInCache;
		}
	*/	List<ChatIterationResult> addToCache = searchLongQueryUncashed(queryOrig);
		query_listOfSearchResults.put(queryOrig, addToCache );
		// each time new query comes => write the results
		serializer.writeObject(query_listOfSearchResults);
		return addToCache;
	}
	

	public List<ChatIterationResult> searchLongQueryUncashed(String queryOrig){
		// no product query search

		List<ChatIterationResult> chatIterationResults= new ArrayList<ChatIterationResult>();
		// first try faq search
		chatIterationResults = luceneSearcher.runSearchChatIterFormat(queryOrig);
		// if faq search is empty then try bloomberg
	//	if (!chatIterationResults.isEmpty())	
			return chatIterationResults;
		
/*		

		// then try web search
		List<HitBase> results = bSearcher.runSearch(queryOrig, 3), augmResults = new ArrayList<HitBase>() ;
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

				EntityExtractionResult eeRes = extractor.extractEntitiesSubtractOrigQuery(text, queryOrig);
				
				chatIterationResults.add(new ChatIterationResult(currSearchRes, eeRes, text));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return chatIterationResults;
		*/
	}

	

	public static void main(String[] args){
		String queryOrig = 
				"best health care mutual funds";
		QnAPairsSearcherBlender searcher = new QnAPairsSearcherBlender();
		List<ChatIterationResult> res = searcher.searchLongQuery(queryOrig);
		System.out.println(res);
	}

	
}
