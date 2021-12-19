package opennlp.tools.chatbot.search_results_blender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import opennlp.tools.chatbot.ChatIterationResult;
import opennlp.tools.chatbot.LongQWebSearcherRst;
import opennlp.tools.chatbot.LongQueryWebSearcher;
import opennlp.tools.chatbot.SearchSessionManager;
import opennlp.tools.chatbot.wrapper.BotRequest;
import opennlp.tools.chatbot.wrapper.BotResponse;
import opennlp.tools.chatbot.wrapper.SearchSessionManagerWrapper;
import opennlp.tools.parse_thicket.Pair;
import opennlp.tools.parse_thicket.kernel_interface.TreeKernelRunner;
import opennlp.tools.textsimilarity.TextProcessor;

public class EntryPointVirtualDialogue extends SearchSessionManagerWrapper {
	private static Logger LOG = Logger
			.getLogger("opennlp.tools.chatbot.BlenderWrapper");
	protected LongQWebSearcherRst  searcher = LongQWebSearcherRst.getInstance();
	protected VirtualDialogueClarificationExpressionGenerator clarificationExpressionGenerator = 
			new VirtualDialogueClarificationExpressionGenerator();
	private TreeKernelRunner browser = new TreeKernelRunner();
	private WebDomainManager yelpManager = new WebDomainManager();
	private static final String WEB_DOMAIN_REGEXP = "^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}$";
	private String currDomain = null;
	private static String oS = System.getProperty("os.name").toLowerCase();

	private static String isWebDomainInQuery(String query){
		String[] tokensInQuery = query.split(" ");
		for(String t: tokensInQuery){
			if (t.endsWith(".com") || t.endsWith(".biz") || t.endsWith(".edu"))
				return t;
		}

		/*
		Pattern pattern = Pattern.compile(WEB_DOMAIN_REGEXP);
		Matcher matcher = pattern.matcher(query);
		if (matcher.find())
			return query.substring(matcher.start() , matcher.end());
		 */
		return null;

	}

