package opennlp.tools.chatbot;

import java.util.List;

import opennlp.tools.similarity.apps.utils.StringDistanceMeasurer;

import org.apache.commons.lang.StringUtils;

public class ClarificationExpressionGenerator {
	// session data
	private List<ChatIterationResult> answerAndClarificationOptions;
	public String originalQuestion;
	
	// processors for use
	private StringDistanceMeasurer meas = new StringDistanceMeasurer();
	
	private static String[] clarifPrefixes = new String[] {
		"Did you mean: '", "Or is it about :'", "It might be connected with: '", "It can be related to :'",
		"Purhaps it is concerning: '"
	};
	
	public void reset(){
		answerAndClarificationOptions = null;
		originalQuestion = null;
	}
	public String generateClarification(String query, List<ChatIterationResult> searchRes0) {
		int count =0;
	    for(ChatIterationResult extrPhrases: searchRes0){
	    	List<String> candidates = extrPhrases.getEeResult().getExtractedNONSentimentPhrasesStr();
	    	// now find the longest
	    	String bestPhrase = ""; int maxLen = -1;
	    	for(String c: candidates){
	    		if (c.length()>maxLen){
	    			bestPhrase = c;
	    			maxLen = c.length();
	    		}
	    	}
	    	extrPhrases.selectedClarificationPhrase=bestPhrase;
	    	searchRes0.set(count, extrPhrases);
	    	count++;
	    }
	    String clarificationStr = "";
	    for(ChatIterationResult extrPhrases: searchRes0){
	    	if (! acceptableClarifPhrase(extrPhrases.selectedClarificationPhrase))
	    		continue;
	    	int randomIndex = new Float( Math.random()*(float)clarifPrefixes.length).intValue();
	    	clarificationStr += clarifPrefixes[randomIndex] + extrPhrases.selectedClarificationPhrase + "'. ";
	    }
	    answerAndClarificationOptions = searchRes0;
	    return clarificationStr;
    }
	
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

	private boolean acceptableClarifPhrase(String ph) {
	   if (ph.length()<30)
		   return false;
	   if  (ph.indexOf(",")>0 || ph.indexOf("...")>0)
		   return false;
	   return true;
    }
	public char[] getBestAvailableCurrentAnswer() {
	    // TODO Auto-generated method stub
	    return null;
    }

	public String matchUserResponseWithGeneratedOptions(String query) {
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