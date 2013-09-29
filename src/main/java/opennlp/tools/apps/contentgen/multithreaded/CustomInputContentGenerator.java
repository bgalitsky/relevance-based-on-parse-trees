package opennlp.tools.apps.contentgen.multithreaded;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import opennlp.tools.textsimilarity.chunker2matcher.ParserChunker2MatcherProcessor;

import org.apache.log4j.Logger;



public class CustomInputContentGenerator extends TrendContentGeneratorMultiThread{

	private static CustomInputContentGenerator m_instance = null;
	private Date lastLoadDate = null;
	private static Logger log = Logger.getLogger(  CustomInputContentGenerator.class);

	protected	List<String> formSeedQueries(){
		String text = "Publishes and comments on leaked documents alleging government and corporate misconduct. "+
		"WikiLeaks has released a document set called the Afghan War Diary, an extraordinary compendium. "+
		"Wikileaks is an international organization, based in Sweden, which publishes anonymous submissions and leaks of otherwise unavailable documents. "+
		"Wikileaks is an international organization , based in Sweden , which publishes ."+
		"This week, the website WikiLeaks published more than seventy-five thousand American military documents on the war in Afghanistan.";
		ParserChunker2MatcherProcessor sm = ParserChunker2MatcherProcessor.getInstance();
		String[] sents =  sm.splitSentences(text);

		return Arrays.asList(sents);
	}

	public static void main(String[] args){
	
		CustomInputContentGenerator gen = new CustomInputContentGenerator();
		gen.processItem(null);
		System.exit(0);
	}

	@Override
	protected int serializeSyntheticEvent(String text, String title) {
		System.err.println(title);
		System.out.println(text);
		return 0;
	}

	@Override
	public void MyEvent(opennlp.tools.apps.contentgen.multithreaded.MyEvent evt) {
		// TODO Auto-generated method stub
		
	}

}
