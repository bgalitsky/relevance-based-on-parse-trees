package opennlp.tools.chatbot;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import opennlp.tools.apps.relevanceVocabs.SentimentVocab;
import opennlp.tools.parse_thicket.ParseThicket;
import opennlp.tools.parse_thicket.ParseTreeNode;
import opennlp.tools.parse_thicket.matching.Matcher;
import opennlp.tools.parse_thicket.matching.PT2ThicketPhraseBuilder;
import opennlp.tools.parse_thicket.opinion_processor.EntityExtractionResult;
import opennlp.tools.similarity.apps.utils.Pair;
import opennlp.tools.textsimilarity.TextProcessor;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;


public class TopicExtractorFromSearchResult {
	private static Matcher matcher;
	private static Logger LOG = Logger
			.getLogger("opennlp.tools.chatbot.TopicExtractorFromSearchResult");
	// sentiment vocabulary for phrase under the focus of sentiment
	SentimentVocab sVocab = SentimentVocab.getInstance();
	//This is used to create an XML with phrases. The same class for acro  & phrases

	// reusing already instantiated matcher
	public TopicExtractorFromSearchResult(Matcher matcherFromTK) {
	    matcher =matcherFromTK;
    }

	public TopicExtractorFromSearchResult() {
	    matcher = new Matcher();
    }

	public EntityExtractionResult extractEntities(String para){
		EntityExtractionResult result = new EntityExtractionResult();
		List<String> extractedNerPhrasesStr = new ArrayList<String>(), 
				extractedNerExactStr = new ArrayList<String>(),
				extractedSentimentPhrasesStr = 
				new ArrayList<String>(), extractedNONSentimentPhrasesStr = 
				new ArrayList<String>(), extractedNerPhraseTags = new ArrayList<String>();
		// no need to change to extract more/less phrases
		ParseThicket pt = matcher.buildParseThicketFromTextWithRST(para);

		List<List<ParseTreeNode>> extractedSentimentPhrases = new ArrayList<List<ParseTreeNode>>(), 
				extractedNONSentimentPhrases = new ArrayList<List<ParseTreeNode>>(),
				extractedNerPhrases = new ArrayList<List<ParseTreeNode>>(),
						extractedNerExactPhrases= new ArrayList<List<ParseTreeNode>>();
		//TODO document examples / cases for each rule
		// now we extract phrases
		List<List<ParseTreeNode>> phrases = pt.getPhrases();
		List<Float> sentimentProfile = pt.getSentimentProfile();
		for(List<ParseTreeNode> phrase: phrases){

			// find a noun phrase under sentiment
			boolean bAccept = true, bNER = false;

			String phraseStr = asString(phrase);


			if (!phrase.get(0).getPhraseType().equals("NP") && !phrase.get(0).getPhraseType().equals("VP") )	
				bAccept = false;

			boolean bSentiment = false;
			for(ParseTreeNode word: phrase){
				if (sVocab.isSentimentWord(word.getWord())){
					bSentiment=true;
					break;
				}
			}

			String nerTagConfirmed = null;
			for(ParseTreeNode word: phrase){
				// no Named Entity
				String nerTag = isNERforPhraseExtraction(word);
				if (nerTag!=null){
					bNER = true;
					nerTagConfirmed = nerTag;
				}

				// no numbers nor prepositions
				if (word.getPos().startsWith("CD") /*|| word.getPos().indexOf("PRP")>-1*/ )
					bAccept = false;
			}
			if (!bAccept)
				continue;
			// was 7 -> 2
			if (phrase.size()>7 || phrase.size()<2)
				bAccept = false;

			if (phrase.get(0).getPos().equals("DT") && phrase.size()<3)
				bAccept = false;
			if (!bAccept)
				continue;

			String cleanedPhraseStr = cleanPhraseString(phraseStr);
			if (cleanedPhraseStr==null)
				bAccept = false;

			if (bAccept){
				if (bNER){
					extractedNerPhrases.add(phrase);
					extractedNerPhrasesStr.add(phraseStr);
					extractedNerPhraseTags.add(nerTagConfirmed );
					// forming exact NER
					List<ParseTreeNode> phraseNER_exact = new ArrayList<ParseTreeNode>();
					String nerExactStr = "";
					for(ParseTreeNode word: phrase){
						String ner = isNERforPhraseExtraction(word);
						if (ner!=null && ner.equals(nerTagConfirmed)){
							phraseNER_exact.add(word);
							nerExactStr+=" "+word.getWord();
						}
					}
					nerExactStr.trim();
					extractedNerExactPhrases.add(phraseNER_exact);
					extractedNerExactStr.add(nerExactStr);
				}
				else if (bSentiment) {
					extractedSentimentPhrasesStr.add(cleanedPhraseStr);					
					extractedSentimentPhrases.add(phrase);
				} else {
					extractedNONSentimentPhrasesStr.add(cleanedPhraseStr);					
					extractedNONSentimentPhrases.add(phrase);
				}
			}
		} 

		result.setExtractedSentimentPhrases(extractedSentimentPhrases);
		result.setExtractedSentimentPhrasesStr(extractedSentimentPhrasesStr);

		result.setExtractedNONSentimentPhrases(extractedNONSentimentPhrases);
		result.setExtractedNONSentimentPhrasesStr(extractedNONSentimentPhrasesStr);
		
		result.setExtractedNerPhrases(extractedNerPhrases);
		result.setExtractedNerPhrasesStr(extractedNerPhrasesStr);
		result.setExtractedNerPhraseTags(extractedNerPhraseTags);
		
		result.setExtractedNerExactPhrases(extractedNerExactPhrases);
		result.setExtractedNerExactStr(extractedNerExactStr);

		result.setSentimentProfile(sentimentProfile );

		return result;
	}

