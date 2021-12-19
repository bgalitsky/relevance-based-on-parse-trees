package opennlp.tools.chatbot.search_results_blender;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import opennlp.tools.parse_thicket.Pair;
import opennlp.tools.parse_thicket.matching.Matcher;
import opennlp.tools.similarity.apps.HitBase;
import opennlp.tools.textsimilarity.ParseTreeChunk;
import opennlp.tools.textsimilarity.ParseTreeChunkListScorer;
import opennlp.tools.textsimilarity.TextProcessor;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;

import edu.stanford.nlp.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class SearchResultsScorer {

    Matcher m = new Matcher();
	private static final double MIN_ACCEPTABLE_ANSWER_SCORE = 2.1d;

    public void runTest(List<String> lst) throws Exception
    {
        System.out.println("Enter text: ");
        Scanner scanner = new Scanner(System.in);
        String queryStr = scanner.nextLine();
        if("quit".equals(queryStr))
        {
            return;
        }
        else
        {
            checkLinguisticScores(queryStr,lst);
            runTest(lst);
        }
    }

    public void checkLinguisticScores(String q, List<String> aList) throws Exception
    {   // convert query into list of tokens
    	List<String> queryTokens = TextProcessor.fastTokenize(q.toLowerCase(), false);
    	
    	List<String> shortListedClasses = new ArrayList<String>();
    	for (String ans: aList) {
    		// convert answer class into the list of tokens
    		List<String> classTokens = TextProcessor.fastTokenize(ans.toLowerCase(), false);
    		// do intersection of tokens
    		classTokens.retainAll(queryTokens);
    		int tokenScore = 0;
    		// count significant tokens / no stopwords
    		for(String word: classTokens){
    			if (word.length()>2 && StringUtils.isAlpha(word))
    				tokenScore++;
    		}
    		if (tokenScore>1)
    			 shortListedClasses.add(ans);
    	}
    	// do it again with lower thresh, if too few results
    	if (shortListedClasses.size()<5)
    		for (String ans: aList) {
        		List<String> classTokens = TextProcessor.fastTokenize(ans.toLowerCase(), false);
        		classTokens.retainAll(queryTokens);
        		int tokenScore = 0;
        		for(String word: classTokens){
        			if (word.length()>2 && StringUtils.isAlpha(word))
        				tokenScore++;
        		}
        		if (tokenScore>=1)
        			 shortListedClasses.add(ans);
        	}
    	// if no overlap give up of do the full list 
    	if (shortListedClasses.isEmpty())
    		shortListedClasses = aList;
    		
        ParseTreeChunkListScorer parseTreeChunkListScorer = new ParseTreeChunkListScorer();

        ArrayList<Pair<String,Double>> pairList = new ArrayList<Pair<String,Double>>();

        for (String ans: shortListedClasses) {

            List<List<ParseTreeChunk>> res = m.assessRelevanceCache(q, ans);
            double score1 = parseTreeChunkListScorer.getParseTreeChunkListScoreAggregPhraseType(res);
            Pair<String,Double> p = new Pair<String, Double>(ans, score1);
            pairList.add(p);
        }

        //Collections.sort(pairList, Comparator.comparing(p -> p.getSecond()));

      System.out.println("***** '" + q + "' ******* falls into the following categories: ");
        for (Pair<String, Double> score: pairList) {
            System.out.println("        " + score.getFirst() + ": " + score.getSecond());
        }
    }
    
    public  List<HitBase> filterHitsByLinguisticScore(String q, List<HitBase> aList) throws Exception
    {   // convert query into list of tokens
    	List<String> queryTokens = TextProcessor.fastTokenize(q.toLowerCase(), false);
    	
    	List<HitBase> shortListedHits = new ArrayList<HitBase>();
    	for (HitBase ans: aList) {
    		// convert answer class into the list of tokens
    		List<String> classTokens = TextProcessor.fastTokenize(ans.getTitle().toLowerCase() 
    				+ ans.getAbstractText().toLowerCase(), false);
    		// do intersection of tokens
    		classTokens.retainAll(queryTokens);
    		int tokenScore = 0;
    		// count significant tokens / no stopwords
    		for(String word: classTokens){
    			if (word.length()>2 && StringUtils.isAlpha(word))
    				tokenScore++;
    		}
    		if (tokenScore>1)
    			 shortListedHits.add(ans);
    	}
    	// do it again with lower thresh, if too few results
    	if (shortListedHits.size()<5)
    		for (HitBase ans: aList) {
        		List<String> classTokens = TextProcessor.fastTokenize(ans.getTitle().toLowerCase() 
        				+ ans.getAbstractText().toLowerCase(), false);
        		classTokens.retainAll(queryTokens);
        		int tokenScore = 0;
        		for(String word: classTokens){
        			if (word.length()>2 && StringUtils.isAlpha(word))
        				tokenScore++;
        		}
        		if (tokenScore>=1)
        			 shortListedHits.add(ans);
        	}
    	// if no overlap give up of do the full list 
    	if (shortListedHits.isEmpty())
    		shortListedHits = aList;
    		
        ParseTreeChunkListScorer parseTreeChunkListScorer = new ParseTreeChunkListScorer();

        List<Pair<HitBase,Double>> pairList = new ArrayList<Pair<HitBase,Double>>();
        List<HitBase> results = new ArrayList<HitBase>();
        for (HitBase ans: shortListedHits) {

            List<List<ParseTreeChunk>> res = m.assessRelevanceCache(q, ans.getTitle().toLowerCase() 
    				+ ans.getAbstractText().toLowerCase());
            double score1 = parseTreeChunkListScorer.getParseTreeChunkListScoreAggregPhraseType(res);
            Pair<HitBase,Double> p = new Pair<HitBase, Double>(ans, score1);
            if (score1>MIN_ACCEPTABLE_ANSWER_SCORE )
            	pairList.add(p);
        }

        //Collections.sort(pairList, Comparator.comparing(p -> p.getSecond()));
        for(Pair<HitBase,Double>p:  pairList){
        	results.add(p.getFirst());
        }

    /*  System.out.println("***** '" + q + "' ******* falls into the following categories: ");
        for (Pair<String, Double> score: pairList) {
            System.out.println("        " + score.getFirst() + ": " + score.getSecond());
        }
*/
        return results;
    }
}