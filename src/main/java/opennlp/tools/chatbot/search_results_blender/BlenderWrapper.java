package opennlp.tools.chatbot.search_results_blender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import opennlp.tools.chatbot.ChatIterationResult;
import opennlp.tools.chatbot.SearchSessionManager;
import opennlp.tools.chatbot.wrapper.BotRequest;
import opennlp.tools.chatbot.wrapper.BotResponse;
import opennlp.tools.chatbot.wrapper.SearchSessionManagerWrapper;
import opennlp.tools.parse_thicket.Pair;
import opennlp.tools.textsimilarity.TextProcessor;

public class BlenderWrapper extends SearchSessionManagerWrapper {
	private static Logger LOG = Logger
			.getLogger("opennlp.tools.chatbot.BlenderWrapper");
	protected WebCustomSearcherBlender searcher = WebCustomSearcherBlender.getInstance();
	protected BlenderClarificationExpressionGenerator clarificationExpressionGenerator = new BlenderClarificationExpressionGenerator();

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
				return runBasicSearch(query, resp);
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
					String anaphoraQuery = AnaphoraProcessor.substituteAnaphoraExtractPhrase(previousQuery, query, searcher.getPhraseExtractor());
					//substituteAnaphora(previousQuery, query);

					return runBasicSearch(anaphoraQuery, resp);
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

	protected BotResponse runBasicSearch(String query, BotResponse resp){
		List<ChatIterationResult> searchRes = searcher.searchLongQuery(query);
		 Pair<String, List<String>> clarificationPair = clarificationExpressionGenerator.generateClarificationPair(query, searchRes);
		// no clarification needed, so just give response as a first paragraph text
		String clarificationStr = clarificationPair.getFirst();
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
			resp.setClarificationOptions(clarificationPair.getSecond());
			queryType = 1;
		}
		return resp;
	}

	public static void main(String[] args){	
		BlenderWrapper wrapper = new BlenderWrapper();

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