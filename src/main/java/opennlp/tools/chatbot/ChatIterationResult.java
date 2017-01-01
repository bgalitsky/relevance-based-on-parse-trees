package opennlp.tools.chatbot;

import opennlp.tools.parse_thicket.opinion_processor.EntityExtractionResult;
import opennlp.tools.similarity.apps.HitBase;

public class ChatIterationResult extends HitBase {
	public EntityExtractionResult eeResult;
	public HitBase hit;
	public String paragraph;

	public ChatIterationResult(HitBase currHit, EntityExtractionResult eeRes, String text){
		this.hit = currHit;
		this.eeResult = eeRes;
		this.paragraph = text;
	}
	public ChatIterationResult(HitBase currSearchRes, EntityExtractionResult eeRes) {
		this.hit = currSearchRes;
		this.eeResult = eeRes;
	}
	public String getParagraph() {
		return this.paragraph;
	}
	
	public String toString(){
		return this.hit.getTitle() + " => " + this.eeResult.getExtractedNerExactStr() + "\n" +
				this.eeResult.getExtractedNONSentimentPhrasesStr()+"\n";
	}
}


