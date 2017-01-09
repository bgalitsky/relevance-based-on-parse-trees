package opennlp.tools.chatbot;

import java.util.List;

import opennlp.tools.similarity.apps.utils.StringDistanceMeasurer;

import org.apache.commons.lang.StringUtils;

public class ClarificationExpressionGenerator {
	// session data
	private List<ChatIterationResult> answerAndClarificationOptions;
	public String originalQuestion;
	private int currBestIndex;
	public String clarificationQuery;
	public String latestAnswer;

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
		clarificationQuery = null;;
	}
	public String generateClarification(String query, List<ChatIterationResult> searchRes0) {

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
	            		extrPhrases.firstClarificationPhrase=candidates.get(i);
	            		candidates.remove(extrPhrases.firstClarificationPhrase);
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

	            extrPhrases.selectedClarificationPhrase=bestPhrase;
	            searchRes0.set(count, extrPhrases);
	            count++;
            } catch (Exception e) {
	            e.printStackTrace();
            }
		}


		String clarificationStr = "";
		count = 0;
		if (this.originalQuestion==null) // very first session
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
				count++;
			} else {
				for(ChatIterationResult extrPhrases: searchRes0){
					try {
	                    if (extrPhrases.selectedClarificationPhrase.length()>2)
	                    	clarificationStr +=  " ["+count + "] "+ extrPhrases.selectedClarificationPhrase + " | ";
	                    if (extrPhrases.firstClarificationPhrase.length()>2)
	                    	clarificationStr +=  " ["+count + "] "+ extrPhrases.firstClarificationPhrase + " | ";
	                    count++;
                    } catch (Exception e) {
	                    e.printStackTrace();
                    }
				}
			}
		this.originalQuestion = query;
		answerAndClarificationOptions = searchRes0;
		return clarificationStr;
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
				|| ph.startsWith("no "))
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

		if (StringUtils.isNumeric(query)){
			int bestIndex = Integer.parseInt(query);
			if (bestIndex < answerAndClarificationOptions.size())
				return answerAndClarificationOptions.get(bestIndex).paragraph;
		}

		// no number is indicated but instead the choice via a phrase
		double bestSim = -1.0; int bestIndex = -1;
		int count = 0;
		for(ChatIterationResult extrPhrases:answerAndClarificationOptions){
			double sim = meas.measureStringDistance(query, extrPhrases.selectedClarificationPhrase);
			if (sim>bestSim){
				bestSim=sim;
				bestIndex = count;
			}
			count++;

		}
		this.currBestIndex = bestIndex;
		return answerAndClarificationOptions.get(bestIndex).paragraph;
	}

}

/*
It can be related to :'your card issuer 's payment guidelines'. It can be related to :'miss payments altogether'. 
It can be related to :'payment grace period'. It can be related to :'making a late credit card payment'. 
Or is it about :'your credit card company'. Did you mean: 'will begin charging a late payment fee'. 
Did you mean: 'all the ways you can waste money'. Did you mean: 'to avoid credit card late'. 
Or is it about :'claim interest and debt recovery costs if'. It can be related to :'can drop your credit'. 

 */