package opennlp.tools.chatbot.text_searcher;

import java.util.HashMap;
import java.util.Map;

import opennlp.tools.chatbot.ChatIterationResult;

public class ChatAnswer extends ChatIterationResult {

	private static final long serialVersionUID = -1565630659114223646L;
	private String phrase;
	private String edu;
	private String docTitle;
	private String sectionTitle;
	private Map<String, String> explanationMap = new HashMap<String, String>();
	private int luceneDocId;
	private Map<String, String> resultantAttrValues = new HashMap<String, String>();
	
	public Map<String, String> getResultantAttrValues() {
		return resultantAttrValues;
	}
	public void setResultantAttrValues(Map<String, String> resultantAttrValues) {
		this.resultantAttrValues = resultantAttrValues;
	}
	public String getPhrase() {
		return phrase;
	}
	public void setPhrase(String phrase) {
		this.phrase = phrase;
	}
	public String getEdu() {
		return edu;
	}
	public void setEdu(String edu) {
		this.edu = edu;
	}
	public String getDocTitle() {
		return docTitle;
	}
	public void setDocTitle(String docTitle) {
		this.docTitle = docTitle;
	}
	public String getSectionTitle() {
		return sectionTitle;
	}
	public void setSectionTitle(String sectionTitle) {
		this.sectionTitle = sectionTitle;
	}
	public Map<String, String> getExplanationMap() {
		return explanationMap;
	}
	public void setExplanationMap(Map<String, String> explanationMap) {
		this.explanationMap = explanationMap;
	}
	public int getLuceneDocId() {
		return luceneDocId;
	}
	public void setLuceneDocId(int luceneDocId) {
		this.luceneDocId = luceneDocId;
	}
	
	public String toString(){
		String res = super.toString() + "\n" +  resultantAttrValues.toString() + " \n ------------------";
		return res;
	}
}
