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

public class EntryPointVirtualPersuasiveDialogue extends SearchSessionManagerWrapper {
	private static Logger LOG = Logger
			.getLogger("opennlp.tools.chatbot.BlenderWrapper");
	protected LongQWebSearcherRst  searcher = LongQWebSearcherRst.getInstance();
	protected VirtualDialogueClarificationExpressionGenerator clarificationExpressionGenerator = 
			new VirtualDialogueClarificationExpressionGenerator();
	private TreeKernelRunner browser = new TreeKernelRunner();
	private WebDomainManager yelpManager = new WebDomainManager();
	//private static final String WEB_DOMAIN_REGEXP = "^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}$";
	private String currDomain = null;
	private static String oS = null ; //System.getProperty("os.name").toLowerCase();

	private static String isWebDomainInQuery(String query){
		String[] tokensInQuery = query.split(" ");
		for(String t: tokensInQuery){
			if (t.endsWith(".com") || t.endsWith(".biz") || t.endsWith(".edu"))
				return t;
		}

		return null;

	}

	public BotResponse runSessionIteraction(String query){
		BotResponse resp = new BotResponse();
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
						clarificationExpressionGenerator.latestAnswer = selectedAnswer;
						resp.mainContent=  selectedAnswer;
						resp.postStringEnumerNextOptions = "Are you OK with this answer? yes/more/no/specify [different topic]/ reduce search to web domain / virtual persuasive dialogue for this topic | for all";
						resp.responseMessage = selectedAnswer + "\n" + resp.postStringEnumerNextOptions;
						showPage(clarificationExpressionGenerator.matchUserResponseWithGeneratedOptionsPair(query).getSecond());
						queryType = 3;
					} else {

						resp.responseMessage = clarificationExpressionGenerator.getBestAvailableCurrentAnswer();
					}
					resp.setResponseObject(clarificationExpressionGenerator);
					return resp;
				} 
				else if (queryType == 3 && query.toLowerCase().indexOf("yes")>-1){
					queryType = 0;
					resp.responseMessage = "Now you can ask a NEW question";
					return resp;
				}
				else if (queryType == 3 && query.toLowerCase().indexOf("more")>-1){
					queryType = 0;
					resp.responseMessage =  clarificationExpressionGenerator.getBestAvailableCurrentAnswer() + "\nNow you can ask a NEW question";
					resp.postStringEnumerNextOptions="Now you can ask a NEW question";
					return resp;
				}
				else if (queryType == 3 && query.toLowerCase().indexOf("reduce ")>-1){
					searcher.setQueryType(queryType);
					queryType = 0;
					String domain = extractDomainFromQuery(query);
					clarificationExpressionGenerator.setDomain(domain);
					List<ChatIterationResult> searchRes = searcher.searchLongQuery(clarificationExpressionGenerator.originalQuestion +
							" site:"+domain);
					queryType = 0;
					resp.responseMessage =  "We are now trying to use the constraint on the domain " + domain +"\n" + 
							getAnswerNum(0, searchRes) + "\nNow you can ask a NEW question";
					resp.postStringEnumerNextOptions = resp.responseMessage;
					return resp;
				}
				else if (queryType == 3 && query.toLowerCase().startsWith("no")){
					queryType = 0;
					List<ChatIterationResult>  searchRes = searcher.searchLongQuery(clarificationExpressionGenerator.originalQuestion+  " " +
							clarificationExpressionGenerator.clarificationQuery);
					resp.responseMessage = "We are now trying to use the constrainst from your previous replies..." +
							"\nI think you will find this information useful:\n";
					resp.preStringIntroducingContent = resp.responseMessage;

					for(int i=0; i< searchRes.size(); i++){
						if (!clarificationExpressionGenerator.isEqualToAlreadyGivenAnswer(getAnswerNum(i, searchRes))){
							resp.responseMessage += getAnswerNum(i, searchRes);
							resp.mainContent =  getAnswerNum(i, searchRes);
							break;
						}
					}
					queryType = 0;

					resp.postStringEnumerNextOptions = "Now you can ask a NEW question";
					resp.responseMessage += resp.postStringEnumerNextOptions;
					return resp;
				} 
				else if (queryType == 3 && AnaphoraProcessor.isAnaphoraQuery(query)){
					queryType = 0; // proceed as regular initial search
					String previousQuery = clarificationExpressionGenerator.originalQuestion;
					String anaphoraQuery = AnaphoraProcessor.substituteAnaphoraExtractPhraseRstMatcher(previousQuery, query, searcher.getPhraseExtractor());

					return runBasicSearch(anaphoraQuery, resp);
				} 
				else if ((queryType == 3 || queryType == 92) && ((query.toLowerCase().indexOf("virtual")>-1 || query.toLowerCase().indexOf("dialogue")>-1)) &&
						(query.toLowerCase().indexOf("topic")>-1)){
					queryType = 91; // virtual dialogue for selected topic

					resp.preStringIntroducingContent = " This is what other people (some of whom are your opponents) are chatting in connection with your selected topic ";
					resp.mainContent = clarificationExpressionGenerator.buildDialogueForCurrSnippet() ;
					resp.postStringEnumerNextOptions = "Are you OK with this answer? yes/no/ join the virtual chat by searching specific things";
					resp.responseMessage = resp.preStringIntroducingContent +" :\n" +  resp.mainContent +"\n" + resp.postStringEnumerNextOptions;
					return resp;
				} 
				else if  ((queryType == 3 || queryType == 91) && ((query.toLowerCase().indexOf("virtual")>-1 || query.toLowerCase().indexOf("dialogue")>-1)) &&
						(query.toLowerCase().indexOf("all")>-1)){
					queryType = 92; // virtual dialogue for all search results
					resp.preStringIntroducingContent = " This is what other people are chatting about your exploration topic";
					resp.mainContent = clarificationExpressionGenerator.buildDialoguesFromAllSnippets();
					resp.postStringEnumerNextOptions = "Are you OK with this answer? yes/no/ join the virtual chat by searching specific things";
					resp.responseMessage = resp.preStringIntroducingContent +" :\n" +  resp.mainContent +"\n" + resp.postStringEnumerNextOptions;
					return resp;
				
				}
				else if (queryType == 3){ // default processing of user response after clarification
					resp.mainContent  = clarificationExpressionGenerator.matchUserResponseWithGeneratedOptions(query);
					if (resp.mainContent !=null){
						clarificationExpressionGenerator.latestAnswer = resp.mainContent;
						queryType = 3;
						resp.postStringEnumerNextOptions = "Are you OK with this answer? yes/more/no/specify [different topic]/reduce search result to domain/"
								+ "virtual [persuasive dialogue for this] topic/ virtual [dialogue for all found] docs";

						resp.responseMessage = resp.mainContent + "\n" + resp.postStringEnumerNextOptions;
					} else {
						resp.responseMessage = resp.mainContent;
						resp.postStringEnumerNextOptions = "";

					}
					return resp;
				}
		
			if (queryType == 4){ // 
				if (query.toLowerCase().equals("no")){
					queryType = 0;
					resp.postStringEnumerNextOptions = "\nNow you can ask a NEW question";
					resp.responseMessage = resp.postStringEnumerNextOptions;
				} else {
					String productPageSearchResult = clarificationExpressionGenerator.searchProductPage(query);
					resp.responseMessage = "Let me tell you more about "+clarificationExpressionGenerator.currentEntity + "\n";
					resp.responseMessage += productPageSearchResult +
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
			if (queryType == 9 || queryType == 91 || queryType == 92) { // user got virtual dialogue content and get in by further searching or get out
				if (query.toLowerCase().startsWith("yes")){
					queryType = 0;
					clarificationExpressionGenerator.reset();
					resp.responseMessage = "\nNow you can ask a NEW question";
				} else  if (query.toLowerCase().startsWith("yes")){
					resp.responseMessage = clarificationExpressionGenerator.getBestAvailableCurrentAnswer();
				} else { // do search
					resp.preStringIntroducingContent = "This is a fragment of a conversation between other people related to your query" ;
					resp.mainContent = clarificationExpressionGenerator.closestDialogueFragment(query);
					resp.postStringEnumerNextOptions = "Are you OK with this answer? yes/no/ search specific things";
					resp.responseMessage = resp.preStringIntroducingContent +":\n" +  resp.mainContent +"\n" + resp.postStringEnumerNextOptions;
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
			resp.preStringIntroducingContent = "I think you will find this information useful";
			resp.mainContent = getAnswerNum(0, searchRes);
			resp.postStringEnumerNextOptions = "Are you happy with this answer? / yes / no / more";
			resp.responseMessage = resp.preStringIntroducingContent +":\n" +  resp.mainContent +"\n" + resp.postStringEnumerNextOptions;
			//todo prepare next steps
					
			//showPage(getAnswerUrl(0, searchRes));
			queryType = 0;
		} else {
			resp.preStringIntroducingContent = "These are the opinions on the topic. Which one do you want to argue for or against? Please select";
			resp.mainContent =clarificationStr;
			resp.setClarificationOptions(clarificationPair.getSecond());
			resp.postStringEnumerNextOptions ="Select an option";
			resp.responseMessage = resp.preStringIntroducingContent +":\n" +  resp.mainContent +"\n" + resp.postStringEnumerNextOptions;
			
			queryType = 1;
		}
		return resp;
	}

	public void showPage(String url){
		if (oS==null)
			return;
		if (url!=null && (oS.indexOf("mac") >= 0 || oS.indexOf("win") >= 0 ))
			browser.runEXE(new String[]{ "bash", "-c",
					"/Applications/Google\\ Chrome.app/Contents/MacOS/Google\\ Chrome --foo --bar=2 " + url}, "/bin");
	}

	public static void main(String[] args){	

		EntryPointVirtualPersuasiveDialogue wrapper = new EntryPointVirtualPersuasiveDialogue();
		while(true){
			System.out.print("\nEnter your selection of a controversial topic >");
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

