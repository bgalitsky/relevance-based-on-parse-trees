package opennlp.tools.chatbot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import opennlp.tools.textsimilarity.TextProcessor;

public class SearchSessionManager {
	private static Logger LOG = Logger
			.getLogger("opennlp.tools.chatbot.SearchSessionManager");

	protected LongQueryWebSearcher searcher = new LongQueryWebSearcher();
	protected ClarificationExpressionGenerator clarificationExpressionGenerator = new ClarificationExpressionGenerator();

	private  Map<String, Integer> queryTypes = new HashMap<String, Integer>() ;
	public SearchSessionManager(){
		queryTypes.put("init query", 0);
		queryTypes.put("init clarification", 1);
		queryTypes.put("first system response", 2);

		queryTypes.put("reformulated query", 10);
		queryTypes.put("further clarification", 11);
		queryTypes.put("further system response", 22);

		queryTypes.put("done with this session", 3);
	}
	
	protected int queryType = 0;
	public void	runSession(){
		System.out.print("Welcome to 'Ask Boris' chatbot! Ask me something about personal finance");
		

		while(true){
			System.out.print("\nEnter your response or query >");
			try {
				//  open up standard input
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				String query = null;

				query = br.readLine();
				if (query.equals("q"))
					System.exit(0);

				if (queryType == 0) {
					clarificationExpressionGenerator.reset();

					List<ChatIterationResult> searchRes0 = searcher.searchLongQuery(query);
					String clarificationStr = clarificationExpressionGenerator.generateClarification(query, searchRes0);
					// no clarification needed, so just give response as a first paragraph text
					if (clarificationStr==null){ 
						System.out.println("I think you will find this information useful:");
						System.out.println(searchRes0.get(0).getParagraph());
						queryType = 0;
					} else {
						System.out.println("I believe these are the main topics of your query: is that what you meant? Please select");
						System.out.println(clarificationStr);
						queryType = 1;
					}
				} else 
					if (queryType == 1){
						String selectedAnswer = clarificationExpressionGenerator.matchUserResponseWithGeneratedOptions(query);
						if (selectedAnswer!=null){
							System.out.println(selectedAnswer);
							clarificationExpressionGenerator.latestAnswer = selectedAnswer;
							System.out.println("Are you OK with this answer? yes/more/no/specify [different topic]");
							queryType = 3;
						} else {
							System.out.println(clarificationExpressionGenerator.getBestAvailableCurrentAnswer());
						}
					} 
					else if (queryType == 3 && query.toLowerCase().indexOf("yes")>-1){
						queryType = 0;
						System.out.println("Now you can ask a NEW question");
					}
					else if (queryType == 3 && query.toLowerCase().indexOf("more")>-1){
						System.out.println(clarificationExpressionGenerator.getBestAvailableCurrentAnswer());
						queryType = 0;
						System.out.println("Now you can ask a NEW question");
					}
					else if (queryType == 3 && query.toLowerCase().indexOf("reduce ")>-1){
						queryType = 0;
						String domain = extractDomainFromQuery(query);
						System.out.println("We are now trying to use the constraint on the domain " + domain);
						clarificationExpressionGenerator.setDomain(domain);
						List<ChatIterationResult> searchRes0 = searcher.searchLongQuery(clarificationExpressionGenerator.originalQuestion +
								" site:"+domain);
						System.out.println(searchRes0.get(0).getParagraph());
						queryType = 0;
						System.out.println("Now you can ask a NEW question");
					}
					else if (queryType == 3 && query.toLowerCase().indexOf("no")>-1){
						queryType = 0;
						System.out.println("We are now trying to use the constrainst from your previous replies...");
						List<ChatIterationResult> searchRes0 = searcher.searchLongQuery(clarificationExpressionGenerator.originalQuestion+  " " +
									clarificationExpressionGenerator.clarificationQuery);
						System.out.println("I think you will find this information useful:");
						for(int i=0; i< searchRes0.size(); i++){
							if (!clarificationExpressionGenerator.isEqualToAlreadyGivenAnswer(searchRes0.get(i).getParagraph())){
								System.out.println(searchRes0.get(i).getParagraph());
								break;
							}
						}
						queryType = 0;
						System.out.println("\nNow you can ask a NEW question");
						// another topic, no 'no, yes, more'
					} else if (queryType == 3){
						String selectedAnswer = clarificationExpressionGenerator.matchUserResponseWithGeneratedOptions(query);
						if (selectedAnswer!=null){
							System.out.println(selectedAnswer);
							clarificationExpressionGenerator.latestAnswer = selectedAnswer;
							System.out.println("Are you OK with this answer? yes/more/no/specify [different topic]/reduce search result to domain");
							queryType = 3;
						} else {
							System.out.println(clarificationExpressionGenerator.getBestAvailableCurrentAnswer());
						}
					}

			} catch (IOException ioe) {
				System.err.println("IO error");
				System.exit(1);
			}
		}
	}

	protected static String extractDomainFromQuery(String query) {
	    String[] tokens = query.split(" ");
	    for(String t: tokens){
	    	int posDot =  t.lastIndexOf('.'), len = t.length();
	    	if (posDot>2 && len-posDot<5 && len-posDot>1)
	    		return t;
	    	
	    }
	    
	    return null;
    }
	public static void main(String[] args){
		System.out.println( extractDomainFromQuery("reduce to finance.yahoo.com"));
		System.setProperty("log4j.debug", "");
		new SearchSessionManager() .runSession();
	}
}