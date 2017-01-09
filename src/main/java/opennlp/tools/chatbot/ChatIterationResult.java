package opennlp.tools.chatbot;

import opennlp.tools.parse_thicket.opinion_processor.EntityExtractionResult;
import opennlp.tools.similarity.apps.HitBase;

public class ChatIterationResult extends HitBase {
	public EntityExtractionResult eeResult;
	public String paragraph;
	public String selectedClarificationPhrase;
	public String firstClarificationPhrase;

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
		
		
		this.eeResult = eeRes;
		this.paragraph = text;
	}
	public ChatIterationResult(HitBase currHit, EntityExtractionResult eeRes) {
		this.setAbstractText(currHit.getAbstractText());
		this.setTitle(currHit.getTitle());
		this.setUrl(currHit.getUrl());
		
		this.eeResult = eeRes;
	}
	public String getParagraph() {
		return this.paragraph;
	}
	
	public String toString(){
		return this.getTitle() + " => " + this.eeResult.getExtractedNerExactStr() + "\n" +
				this.eeResult.getExtractedNONSentimentPhrasesStr()+"\n";
	}
}


