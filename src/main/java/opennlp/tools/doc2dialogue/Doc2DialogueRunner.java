package opennlp.tools.doc2dialogue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.clulab.discourse.rstparser.DiscourseTree;

import opennlp.tools.chatbot.ChatBotCacheSerializer;
import opennlp.tools.chatbot.ChatIterationResult;
import opennlp.tools.parse_thicket.Pair;
import opennlp.tools.parse_thicket.ParseTreeNode;
import opennlp.tools.parse_thicket.external_rst.MatcherExternalRST;
import opennlp.tools.parse_thicket.external_rst.PT2ThicketPhraseBuilderExtrnlRST;
import opennlp.tools.parse_thicket.external_rst.ParseCorefBuilderWithNERandRST;
import opennlp.tools.parse_thicket.external_rst.ParseThicketWithDiscourseTree;
import opennlp.tools.similarity.apps.BingQueryRunner;
import opennlp.tools.similarity.apps.HitBase;
import opennlp.tools.similarity.apps.utils.StringDistanceMeasurer;
import opennlp.tools.textsimilarity.ParseTreeChunk;
import opennlp.tools.textsimilarity.ParseTreeChunkListScorer;
import opennlp.tools.textsimilarity.TextProcessor;

public class Doc2DialogueRunner {
	protected Doc2DialogueBuilder builder = new Doc2DialogueBuilder();
	protected ParagraphsFromWebPageExtractor exractor = new ParagraphsFromWebPageExtractor ();
	
	public Doc2DialogueBuilder getDialogueBuilder(){
		return builder;
	}
	
	public String runDoc2DialogForUrl(String url){
		Pair<String[], Map<String, String>> p = exractor.
				extractSentencesAndSectionMapFromPage(url);
		if (p==null)
			return null;
		StringBuffer buf = new StringBuffer();
		String[] sents = p.getFirst();
		for(String t: sents){
			List<List<ParseTreeNode>> dialogue = builder.buildDialogueFromParagraph(t); 
			if (dialogue == null)
				continue;
			for(List<ParseTreeNode>u: dialogue){
				buf.append(ParseTreeNode.toWordString(u)+"\n");
			}
			buf.append("\n\n");
		}
		return buf.toString();
	}
	
	public String runDoc2DialogForStringArray(List<String> sents){
		StringBuffer buf = new StringBuffer();
		buf.append("\n\n");
		for(String t: sents){
			List<List<ParseTreeNode>> dialogue = builder.buildDialogueFromParagraph(t); 
			if (dialogue == null)
				continue;
			for(List<ParseTreeNode>u: dialogue){
				buf.append(ParseTreeNode.toWordString(u)+"\n");
			}
			buf.append("\n\n");
		}
		return buf.toString();
	}


	public static void main(String[] args){
		Doc2DialogueRunner runner = new Doc2DialogueRunner();
		if (args.length!=1)
			System.err.println("Usage: Java -jar doc2dialogue.jar [url /filename in local dir/ text in quotes ]");
		String arg = args[0]; String dialogue = null;
		if ( arg.startsWith("http") || arg.startsWith("www.") ){
			dialogue = runner.runDoc2DialogForUrl(arg);
		} else {
			String current;
            try {
	            current = new java.io.File( "." ).getCanonicalPath();
	            File textFile = new File(/*current+File.pathSeparator+*/arg); 
				boolean exists = textFile.exists();
				if (exists){
					String text = FileUtils.readFileToString(textFile, "utf-8");
					String[] sentsAr = text.split("\n\n");
					List<String> sents = Arrays.asList(sentsAr);
							//TextProcessor.splitToSentences(text);
					dialogue = runner.runDoc2DialogForStringArray( sents);
				} else {
					if (arg.length()>150){
					List<String> sents = TextProcessor.splitToSentences(arg);
					dialogue = runner.runDoc2DialogForStringArray( sents);
					} else {
						System.err.println("Text is corrupt or too short");
					}
				}
            } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
			
		}
		
	/*	String dialogue = runner.runDoc2DialogForUrl(nZ@pxDh3
				//"https://www.thebalance.com/insufficient-funds-315343"
				//"https://www.creditcardinsider.com/blog/can-you-pay-a-credit-card-with-a-credit-card/"	);
				//"https://en.wikipedia.org/wiki/Blind_date"
				//"https://www.thestreet.com/how-to/invest-in-real-estate-14735368"
				"https://usautosales.info/blogs/1924/the-perfect-car/tire-patch-or-tire-plug-fix-your-flat/"
				
				);
		*/
		System.out.println(dialogue);
	}
}
