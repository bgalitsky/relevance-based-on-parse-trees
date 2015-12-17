package opennlp.tools.parse_thicket.opinion_processor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import opennlp.tools.jsmlearning.ProfileReaderWriter;
import opennlp.tools.parse_thicket.ParseTreeNode;
import opennlp.tools.textsimilarity.ParseTreeChunk;

public class TwitterEngineRunner {
	private List<File> queue;
	private final static String twSource = "/Users/bgalitsky/Documents/workspace/TwitterMiner/data/TwitterArtistsDynamicsTot12_07.csv";
	TwitterFilter neExtractor = new TwitterFilter();
	
	public void processTweetFile(){
		List<String[]> report = new ArrayList<String[]>();
		report.add(new String[] { "text", "phrases of potential interest list" , });

		List<String[]> texts = ProfileReaderWriter.readProfiles(twSource);
		for(String[] text: texts){
			EntityExtractionResult result=null;
			if (text==null || text.length<3)
				continue;
			String cleanedTweet = text[3].replace("/\\bs\\@+/ig","");
			try {
				result = neExtractor.extractEntities(cleanedTweet);
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
			report.add(new String[]{text[0],text[3]});
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
			
			ProfileReaderWriter.writeReport(report, "phrasesExtractedFromTweets.csv");
		}
	}

	
	public static void main(String[] args){
		TwitterEngineRunner runner = new TwitterEngineRunner();
		runner.processTweetFile();

	}
}

/*
	public void processDirectory(String path){
		List<String[]> report = new ArrayList<String[]>();
		report.add(new String[] { "filename", "named entity list", "phrases of potential interest list" });

		List<String> allNamedEntities = new ArrayList<String>();

		addFiles(new File(path));
		for(File f: queue){
			List<String> entities = (List<String>) extractEntities(f.getAbsolutePath()).getFirst();
			List<String> opinions = (List<String>) extractEntities(f.getAbsolutePath()).getSecond();
			report.add(new String[]{ f.getName(), entities.toString(),  opinions.toString()});	
			ProfileReaderWriter.writeReport(report, "nameEntitiesExtracted.csv");

			allNamedEntities.addAll(entities);

			allNamedEntities = new ArrayList<String>(new HashSet<String> (allNamedEntities ));


		}
		ProfileReaderWriter.writeReport(report, "nameEntitiesTopicsOfInterestExtracted.csv");
	} 
} */
