package opennlp.tools.parse_thicket.opinion_processor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import opennlp.tools.jsmlearning.ProfileReaderWriter;
import opennlp.tools.parse_thicket.ParseTreeNode;
import opennlp.tools.similarity.apps.BingQueryRunner;
import opennlp.tools.similarity.apps.HitBase;
import opennlp.tools.similarity.apps.utils.PageFetcher;
import opennlp.tools.similarity.apps.utils.StringDistanceMeasurer;

public class PerformingArtGeniusWebMiner extends GeniusWebMiner{
	protected HighSchoolForAStateListManager schoolNameMgr= new HighSchoolForAStateListManager();
	protected String[] arts = new String[]{"singer", "pianist", "violinist"};
	protected String[] winnerQueries = new String[]{"winner of competition", "concerto winner", "young artist competition", 
			"Orchestra Award", "award winning", "first place", "1st place", "2nd place", "second place",
			"3rd place", "third place",
			"musician of the year", "best performer", "best pianist" };
	protected PageFetcher fetcher = new PageFetcher();
	
	protected Set<String> globalListToAvoidDupes = new HashSet<String>();
	
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
				for(String a: arts)
					for(String w: winnerQueries ){
						String query = school +" "+ w + " " + a;
						List<HitBase> hits = brunner.runSearch(query);
						for(HitBase h: hits){
							String text = h.getTitle() + " " + h.getAbstractText();
							EntityExtractionResult result = neExtractor.extractEntities(text);
							List<String> extractedNames = result.extractedNERWords;
							// remove .com
							if (extractedNames.toString().indexOf('.')>-1)
								continue;
							
							extractedNames.removeAll(globalListToAvoidDupes);
							globalListToAvoidDupes.addAll(extractedNames);

							if (!extractedNames.isEmpty() && extractedNames.get(0)!=null && extractedNames.get(0).split(" ").length>1){
								Boolean isGenuineName = confirmThatFirstLastNameIsNotAnEntityOnItsOwn(extractedNames);
								if (!isGenuineName)
									continue;
								extractedNames = new ArrayList<String>(new HashSet<String>(extractedNames));
								for(String name: extractedNames){
									List<HitBase> gossipsForAnEntity = getGossipsForAnEntity(name, loc[2], a.toLowerCase());
									List<HitBase> getYouTubeHitsAnEntity = getYouTubeHitsAnEntity(name, loc[2], a.toLowerCase());
									gossipsForAnEntity .addAll(getYouTubeHitsAnEntity);
									
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
		//report.add((String[])nodePhrases.toArray(new String[0]));
		for(HitBase h: result.hits){
			report.add(new String[] {h.getTitle(), h.getAbstractText(), h.getUrl()});
		}
		report.add(new String[]{"-------------------------"});	
		ProfileReaderWriter.writeReport(report, "geniusMinedOnTheWebArts2withYTresultsPlusGossips.csv");
	}
	
	protected List<HitBase> getYouTubeHitsAnEntity(String extractedName,
			String location, String genre) {
		String query = "\""+extractedName+"\"" ; //+ " " + genre;
		
		List<HitBase> hits = brunner.runSearch("site:www.youtube.com "+ query), hitsResult = new ArrayList<HitBase>();	
		// no verif for youtube
		for(HitBase h: hits){
			boolean bConfirmed = true;
			String lookup = h.getTitle().toLowerCase(); // + " " + h.getAbstractText()).toLowerCase();
			for(String namePart: extractedName.split(" ")){
				if (lookup.indexOf(namePart.toLowerCase())<0){
					bConfirmed = false;
				}				
			}
			
		//	if (lookup.indexOf(genre)<0)
		//		bConfirmed = false;
			
			
			
			if (bConfirmed)
				hitsResult.add(h);
			// if found a channel, no need to continue
			if (h.getUrl().indexOf("/channel/")>-1)
				break;
			
		}
		
		return hitsResult;
	}
	
	protected List<HitBase> getGossipsForAnEntity(String name,
			String location, String sport) {
		String query =  "\"" + name +   "\" " +
				location+ " "+sport;
		List<HitBase> hits = brunner.runSearch(query), hitsResult = new ArrayList<HitBase>();

		for(HitBase h: hits){
			boolean bConfirmed = true;
			String lookup = (h.getTitle() + " " + h.getAbstractText()).toLowerCase();
			if (lookup.indexOf("wikipedia")>-1)
				continue;
			
			for(String namePart: name.split(" ")){
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
	
	HitBase getLastFMLink(String name){
		String url = "http://www.last.fm/music/"+name.replace(' ', '+');
		String content = fetcher.fetchOrigHTML(url);
		if (content.length()< 3 || content.indexOf("Page Not Found — Last.fm")>0 || content.indexOf("don't have a wiki here")>0)
			return null;
		
		String title = StringUtils.substringBetween(content, "<title>", "</title>");
		HitBase hit = new HitBase();
		hit.setTitle(title);
		hit.setAbstractText(title);
		hit.setUrl(url);
		return hit;
		
	}
	
	HitBase getBandCamp(String name){
		String url = "http://bandcamp.com/search?q="+name.replace(' ', '+');
		String content = fetcher.fetchOrigHTML(url);
		String[] names = name.split(" ");
		for(String n: names){
		  if (content.indexOf(n)<-1)
			  return null;
		}
		/*<div class="subhead">
        
        from Programa 26-10-13 Beatles by the grosos
    
    by Eddie Vedder
</div> */
		try {
			String[] titles = StringUtils.substringsBetween(content, "<div class=\"subhead\">", "</div>");
			String buf = "";
			for(String t: titles){
				buf+=t.trim()+" ";
			}
			buf.trim();
			HitBase hit = new HitBase();
			hit.setTitle(buf);
			hit.setAbstractText(buf);
			hit.setUrl(url);
			return hit;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
	}
	
	HitBase getSoundCloud(String name){
		String url = "http://bandcamp.com/search?q="+name.replace(' ', '+');
		String content = fetcher.fetchOrigHTML(url);
	
		  if (content.indexOf(name)<-1)
			  return null;
	
		// <li><h2><a href="/spencer-martin-cama-o-ekroth">Spencer Martin Ekroth</a></h2></li>
		try {
			String[] titles = StringUtils.substringsBetween(content, "<li><h2><a href=", "</a></h2>");
			String buf = "";
			for(String t: titles){
				t = t.substring(t.indexOf('<'));
				buf+=t+" ";
			}
			buf.trim();
			HitBase hit = new HitBase();
			hit.setTitle(buf);
			hit.setAbstractText(buf);
			hit.setUrl(url);
			return hit;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
	}
	
	
	public static void main(String[] args){
		PerformingArtGeniusWebMiner miner = new PerformingArtGeniusWebMiner();
		miner.mineForGeniousSchools();
	}
}







