package opennlp.tools.chatbot.qna_pairs_adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import opennlp.tools.chatbot.ChatIterationResult;
import opennlp.tools.chatbot.ClarificationExpressionGenerator;
import opennlp.tools.chatbot.search_results_blender.BlenderClarificationExpressionGenerator;
import opennlp.tools.parse_thicket.Pair;
import opennlp.tools.similarity.apps.utils.StringDistanceMeasurer;
import opennlp.tools.similarity.apps.utils.ValueSortMap;
import opennlp.tools.textsimilarity.TextProcessor;

import org.apache.commons.lang.StringUtils;

public class QnAPairsClarificationExpressionGenerator extends BlenderClarificationExpressionGenerator{
	private static Logger LOG = Logger
			.getLogger("oracle.cloud.bots.search_results_blender.BlenderClarificationExpressionGenerator");
	private static final int MIN_N_SEARCH_RESULTS_FOR_CATEGORY_GROUPING = 5;
	private List<ChatIterationResult> answerAndClarificationOptionsCurrent 
	= new ArrayList<ChatIterationResult>();

	public Pair<String, List<String>> generateClarificationPair(String query, List<ChatIterationResult> searchRes0) {
		List<String> results = new ArrayList<String>();
		if (searchRes0.isEmpty())
			return new Pair<String, List<String>>( "I could not find results for this query", results);

		this.originalQuestion = query;
		String clarification = "";
		//getEeResult() == null, then not canonical search path
		if (searchRes0.get(0).getEeResult()==null) // custom search, dont need to do topics
			if (searchRes0.size()<MIN_N_SEARCH_RESULTS_FOR_CATEGORY_GROUPING)			
			{
				for(ChatIterationResult extrPhrases: searchRes0){
					results.add(extrPhrases.getTitle());
					clarification+=extrPhrases.getTitle() //+ " ["+ count + "] "
							+ " | ";
					extrPhrases.firstClarificationPhrase = extrPhrases.getTitle();
					extrPhrases.selectedClarificationPhrase = extrPhrases.getTitle();
				}

				answerAndClarificationOptions = searchRes0;
				// TODO: we assume no noise keywords in  the query such as "tell me about ..."
				this.currentEntity = query;
				return new Pair<String, List<String>>( clarification, results) ;
			} else { // group by categories
				Map<String, Integer> categsCount = new HashMap<String, Integer>();
				for(ChatIterationResult extrPhrases: searchRes0){
					//String catStr = extrPhrases.getSelectedClarificationPhrase();
					// path 
					String catStr = extrPhrases.getFirstClarificationPhrase();
					if (catStr==null || catStr.length()<10)
						continue;
					catStr = StringUtils.substringBetween(catStr ,  "[", "]");
					String[] catStrs = catStr.split(",");
					for(String c: catStrs){
						c = c.trim();
						Integer count = categsCount.get(c);
						if (count==null)
							categsCount.put(c,1);
						else
							categsCount.put(c,count+1);
					}
				}

				Map<String, Integer>  categsCountSorted = ValueSortMap.sortMapByValue(categsCount, false);
				int count = 0;
				for(String key: categsCountSorted.keySet()){
					results.add(key);
					clarification+= key //+" ["+ count + "]"
							+ " | ";
					count++;
				}
				answerAndClarificationOptions = searchRes0;

				this.currentEntity = query;
				return new Pair<String, List<String>>( clarification, results) ;
			}
		// should not be here
		return new Pair<String, List<String>>( "", results) ;

	} 

