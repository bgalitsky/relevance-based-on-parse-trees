package opennlp.tools.chatbot.wrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import opennlp.tools.chatbot.ChatIterationResult;
import opennlp.tools.chatbot.LongQueryWebSearcher;
import opennlp.tools.chatbot.SearchSessionManager;
import opennlp.tools.textsimilarity.TextProcessor;

public class SearchSessionManagerWrapper extends SearchSessionManager {
	private static Logger LOG = Logger
			.getLogger("opennlp.tools.chatbot.SearchSessionManagerWrapper");
	private long sessionId;
	LongQueryWebSearcher searcherWithCaching = LongQueryWebSearcher.getInstance();

	public void reset(){
		queryType = 0;
		clarificationExpressionGenerator.reset();
		sessionId = (long) (Math.random()*10000000000l);
	}
	
	private static SearchSessionManagerWrapper instance;
	public synchronized static SearchSessionManagerWrapper getInstance() {
	    if (instance == null)
	      instance = new SearchSessionManagerWrapper();

	    return instance;
	  }

	public BotResponse runSessionIteration(BotRequest request){
		String query = request.getQuery();
		BotResponse resp = runSessionIteraction(query);
		resp.setUserId(request.getUserID());
		resp.setSessionId(request.sessionId );
		return resp;
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

			if (queryType == 0) {
				//clarificationExpressionGenerator.reset();
				if (query.toLowerCase().startsWith("anything") || 
						query.toLowerCase().indexOf(" up to")>-1 || query.toLowerCase().indexOf("what's up")>-1 
						|| query.toLowerCase().indexOf(" want me to know")>-1){
					queryType = 6;
					resp.responseMessage = searcherWithCaching.produceAlertForRandomTopic();
					queryType = 0;
					logSilent("Now you can ask a NEW question");
					resp.responseMessage += "\nNow you can ask a NEW question";
					return resp;
				}

				List<ChatIterationResult> searchRes = searcher.searchLongQuery(query);
				String clarificationStr = clarificationExpressionGenerator.generateClarification(query, searchRes);
				// no clarification needed, so just give response as a first paragraph text
				if (clarificationStr==null){ 
					logSilent("I think you will find this information useful:\n");
					logSilent(getAnswerNum(0, searchRes));
					resp.responseMessage = "I think you will find this information useful:"+getAnswerNum(0, searchRes);
					queryType = 0;
				} else {
					logSilent("I believe these are the main topics of your query: is that what you meant? Please select");
					logSilent(clarificationStr);
					resp.responseMessage = "I believe these are the main topics of your query: is that what you meant? Please select \n"+ 
							clarificationStr;
					queryType = 1;
				}
				return resp;
			} else 
				if (queryType == 1){
					String selectedAnswer = clarificationExpressionGenerator.matchUserResponseWithGeneratedOptions(query);
					if (selectedAnswer!=null){
						logSilent(selectedAnswer);
						clarificationExpressionGenerator.latestAnswer = selectedAnswer;
						logSilent("Are you OK with this answer? yes/more/no/specify [different topic] / reduce search to web domain");
						resp.responseMessage = selectedAnswer + "\n" + "Are you OK with this answer? yes/more/no/specify [different topic]/ reduce search to web domain";
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
				else if (queryType == 3 && query.toLowerCase().indexOf("no")>-1){
					queryType = 0;
					logSilent("We are now trying to use the constrainst from your previous replies...");
					List<ChatIterationResult>  searchRes = searcher.searchLongQuery(clarificationExpressionGenerator.originalQuestion+  " " +
							clarificationExpressionGenerator.clarificationQuery);
					logSilent("I think you will find this information useful:");
					resp.responseMessage = "We are now trying to use the constrainst from your previous replies..." +
							"\nI think you will find this information useful:\n";

					for(int i=0; i< searchRes.size(); i++){
						if (!clarificationExpressionGenerator.isEqualToAlreadyGivenAnswer(getAnswerNum(0, searchRes))){
							logSilent(getAnswerNum(0, searchRes));
							resp.responseMessage += getAnswerNum(0, searchRes);
							break;
						}
					}
					queryType = 0;
					logSilent("\nNow you can ask a NEW question");
					resp.responseMessage += "\nNow you can ask a NEW question";
					return resp;
				} 
				else if (queryType == 3 && isSameEntityQuery(query)){
					queryType = 4;
					String productPageSeaarchResult = clarificationExpressionGenerator.searchProductPage(query);

					resp.responseMessage = "Let me tell you more about "+clarificationExpressionGenerator.currentEntity + "\n";

					resp.responseMessage += productPageSeaarchResult +
							"\n More questions about this product?\n";
					return resp;
				} 
				else if (queryType == 3){ // default processing of user response after clarification
					String selectedAnswer = clarificationExpressionGenerator.matchUserResponseWithGeneratedOptions(query);
					if (selectedAnswer!=null){
						logSilent(selectedAnswer);
						clarificationExpressionGenerator.latestAnswer = selectedAnswer;
						logSilent("Are you OK with this answer? yes/more/no/specify [different topic]/reduce search result to domain");
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

		} catch (Exception ioe) {
			ioe.printStackTrace();
		}
		return resp;
	}

	protected boolean isSameEntityQuery(String query) {
		if ( (query.toLowerCase().indexOf("this")>-1) || (query.toLowerCase().indexOf("that")>-1) ||
				(query.toLowerCase().indexOf("it ")>-1) || (query.toLowerCase().indexOf("its ")>-1))
			return true;
		return false;
	}

	protected String getAnswerNum(int i, List<ChatIterationResult> searchResult) {
		if (searchResult==null || searchResult.isEmpty())
			return "I got stuck trying to find this answer. Lets try to go back...";
		if (i>= searchResult.size())
			return  searchResult.get(0).getParagraph();

		return searchResult.get(i).getParagraph();
	}
	
	protected String getAnswerUrl(int i, List<ChatIterationResult> searchResult) {
		if (searchResult==null || searchResult.isEmpty())
			return "I got stuck trying to find this answer. Lets try to go back...";
		if (i>= searchResult.size())
			return  searchResult.get(0).getUrl();

		return searchResult.get(i).getUrl();
	}

	protected void logSilent(String msg) {
		//String version = "ver0.8";
		//LOG.info(version  + " | " + msg); 
	}

	public static void main(String[] args){
		System.setProperty("log4j.debug", "");
		SearchSessionManagerWrapper wrapper = new SearchSessionManagerWrapper();

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