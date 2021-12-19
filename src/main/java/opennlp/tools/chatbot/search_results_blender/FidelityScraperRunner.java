package opennlp.tools.chatbot.search_results_blender;

import java.util.ArrayList;
import java.util.List;

import opennlp.tools.jsmlearning.ProfileReaderWriter;
import opennlp.tools.parse_thicket.Pair;

public class FidelityScraperRunner {
	private FidelityScraper scraper = new FidelityScraper();
	private ExtractedQaPairsIndexer indexer = new ExtractedQaPairsIndexer();
	
	
	
	
	private static final String[] faqTopics = new String[]{"About Your Account", "Placing Orders",
		"Order Types",	"Margin	Trading", 
		"Restrictions",	"Trade Armor"};
	private static final String domain = "www.fidelity.com";

	public List<Pair<String, String>> scrapeSiteFaqPairs(){
		List<Pair<String, String>> results = new ArrayList<Pair<String, String>>();
		for(String topic: faqTopics){
			results.addAll(
					scraper.getAListOfFAQ_QuestionsAndAnswers(topic+ " faq", domain));
		}
		return results; 
	}
	
	public void indexFAQ(){
		List<String[]> report = new ArrayList<String[]>();
		
		List<Pair<String, String>> pairList = scrapeSiteFaqPairs();
		for(Pair<String, String> p:  pairList){
			indexer.putQuestionAnswerPairIntoIndex(p.getFirst(), p.getSecond());
			report.add(new String[]{p.getFirst(),  p.getSecond()});
		}
		ProfileReaderWriter.writeReport(report, "faqList1All.csv");
		indexer.close();
	}
	
	public static void main(String[] args){
		new FidelityScraperRunner().indexFAQ();
	}
}
