package opennlp.tools.parse_thicket.opinion_processor;

import java.util.ArrayList;
import java.util.List;

import opennlp.tools.jsmlearning.ProfileReaderWriter;
import opennlp.tools.parse_thicket.ParseTreeNode;
import opennlp.tools.similarity.apps.BingQueryRunner;
import opennlp.tools.similarity.apps.HitBase;
import opennlp.tools.similarity.apps.utils.StringDistanceMeasurer;

public class GeniusWebMiner {
	protected final static String locSource = "/Users/bgalitsky/Documents/relevance-based-on-parse-trees/src/test/resources/opinions/us_counties.csv";;
	PersonExtractor neExtractor = new PersonExtractor();
	List<String[]> locationsForGenuises = null;
	protected BingQueryRunner brunner = new BingQueryRunner();
	protected String[] winnerQueries = new String[]{"winner", "best player", "player of the year", "best athlete"};
	protected String[] sports = new String[]{"baseball", "hokkey", "regby"};
	protected StringDistanceMeasurer meas = new StringDistanceMeasurer();
	protected String[] socialDomains = new String[]{"pages", "facebook", "linkedin", "twitter", "freebase", "imdb", "maxpreps", "wikipedia", "obituaries",
			"stats", "highlights", "bio", "hudl", "hall"};
	
	protected List<String[]> report = new ArrayList<String[]>();
	

	protected static String BING_KEY = "WFoNMM706MMJ5JYfcHaSEDP+faHj3xAxt28CPljUAHA";
	
	
	public GeniusWebMiner(){
		locationsForGenuises = ProfileReaderWriter.readProfiles(locSource);
		locationsForGenuises.remove(0);
	}
	
	public List<EntityExtractionResult> mineForGenious(){
		List<EntityExtractionResult> results = new ArrayList<EntityExtractionResult>();

		
		//for(String[] loc: locationsForGenuises){
		for(int i = locationsForGenuises.size()-1; i>=0; i--){
			String[] loc = locationsForGenuises.get(i);
			for(String s: sports)
			for(String w: winnerQueries ){
				String query = loc[1]+ " "+loc[2] + " High school " + w + " " + s;
				List<HitBase> hits = brunner.runSearch(query);
				for(HitBase h: hits){
					String text = h.getTitle() + " " + h.getAbstractText();
					EntityExtractionResult result = neExtractor.extractEntities(text);
					List<String> extractedName = result.extractedNERWords;
					
					if (!extractedName.isEmpty() && extractedName.get(0)!=null && extractedName.get(0).split(" ").length>1){
						Boolean isGenuineName = confirmThatFirstLastNameIsNotAnEntityOnItsOwn(extractedName);
						if (!isGenuineName)
							continue;
						List<HitBase> gossipsForAnEntity = getGossipsForAnEntity(extractedName, loc[2].toLowerCase(), s.toLowerCase());
						
						if ( !gossipsForAnEntity.isEmpty()){
							result.setGossipHits(gossipsForAnEntity);
							results.add(result);
							extendReport( result, text, query);
						}
					}
				}
			}		
		}	
		return results;
	}
	
	protected Boolean confirmThatFirstLastNameIsNotAnEntityOnItsOwn(
			List<String> extractedName) {
		
		String query = extractedName.toString().replace('[', '"').replace(']', '"') .
				replace(',', ' ').trim();
		List<HitBase> hits = brunner.runSearch(query);
		for(HitBase h: hits){
			String title = h.getTitle();
			// first check if we found facebook/linkedin page
			for (String s: socialDomains){
				if (title.toLowerCase().indexOf(s)>-1)
					return true;
			}
			// then see if it IS an entity on its own
			double dist = meas.measureStringDistanceNoStemming(query, title);
			if (dist>0.8)
				return false;
		}
		return true;
	}

	protected List<HitBase> getGossipsForAnEntity(List<String> extractedName,
			String location, String sport) {
		String query = extractedName.toString().replace('[', '"').replace(']', '"') .
				replace(',', ' ').trim()+ " " + location+ " "+sport;
		
		List<HitBase> hits = brunner.runSearch(query), hitsResult = new ArrayList<HitBase>();
		
		for(HitBase h: hits){
			boolean bConfirmed = true;
			String lookup = (h.getTitle() + " " + h.getAbstractText()).toLowerCase();
			for(String namePart: extractedName){
				if (lookup.indexOf(namePart.toLowerCase())<0){
					bConfirmed = false;
				}
					
			}
			
			if (lookup.indexOf(sport)<0)
				bConfirmed = false;
			
			if (bConfirmed)
				hitsResult.add(h);
			
		}
		
		return hitsResult;
	}

	protected void extendReport(EntityExtractionResult result, String text, String query){

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
		report.add((String[])stringPhrases.toArray(new String[0]));
		report.add((String[])nodePhrases.toArray(new String[0]));
		for(HitBase h: result.hits){
			report.add(new String[] {h.getTitle(), h.getAbstractText()});
		}
		report.add(new String[]{"-------------------------"});	
		ProfileReaderWriter.writeReport(report, "geniusMinedOnTheWebWithGossips2.csv");
	}
	
	
	public static void main(String[] args){
		GeniusWebMiner miner = new GeniusWebMiner();
		miner.mineForGenious();
	}
}
