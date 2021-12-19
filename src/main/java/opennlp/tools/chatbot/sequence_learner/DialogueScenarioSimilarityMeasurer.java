package opennlp.tools.chatbot.sequence_learner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.stanford.nlp.util.StringUtils;
import opennlp.tools.similarity.apps.utils.StringDistanceMeasurer;

public class DialogueScenarioSimilarityMeasurer {
	private Map<Integer, Integer> mapBetweenTwoScenarios = new HashMap<Integer, Integer>();
	private Map<Integer, Float> similarityForEachUteranceIMap = new HashMap<Integer, Float>();
	
	private StringDistanceMeasurer measurer = new StringDistanceMeasurer();
	
	// TODO - upgrate to linguistic similarity
	// make class for Utterance
	public float utterance2utteranceSimilarity(String ut1, String ut2){
		return new Float(measurer.measureStringDistance(ut1, ut2));
	}
	

	public float similarity(DialogueScenario sc1, DialogueScenario sc2){
		buildMappingFromOneScenarioToAnother(sc1, sc2);
		// form two strings with indexes for utterances for each scenario 
		//and then compute distance between these strings
		Set<Integer> keySet = mapBetweenTwoScenarios.keySet();
		
		String sc1_CodeStr = "", sc2_CodeStr = "";
		for(Integer k: keySet){
			sc1_CodeStr+=k+"";
			sc2_CodeStr+=mapBetweenTwoScenarios.get(k)+"";	
		}
		int editDistSc1_Sc2 = StringUtils.editDistance(sc1_CodeStr, sc1_CodeStr);
		
		//compute sum of similarities for the map
		float totalSim = 0f;
		keySet = similarityForEachUteranceIMap.keySet();
		for(Integer k: keySet){
			totalSim+=
					similarityForEachUteranceIMap.get(k);	
		}
		totalSim = totalSim/(float)similarityForEachUteranceIMap.size();
		
		// divide total averaged similarity for individual utterance 
		//pairs by how maps needed to re-shuffle scenarios
		return totalSim/(float)editDistSc1_Sc2;
	}
	
	private void buildMappingFromOneScenarioToAnother(DialogueScenario sc1, DialogueScenario sc2) {
		List<Integer> alreadyAssociatedUtterances = new ArrayList<Integer>();
		for(int i = 0; i< sc1.utterances.size(); i++){
			// find utterance j in sc2 closest to i
			float minDist = -1; int bestJ = -1;
			for(int j = 0; j< sc2.utterances.size(); i++){
				float currSimilarity = utterance2utteranceSimilarity(sc1.utterances.get(i), 
						sc2.utterances.get(j));
				if (currSimilarity > minDist && !alreadyAssociatedUtterances.contains(j)){
					minDist = currSimilarity;
					bestJ = j;
				}
			}
			// confirm bestJ
			alreadyAssociatedUtterances.add(bestJ);
			mapBetweenTwoScenarios.put(i, bestJ);
			similarityForEachUteranceIMap.put(i, minDist);
			
		}
		
	}
}