	public BotResponse runSessionIteraction(String query){
		BotResponse resp = new BotResponse();
		//		System.out.print("\nEnter your response or query >");
		try {
			if (query.equals("q"))
				System.exit(0);

			if (query.toLowerCase().startsWith("change topic")){
				queryType = 0;
				clarificationExpressionGenerator.reset();
				resp.responseMessage = "You can ask a NEW question now";
				return resp;
			}

			if (query.toLowerCase().startsWith("do order") || query.toLowerCase().startsWith("do reservation") || isWebDomainInQuery(query)!=null){
				currDomain = isWebDomainInQuery(query);
				queryType = 7;
				clarificationExpressionGenerator.reset();
				resp.responseMessage = "You can request your order now";
				return resp;
			}

			if (queryType == 0) {
				return runBasicSearch(query, resp);
			} else 
				if (queryType == 1){
					String selectedAnswer = clarificationExpressionGenerator.matchUserResponseWithGeneratedOptions(query);
					if (selectedAnswer!=null){
						logSilent(selectedAnswer);
						clarificationExpressionGenerator.latestAnswer = selectedAnswer;
						logSilent("Are you OK with this answer? yes/more/no/specify [different topic] / reduce search to web domain");
						resp.responseMessage = selectedAnswer + "\n" + "Are you OK with this answer? yes/more/no/specify [different topic]/ reduce search to web domain / virtual dialogue";
						showPage(clarificationExpressionGenerator.matchUserResponseWithGeneratedOptionsPair(query).getSecond());
						queryType = 3;
					} else {
						logSilent(clarificationExpressionGenerator.getBestAvailableCurrentAnswer());
						resp.responseMessage = clarificationExpressionGenerator.getBestAvailableCurrentAnswer();
					}
					resp.setResponseObject(clarificationExpressionGenerator);
					return resp;
				} 
				else if (queryType == 3 && query.toLowerCase().indexOf("yes")>-1){
					queryType = 0;
					logSilent("Now you can ask a NEW question");
					resp.responseMessage = "Now you can ask a NEW question";
					return resp;
				}
				else if (queryType == 3 && query.toLowerCase().indexOf("more")>-1){
					logSilent(clarificationExpressionGenerator.getBestAvailableCurrentAnswer());
					queryType = 0;
					logSilent("Now you can ask a NEW question");
					resp.responseMessage =  clarificationExpressionGenerator.getBestAvailableCurrentAnswer() + "\nNow you can ask a NEW question";
					return resp;
				}
				else if (queryType == 3 && query.toLowerCase().indexOf("reduce ")>-1){
					searcher.setQueryType(queryType);
					queryType = 0;
					String domain = extractDomainFromQuery(query);
					logSilent("We are now trying to use the constraint on the domain " + domain);
					clarificationExpressionGenerator.setDomain(domain);
					List<ChatIterationResult> searchRes = searcher.searchLongQuery(clarificationExpressionGenerator.originalQuestion +
							" site:"+domain);
					logSilent(getAnswerNum(0, searchRes));
					queryType = 0;
					logSilent("Now you can ask a NEW question");
					resp.responseMessage =  "We are now trying to use the constraint on the domain " + domain +"\n" + 
							getAnswerNum(0, searchRes) + "\nNow you can ask a NEW question";
					return resp;
				}
				else if (queryType == 3 && query.toLowerCase().startsWith("no")){
					queryType = 0;
					logSilent("We are now trying to use the constrainst from your previous replies...");
					List<ChatIterationResult>  searchRes = searcher.searchLongQuery(clarificationExpressionGenerator.originalQuestion+  " " +
							clarificationExpressionGenerator.clarificationQuery);
					logSilent("I think you will find this information useful:");
					resp.responseMessage = "We are now trying to use the constrainst from your previous replies..." +
							"\nI think you will find this information useful:\n";

					for(int i=0; i< searchRes.size(); i++){
						if (!clarificationExpressionGenerator.isEqualToAlreadyGivenAnswer(getAnswerNum(i, searchRes))){
							logSilent(getAnswerNum(0, searchRes));
							resp.responseMessage += getAnswerNum(i, searchRes);
							break;
						}
					}
					queryType = 0;
					logSilent("\nNow you can ask a NEW question");
					resp.responseMessage += "\nNow you can ask a NEW question";
					return resp;
				} 
				else if (queryType == 3 && AnaphoraProcessor.isAnaphoraQuery(query)){
					queryType = 0; // proceed as regular initial search
					String previousQuery = clarificationExpressionGenerator.originalQuestion;
					String anaphoraQuery = AnaphoraProcessor.substituteAnaphoraExtractPhraseRstMatcher(previousQuery, query, searcher.getPhraseExtractor());
					//substituteAnaphora(previousQuery, query);

					return runBasicSearch(anaphoraQuery, resp);
				} 
				else if (queryType == 3 && (query.toLowerCase().indexOf("virtual")>-1 || query.toLowerCase().indexOf("dialogue")>-1)){
					queryType = 9; // virtual dialogue
					resp.responseMessage = " This is what other people are chatting about your topic :\n" 
							+ clarificationExpressionGenerator.buildDialoguesFromAllSnippets() + 
							"\nAre you OK with this answer? yes/no/ join the virtual chat by searching specific things";

					return resp;
				} 
				else if (queryType == 3){ // default processing of user response after clarification
					String selectedAnswer = clarificationExpressionGenerator.matchUserResponseWithGeneratedOptions(query);
					if (selectedAnswer!=null){
						logSilent(selectedAnswer);
						clarificationExpressionGenerator.latestAnswer = selectedAnswer;
						logSilent("Are you OK with this answer? yes/more/no/specify [different topic]/reduce search result to domain/virtual dialogue");
						queryType = 3;
						resp.responseMessage = selectedAnswer + "\nAre you OK with this answer? yes/more/no/specify [different topic]/reduce search result to domain";
					} else {
						logSilent(clarificationExpressionGenerator.getBestAvailableCurrentAnswer());
						resp.responseMessage = clarificationExpressionGenerator.getBestAvailableCurrentAnswer();
					}
					return resp;
				}
			if (queryType == 4){ // 
				if (query.toLowerCase().equals("no")){
					queryType = 0;
					logSilent("\nNow you can ask a NEW question");
					resp.responseMessage = "\nNow you can ask a NEW question";
				} else {
					String productPageSeaarchResult = clarificationExpressionGenerator.searchProductPage(query);
					resp.responseMessage = "Let me tell you more about "+clarificationExpressionGenerator.currentEntity + "\n";
					resp.responseMessage += productPageSeaarchResult +
							"\n More questions about this product?\n";
				}
				return resp;
			}

			if (queryType == 7){
				yelpManager.setWebDomain(currDomain);
				String url = yelpManager.formYelpRequestURL(query);
				if (url!=null){
					showPage(url);
					resp.responseMessage = "You can make your choice and order";
				} else {
					resp.responseMessage = "Could not interpret your order";
				}
				queryType = 0;
			}
			if (queryType == 9) { // user got virtual dialogue content and get in by further searching or get out
			    if (query.toLowerCase().startsWith("yes")){
					queryType = 0;
					resp.responseMessage = "\nNow you can ask a NEW question";
			    } else  if (query.toLowerCase().startsWith("yes")){
			    	resp.responseMessage = clarificationExpressionGenerator.getBestAvailableCurrentAnswer();
			    } else { // do search
			    	resp.responseMessage = " This is what has been answered to people with similar question :\n" 
							+ clarificationExpressionGenerator.closestDialogueFragment(query)+ 
							"\nAre you OK with this answer? yes/no/ search specific things";
			    }
			    return resp;
			}

		} catch (Exception ioe) {
			ioe.printStackTrace();
		}
		return resp;
	}

