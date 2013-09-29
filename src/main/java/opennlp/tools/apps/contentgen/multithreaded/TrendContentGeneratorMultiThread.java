package opennlp.tools.apps.contentgen.multithreaded;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import opennlp.tools.similarity.apps.HitBase;
import opennlp.tools.similarity.apps.utils.StringCleaner;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;



public abstract class TrendContentGeneratorMultiThread implements MyEventListener {

	private static TrendContentGeneratorMultiThread m_instance = null;
	StringCleaner cleaner= new StringCleaner();
	private static Logger log = Logger.getLogger(  TrendContentGeneratorMultiThread.class);
	private int threadFinishedCounter=0;
	private int threadSentFinderFinishedCounter=0;
	
	public void processItem(Object item){
		List<String> formedTopics = new ArrayList<String>();
		List<List<HitBase>> searchResultFormedTopics = new ArrayList<List<HitBase>>();

		formedTopics =  formSeedQueries();
		BingQueryRunner bwrunner = new BingQueryRunner();
		ArrayList<BingWebQueryRunnerThread> threadRunners=new ArrayList<BingWebQueryRunnerThread>();
		for (String topic:formedTopics ){
			//TODO make multithreading
			if(topic!=null){
				threadRunners.add(new BingWebQueryRunnerThread(topic));
			}
			//List<HitBase> searchResult = bwrunner.runSearch(topic);
			//searchResultFormedTopics.add(searchResult);
		}
		for (BingWebQueryRunnerThread bingWebQueryRunnerThread : threadRunners) {
			bingWebQueryRunnerThread.addMyEventListener(this);
			Thread t= new Thread(bingWebQueryRunnerThread);
			t.start();
		}
		while (threadFinishedCounter<threadRunners.size()-1) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				log.error(e);
				e.printStackTrace();
			}			
		}
		for (BingWebQueryRunnerThread bingWebQueryRunnerThread : threadRunners) {
			searchResultFormedTopics.add(bingWebQueryRunnerThread.getResults());
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
					ArrayList<RelatedSenteceFinderThread> threadSentenceFinders= new ArrayList<RelatedSenteceFinderThread>();
					for (String sentence : sentences) { 
						//TODO Multithread stuff 
						if (sentence.length()<60)
							continue;
						threadSentenceFinders.add(new RelatedSenteceFinderThread(sentence, Arrays.asList(sentences)));
						/**/					 
					}
					
					for (RelatedSenteceFinderThread relatedSenteceFinder : threadSentenceFinders) {
						relatedSenteceFinder.addMyEventListener(this);
						Thread t= new Thread(relatedSenteceFinder);
						t.start();
					}
					while(threadSentFinderFinishedCounter<threadSentenceFinders.size()-1){
						try {
							Thread.sleep(1000);	
						} catch (Exception e) {
							log.error(e);
							e.printStackTrace();
						}
					}
					for (RelatedSenteceFinderThread relatedSenteceFinder : threadSentenceFinders) {
						List<HitBase> result = relatedSenteceFinder.getResult();
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
		

	@Override
	public void MyEvent(eu.agilelabs.web.apps.newsgen.MyEvent evt) {
		if(evt.getSource() instanceof BingWebQueryRunnerThread )
			threadFinishedCounter++;
		if(evt.getSource() instanceof RelatedSenteceFinder)
			threadSentFinderFinishedCounter++;
		
	}

}