	public EntityExtractionResult extractEntitiesSubtractOrigQuery(String para, String query){
		EntityExtractionResult result = new EntityExtractionResult();
		List<String> extractedNerPhrasesStr = new ArrayList<String>(), 
				extractedNerExactStr = new ArrayList<String>(),
				extractedSentimentPhrasesStr = 
				new ArrayList<String>(), extractedNONSentimentPhrasesStr = 
				new ArrayList<String>(), extractedNerPhraseTags = new ArrayList<String>();
		
		// Simple approach  - just subtract as keywords
		List<String> queryKeywords = TextProcessor.fastTokenize(query.toLowerCase(), false);
		
		// no need to change to extract more/less phrases
		ParseThicket pt = matcher.buildParseThicketFromTextWithRST(para);

		List<List<ParseTreeNode>> extractedSentimentPhrases = new ArrayList<List<ParseTreeNode>>(), 
				extractedNONSentimentPhrases = new ArrayList<List<ParseTreeNode>>(),
				extractedNerPhrases = new ArrayList<List<ParseTreeNode>>(),
						extractedNerExactPhrases= new ArrayList<List<ParseTreeNode>>();
		//TODO document examples / cases for each rule
		// now we extract phrases
		List<List<ParseTreeNode>> phrases = pt.getPhrases();
		List<Float> sentimentProfile = pt.getSentimentProfile();
		for(List<ParseTreeNode> phrase: phrases){

			// find a noun phrase under sentiment
			boolean bAccept = true, bNER = false;

			String phraseStr = asString(phrase);


			if (!phrase.get(0).getPhraseType().equals("NP") && !phrase.get(0).getPhraseType().equals("VP") )	
				bAccept = false;

			if (isCoveredByQueryPhrase(phrase, queryKeywords )){
				bAccept = false;
			}
			boolean bSentiment = false;
			for(ParseTreeNode word: phrase){
				if (sVocab.isSentimentWord(word.getWord())){
					bSentiment=true;
					break;
				}
			}

			String nerTagConfirmed = null;
			for(ParseTreeNode word: phrase){
				// no Named Entity
				String nerTag = isNERforPhraseExtraction(word);
				if (nerTag!=null){
					bNER = true;
					nerTagConfirmed = nerTag;
				}

				// no numbers nor prepositions
				if (word.getPos().startsWith("CD") /*|| word.getPos().indexOf("PRP")>-1*/ )
					bAccept = false;
			}
			if (!bAccept)
				continue;
			// was 7 -> 2
			if (phrase.size()>7 || phrase.size()<2)
				bAccept = false;

			//if (phrase.get(0).getPos().equals("DT") && phrase.size()<3)
			//	bAccept = false;
			
			if (!bAccept)
				continue;

			String cleanedPhraseStr = cleanPhraseString(phraseStr);
			if (cleanedPhraseStr==null)
				bAccept = false;

			if (bAccept){
				if (bNER){
					extractedNerPhrases.add(phrase);
					extractedNerPhrasesStr.add(phraseStr);
					extractedNerPhraseTags.add(nerTagConfirmed );
					// forming exact NER
					List<ParseTreeNode> phraseNER_exact = new ArrayList<ParseTreeNode>();
					String nerExactStr = "";
					for(ParseTreeNode word: phrase){
						String ner = isNERforPhraseExtraction(word);
						if (ner!=null && ner.equals(nerTagConfirmed)){
							phraseNER_exact.add(word);
							nerExactStr+=" "+word.getWord();
						}
					}
					nerExactStr.trim();
					extractedNerExactPhrases.add(phraseNER_exact);
					extractedNerExactStr.add(nerExactStr);
				}
				else if (bSentiment) {
					extractedSentimentPhrasesStr.add(cleanedPhraseStr);					
					extractedSentimentPhrases.add(phrase);
				} else {
					extractedNONSentimentPhrasesStr.add(cleanedPhraseStr);					
					extractedNONSentimentPhrases.add(phrase);
				}
			}
		} 

		result.setExtractedSentimentPhrases(extractedSentimentPhrases);
		result.setExtractedSentimentPhrasesStr(extractedSentimentPhrasesStr);

		result.setExtractedNONSentimentPhrases(extractedNONSentimentPhrases);
		result.setExtractedNONSentimentPhrasesStr(extractedNONSentimentPhrasesStr);
		
		result.setExtractedNerPhrases(extractedNerPhrases);
		result.setExtractedNerPhrasesStr(extractedNerPhrasesStr);
		result.setExtractedNerPhraseTags(extractedNerPhraseTags);
		
		result.setExtractedNerExactPhrases(extractedNerExactPhrases);
		result.setExtractedNerExactStr(extractedNerExactStr);

		result.setSentimentProfile(sentimentProfile );

		return result;
	}





