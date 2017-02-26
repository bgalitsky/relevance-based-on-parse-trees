package opennlp.tools.chatbot.wrapper;

import opennlp.tools.chatbot.ClarificationExpressionGenerator;

public class BotResponse {
	public String responseMessage;
	private ClarificationExpressionGenerator responseObj;
	public String toString(){
		return responseMessage;
	}
	public void setResponseObject(ClarificationExpressionGenerator clarificationExpressionGenerator) {
	   this.responseObj = clarificationExpressionGenerator;
	    
    }
	// assuming first search result is given
	public String getAnswerURL(){
		try {
	        return this.responseObj.getAnswerAndClarificationOptions().get(0).getUrl();
        } catch (Exception e) {
        	// if unavailable return empty
	        return "";
        }
	}
}
