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

import opennlp.tools.chatbot.wrapper.SearchSessionManagerWrapper;
import opennlp.tools.parse_thicket.opinion_processor.EntityExtractionResult;
import opennlp.tools.similarity.apps.BingQueryRunner;
import opennlp.tools.similarity.apps.HitBase;
import opennlp.tools.similarity.apps.utils.PageFetcher;

public class LongQWebSearcherRst {
	private static Logger LOG = Logger
			.getLogger("opennlp.tools.chatbot.LongQueryWebSearcher");
	protected TopicExtractorFromSearchResultRstMatcher extractor = new TopicExtractorFromSearchResultRstMatcher();
	protected PageFetcher pFetcher = new PageFetcher();
	protected BingQueryRunner bSearcher = new BingQueryRunner();
	protected SnippetToParagraphAndSectionHeaderContent paraFormer = new SnippetToParagraphAndSectionHeaderContent();
	protected Tika tika = new Tika();
	protected int queryType;
	private Map<String, List<ChatIterationResult>> query_listOfSearchResults = new HashMap<String, List<ChatIterationResult>>();
	private ChatBotCacheSerializer serializer = new ChatBotCacheSerializer();
	
	private static LongQWebSearcherRst  instance;
	public synchronized static LongQWebSearcherRst  getInstance() {
	    if (instance == null)
	      instance = new LongQWebSearcherRst ();

	    return instance;
	  }
	
	public TopicExtractorFromSearchResultRstMatcher  getPhraseExtractor(){
		return extractor;
	}
	public List<ChatIterationResult> searchLongQuery(String queryOrig){
		queryOrig = queryOrig.trim();
		List<ChatIterationResult> foundInCache = query_listOfSearchResults.get(queryOrig);
		if (foundInCache != null){
			return foundInCache;
		}
		List<ChatIterationResult> addToCache = searchLongQueryUncashed(queryOrig);
		query_listOfSearchResults.put(queryOrig, addToCache );
		// each time new query comes => write the results
		serializer.writeObject(query_listOfSearchResults);
		return addToCache;
	}
	
	public List<ChatIterationResult> searchLongQueryUncashed(String queryOrig){
		/*if (isProductQuery(queryOrig)){
			try {
	            List<ChatIterationResult> res =  searchProductQuery(queryOrig);
	            return res;
            } catch (Exception e) {
	            LOG.info("failed product page detail extraction. Proceeding with regular extraction path.");
            }
		} */

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
		if (this.queryType!=0)
			return false;
		if (queryOrig.toLowerCase().startsWith("product:"))
			return true;
		
		String[] terms = queryOrig.split(" ");
		for(String t: terms){
			if (!StringUtils.isAlpha(t) || StringUtils.isAllUpperCase(t))
				return true;
		}
		return false;
	}

