package opennlp.tools.chatbot.wrapper;

import java.io.Serializable;
import java.util.List;

import opennlp.tools.chatbot.ClarificationExpressionGenerator;

public class BotResponse implements Serializable{
	public String responseMessage;
	protected ClarificationExpressionGenerator responseObj;
	protected List<String> clarificationOptions;
	
	// A key trip of an answer: - Introduction (what is going to be in an answer)
	//   - Answer conrent   
	// Answer conclusion / list of further options for user to chose
	
	public String preStringIntroducingContent;
	public String mainContent;
	public String postStringEnumerNextOptions;
	
	
	
	public String getResponseMessage() {
		return responseMessage;
	}
	public void setResponseMessage(String responseMessage) {
		this.responseMessage = responseMessage;
	}
	public ClarificationExpressionGenerator getResponseObj() {
		return responseObj;
	}
	public void setResponseObj(ClarificationExpressionGenerator responseObj) {
		this.responseObj = responseObj;
	}
	public List<String> getClarificationOptions() {
		return clarificationOptions;
	}
	public void setClarificationOptions(List<String> clarificationOptions) {
		this.clarificationOptions = clarificationOptions;
	}
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
	public void setSessionId(Object sessionId) {
	    // TODO Auto-generated method stub
	    
    }
	public void setUserId(Object userID) {
	    // TODO Auto-generated method stub
	    
    }
}
