package opennlp.tools.chatbot.search_results_blender;

import opennlp.tools.chatbot.TopicExtractorFromSearchResult;
import opennlp.tools.chatbot.TopicExtractorFromSearchResultRstMatcher;
import opennlp.tools.parse_thicket.opinion_processor.EntityExtractionResult;

public class AnaphoraProcessor {
	protected static final String[] anaphoraPrepositions = new String[]{
	"this ", "that ", "these ", "them ", "those ", "it ", "its "
	};
			
	public static boolean isAnaphoraQuery(String query) {
		for(String anaphoraPreposition: anaphoraPrepositions){
			if (query.indexOf(anaphoraPreposition)>-1){
				return true;
			}
		}
		return false;
	}
	
	public static String substituteAnaphora(String previuosQuery, String query){
		query = query + " "; // to match PRP ening with space
		for(String anaphoraPreposition: anaphoraPrepositions){
			if (query.indexOf(anaphoraPreposition)>-1){
				return query.replace(anaphoraPreposition, " "+previuosQuery+" ");
			}
		}
		// failed substitution
		return query;
	}
	
	public static String substituteAnaphoraExtractPhrase(String previuosQuery, String query, TopicExtractorFromSearchResult extractor){
		EntityExtractionResult eer = extractor.extractEntities(previuosQuery);
		query = query + " "; // to match PRP ening with space
		
		String extractedPhrase = null;
		if (!eer.getExtractedNerExactStr().isEmpty())
			extractedPhrase = eer.getExtractedNerExactStr().get(0);
		else if (!eer.getExtractedNONSentimentPhrasesStr().isEmpty()){
			for(int i=0; i< eer.getExtractedNONSentimentPhrases().size(); i++){
				if (eer.getExtractedNONSentimentPhrases().get(i).get(0).getPhraseType().startsWith("NP")){
					extractedPhrase = eer.getExtractedNONSentimentPhrasesStr().get(i);
					break;
				}
			}
		}
		else if (!eer.getExtractedSentimentPhrasesStr().isEmpty())
			extractedPhrase = eer.getExtractedSentimentPhrasesStr().get(0);
		
		
		for(String anaphoraPreposition: anaphoraPrepositions){
			if (query.indexOf(anaphoraPreposition)>-1){
				return query.replace(anaphoraPreposition, " "+extractedPhrase+" ");
			}
		}
		// failed substitution
		return query;
	}
	
	public static String substituteAnaphoraExtractPhraseRstMatcher(String previuosQuery, String query, TopicExtractorFromSearchResultRstMatcher extractor){
		EntityExtractionResult eer = extractor.extractEntities(previuosQuery);
		query = query + " "; // to match PRP ening with space
		
		String extractedPhrase = null;
		if (!eer.getExtractedNerExactStr().isEmpty())
			extractedPhrase = eer.getExtractedNerExactStr().get(0);
		else if (!eer.getExtractedNONSentimentPhrasesStr().isEmpty()){
			for(int i=0; i< eer.getExtractedNONSentimentPhrases().size(); i++){
				if (eer.getExtractedNONSentimentPhrases().get(i).get(0).getPhraseType().startsWith("NP")){
					extractedPhrase = eer.getExtractedNONSentimentPhrasesStr().get(i);
					break;
				}
			}
		}
		else if (!eer.getExtractedSentimentPhrasesStr().isEmpty())
			extractedPhrase = eer.getExtractedSentimentPhrasesStr().get(0);
		
		
		for(String anaphoraPreposition: anaphoraPrepositions){
			if (query.indexOf(anaphoraPreposition)>-1){
				return query.replace(anaphoraPreposition, " "+extractedPhrase+" ");
			}
		}
		// failed substitution
		return query;
	}
}