	public List<ChatIterationResult> searchProductQuery(String queryOrig){
		List<ChatIterationResult> chatIterationResults= new ArrayList<ChatIterationResult>();
		queryOrig = queryOrig.replace("product:", "");		

		List<HitBase> results = bSearcher.runSearch("site:www.amazon.com "+ queryOrig, 6);
		HitBase currSearchRes = results.get(0);
		try {
			String amazonUrl = currSearchRes.getUrl();
			String content = pFetcher.fetchOrigHTML( amazonUrl); //">Technical Details</h2>","\">See More</\""
			String cleanedContent = getCleanedContentFromHTML(content);
			String areaWithAttrValues = StringUtils.substringBetween(content, "Technical Details","See More");
			if (areaWithAttrValues ==null)
				areaWithAttrValues = StringUtils.substringBetween(content, "Technical Details","Product Description");
			
			if (areaWithAttrValues!=null){
				String[] aVareas = areaWithAttrValues.split("\"a-nowrap\"");
				//class="a-nowrap">Battery Average Life
				//		</th><td>330 Photos
				//		</td></tr>
				for(String aVarea: aVareas){
					String attribute = StringUtils.substringBetween(aVarea, ">", "<").trim();
					if (attribute == null || attribute.length()<3 || (!StringUtils.isAlphanumericSpace(attribute ) ))
						continue;
					String value = StringUtils.substringBetween(aVarea, "<td>", "</td>").trim();
					if (value == null || value.length()<1 /*|| (!StringUtils.isAlphanumericSpace(value ) )*/)
						continue;
					value = extractValueFromHTML(value);
					ChatIterationResult result = new ChatIterationResult(currSearchRes, null, areaWithAttrValues);
					result.setParagraph(attribute + " : " + value );
					result.setTitle(attribute);
					result.setPageContentCleaned(cleanedContent);
					chatIterationResults.add(result);
				}
				if (!chatIterationResults.isEmpty())
					return chatIterationResults;
			}
				areaWithAttrValues = StringUtils.substringBetween(content, "Product Information","Additional Information");
				if (areaWithAttrValues ==null)
					areaWithAttrValues = StringUtils.substringBetween(content, "Technical Details","Additional Information");
				if (areaWithAttrValues!=null){
					String[] aVareas = areaWithAttrValues.split("prodDetSectionEntry");
					//class="a-nowrap">Battery Average Life
					//		</th><td>330 Photos
					//		</td></tr>
					for(String aVarea: aVareas){
						String attribute = StringUtils.substringBetween(aVarea, ">", "<").trim();
						if (attribute == null || attribute.length()<3 /*|| (!StringUtils.isAlphanumericSpace(attribute ) )*/)
							continue;
						String value = StringUtils.substringBetween(aVarea, "<td", "</td>").trim();
						if (value == null || value.length()<1 /*|| (!StringUtils.isAlphanumericSpace(value ) )*/)
							continue;
						value = extractValueFromHTML(value);
						ChatIterationResult result = new ChatIterationResult(currSearchRes, null, areaWithAttrValues);
						result.setParagraph(attribute + " : " + value );
						result.setTitle(attribute);
						result.setPageContentCleaned(cleanedContent);
						chatIterationResults.add(result);
						
					}
					if (!chatIterationResults.isEmpty())
						return chatIterationResults;
				}
				areaWithAttrValues = StringUtils.substringBetween(content, "Product Information","Customer Reviews");
				if (areaWithAttrValues==null)
					areaWithAttrValues = StringUtils.substringBetween(content, "Product Information",
							"Sponsored Products Related To");
				if (areaWithAttrValues==null)
					areaWithAttrValues = StringUtils.substringBetween(content, "Product Information",
							"Frequently Bought Together");
				
				if (areaWithAttrValues!=null){
					String[] aVareas = areaWithAttrValues.split("prodDetSectionEntry");
					if (aVareas==null || aVareas.length<2)
						//<tr><td class="label">Size</td><td class="value">16-Inch Small/Medium</td></tr>
						aVareas = areaWithAttrValues.split("class=\"label\"");
					//class="a-nowrap">Battery Average Life
					//		</th><td>330 Photos
					//		</td></tr>
					for(String aVarea: aVareas){
						String attribute = StringUtils.substringBetween(aVarea, ">", "<");
						if (attribute == null || attribute.length()<3 /*|| (!StringUtils.isAlphanumericSpace(attribute ) )*/)
							continue;
						attribute = attribute.trim(); 
						String value = StringUtils.substringBetween(aVarea, "<td", "</td>");
						if (value == null || value.length()<1 /*|| (!StringUtils.isAlphanumericSpace(value ) )*/)
							continue;
						value = extractValueFromHTML(value);
						ChatIterationResult result = new ChatIterationResult(currSearchRes, null, areaWithAttrValues);
						result.setParagraph(attribute + " : " + value );
						result.setTitle(attribute);
						result.setPageContentCleaned(cleanedContent);
						chatIterationResults.add(result);
					}
					if (!chatIterationResults.isEmpty())
						return chatIterationResults;
				}
					areaWithAttrValues = StringUtils.substringBetween(content, "\">Shop now</a>","text/javascript");
					if (areaWithAttrValues!=null){
						String[] aVareas = areaWithAttrValues.split("attribute\"");
						//class="a-nowrap">Battery Average Life
						//		</th><td>330 Photos
						//		</td></tr>
						for(String aVarea: aVareas){
							String attribute = StringUtils.substringBetween(aVarea, ">", "<").trim();
							if (attribute == null || attribute.length()<3 )
								continue;
							String value = StringUtils.substringBetween(aVarea, "<td", "</td>").trim();
							if (value == null || value.length()<1 /*|| (!StringUtils.isAlphanumericSpace(value ) )*/)
								continue;
							value = extractValueFromHTML(value);
										
							ChatIterationResult result = new ChatIterationResult(currSearchRes, null, areaWithAttrValues);
							result.setParagraph(attribute + " : " + value );
							result.setTitle(attribute);
							result.setPageContentCleaned(cleanedContent);
							chatIterationResults.add(result);
						}
						if (!chatIterationResults.isEmpty())
							return chatIterationResults;	
					}
						areaWithAttrValues = StringUtils.substringBetween(content, "Product Details","Sponsored Products Related");
						if (areaWithAttrValues!=null){
							//<li><b>Domestic Shipping: </b>Item can be shipped within U.S.</li>
							String[] aVareas = areaWithAttrValues.split("<li>");

							for(String aVarea: aVareas){
								String attribute = StringUtils.substringBetween(aVarea, "<b>", ":").trim();
								if (attribute == null || attribute.length()<3 )
									continue;
								String value = StringUtils.substringBetween(aVarea, "</b>", "</li>").trim();
								if (value == null || value.length()<1)
									continue;
								value = extractValueFromHTML(value);
											
								ChatIterationResult result = new ChatIterationResult(currSearchRes, null, areaWithAttrValues);
								result.setParagraph(attribute + " : " + value );
								result.setTitle(attribute);
								result.setPageContentCleaned(cleanedContent);
								chatIterationResults.add(result);
							}
							if (!chatIterationResults.isEmpty())
								return chatIterationResults;	
							
					}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return chatIterationResults;
	}

	private String extractValueFromHTML(String value){
		int posStart = value.indexOf("\">");
		if (posStart>0){
			value = value.substring(posStart+2, value.length());
		}
		String valueReduced = StringUtils.substringBetween(value, ">", "<");
		if (valueReduced!=null && valueReduced.length()>3)
			value = valueReduced;
		value = value.trim();
		return value;
	}

	protected String combineSentences(List<String> originalSentences) {
		StringBuffer res = new StringBuffer();
		for(String s: originalSentences){
			res.append(s+" ");
		}
		return res.toString();
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
	
	private String getCleanedContentFromHTML(String rawContent){
	String content = null;

		try {
			InputStream stream = new ByteArrayInputStream(rawContent.getBytes(StandardCharsets.UTF_8));
			content = tika.parseToString(stream);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TikaException e) {
			e.printStackTrace();
		}
		return content;
	}
	public static void main(String[] args){
		String[] products = new String[] {
				"moon eclipse"
				/*"Sony DSC RX100 Sensor Digital Camera",
				"Acer V173 Djb 17-Inch LCD Monitor",
				"CUK HP 15z White Silver Student Notebook",
				"LG 24UD58-B 24-Inch 4K UHD IPS Monitor", 
				"Kindle E-reader - Black Glare-Free Touchscreen Display",
				"HP OfficeJet Pro 6968 Wireless All-in-One Photo Printer ",
		"Brother FAX-2840 High Speed Mono Laser Fax Machine",
				"Dell Inspiron 24 3000 Series All-In-One",
				"Crucial 8GB Single DDR3L  Laptop Memory", 
				"Avera 32AER10N LED-LCD HDTV",
				"Escort Passport 9500IX Radar Detector", 
				"Motorola Moto E Android Prepaid Phone",
				"PUBLIC Bikes Women C1 Dutch  City Bike",
				"Schwinn Women's Wayfare Hybrid Bike",
				"Outmate 6 pcs Aluminum D-ring Locking Carabiner",
				"Venture's Pal Lightweight Packable Durable Hiking Backpack",
			"Triple Eight Helmet with Sweatsaver Liner 123",
				"Sony HD Video Recording HDRCX405"*/
		};
		LongQWebSearcherRst searcher = new LongQWebSearcherRst();
		for(String q: products){
		List<ChatIterationResult> res = searcher.searchLongQuery(q);
		System.out.println(q + "\n"+  res + "\n\n");
		}
	}

	public void setQueryType(int queryType) {
	    this.queryType = queryType;
	    
    }
}
