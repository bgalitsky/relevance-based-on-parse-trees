package opennlp.tools.chatbot;

import java.util.ArrayList;
import java.util.List;
import opennlp.tools.parse_thicket.kernel_interface.SnippetToParagraphFull;
import opennlp.tools.parse_thicket.opinion_processor.EntityExtractionResult;
import opennlp.tools.similarity.apps.BingQueryRunner;
import opennlp.tools.similarity.apps.HitBase;
import opennlp.tools.similarity.apps.utils.PageFetcher;

public class LongQueryWebSearcher {
	private TopicExtractorFromSearchResult extractor = new TopicExtractorFromSearchResult();
	private PageFetcher pFetcher = new PageFetcher();
	private BingQueryRunner bSearcher = new BingQueryRunner();
	private SnippetToParagraphFull paraFormer = new SnippetToParagraphFull();
	
	public List<ChatIterationResult> searchLongQuery(String queryOrig){
		List<ChatIterationResult> chatIterationResults= new ArrayList<ChatIterationResult>();
		
		List<HitBase> results = bSearcher.runSearch(queryOrig, 6), augmResults = new ArrayList<HitBase>() ;
		// populate with orig text
		for(HitBase currSearchRes: results){
			HitBase augmRes = paraFormer.formTextFromOriginalPageGivenSnippet(currSearchRes);
			augmResults.add(augmRes);
		}
		
		// extract phrases and entities
		for(HitBase currSearchRes: augmResults){
			String text = combineSentences(currSearchRes.getOriginalSentences());
			if (text==null || text.length()<50)
				text = currSearchRes.getAbstractText();
			
			EntityExtractionResult eeRes = extractor.extractEntitiesSubtractOrigQuery(text, queryOrig);
			chatIterationResults.add(new ChatIterationResult(currSearchRes, eeRes, text));
		}
		return chatIterationResults;
	}
	
	
	
	private String combineSentences(List<String> originalSentences) {
	    StringBuffer res = new StringBuffer();
	    for(String s: originalSentences){
	    	res.append(s+" ");
	    }
	    return res.toString();
    }



	public static List<String> cleanListOfSents(List<String> sents) {
		List<String> sentsClean = new ArrayList<String>();
		for (String s : sents) {
			if (s == null || s.trim().length() < 30 || s.length() < 20)
				continue;
			sentsClean.add(s);
		}
		return sentsClean;
	}

	public static String cleanSpacesInCleanedHTMLpage(String pageContent){ //was 4 spaces 
		//was 3 spaces => now back to 2
		//TODO - verify regexp!!
		pageContent = pageContent.trim().replaceAll("([a-z])(\\s{2,3})([A-Z])", "$1. $3")
				.replace("..", ".").replace(". . .", " ").
				replace(".    .",". ").trim(); // sometimes   html breaks are converted into ' ' (two spaces), so
		// we need to put '.'
		return pageContent;
	}

	public static void main(String[] args){
		String queryOrig = "can I pay with one credit card for another";
		LongQueryWebSearcher searcher = new LongQueryWebSearcher();
		List<ChatIterationResult> res = searcher.searchLongQuery(queryOrig);
		System.out.println(res);
	}
}
