package opennlp.tools.apps.contentgen.multithreaded;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import opennlp.tools.similarity.apps.utils.PageFetcher;
import opennlp.tools.similarity.apps.utils.Utils;
import opennlp.tools.textsimilarity.chunker2matcher.ParserChunker2MatcherProcessor;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;

public class PopularLocalBingNewsInputContentGenerator extends TrendContentGeneratorSequencial{

	//TODO outsource the timeout value
	PageFetcher pFetcher=new PageFetcher();
	private static PopularLocalBingNewsInputContentGenerator m_instance = null;
	private Date lastLoadDate = null;
	private static Logger log = Logger.getLogger(  PopularLocalBingNewsInputContentGenerator.class);
	
	
		
		public PopularLocalBingNewsInputContentGenerator(){};
		private final String[][] extractionTemplate = new String[][]{
				{null},
				{"",""},
				{"?","-"},
				{":","…"},
				{"?", "…"}			
		};
		
		private final String[] delimitersForNews = new String[]{"…", "?", ":"};
		
		
		
		protected List<String> formSeedQueries(){
			String[] sents = null;										// this func is somewhere else now?
			String downloadedPage = pFetcher.fetchPage("http://www.bing.com/news?q=&");
			if (downloadedPage!=null && downloadedPage.length()>100){
				String pageContent = Utils.fullStripHTML(downloadedPage);
				ParserChunker2MatcherProcessor sm = ParserChunker2MatcherProcessor.getInstance();
				sents =  sm.splitSentences(pageContent);
			}
			
			List<String> rawSeedSents = Arrays.asList(sents);
			rawSeedSents  = rawSeedSents.subList(1, rawSeedSents.size()); 
			List<String> extractedSents = new ArrayList<String>();
			for(int i=1; i< rawSeedSents.size(); i++ ){
				try {
					String sent = rawSeedSents.get(i);
					if (i==1){
						int endIndex = sent.indexOf("…");
						if (endIndex>0){
							sent = sent.substring(0, endIndex);
						}	
						extractedSents.add(sent);
						continue;
					}
					if (i==2){
						int startInd = sent.indexOf(extractionTemplate[i][0]);
						int endInd =  sent.indexOf(extractionTemplate[i][1]);
						if (startInd>0 && endInd>startInd && startInd-endInd>60){
							sent = sent.substring(startInd, endInd);
							extractedSents.add(sent);
							continue;
						} else if (startInd>endInd){
							sent = sent.substring(startInd);
							extractedSents.add(sent);
							continue;
						}
					} else if (i>2){
						int startInd = sent.indexOf("?");
						int endInd =  sent.indexOf("…");
						if (startInd>0 && endInd>startInd && startInd-endInd>60){
							sent = sent.substring(startInd, endInd);
							extractedSents.add(sent);
						}
						else if (startInd>0 && sent.length()-startInd>50){
							sent = sent.substring(startInd);
							extractedSents.add(sent);
						} else if (endInd>0 && endInd>50) {
							sent = sent.substring(0, endInd);
							extractedSents.add(sent);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
					
			}
			
			String delim = "_&_";
			List<String > extractedSentsProc = new ArrayList<String >(); 
			for(int i=0; i< extractedSents.size(); i++){
				
				String sent = extractedSents.get(i);
				for(int d=0; d< delimitersForNews.length; d++){
					sent = sent.replace(delimitersForNews[d], delim );				
				}
				
				String[] sentParts = sent.split(delim);
				for (String newPart: sentParts){
					if(newPart.length()>60)
						try {
							extractedSentsProc.add(processExtractedSentenceBing(newPart));
						} catch (Exception e) {
							e.printStackTrace();
						}
				}
			}		
			try {
				extractedSentsProc = extractedSentsProc.subList(0, extractedSentsProc.size()-4);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return extractedSentsProc;
		}
		
		public static String processExtractedSentenceBing(String sent){
			sent=sent.trim();
			// remove fist and last words, usually irrelevant
			int start = StringUtils.indexOf(sent, " ");
			int end = StringUtils.lastIndexOf(sent, " ");
			try {
				if (start>0  && end>start)
					sent = sent.substring(start, end).trim();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			sent = sent.replace("1 hour ago", " ").replace("hours ago", " ").replace("More stories", " ").replace("minutes ago", " ").replace("hour ago", " ").
			replace("More World news", " ").replace("storiesCategory", " ").replace("More news", " ").replace("day ago", " ").replace("days ago", " ").
			replace("‘", " ").replace("?"," ").replace("–", "").replace("?", " ").
			replace("Sports news", " ").replace("Business news", " ").replace("More", " ").replace("Politics news", " ");;
			int startInd =  sent.indexOf("More stories");
			if (startInd>0 && sent.length()-startInd>50){
				sent = sent.substring(startInd, sent.length());
			}
			return sent;
		}

		public static void main(String[] args){
			PopularLocalBingNewsInputContentGenerator gen = new PopularLocalBingNewsInputContentGenerator();
			gen.processItem(null);
			System.exit(0);
		}

	
	@Override
	protected int serializeSyntheticEvent(String text, String title) {
		
		return 0;
	}

	

}
