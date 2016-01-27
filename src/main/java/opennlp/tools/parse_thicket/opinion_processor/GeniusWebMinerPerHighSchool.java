package opennlp.tools.parse_thicket.opinion_processor;

import java.util.ArrayList;
import java.util.List;

import opennlp.tools.jsmlearning.ProfileReaderWriter;
import opennlp.tools.parse_thicket.ParseTreeNode;
import opennlp.tools.similarity.apps.BingQueryRunner;
import opennlp.tools.similarity.apps.HitBase;
import opennlp.tools.similarity.apps.utils.StringDistanceMeasurer;

public class GeniusWebMinerPerHighSchool extends GeniusWebMiner{
	protected HighSchoolForAStateListManager schoolNameMgr= new HighSchoolForAStateListManager();

	public List<EntityExtractionResult> mineForGeniousSchools(){
		List<EntityExtractionResult> results = new ArrayList<EntityExtractionResult>();


		//for(String[] loc: locationsForGenuises){
		for(int i = locationsForGenuises.size()-1; i>=0; i--){
			String[] loc = locationsForGenuises.get(i);
			String state = loc[2];
			if (state==null || state.length()<3)
				continue;
			List<String> schools = schoolNameMgr.getAListOfHighSchoolsForAState(state);
			for(String school: schools){
				for(String s: sports)
					for(String w: winnerQueries ){
						String query = school +" "+ w + " " + s;
						List<HitBase> hits = brunner.runSearch(query);
						for(HitBase h: hits){
							String text = h.getTitle() + " " + h.getAbstractText();
							EntityExtractionResult result = neExtractor.extractEntities(text);
							List<String> extractedName = result.extractedNERWords;
							if (extractedName.toString().indexOf('.')>-1)
								continue;

							if (!extractedName.isEmpty() && extractedName.get(0)!=null && extractedName.get(0).split(" ").length>1){
								Boolean isGenuineName = confirmThatFirstLastNameIsNotAnEntityOnItsOwn(extractedName);
								if (!isGenuineName)
									continue;
								List<HitBase> gossipsForAnEntity = getGossipsForAnEntity(extractedName, loc[2], s.toLowerCase());

								if ( !gossipsForAnEntity.isEmpty()){
									result.setGossipHits(gossipsForAnEntity);
									results.add(result);
									extendReportSc( result, text, query);
								}
							}
						}
					}
			}		
		}	
		return results;
	}

	
	protected void extendReportSc(EntityExtractionResult result, String text, String query){

		report.add(new String[]{text});
		report.add(new String[]{"extracted name below: \\/", query});
		report.add((String[])result.extractedNERWords.toArray(new String[0]));
		//report.add((String[])result.extractedSentimentPhrases.toArray(new String[0]));
		List<String> stringPhrases = new ArrayList<String>(),
				nodePhrases = new ArrayList<String>();
		for(List<ParseTreeNode> chList: result.extractedSentimentPhrases){
			String buf = "", nodeBuf="";
			for(ParseTreeNode ch: chList){
				buf+=ch.getWord()+ " ";
				nodeBuf+=ch.toString()+ " ";
			}
			stringPhrases.add(buf.trim());
			nodePhrases.add(nodeBuf.trim());
		}
		//report.add((String[])stringPhrases.toArray(new String[0]));
		/*report.add((String[])nodePhrases.toArray(new String[0]));
		for(HitBase h: result.hits){
			report.add(new String[] {h.getTitle(), h.getAbstractText()});
		}*/
		report.add(new String[]{"-------------------------"});	
		ProfileReaderWriter.writeReport(report, "geniusMinedOnTheWeb4SchThoroughVerif.csv");
	}
	


	public static void main(String[] args){
		GeniusWebMinerPerHighSchool miner = new GeniusWebMinerPerHighSchool();
		miner.mineForGeniousSchools();
	}
}
