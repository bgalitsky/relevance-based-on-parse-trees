package opennlp.tools.chatbot.qna_pairs_adapter;

import java.util.List;

import opennlp.tools.chatbot.wrapper.BotResponse;

public class QnAPairsBotResponse extends BotResponse {
	private String preMessage;
	private String answer;
	private String postMessage;
	private List<String> templateQuestions;
	private List<String> categories;
	private String clarificationType; // "canonicalQuestions", "topics", "categories", "categories-paths"

	public String getPreMessage() {
		return preMessage;
	}
	public void setPreMessage(String preMessage) {
		this.preMessage = preMessage;
	}
	public String getAnswer() {
		return answer;
	}
	public void setAnswer(String answer) {
		this.answer = answer;
	}
	public String getPostMessage() {
		return postMessage;
	}
	public void setPostMessage(String postMessage) {
		this.postMessage = postMessage;
	}
	public List<String> getTemplateQuestions() {
		return templateQuestions;
	}
	public void setTemplateQuestions(List<String> templateQuestions) {
		this.templateQuestions = templateQuestions;
	}
	public List<String> getCategories() {
		return categories;
	}
	public void setCategories(List<String> categories) {
		this.categories = categories;
	}
	public void setReturnType(String type) {
		clarificationType = type;		
	}
	public String getReturnType(String type) {
		return clarificationType;		
	}

	public String toString(){
		if (clarificationType!=null){
			if (clarificationType.equals("categories-paths") && categories!=null){
				return "Please select categories from the list "+ categories.toString();
			} else if (templateQuestions!=null) {
				return "Please select canonical questions from the list "+ templateQuestions.toString();
			} 
		}
		//return preMessage + " | " + answer + " | " + postMessage;
		return this.responseMessage;

	}

}
