package opennlp.tools.chatbot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class SearchSessionManager {
	private static Logger LOG = Logger
			.getLogger("opennlp.tools.chatbot.SearchSessionManager");

	private LongQueryWebSearcher searcher = new LongQueryWebSearcher();
	private ClarificationExpressionGenerator clarificationExpressionGenerator = new ClarificationExpressionGenerator();

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
	public void	runSession(){
		System.out.print("Welcome to 'Ask Boris' chatbot! Ask me something about personal finance");
		int queryType = 0;

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
					else if (queryType == 3 && query.toLowerCase().indexOf("no")>-1){
						queryType = 0;
						System.out.println("We are now trying to use the constrainst from your previous replies...");
						List<ChatIterationResult> searchRes0 = searcher.searchLongQuery(clarificationExpressionGenerator.originalQuestion+  " " +
									clarificationExpressionGenerator.clarificationQuery);
						System.out.println("I think you will find this information useful:");
						for(int i=0; i< searchRes0.size(); i++){
							if (!clarificationExpressionGenerator.isEqualToAlreadyGivenAnswer(searchRes0.get(0).getParagraph())){
								System.out.println(searchRes0.get(0).getParagraph());
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
							System.out.println("Are you OK with this answer? yes/more/no/specify [different topic]");
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

	public static void main(String[] args){
		System.setProperty("log4j.debug", "");
		new SearchSessionManager() .runSession();
	}
}