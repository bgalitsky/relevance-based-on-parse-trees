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
import opennlp.tools.chatbot.LongQueryWebSearcher;
import opennlp.tools.chatbot.SearchSessionManager;
import opennlp.tools.chatbot.wrapper.BotRequest;
import opennlp.tools.chatbot.wrapper.BotResponse;
import opennlp.tools.chatbot.wrapper.SearchSessionManagerWrapper;
import opennlp.tools.parse_thicket.Pair;
import opennlp.tools.parse_thicket.kernel_interface.TreeKernelRunner;
import opennlp.tools.textsimilarity.TextProcessor;

public class DialogueManagerRus extends SearchSessionManagerWrapper {
	private static Logger LOG = Logger
			.getLogger("opennlp.tools.chatbot.BlenderWrapper");
	protected LongQueryWebSearcher  searcher = LongQueryWebSearcher .getInstance();
	protected BlenderClarificationExpressionGenerator clarificationExpressionGenerator = new BlenderClarificationExpressionGenerator();
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
				resp.responseMessage = "Вы можете задать новый вопрос";
				return resp;
			}
			
			if (query.toLowerCase().startsWith("do order") || query.toLowerCase().startsWith("do reservation") || isWebDomainInQuery(query)!=null){
				currDomain = isWebDomainInQuery(query);
				queryType = 7;
				clarificationExpressionGenerator.reset();
				resp.responseMessage = "Вы можете запросить свой заказ";
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
						logSilent("Вас устраивает предложенный ответ? да/подробнее/нет/уточни [другая тема] / reduce search to web domain");
						resp.responseMessage = selectedAnswer + "\n" + "Вас устраивает предложенный ответ? да/подробнее/нет/уточни [другая тема]/ reduce search to web domain";
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
					logSilent("Можете задать новый вопрос");
					resp.responseMessage = "Можете задать новый вопрос";
					return resp;
				}
				else if (queryType == 3 && query.toLowerCase().indexOf("more")>-1){
					logSilent(clarificationExpressionGenerator.getBestAvailableCurrentAnswer());
					queryType = 0;
					logSilent("Можете задать новый вопрос");
					resp.responseMessage =  clarificationExpressionGenerator.getBestAvailableCurrentAnswer() + "\nМожете задать новый вопрос";
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
					logSilent("Можете задать новый вопрос");
					resp.responseMessage =  "We are now trying to use the constraint on the domain " + domain +"\n" + 
							getAnswerNum(0, searchRes) + "\nМожете задать новый вопрос";
					return resp;
				}
				else if (queryType == 3 && query.toLowerCase().indexOf("no")>-1){
					queryType = 0;
					logSilent("We are now trying to use the constrainst from your previous replies...");
					List<ChatIterationResult>  searchRes = searcher.searchLongQuery(clarificationExpressionGenerator.originalQuestion+  " " +
							clarificationExpressionGenerator.clarificationQuery);
					logSilent("Эта информация может оказаться полезной: ");
					resp.responseMessage = "We are now trying to use the constrainst from your previous replies..." +
							"\nЭта информация может оказаться полезной:\n";

					for(int i=0; i< searchRes.size(); i++){
						if (!clarificationExpressionGenerator.isEqualToAlreadyGivenAnswer(getAnswerNum(i, searchRes))){
							logSilent(getAnswerNum(0, searchRes));
							resp.responseMessage += getAnswerNum(i, searchRes);
							break;
						}
					}
					queryType = 0;
					logSilent("\nМожете задать новый вопрос");
					resp.responseMessage += "\nМожете задать новый вопрос";
					return resp;
				} 
				else if (queryType == 3 && AnaphoraProcessor.isAnaphoraQuery(query)){
					queryType = 0; // proceed as regular initial search
					String previousQuery = clarificationExpressionGenerator.originalQuestion;
					String anaphoraQuery = AnaphoraProcessor.substituteAnaphoraExtractPhrase(previousQuery, query, searcher.getPhraseExtractor());
					//substituteAnaphora(previousQuery, query);

					return runBasicSearch(anaphoraQuery, resp);
				} 
				else if (queryType == 3){ // default processing of user response after clarification
					String selectedAnswer = clarificationExpressionGenerator.matchUserResponseWithGeneratedOptions(query);
					if (selectedAnswer!=null){
						logSilent(selectedAnswer);
						clarificationExpressionGenerator.latestAnswer = selectedAnswer;
						logSilent("Вас устраивает предложенный ответ? да/подробнее/нет/уточни [другая тема]/reduce search result to domain");
						queryType = 3;
						resp.responseMessage = selectedAnswer + "\nВас устраивает предложенный ответ? да/подробнее/нет/уточни [другая тема]/reduce search result to domain";
					} else {
						logSilent(clarificationExpressionGenerator.getBestAvailableCurrentAnswer());
						resp.responseMessage = clarificationExpressionGenerator.getBestAvailableCurrentAnswer();
					}
					return resp;
				}
			if (queryType == 4){ // 
				if (query.toLowerCase().equals("no")){
					queryType = 0;
					logSilent("\nМожете задать новый вопрос");
					resp.responseMessage = "\nnМожете задать новый вопрос";
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

		} catch (Exception ioe) {
			ioe.printStackTrace();
		}
		return resp;
	}

	protected BotResponse runBasicSearch(String query, BotResponse resp){
		List<ChatIterationResult> searchRes = searcher.searchLongQuery(query);
		 Pair<String, List<String>> clarificationPair = clarificationExpressionGenerator.generateClarificationPair(query, searchRes);
		// no clarification needed, so just give response as a first paragraph text
		String clarificationStr = clarificationPair.getFirst();
		if (clarificationStr==null){ 
			logSilent("I think you will find this information useful:\n");
			logSilent(getAnswerNum(0, searchRes));
			resp.responseMessage = "I think you will find this information useful:"+getAnswerNum(0, searchRes);
			showPage(getAnswerUrl(0, searchRes));
			queryType = 0;
		} else {
			logSilent("Это основные темы вашего запроса: это то, что вы имели в виду? Пожалуйста выберите");
			logSilent(clarificationStr);
			resp.responseMessage = "Это основные темы вашего запроса: это то, что вы имели в виду? Пожалуйста выберите \n"+ 
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
		
		DialogueManagerRus wrapper = new DialogueManagerRus();
		while(true){
			System.out.print("\nВведите свой запрос >");
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

