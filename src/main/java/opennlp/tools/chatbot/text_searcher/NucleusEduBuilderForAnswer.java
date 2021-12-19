package opennlp.tools.chatbot.text_searcher;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import opennlp.tools.jsmlearning.ProfileReaderWriter;

public class NucleusEduBuilderForAnswer {
	private ElementaryDiscourseUnitsBuilderForIndexing builder = new ElementaryDiscourseUnitsBuilderForIndexing ();

	public void run(){
		List<String[]> lines = ProfileReaderWriter.readProfiles("/Users/bgalitsk/faq_bot/src/test/resources/Fidelity_FAQs.csv"), 
				report = new ArrayList<String[]>();
		lines.remove(0);	
		for(String[] line: lines){
			String answer = line[11].replace("</p><p>", " ").replace("</p>","").replace("<p>","").replace("</li>", " ").replace("<li>", " ").
					replace("<strong>", " ").replace("</strong>", " ").replace("</ul>", " ").replace("<ul>", " ");
			String[] links = StringUtils.substringsBetween(answer, "<a", "</a>");
			if (links!=null){
				for(String l: links){
					answer = answer.replace(l, " ");
				}
				answer = answer.replace("<a"," ").replace("</a>", " ");
			}
			
			links = StringUtils.substringsBetween(answer, "<img", "/>");
			if (links!=null){
				for(String l: links){
					answer = answer.replace(l, " ");
				}
				answer = answer.replace("<img"," ").replace("/>", " ");
			}

			if (answer==null || answer.length()<20)
				continue;
			ParseThicketWithDiscourseUnitsForIndexing pt;
			try {
				pt = builder.buildParseThicket(answer);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				continue;
			}
			if (pt==null || pt.eDUs==null)
				continue;
			
			List<String> results = new ArrayList<String>();
			results.add(line[1]); results.add(line[11]);
			results.add(answer); 
			results.addAll(pt.eDUs); System.out.println(pt.eDUs);
			report.add( (String[]) results.toArray(new String[0]));
		}
		
		ProfileReaderWriter.writeReport(report, "alternativeQuestionsGenerator.csv");
	}
	
	public static void main(String[] args){
		new NucleusEduBuilderForAnswer().run();
	}
}
