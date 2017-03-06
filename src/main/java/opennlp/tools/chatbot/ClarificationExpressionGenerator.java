package opennlp.tools.chatbot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import opennlp.tools.similarity.apps.utils.StringDistanceMeasurer;
import opennlp.tools.textsimilarity.TextProcessor;

import org.apache.commons.lang.StringUtils;

public class ClarificationExpressionGenerator {
	private static Logger LOG = Logger
			.getLogger("opennlp.tools.chatbot.ClarificationExpressionGenerator");
	// session data
	private List<ChatIterationResult> answerAndClarificationOptions;
	public String originalQuestion;
	private int currBestIndex;
	public String clarificationQuery;
	public String latestAnswer;
	public String domain;
	public String currentEntity = null;

	public static Logger getLOG() {
		return LOG;
	}

	public static void setLOG(Logger lOG) {
		LOG = lOG;
	}

	public List<ChatIterationResult> getAnswerAndClarificationOptions() {
		return answerAndClarificationOptions;
	}

	public void setAnswerAndClarificationOptions(List<ChatIterationResult> answerAndClarificationOptions) {
		this.answerAndClarificationOptions = answerAndClarificationOptions;
	}

	public String getOriginalQuestion() {
		return originalQuestion;
	}

	public void setOriginalQuestion(String originalQuestion) {
		this.originalQuestion = originalQuestion;
	}

	public int getCurrBestIndex() {
		return currBestIndex;
	}

	public void setCurrBestIndex(int currBestIndex) {
		this.currBestIndex = currBestIndex;
	}

	public String getClarificationQuery() {
		return clarificationQuery;
	}

	public void setClarificationQuery(String clarificationQuery) {
		this.clarificationQuery = clarificationQuery;
	}

	public String getLatestAnswer() {
		return latestAnswer;
	}

	public void setLatestAnswer(String latestAnswer) {
		this.latestAnswer = latestAnswer;
	}

	public String getCurrentEntity() {
		return currentEntity;
	}

	public void setCurrentEntity(String currentEntity) {
		this.currentEntity = currentEntity;
	}

	public StringDistanceMeasurer getMeas() {
		return meas;
	}

	public void setMeas(StringDistanceMeasurer meas) {
		this.meas = meas;
	}

	public static String[] getClarifPrefixes() {
		return clarifPrefixes;
	}

	public static void setClarifPrefixes(String[] clarifPrefixes) {
		ClarificationExpressionGenerator.clarifPrefixes = clarifPrefixes;
	}

	public String getDomain() {
		return domain;
	}

	// processors for use
	private StringDistanceMeasurer meas = new StringDistanceMeasurer();

	public boolean isEqualToAlreadyGivenAnswer(String answer){
		return meas.measureStringDistance(this.latestAnswer, answer)>0.8;
	}

	private static String[] clarifPrefixes = new String[] { "Possibly associated with :'", "Possible related to :'",
		"Did you mean: '", "Or is it about :'", "Could be connected with: '", "It can be related to :'",
		"Purhaps it is concerning: '"
	};

	public void reset(){
		answerAndClarificationOptions = null;
		originalQuestion = null;
		currBestIndex = -1;
		clarificationQuery = null;
		domain="";
		currentEntity = null;
	}
	
