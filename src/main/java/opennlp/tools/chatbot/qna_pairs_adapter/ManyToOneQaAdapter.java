package opennlp.tools.chatbot.qna_pairs_adapter;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;


import opennlp.tools.jsmlearning.ProfileReaderWriter;


public class ManyToOneQaAdapter {
	// create new index (true)
	protected ExtractedQnAPairsIndexer indexer = new ExtractedQnAPairsIndexer(true);
	protected Tika tika = new Tika();
	protected Map<String, List<String>> synonymExpander = new HashMap<String, List<String>>();
	protected Set<String> synonymExpanderKeys = null, alreadyInList = new HashSet<String>();

	public void importData(String fileName){
		List<String[]> linesEval  = ProfileReaderWriter.readProfiles(fileName );
		// form entries for synonyms in the format : individual => list of synonyms
		formSynonymPairs(linesEval);

		String question = null, answer = null, categoriesStr = null; String[] categories = null;
		for(String[] l: linesEval){
			if (l==null || l.length<2 )
				continue;
			question = l[1]; 
			if (l[2]!=null && l[2].length()>10 )
				answer = l[2];
			if (l.length>2 && l[3]!=null && l[3].length()>10 ){
				categoriesStr = l[3];
				categoriesStr = StringUtils.substringBetween(categoriesStr, "[", "]");
				categories = categoriesStr.split(", ");
			}
			
			// otherwise answer stays the same as previous record
			if (question!=null && question.length()>10 
					&& answer!=null && answer.length()>10){

				List<String> phrasings = applyPhrasingMultiplier(question);
				List<String> categoriesLst = new ArrayList<String>();
				if (categories!=null)
					categoriesLst = Arrays.asList(categories);
					
				for(String phrasing: phrasings){
					//System.out.println(phrasing);
					indexer.putQuestionAnswerPairIntoIndex(phrasing, answer, categoriesLst);
				}
				
			} else {
				//System.err.println("Wrong line "+ Arrays.asList(l)+"\n");
			}
		}
		indexer.close();
	}
	private void formSynonymPairs(List<String[]> linesEval){
		String question = null;
		for(String[] l: linesEval){
			if (l==null || l.length<2 )
				continue;
			question = l[1]; 
			if (question!=null && question.length()>10 ){
				String[] synGroups = StringUtils.substringsBetween(question, "[", "]");
				if (synGroups==null || synGroups.length<1)
					continue;
				for(String group:  synGroups){
					String[] tokens = group.split("/");
					for(String token: tokens){
						List<String> values = synonymExpander.get(token);
						if (values==null){
							values = new ArrayList<String>();
							values.addAll(Arrays.asList(tokens));
							values.remove(token);
							synonymExpander.put(token, values);
						} else {
							values.addAll(Arrays.asList(tokens));
							values.remove(token);
							synonymExpander.put(token, values);
						}
					}
				}

			}
		}
		synonymExpanderKeys = synonymExpander.keySet();
	}
	
	private List<String> applyPhrasingMultiplier(String original){
		List<String> results = new ArrayList<String>();
		original = original + " "; // for matching
		
		for(String token: synonymExpanderKeys){
			if (original.indexOf(token+" ")>-1){ // need to do multiplication
				// clean from multi-
				// only first synonym entry should stay - and it will be replaced by individual synonym
				String[] toDeletes = StringUtils.substringsBetween(original, "/", "]");
				if (toDeletes==null){
					String toAdd = original.replace('/', ' ').replace('[',' ').replace(']',' ').trim();
					if (!alreadyInList.contains(toAdd)){
						results.add(toAdd);
						alreadyInList.add(toAdd);
					}
					continue;
				}
				for(String del: toDeletes){
					original = original.replace(del, " ");
				}
				original = original.replace('/', ' ').replace('[',' ').replace(']',' ').trim();
				
				
				List<String> multiEntries = synonymExpander.get(token);
				for(String entry: multiEntries){
					String toAdd = original.replace(token, entry);
					if (!alreadyInList.contains(toAdd)){
						results.add(toAdd);
						alreadyInList.add(toAdd);
					}
				}
			} 
				
		}
		if (results.isEmpty()){
			String toAdd = original.replace('/', ' ').replace('[',' ').replace(']',' ').trim();
			if (!alreadyInList.contains(toAdd)){
				results.add(toAdd);
				alreadyInList.add(toAdd);
			}
		}
		return results;
	}
	
	public static void main(String[] args) {
		new ManyToOneQaAdapter().importData(System.getProperty("user.dir") + "/src/test/resources/Faq_109740462_en.csv");	
	}
}
