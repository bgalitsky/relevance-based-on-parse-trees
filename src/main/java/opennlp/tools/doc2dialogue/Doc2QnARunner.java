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
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.clulab.discourse.rstparser.DiscourseTree;

import opennlp.tools.chatbot.ChatBotCacheSerializer;
import opennlp.tools.chatbot.ChatIterationResult;
import opennlp.tools.jsmlearning.ProfileReaderWriter;
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

public class Doc2QnARunner {
	protected Doc2QnABuilder builder;
	protected ParagraphsFromWebPageExtractor exractor;
	Tika tika = new Tika();
	String QnAcategory = "ERP";
	String[] header = new String[]{"category_path",	"questions", "content"};

	public Doc2QnABuilder getDialogueBuilder(){
		return builder;
	}

	public Doc2QnARunner(){		
		builder = new Doc2QnABuilder();
		exractor = new ParagraphsFromWebPageExtractor ();
	}


	public List<Pair<List<String>, List<String>>> paragraphsAndSubtitlesFromDocExtractor(String path){
		String text = null;
		try {
			text = tika.parseToString(new File(path));
		} catch (IOException e) {

			e.printStackTrace();
			return null;
		} catch (TikaException e) {

			e.printStackTrace();
		}
		List<Pair<List<String>, List<String>>> blocks = new ArrayList<Pair<List<String>, List<String>>>();

		String[] portions = text.split("\n\n");
		List<String> answers = new ArrayList<String>(); 
		List<String> questions = new ArrayList<String>();
		for(String p: portions){
			if (p!=null && p.length()<4) {// end of block
				if (answers.size()>0) { 
					blocks.add(new Pair<List<String>, List<String>>(questions, answers));
				}
				answers = new ArrayList<String>();  questions = new ArrayList<String>();

			} else {
				if (answers.size()==0 &&  p.length()< 50){
					questions.add(p);
				} else {
					answers.add(p);
				}
			}
		}


		return blocks;
	}



	public String runDoc2DialogForStringArray(List<String> sents){
		StringBuffer buf = new StringBuffer();
		buf.append("\n\n");
		for(String t: sents){
			List<List<ParseTreeNode>> dialogue = builder.buildQuestionForParagraph(t); 
			if (dialogue == null)
				continue;
			for(List<ParseTreeNode>u: dialogue){
				buf.append(ParseTreeNode.toWordString(u)+"\n");
			}
			buf.append("\n\n");
		}
		return buf.toString();
	}

	public List<String[]> runDoc2DialogForADoc(String path){
		List<String[]> qnAreport = new ArrayList<String[]>();

		List<Pair<List<String>, List<String>>> blocks =paragraphsAndSubtitlesFromDocExtractor(path);


		String reportFileName = new File(path).getName()+".csv";
		qnAreport.add(header);
		for(Pair<List<String>, List<String>> pair: blocks){
			StringBuffer bufQuestions = new StringBuffer(), bufAnswer = new StringBuffer();
			List<List<ParseTreeNode>> newQuestionsForBlock = new ArrayList<List<ParseTreeNode>>();
			for(String answer:pair.getSecond()){
				List<List<ParseTreeNode>> newQuestions = builder.buildQuestionForParagraph(answer); 
				if (newQuestions != null){
					newQuestionsForBlock.addAll(newQuestions);
				}			
			}

			for(List<ParseTreeNode>q: newQuestionsForBlock){
				bufQuestions.append(ParseTreeNode.toWordString(q)+"\n");
			}
			for(String qOld: pair.getFirst()){
				bufQuestions.append(qOld + "\n");
			}

			for(String a: pair.getSecond()){
				bufAnswer.append(a+"\n");
			}
			if (bufQuestions.toString().length()>20 && bufAnswer.toString().length()>100){
				qnAreport.add(new String[]{this.QnAcategory, bufQuestions.toString(), bufAnswer.toString() });
				ProfileReaderWriter.writeReport(qnAreport, reportFileName);
			}

		}
		return 	qnAreport;
	}


	public static void main(String[] args){
		Doc2QnARunner runner = new Doc2QnARunner();
		if (args.length!=1)
			System.err.println("Usage: java -Xmx8g -jar doc2QnA.jar full_path_filename . Building QnA takes minutes. \n"
					+ "It will add '.csv' to your filename in the output CSV which you can load to Bots QnA. You can open thi csv in Excel. ");
		String arg = args[0]; String dialogue = null;
		
		try {
			File file = new File(args[0]);
			runner.runDoc2DialogForADoc(file.getCanonicalPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(0);
		runner.runDoc2DialogForADoc(//"/Users/bgalitsk/Downloads/US_Volunteering_and_Your_Community_Day.pdf");
				"/Users/bgalitsk/Documents/relevance-based-on-parse-trees-master/"
				 //visa-credit-cardholder-agreement-and-disclosure.pdf");
		//"/Users/bgalitsk/Documents/relevance-based-on-parse-trees-master/soc_criminology_schemata_fall09.pdf");
		//+"account_opening_wp.pdf"
		//+"guide_to_mutual_fund_investing.pdf"
		//		+"sec-guide-to-mutual-funds.pdf"
		//		+"mutualfund_basics.pdf");
				+"SBP-IVG-1_investment_guide.pdf");

	}

}
