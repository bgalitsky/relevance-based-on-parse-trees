package opennlp.tools.apps.contentgen.multithreaded;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import eu.agilelabs.syntMatch.SyntMatcher;
import eu.agilelabs.utils.text.StringCleaner;
import eu.agilelabs.web.apps.commons.HitBase;
import eu.agilelabs.web.apps.commons.bing.BingWebQueryRunner;
import eu.agilelabs.web.apps.commons.bing.BingWebQueryRunnerThread;

public abstract class TrendContentGeneratorSequencial {

	StringCleaner cleaner= new StringCleaner();
	private static Logger log = Logger.getLogger(  TrendContentGeneratorSequencial.class);
	private RelatedSenteceFinder rsf = new RelatedSenteceFinder();
	
	public void processItem(Object item){
		List<String> formedTopics = new ArrayList<String>();
		List<List<HitBase>> searchResultFormedTopics = new ArrayList<List<HitBase>>();

		formedTopics =  formSeedQueries();
		BingWebQueryRunner bwrunner = new BingWebQueryRunner();
		for (String topic:formedTopics ){
			
			if(topic!=null){
				List<HitBase> searchResult = bwrunner.runSearch(topic);
				searchResultFormedTopics.add(searchResult);
			}

		}
	
		
		//now deal with just one result from trends
		int count=0;
		for(List<HitBase> res: searchResultFormedTopics){
			if(res==null)
				continue;
			for(int subStoryN=0; subStoryN<res.size()-1; subStoryN+=2){
				String article = res.get(subStoryN).getTitle() + " ; "+ res.get(subStoryN).getAbstractText(); 
				log.debug("Generating content for seed sentence "+article);
				SyntMatcher sm = SyntMatcher.getInstance();
				String[] sentences =  sm.getSentenceDetector_EN().sentDetect(StringUtils.replace(article, "...",". "));
				OriginalContentYahooFetcher bf = new OriginalContentYahooFetcher();
				try {
					StringBuffer sumBuf = new StringBuffer(); String title = null;
					List<HitBase> result = new ArrayList<HitBase>();
					for (String sentence : sentences) { 
						if (sentence.length()<60)
							continue;
						result=rsf.findRelatedOpinionsForSentence(sentence, Arrays.asList(sentences));
						List<Fragment> bestFragments = bf.getBestOverall(result);
						if(result==null)
							continue;
						title = cleaner.processSnapshotForMatching(result.get(0).getTitle());
						int sentCount=0;
						// no original sent which is a snapshot part
						//sumBuf.append("<p>"+sentence + " \n "); sentCount++;

						for (Fragment f : bestFragments) {
							if (sumBuf.toString().endsWith("</p>\n "))
								sumBuf.append("<p>");
							sumBuf.append( f.getResultText() );
							if ((sentCount % 3) ==0)
								sumBuf.append("</p>\n ");
							else 
								sumBuf.append("\n ");
							sentCount++;
						}	
					}
					
						
					String text = sumBuf.toString();
					if (count< formedTopics.size()&& subStoryN==0 )
						title = formedTopics.get(count);
					else {
						int endOfTitle = text.indexOf(';');
						if (endOfTitle>-1 && endOfTitle<70)
							title = text.substring(0, endOfTitle );
						else {
							endOfTitle = text.indexOf('.');
							if (endOfTitle>-1 && endOfTitle<70)
								title = text.substring(0, endOfTitle );
						}
						
					}
					title = OriginalContentYahooFetcher.processSentence(title);
					log.debug("About to create event with title:"+title);
					int event_id = serializeSyntheticEvent(text, title);
					log.debug("Created event with id="+event_id);
					
				} catch (Exception e) {
					log.error("Problem creating content", e);
				}
				
			}
			count++;
		}
		System.err.println("yuhhuuuu");
	}

	
	/**
	 * Generates base queries for the web searches that used for content genration
	 * @return
	 */
	abstract protected List<String> formSeedQueries();


	/**
	 * Stores the generated content in the underlying data structure
	 * @param text
	 * @param title
	 * @return
	 */
	abstract protected int serializeSyntheticEvent(String text, String title);
		



}
