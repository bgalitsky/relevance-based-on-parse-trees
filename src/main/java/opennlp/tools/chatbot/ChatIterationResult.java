package opennlp.tools.chatbot;

import java.io.Serializable;

import opennlp.tools.parse_thicket.opinion_processor.EntityExtractionResult;
import opennlp.tools.similarity.apps.HitBase;

public class ChatIterationResult extends HitBase implements Serializable{
	public EntityExtractionResult eeResult;
	public String paragraph;
	public String selectedClarificationPhrase;
	public String firstClarificationPhrase;
	public String pageContentCleaned;
	

	public String getFirstClarificationPhrase() {
		return firstClarificationPhrase;
	}
	public void setFirstClarificationPhrase(String firstClarificationPhrase) {
		this.firstClarificationPhrase = firstClarificationPhrase;
	}
	public String getPageContentCleaned() {
		return pageContentCleaned;
	}
	public void setPageContentCleaned(String pageContentCleaned) {
		this.pageContentCleaned = pageContentCleaned;
	}
	public EntityExtractionResult getEeResult() {
		return eeResult;
	}
	public void setEeResult(EntityExtractionResult eeResult) {
		this.eeResult = eeResult;
	}
	public HitBase getHit() {
		return this.getHit();
	}
	public String getSelectedClarificationPhrase() {
		return selectedClarificationPhrase;
	}
	public void setSelectedClarificationPhrase(String selectedClarificationPhrase) {
		this.selectedClarificationPhrase = selectedClarificationPhrase;
	}
	public void setParagraph(String paragraph) {
		this.paragraph = paragraph;
	}
	public ChatIterationResult(HitBase currHit, EntityExtractionResult eeRes, String text){
		this.setAbstractText(currHit.getAbstractText());
		this.setTitle(currHit.getTitle());
		this.setUrl(currHit.getUrl());
		this.setSectionHeaderContent(currHit.getSectionHeaderContent());
		this.setOriginalSentences(currHit.getOriginalSentences());
		this.eeResult = eeRes;
		this.paragraph = text;
	}
	public ChatIterationResult(HitBase currHit, EntityExtractionResult eeRes) {
		this.setAbstractText(currHit.getAbstractText());
		this.setTitle(currHit.getTitle());
		this.setUrl(currHit.getUrl());
		this.setSectionHeaderContent(currHit.getSectionHeaderContent());

		this.eeResult = eeRes;
	}
	public ChatIterationResult() {
	    // TODO Auto-generated constructor stub
    }
	public String getParagraph() {
		return this.paragraph;
	}

	public String toString(){
		if ( this.eeResult!=null && this.eeResult.getExtractedNerExactStr()!=null 
				&& this.eeResult.getExtractedNONSentimentPhrasesStr()!=null)
			return this.getTitle() + " => " + this.eeResult.getExtractedNerExactStr() + "\n" +
			this.eeResult.getExtractedNONSentimentPhrasesStr()+"\n";
		else 
			if (this.paragraph!=null)
				return this.paragraph;
			else 
				return this.getTitle();
	}
}