	public String generateClarification(String query, List<ChatIterationResult> searchRes0) {
		if (searchRes0.isEmpty())
			return "I got stuck searching for results, going back ...";

		
		//getEeResult() == null, then not canonical search path
		if (searchRes0.get(0).getEeResult()==null) // not real search results, then expect clarification in title
		{
			String clarification = "I believe your query is about a product. These are the attributes of your interest:\n";
			int count = 0; 
			for(ChatIterationResult extrPhrases: searchRes0){
				 clarification+=extrPhrases.getTitle() + " ["+ count + "] | ";
				 extrPhrases.firstClarificationPhrase = extrPhrases.getTitle();
				 extrPhrases.selectedClarificationPhrase = extrPhrases.getTitle();
				 count++;
			}
			this.originalQuestion = query;
			answerAndClarificationOptions = searchRes0;
			// TODO: we assume no noise keywords in  the query such as "tell me about ..."
			currentEntity = query;
			return clarification;
		}
					
					
		
		int count =0;
		for(ChatIterationResult extrPhrases: searchRes0){
			try {
				List<String> candidates = extrPhrases.getEeResult().getExtractedNONSentimentPhrasesStr();
				if (candidates.isEmpty()) 
					continue;

				// now find the longest and the first one
				String bestPhrase = ""; int maxLen = -1; int bestIndex = -1;
				for(int i=0; i< candidates.size(); i++){
					if (candidates.get(i).length()>5 && meas.measureStringDistance(query, candidates.get(i))<0.7
							&& acceptableClarifPhrase(candidates.get(i)))
					{
						extrPhrases.firstClarificationPhrase=cleanPhrase(candidates.get(i));
						candidates.remove(candidates.get(i));
						break;
					}
				}

				for(String c: candidates){
					if (c.length()>maxLen && 
							(extrPhrases.firstClarificationPhrase==null || 
							meas.measureStringDistance(c, extrPhrases.firstClarificationPhrase)<0.7)
							&& acceptableClarifPhrase(c)
							&& meas.measureStringDistance(c, query)<0.7){
						bestPhrase = c;
						maxLen = c.length();
					}
				}

				extrPhrases.selectedClarificationPhrase=cleanPhrase(bestPhrase);
				searchRes0.set(count, extrPhrases);
				count++;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}


		String clarificationStr = "";
		count = 0;
		if (this.originalQuestion==null) {// very first session
			for(ChatIterationResult extrPhrases: searchRes0){

				try {
					if (extrPhrases.selectedClarificationPhrase!=null && extrPhrases.selectedClarificationPhrase.length()>2){
						int randomIndex = new Float( Math.random()*(float)clarifPrefixes.length).intValue();
						clarificationStr += clarifPrefixes[randomIndex] + extrPhrases.selectedClarificationPhrase + "'["+count + "]. ";
					}
					// adding first clarification phrase
					if (extrPhrases.firstClarificationPhrase!=null && extrPhrases.firstClarificationPhrase.length()>2){
						int randomIndex = new Float( Math.random()*(float)clarifPrefixes.length).intValue();
						clarificationStr += clarifPrefixes[randomIndex] + extrPhrases.firstClarificationPhrase + "'["+count + "]. ";
						if (count % 3== 0)
							clarificationStr +="\n";
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				Map<String, String> sectionHeaderContent = extrPhrases.getSectionHeaderContent();
				if  (sectionHeaderContent!=null && clarificationStr.length()<150){
					Set<String> headers = sectionHeaderContent.keySet();
					if (!headers.isEmpty()){
						clarificationStr+= "\nThese are the other areas of interest:";
						for(String header: headers){
							clarificationStr += header + " | ";
						}
					}
					clarificationStr+= "\n";
				}
				count++;
			} 
		}else {
			for(ChatIterationResult extrPhrases: searchRes0){

				try {
					if (extrPhrases.selectedClarificationPhrase!=null && extrPhrases.selectedClarificationPhrase.length()>2){
						clarificationStr +=  extrPhrases.selectedClarificationPhrase + "'["+count + "]. ";
					}
					// adding first clarification phrase
					if (extrPhrases.firstClarificationPhrase!=null && extrPhrases.firstClarificationPhrase.length()>2){
						clarificationStr +=  extrPhrases.firstClarificationPhrase + "'["+count + "]. ";
						if (count % 3== 0)
							clarificationStr +="\n";
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				Map<String, String> sectionHeaderContent = extrPhrases.getSectionHeaderContent();
				// too short clarification str so far
				if  (sectionHeaderContent!=null && clarificationStr.length()<150
						){
					Set<String> headers = sectionHeaderContent.keySet();
					if (!headers.isEmpty()){
						clarificationStr+= "\nThese are the other areas of interest:";
						for(String header: headers){
							clarificationStr += header + " | ";
						}
					}
					clarificationStr+= "\n";
				}

				count++;
			} 
		}
		this.originalQuestion = query;
		answerAndClarificationOptions = searchRes0;
		return clarificationStr;
	}

	private String cleanPhrase(String phrase){
		//TODO further cleaning
		return phrase.replace("-rrb-", "").replace("-lrb-", "");
	}
	/*
	private List<String> selectBestPhrases(List<String> candidates){
	// now find the longest and the first one
				String bestPhrase = ""; int maxLen = -1; int bestIndex = -1;
				if (candidates.get(0).length()>5)
					extrPhrases.firstClarificationPhrase=candidates.get(0);
				candidates.remove(extrPhrases.firstClarificationPhrase);

				for(String c: candidates){
					try {
		                if (c.length()>maxLen && 
		                		meas.measureStringDistance(c, extrPhrases.firstClarificationPhrase)<0.8){
		                	bestPhrase = c;
		                	maxLen = c.length();
		                }
	                } catch (Exception e) {
		                e.printStackTrace();
	                }
				}
	 */

	/*
	 * , How to pay my credit card bill with another credit card ... => []
[my credit card bill]
, Can You Use One Credit Card to Pay Another Credit Card Bill? => []
[]
, Can You Pay a Credit Card With a Credit Card? | Credit.com => []
[any of these scenarios, these scenarios, apply to you, your credit cards, to pay another credit card bill]
, Can I pay credit card with another credit card? YEP! => []
[way, can pay my credit card bill, pay my credit card bill, my credit card bill, wants to get their credit, to get their credit, get their credit]
, How to Pay a Credit Card Bill With Another Credit Card ... => []
[to pay off another, balance transfers, easiest way, is via a balance transfer, balance transfer, cash advances you, cash advances, taking a cash advance from another card, cash advance]
, Should I use one credit card to pay off another? | Credit ... => []
[have a question, question, have advice to share, advice to share, to share, combined knowledge and experience, everyone in the credit karma community, credit karma community]
, can i pay off my 1 credit card with another? | Yahoo Answers => []
[small bill, same time, my capitalone card, min pay, received it, my capitalone card .]
, Can You Pay a Credit Card With a Credit Card? | Fox Business => []
[any of these scenarios, these scenarios, apply to you, your credit cards, to pay another credit card bill]
	 */
	//'s
	boolean acceptableClarifPhrase(String ph) {
		if (ph==null)
			return false;

		ph = ph.toLowerCase();

		if (ph.startsWith("our") || ph.startsWith("my") || ph.startsWith("no ") || ph.startsWith("could ")
				|| ph.startsWith("no ") || ph.startsWith("get ") || ph.startsWith("contact us") || ph.indexOf("editorial ")>-1 
				|| ph.startsWith("any ") || ph.indexOf("also like")>-1 ||  ph.indexOf("also like")>-1 || ph.indexOf("clickred")>-1 || 
				ph.startsWith("join ") || ph.indexOf("enjoy the article")>-1  || ph.indexOf("company info")>-1  
				|| ph.indexOf("welcome to")>-1  || ph.indexOf(".")>-1  || ph.indexOf("company info")>-1  
				)
			return false;

		if (ph.length()<20)
			return false;
		if  (ph.indexOf(",")>0 || ph.indexOf("...")>0)
			return false;
		return true;
	}
	public String getBestAvailableCurrentAnswer() {
		if (this.currBestIndex==0 && answerAndClarificationOptions.size()>0)
			return answerAndClarificationOptions.get(1).paragraph;
		else
			return answerAndClarificationOptions.get(0).paragraph;	

	}

	public String matchUserResponseWithGeneratedOptions(String query) {
		this.clarificationQuery = query;
		if (StringUtils.isEmpty(query))
			return answerAndClarificationOptions.get(0).paragraph;

		if (StringUtils.isNumeric(query)){
			int bestIndex = Integer.parseInt(query);
			if (bestIndex < answerAndClarificationOptions.size())
				return answerAndClarificationOptions.get(bestIndex).paragraph;
		}

		// no number is indicated but instead the choice via a phrase
		double bestSim = -1.0; int bestIndex = -1;
		int count = 0;
		String bestCandidate = null;
		for(ChatIterationResult extrPhrases:answerAndClarificationOptions){
			try {
	            List<String> candidates = new ArrayList<String>();
	            candidates.add(extrPhrases.selectedClarificationPhrase);
	            candidates.add(extrPhrases.firstClarificationPhrase);
	            Map<String, String> sectionHeaderContent = extrPhrases.getSectionHeaderContent();
	            if  (sectionHeaderContent!=null){
	            	Set<String> headers = sectionHeaderContent.keySet();
	            	candidates.addAll(headers);
	            }
	            for(String cand: candidates){
	            	if (cand==null)
	            		continue;
	            	double sim = meas.measureStringDistance(query, cand);
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
		// now we need to figure out: whole doc or a section
		try {
	        ChatIterationResult extrPhrases = answerAndClarificationOptions.get(bestIndex);
	        String answer =  extrPhrases.getSectionHeaderContent().get(bestCandidate);
	        if (answer!=null)
	        	return answer;
        } catch (Exception e) {
	       // if not section then the whole doc
        }
		
		return answerAndClarificationOptions.get(bestIndex).paragraph;
	}
	public void setDomain(String domainCurr) {
	    this.domain = domainCurr;	    
    }

	public String searchProductPage(String query){
		int endOfPageSearchQueryPos = -1;
		String[] endOfPageSearchQueryMarker = new String[]{"this","that", " it", "product", "item", "this thing"}; 
		for(String e: endOfPageSearchQueryMarker){
			endOfPageSearchQueryPos = query.indexOf(e);
			if (endOfPageSearchQueryPos>3){
				query = query.substring(0, endOfPageSearchQueryPos);
			}
		}
		String cleanContent = this.answerAndClarificationOptions.get(0).getPageContentCleaned();
		List<String> sents = TextProcessor.splitToSentences(cleanContent);
		double bestSim = -1; String bestS=null;
		for(String s: sents){
			double scoreCurr = meas.measureStringDistance(s, query);
			if (scoreCurr>bestSim){
				bestSim=scoreCurr;
				bestS = s;
			}
		}
		
		String cleanedSent = bestS.replace("   ", " ").replace("  ", " ").replace("  ", " ").replace("  ", " ").replace("  ", " ");
		return bestS;
	}


}

/*
It can be related to :'your card issuer 's payment guidelines'. It can be related to :'miss payments altogether'. 
It can be related to :'payment grace period'. It can be related to :'making a late credit card payment'. 
Or is it about :'your credit card company'. Did you mean: 'will begin charging a late payment fee'. 
Did you mean: 'all the ways you can waste money'. Did you mean: 'to avoid credit card late'. 
Or is it about :'claim interest and debt recovery costs if'. It can be related to :'can drop your credit'. 

 */