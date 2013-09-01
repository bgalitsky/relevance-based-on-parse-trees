package opennlp.tools.parse_thicket.apps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;

import opennlp.tools.similarity.apps.utils.ValueSortMap;

public class MostFrequentWordsFromPageGetter {
	
	public List<String> getMostFrequentWordsInText(String input)
	{
		int maxRes = 4;
		Scanner in = new Scanner(input);
        in.useDelimiter("\\s+");
        Map<String, Integer> words = 
                new HashMap<String, Integer>();
        
        while (in.hasNext()) {
            String word = in.next();
            if (!StringUtils.isAlpha(word) || word.length()<4 )
            	continue;
            
            if (!words.containsKey(word)) {
                words.put(word, 1);
            } else {
                words.put(word, words.get(word) + 1);
            }
        }
        
        words = ValueSortMap.sortMapByValue(words, false);
        List<String> results = new ArrayList<String>(words.keySet());
		
		if (results.size() > maxRes )
			results = results.subList(0, maxRes); // get maxRes elements
       
        return results;
    }
	public List<String> getMostFrequentWordsInTextArr(String[] longestSents) {
		StringBuffer buffer = new StringBuffer();
		for(String s: longestSents){
			buffer.append(s);
		}
		
		return getMostFrequentWordsInText(buffer.toString());
	}
	
}