	private boolean isCoveredByQueryPhrase(List<ParseTreeNode> phrase, List<String> queryKeywords) {
		List<ParseTreeNode> phraseRemaining = new ArrayList<ParseTreeNode>();
		for(ParseTreeNode p: phrase){
			if (!queryKeywords.contains(p.getWord().toLowerCase())){
				phraseRemaining.add(p);
			} /*else {
				System.out.println("found match: "+ queryKeywords + " => " + p.getWord().toLowerCase());
			}*/
		}
		if (phraseRemaining.size()>1)
			return false;
		else
			return true;
    }

	private String cleanPhraseString(String phraseStr) {
		String p = phraseStr.toLowerCase();

		if (p.startsWith("*") || p.startsWith("&") || p.startsWith("$"))
			return null;

		if (p.startsWith("this ") || p.startsWith("other "))
			return null;

		if (p.startsWith("a "))
			p = p.substring(2, p.length());
		if (p.startsWith("the "))
			p = p.substring(4, p.length());
		if (p.startsWith(", "))
			p = p.substring(2, p.length());

		return p;
	}

	private String asString(List<ParseTreeNode> phrase) {
		String buf = "";
		for(ParseTreeNode p: phrase)
			buf+=p.getWord()+" ";
		buf = buf.replace("-rrb-", "");
		return buf.trim();
	}

	private String isNERforPhraseExtraction(ParseTreeNode word){
		if (word.getNe() == null)
			return null;
		

		if (!(word.getPos().startsWith("NN") || word.getPos().startsWith("PR") || word.getPos().startsWith("IN")|| 
				word.getPos().startsWith("JJ") || word.getPos().startsWith("DT")))
			return null;
				

		if (word.getNe().equals("ORGANIZATION"))
				return "ORGANIZATION";
		if(word.getNe().equals("LOCATION"))
			return "LOCATION";
					
		if(word.getNe().equals("PERSON") ) 
			return "PERSON";
		
		if(word.getNe().equals("MONEY") ) 
			return "MONEY";
		if(word.getNe().equals("DATE") ) 
			return "DATE";
		if(word.getNe().equals("TIME") ) 
			return "TIME";

		return null;

	}
}

/*
 * Naïve  sentiment prediction systems work just by looking at words in isolation, giving positive points for positive words and negative points for negative words and then summing up these points. That way, the order of words is ignored and important information is lost. The deep learning model of (Socher et al 2013) builds a representation of whole sentences based on the sentence structure. It computes the sentiment based on how words compose the meaning of longer phrases. However, in most applications just taking individual sentences into account do not give accurate results and rhetoric information needs to be taken into account to determine the overall sentiment of a paragraph and then back to the individual sentence level.
 */