	protected BotResponse runBasicSearch(String query, BotResponse resp){
		List<ChatIterationResult> searchRes = searcher.searchLongQuery(query);
		Pair<String, List<String>> clarificationPair = clarificationExpressionGenerator.generateClarificationPairVirt(query, searchRes);
		// no clarification needed, so just give response as a first paragraph text
		String clarificationStr = clarificationPair.getFirst();
		if (clarificationStr==null){ 
			logSilent("I think you will find this information useful:\n");
			logSilent(getAnswerNum(0, searchRes));
			resp.responseMessage = "I think you will find this information useful:"+getAnswerNum(0, searchRes);
			showPage(getAnswerUrl(0, searchRes));
			queryType = 0;
		} else {
			logSilent("I believe these are the main topics of your query: is that what you meant? Please select");
			logSilent(clarificationStr);
			resp.responseMessage = "I believe these are the main topics of your query: is that what you meant? Please select \n"+ 
					clarificationStr;
			resp.setClarificationOptions(clarificationPair.getSecond());
			queryType = 1;
		}
		return resp;
	}

	public void showPage(String url){

		if (url!=null && (oS.indexOf("mac") >= 0 || oS.indexOf("win") >= 0 ))
			browser.runEXE(new String[]{ "bash", "-c",
					"/Applications/Google\\ Chrome.app/Contents/MacOS/Google\\ Chrome --foo --bar=2 " + url}, "/bin");
	}

	public static void main(String[] args){	

		EntryPointVirtualDialogue wrapper = new EntryPointVirtualDialogue();
		while(true){
			System.out.print("\nEnter your response or query >");
			try {
				//  open up standard input
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				String query = null;
				query = br.readLine();

				if (query.equals("q"))
					System.exit(0);
				BotResponse resp = wrapper.runSessionIteraction(query);
				System.out.println(resp);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

