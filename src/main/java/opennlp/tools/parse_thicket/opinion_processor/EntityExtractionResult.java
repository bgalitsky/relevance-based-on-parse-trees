package opennlp.tools.parse_thicket.opinion_processor;

import java.util.List;

import opennlp.tools.parse_thicket.ParseTreeNode;
import opennlp.tools.similarity.apps.HitBase;

public class EntityExtractionResult {
	List<List<ParseTreeNode>> extractedNERs;
	List<String> extractedNERWords;
	List<List<ParseTreeNode>> extractedSentimentPhrases;
	List<HitBase> hits;

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

	public void setGossipHits(List<HitBase> hitsForAnEntity) {
		hits = hitsForAnEntity;		
	}

	public List<List<ParseTreeNode>> getExtractedNERs() {
		return extractedNERs;
	}

	public void setExtractedNERs(List<List<ParseTreeNode>> extractedNERs) {
		this.extractedNERs = extractedNERs;
	}

	public List<HitBase> getHits() {
		return hits;
	}

	public void setHits(List<HitBase> hits) {
		this.hits = hits;
	}

	public List<String> getExtractedNERWords() {
		return extractedNERWords;
	}

	public List<List<ParseTreeNode>> getExtractedSentimentPhrases() {
		return extractedSentimentPhrases;
	}



}