	public QnAPairsBotResponse generateClarificationViaBotResponse(String query, List<ChatIterationResult> searchRes0) {
		List<String> results = new ArrayList<String>();
		QnAPairsBotResponse clObject = new QnAPairsBotResponse();
		if (searchRes0.isEmpty()){
			clObject.setPreMessage("I could not find results for this query");
			return clObject;
		}

		this.originalQuestion = query;
		String clarification = "";
		//getEeResult() == null, then not canonical search path
		if (searchRes0.get(0).getEeResult()!=null){ // custom search, dont need to do topics
			clObject.setPreMessage("Error in dialog management: searchRes0.get(0).getEeResult()!=null");
			return clObject;
		}
		answerAndClarificationOptions = searchRes0;
		
		

		// group by categories
		Map<String, Integer> categsCount = new HashMap<String, Integer>();
		for(ChatIterationResult extrPhrases: searchRes0){
			//String catStr = extrPhrases.getSelectedClarificationPhrase();
			// path 
			String catStr = extrPhrases.getFirstClarificationPhrase();
			// if problematic try Selected
			if (catStr==null || catStr.length()<10)
				catStr = extrPhrases.getSelectedClarificationPhrase();
			// if still problematic => give up
			if (catStr==null || catStr.length()<10)
				continue;
			catStr = StringUtils.substringBetween(catStr ,  "[", "]");
			if (catStr==null)
				continue;
			String[] catStrs = catStr.split(",");
			if (catStrs==null)
				continue;
			
			for(String c: catStrs){
				c = c.trim();
				Integer count = categsCount.get(c);
				if (count==null)
					categsCount.put(c,1);
				else
					categsCount.put(c,count+1);
			}
		}

		Map<String, Integer>  categsCountSorted = ValueSortMap.sortMapByValue(categsCount, false);
		int count = 0;
		for(String key: categsCountSorted.keySet()){
			results.add(key);
			clarification+= key //+" ["+ count + "]"
					+ " | ";
			count++;
		}
		clObject.setCategories(results);
		answerAndClarificationOptions = searchRes0;
		this.currentEntity = query;
		
		// now process template questions
		results = new ArrayList<String>();
		for(ChatIterationResult extrPhrases: searchRes0){
			results.add(extrPhrases.getTitle());
			clarification+=extrPhrases.getTitle() //+ " ["+ count + "] "
					+ " | ";
			if (searchRes0.size()<MIN_N_SEARCH_RESULTS_FOR_CATEGORY_GROUPING){
			   extrPhrases.firstClarificationPhrase = extrPhrases.getTitle();
			   extrPhrases.selectedClarificationPhrase = extrPhrases.getTitle();
			}
		}

		// TODO: we assume no noise keywords in  the query such as "tell me about ..."
		this.currentEntity = query;
		clObject.setTemplateQuestions(results);
		clObject.setAnswer(clarification);
		clObject.setResponseMessage(clarification);
		
		if (searchRes0.size()<MIN_N_SEARCH_RESULTS_FOR_CATEGORY_GROUPING)	{
			clObject.setReturnType("templateQuestions");
			clObject.setClarificationOptions(clObject.getTemplateQuestions());
		}
		else {
			clObject.setReturnType("categories-paths");
			clObject.setClarificationOptions(clObject.getCategories());
		}

		
		return clObject ;
	} 

	public Pair<String, List<String>> matchUserResponseWithGeneratedOptionsMultipleResults(String query) {
		List<String> results = new ArrayList<String>();

		this.clarificationQuery = query;
		if (StringUtils.isEmpty(query))
			return new Pair<String, List<String>> (answerAndClarificationOptions.get(0).paragraph, results);

		if (StringUtils.isNumeric(query)){
			int bestIndex = Integer.parseInt(query);
			if (bestIndex < answerAndClarificationOptions.size())
				return new Pair<String, List<String>> (answerAndClarificationOptions.get(bestIndex).paragraph, results);
		}

		// no number is indicated but instead the choice via a phrase
		double bestSim = -1.0; int bestIndex = -1;
		int count = 0;
		String bestCandidate = null;
		// first we find the closest match and similarity value for it
		for(ChatIterationResult extrPhrases:answerAndClarificationOptions){
			try {
				List<String> candidates = new ArrayList<String>();
				candidates.add(extrPhrases.selectedClarificationPhrase);
				candidates.add(extrPhrases.getTitle());

				for(String cand: candidates){
					if (cand==null)
						continue;
					double sim = meas.measureStringDistance(query, cand.replace('[',' ').replace(']',' ').replace(',',' '));
					if (sim>bestSim){
						bestSim=sim;
						bestIndex = count;
						bestCandidate = cand;
					}
				}
				count++;
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		this.currBestIndex = bestIndex;
		// now we find candidates with good similarity value 
		float MARGIN = 0.1f;
		answerAndClarificationOptionsCurrent = new ArrayList<ChatIterationResult>();

		for(ChatIterationResult extrPhrases:answerAndClarificationOptions){
			try {
				List<String> candidates = new ArrayList<String>();
				candidates.add(extrPhrases.selectedClarificationPhrase);
				candidates.add(extrPhrases.getTitle());

				for(String cand: candidates){
					if (cand==null)
						continue;
					double sim = meas.measureStringDistance(query, cand.replace('[',' ').replace(']',' ').replace(',',' '));
					if (sim>bestSim-MARGIN){
						bestSim=sim;

						bestCandidate = cand;
						results.add(extrPhrases.getTitle() +" | "+extrPhrases.getSelectedClarificationPhrase() + "\n" + extrPhrases.paragraph
								);
						answerAndClarificationOptionsCurrent .add(extrPhrases);
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		String bestAnswer = "";
		if (
				bestIndex<answerAndClarificationOptions.size())
			bestAnswer = answerAndClarificationOptions.get(bestIndex).paragraph;
		// remove all unselected answers
		answerAndClarificationOptions.clear();
		answerAndClarificationOptions.addAll(answerAndClarificationOptionsCurrent);

		return new Pair<String, List<String>> (bestAnswer, results);
	}
}
