package opennlp.tools.parse_thicket.opinion_processor;

import java.util.List;

import opennlp.tools.parse_thicket.ParseTreeNode;

public class EntityExtractionResult {
	List<List<ParseTreeNode>> extractedNERs;
	List<String> extractedNERWords;
	List<List<ParseTreeNode>> extractedSentimentPhrases;

	public void setExtractedNERWords(List<String> extractedNERWords) {
		this.extractedNERWords = extractedNERWords;	
	}

	public void setExtractedSentimentPhrases(
			List<List<ParseTreeNode>> extractedSentimentPhrases) {
		this. extractedSentimentPhrases =  extractedSentimentPhrases;	
	}

	public void setExtractedNER(List<List<ParseTreeNode>> extractedNERs) {
		this.extractedNERs = extractedNERs;
	}



}
