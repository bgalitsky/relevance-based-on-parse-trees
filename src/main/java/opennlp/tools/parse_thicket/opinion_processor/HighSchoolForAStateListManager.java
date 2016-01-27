package opennlp.tools.parse_thicket.opinion_processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import opennlp.tools.similarity.apps.utils.PageFetcher;


public class HighSchoolForAStateListManager {
	public Map<String, List<String>> state_ListOfSchools =  new HashMap<String, List<String>>();
	PageFetcher fetcher = new PageFetcher();
	private static String prefix = "https://en.wikipedia.org/wiki/List_of_high_schools_in_";

	public List<String> getAListOfHighSchoolsForAState(String state){

		
		List<String> results = state_ListOfSchools.get(state);
		if (results!=null)
			return results;
				
				
	    results =  new ArrayList<String>();
		
		int len = state.length();
		String statePostFix = state.substring(0, 1).toUpperCase()+state.substring(1,len);
		statePostFix = statePostFix.replace(' ', '_');

		/*	int spaceN = statePostFix.indexOf(' ');
		if (spaceN>-1){
			statePostFix = statePostFix.substring(0, spaceN)+ "_" 
					+ statePostFix.substring(spaceN, spaceN+1).toUpperCase() + statePostFix.substring(spaceN+2);
		}
		 */
		String url = prefix + statePostFix;
		String content = fetcher.fetchOrigHTML(url);
		String[] candSchoolNames = StringUtils.substringsBetween(content, "title=\"", "\"");
		for(String s:candSchoolNames){
			if (s.toLowerCase().indexOf("list of")<0 && s.toLowerCase().indexOf("schools")<0 &&
					s.toLowerCase().indexOf("district")<0 && s.toLowerCase().indexOf("page")<0 
					
					){
				if ((s.toLowerCase().indexOf("school")>-1) || (s.toLowerCase().indexOf("academy")>-1) ||
						(s.toLowerCase().indexOf("college")>-1)){
					results.add(s.replace('(', ' ').replace(')', ' '));
				}
			}
		}
		state_ListOfSchools.put(state, results);
		return results;
	}

	public static void main(String[] args){
		List<String> res = 
				new HighSchoolForAStateListManager().getAListOfHighSchoolsForAState("New York");
		System.out.println(res);
		res = 
				new HighSchoolForAStateListManager().getAListOfHighSchoolsForAState("Maryland");
		System.out.println(res);
	}

}
