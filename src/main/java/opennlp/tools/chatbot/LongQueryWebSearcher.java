package opennlp.tools.chatbot;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

import opennlp.tools.parse_thicket.opinion_processor.EntityExtractionResult;
import opennlp.tools.similarity.apps.BingQueryRunner;
import opennlp.tools.similarity.apps.HitBase;
import opennlp.tools.similarity.apps.utils.PageFetcher;

public class LongQueryWebSearcher {
	private static Logger LOG = Logger
			.getLogger("opennlp.tools.chatbot.LongQueryWebSearcher");
	private TopicExtractorFromSearchResult extractor = new TopicExtractorFromSearchResult();
	private PageFetcher pFetcher = new PageFetcher();
	private BingQueryRunner bSearcher = new BingQueryRunner();
	private SnippetToParagraphAndSectionHeaderContent paraFormer = new SnippetToParagraphAndSectionHeaderContent();

	public List<ChatIterationResult> searchLongQuery(String queryOrig){
		if (isProductQuery(queryOrig)){
			return searchProductQuery(queryOrig);
		}

		List<ChatIterationResult> chatIterationResults= new ArrayList<ChatIterationResult>();

		List<HitBase> results = bSearcher.runSearch(queryOrig, 6), augmResults = new ArrayList<HitBase>() ;
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
	}

	private boolean isProductQuery(String queryOrig) {
		String[] terms = queryOrig.split(" ");
		for(String t: terms){
			if (!StringUtils.isAlpha(t) || StringUtils.isAllUpperCase(t))
				return true;
		}
		return false;
	}

	public List<ChatIterationResult> searchProductQuery(String queryOrig){
		List<ChatIterationResult> chatIterationResults= new ArrayList<ChatIterationResult>();

		List<HitBase> results = bSearcher.runSearch("site:www.amazon.com "+ queryOrig, 6), augmResults = new ArrayList<HitBase>() ;
		// populate with orig text
		HitBase currSearchRes = results.get(0);
		try {
			String amazonUrl = currSearchRes.getUrl();
			String content = pFetcher.fetchOrigHTML( amazonUrl); //">Technical Details</h2>","\">See More</\""
			String areaWithAttrValues = StringUtils.substringBetween(content, "Technical Details","See More");
			String[] aVareas = areaWithAttrValues.split("\"a-nowrap\"");
			//class="a-nowrap">Battery Average Life
			//		</th><td>330 Photos
			//		</td></tr>
			for(String aVarea: aVareas){
				String attribute = StringUtils.substringBetween(aVarea, ">", "<").trim();
				if (attribute == null || attribute.length()<3 || (!StringUtils.isAlphanumericSpace(attribute ) ))
					continue;
				String value = StringUtils.substringBetween(aVarea, "<td>", "</td>").trim();
				if (value == null || value.length()<1 || (!StringUtils.isAlphanumericSpace(value ) ))
					continue;
				ChatIterationResult result = new ChatIterationResult(currSearchRes, null, areaWithAttrValues);
				result.setParagraph(attribute + " : " + value );
				result.setTitle(attribute);
				chatIterationResults.add(result);
			}
		} catch (Exception e) {
			e.printStackTrace();
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
		String queryOrig = //"can I pay with one credit card for another";
				"Sony DSC RX100 Sensor Digital Camera";
		LongQueryWebSearcher searcher = new LongQueryWebSearcher();
		List<ChatIterationResult> res = searcher.searchLongQuery(queryOrig);
		System.out.println(res);
	}
}
