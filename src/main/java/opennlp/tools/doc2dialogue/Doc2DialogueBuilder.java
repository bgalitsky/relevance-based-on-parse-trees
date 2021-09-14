package opennlp.tools.doc2dialogue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.clulab.discourse.rstparser.DiscourseTree;

import opennlp.tools.chatbot.ChatBotCacheSerializer;
import opennlp.tools.chatbot.ChatIterationResult;
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

public class Doc2DialogueBuilder {
	protected MatcherExternalRST matcher = new MatcherExternalRST();
	protected ParseCorefBuilderWithNERandRST ptBuilder = matcher.ptBuilderRST;
	protected PT2ThicketPhraseBuilderExtrnlRST phraseBuilder = matcher.phraseBuilder;
	protected Set<String> dupes = new HashSet<String> ();
	protected BingQueryRunner brunner = new BingQueryRunner();
	protected ParseTreeChunkListScorer scorer = new ParseTreeChunkListScorer();

	//private Map<String, List<HitBase>> query_listOfSearchResults = new HashMap<String, List<HitBase>>();
	private Map<String, String> queryConfirmed = new HashMap<String, String>();
	private BingCacheSerializer serializer = new BingCacheSerializer();
	private StringDistanceMeasurer meas = new StringDistanceMeasurer();
	private boolean bSentiments = true;;

	public Doc2DialogueBuilder(){
		//query_listOfSearchResults = (Map<String, List<HitBase>>) serializer.readObject();
		queryConfirmed =  (Map<String, String>)serializer.readObject();
		if (queryConfirmed ==null) 
			queryConfirmed = new HashMap<String, String>();
	}

	public List<List<ParseTreeNode>> buildDialogueFromParagraph(String text){
		ParseThicketWithDiscourseTree pt=null;
		try {
			pt = ptBuilder.buildParseThicket(text);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		if (pt==null)
			return null;
		List<List<ParseTreeNode>> phrs = phraseBuilder.buildPT2ptPhrases(pt);
		pt.setPhrases(phrs);

		DiscourseTree dt = pt.getDt();
		List<List<ParseTreeNode>> phrases = new ArrayList<List<ParseTreeNode>>();

		formEduPhrases(dt,  phrases,  pt, "phrases" );

		List<List<ParseTreeNode>> shuffledPhrases = shuffleEduPhrasesWithInsertedQuestions(phrases);

		/*for(List<ParseTreeNode>p: shuffledPhrases){
			System.out.println(ParseTreeNode.toWordString(p));
		}*/

		return shuffledPhrases;
	}

	private List<List<ParseTreeNode>> shuffleEduPhrasesWithInsertedQuestions(List<List<ParseTreeNode>> phrases) {
		int[] periods = new int[phrases.size()], questions = new int[phrases.size()]; 
		for(int i = 0; i<phrases.size(); i++ ){
			if (phrases.get(i).get(phrases.get(i).size()-1).getWord().startsWith("."))
				periods[i] = 1;
			else 
				periods[i] = 0;

			if (phrases.get(i).get(phrases.get(i).size()-1).getWord().startsWith("?"))
				questions[i] = 1;
			else 
				questions[i] = 0;
		}


		List<List<ParseTreeNode>> phrasesNew = new ArrayList<List<ParseTreeNode>>(phrases);
		int currQuestion = -1;
		for(int i = phrases.size()-1; i>=0; i-- ){
			if (questions[i] == 1){
				currQuestion = i;
			} else  if (periods[i] == 1 && (currQuestion >=0  || i ==0) && i< currQuestion-1 ) {
				List<ParseTreeNode> p = phrasesNew.get(currQuestion);
				phrasesNew.remove(currQuestion);
				phrasesNew.add(i+1, p);
				currQuestion = -1; 
			}
		}
		return phrasesNew;
	}

	private void formEduPhrases(DiscourseTree dt, List<List<ParseTreeNode>> phrases,  ParseThicketWithDiscourseTree pt, String typeAbove   ) {
		List<List<ParseTreeNode>> nodesThicket = pt.getNodesThicket();
		Map<Integer, List<List<ParseTreeNode>>> sentNumPhrases = pt.getSentNumPhrases();
		if (dt.isTerminal()) {
			List<ParseTreeNode> phraseEdu = new ArrayList<ParseTreeNode>();

			try {
				for(int i = dt.firstToken().copy$default$2(); i<=dt.lastToken().copy$default$2(); i++){
					phraseEdu.add(nodesThicket.get(dt.lastSentence()).get(i)); 
				}
				//System.out.println(typeAbove);
				if (typeAbove.equals("Satellite")){
					List<ParseTreeNode> phraseQuestionAdd = new ArrayList<ParseTreeNode>();
					//phraseQuestionAdd.add(new ParseTreeNode(" - ", "", null, null));

					List<ParseTreeNode> bestPhrase = null;
					try {
						bestPhrase = tryToFindBestPhrase(sentNumPhrases.get(dt.lastSentence()));
					} catch (Exception e) {
						//e.printStackTrace();
					}
					if (bestPhrase != null) {
						if (dupes.contains(ParseTreeNode.toWordString(bestPhrase))){
							bestPhrase = null;
						} else 
							dupes.add(ParseTreeNode.toWordString(bestPhrase));
					}
					if (bestPhrase==null) {
						//System.out.println("Could not find a phrase to build a question from");
						/*	bestPhrase= phraseEdu;
						phraseAdd.add(new ParseTreeNode( "What", "QQ", null, 0));
						for(ParseTreeNode n: phraseEdu){
							if (n.getPos().equals("RB")){
								continue;
							}

							if (n.getWord().equals("I") || n.getWord().equals("me")){
								phraseAdd.add(new ParseTreeNode( "you", "PRP", null, 0));
								continue;
							}
							if (n.getWord().equals("my")){
								phraseAdd.add(new ParseTreeNode( "your", "PRP", null, 0));
								continue;
							}
							if (n.getPos().equals("VBN")){
								phraseAdd.add(n);
								break;
							}
							if (n.getPos().startsWith("NN")){
								phraseAdd.add(n);
								break;
							}

						} */
					} else {
						for(ParseTreeNode n: bestPhrase){
							if (n.getPos().equals("RB") || n.getPos().equals("CD") || n.getPos().equals("-RRB-")){
								continue;
							}
					/*		if (n.getWord().equals("I") || n.getWord().equals("me")){
								phraseQuestionAdd.add(new ParseTreeNode( "you", "PRP", null, 0));
								continue;
							} */
							if (n.getWord().equals("my")){
								phraseQuestionAdd.add(new ParseTreeNode( "your", "PRP", null, 0));
								continue;
							}
							// default
							phraseQuestionAdd.add(n);
						}

						String posLast = phraseQuestionAdd.get(phraseQuestionAdd.size()-1).getPos();
						if (posLast.startsWith("P") || posLast.equals("TO") || posLast.startsWith("J"))
							phraseQuestionAdd.remove(phraseQuestionAdd.size()-1);
						// processing the tail: removing wrong ending POSs
						// 1)-  What Happens When You Install a Patch (System 
						// 2)-  What happens if I put a conductor in the third hole of a  ?
						int count = 0;
						for(int j = phraseQuestionAdd.size()-1; j>=3; j--){
							// remove all till non-alpha
							if (!StringUtils.isAlpha(phraseQuestionAdd.get(j).getWord())){
								for(int c = 0; c<=count ; c++)
									phraseQuestionAdd.remove(phraseQuestionAdd.size()-1);

								break;
							}
							count++;
						}

						ParseTreeNode endNodePos = phraseQuestionAdd.get(phraseQuestionAdd.size()-1);
						if ( endNodePos.getWord().equals("a") || endNodePos.getWord().equals("the") || 
								endNodePos.getPos().startsWith("TO") ){
							phraseQuestionAdd.remove(phraseQuestionAdd.size()-1);
							if ( endNodePos.getWord().equals("a") || endNodePos.getWord().equals("the") ||  endNodePos.getPos().startsWith("TO") ||
									endNodePos.getPos().startsWith("PRP")){
								phraseQuestionAdd.remove(phraseQuestionAdd.size()-1);
							}
						}

						phraseQuestionAdd.add(new ParseTreeNode( "?", "QQ", null, 0));

						//System.out.println(ParseTreeNode.toWordString(phraseQuestionAdd));

						//System.out.println( sentNumPhrases.get(dt.lastSentence()));

						List<ParseTreeNode> minedQPhrase = //correctQuestionViaWebMining(
								(phraseQuestionAdd);
						
						if (minedQPhrase!=null)
							minedQPhrase.add(0, new ParseTreeNode(" - ", "", null, null));
						else if (phraseQuestionAdd !=null)
							phraseQuestionAdd.add(0, new ParseTreeNode(" - ", "", null, null));
							

						if (minedQPhrase!=null && !minedQPhrase.isEmpty())
							phrases.add(minedQPhrase);
						else 
							phrases.add(phraseQuestionAdd);
					}			
				}
				if (bSentiments ){
				// assess sentiment
				int sentiment = ptBuilder.getSentiment(ParseTreeNode.toWordString(phraseEdu));
			/*	if (sentiment >2)
					phraseEdu.add(new ParseTreeNode(" ++++ ", "", null, null));
				else 
					if (sentiment < 0)
						phraseEdu.add(new ParseTreeNode(" ---- ", "", null, null));
						*/
				}
				
				phrases.add(phraseEdu);
				//System.out.println(ParseTreeNode.toWordString(phraseEdu));
				//System.out.println(phraseEdu+"\n");
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		} else {
			DiscourseTree[] kids = dt.children();

			if (kids != null) {
				int order =0;
				for (DiscourseTree kid : kids) {
					String type = "Nucleus";
					if  ((order==0 && dt.relationDirection().toString().startsWith("Right") ||
							( order==1 && dt.relationDirection().toString().startsWith("Left"))))
						type = "Satellite";
					formEduPhrases(kid, phrases, pt, type);
					order++;
				}
			}
			return;
		}
	}

	private List<ParseTreeNode> tryToFindBestPhrase(List<List<ParseTreeNode>> list) {
		List<ParseTreeNode> results = new ArrayList<ParseTreeNode>();

		//[<8>NP'a':DT, <9>NP'virtual':JJ, <10>NP'personal':JJ, <11>NP'assistant':NN]
		for( List<ParseTreeNode> ps: list){
			if (dupes.contains(ParseTreeNode.toWordString(ps)))
				continue;
			if (ps.get(0).getPos().equals("DT") && ps.size()>3 &&
					( ps.get(1).getPos().equals("JJ") || ps.get(1).getPos().equals("NN") ) &&
					( ps.get(2).getPos().equals("JJ") || ps.get(2).getPos().equals("NN") ) &&
					ps.size()< 8){
				double flag = Math.random();
				if (flag>0.66){
					results.add(new ParseTreeNode("Tell", "", null, null));
					results.add(new ParseTreeNode("me", "", null, null));
				} else if (flag<0.33){
					results.add(new ParseTreeNode("Can", "", null, null));
					results.add(new ParseTreeNode("you", "", null, null));
					results.add(new ParseTreeNode("share", "", null, null));
				} else {
					results.add(new ParseTreeNode("How", "", null, null));
					results.add(new ParseTreeNode("can", "", null, null));
					results.add(new ParseTreeNode("I", "", null, null));
					results.add(new ParseTreeNode("learn", "", null, null));
				}


				results.add(new ParseTreeNode("more", "", null, null));
				results.add(new ParseTreeNode("about", "", null, null));
				
				dupes.add(ParseTreeNode.toWordString(ps));

				ps.remove(0); // remove preposition
				results.addAll(ps); 
				return results;
			}
			//[<18>NP'naturalistic':JJ, <19>NP'conversations':NNS]
			if (ps.size()>=2 &&
					( ps.get(0).getPos().equals("JJ") || ps.get(0).getPos().equals("NN") ) &&
					( ps.get(1).getPos().equals("JJ") || ps.get(1).getPos().equals("NN") ) &&
		//			( ps.get(2).getPos().equals("JJ") || ps.get(2).getPos().equals("NN") ) &&
					ps.size()< 8){
				double flag = Math.random();
				if (flag>0.66){
					results.add(new ParseTreeNode("Provide", "", null, null));
					results.add(new ParseTreeNode("details", "", null, null));
					results.add(new ParseTreeNode("on", "", null, null));
				} else if (flag<0.33){
					results.add(new ParseTreeNode("Mind", "", null, null));
					results.add(new ParseTreeNode("telling", "", null, null));
					results.add(new ParseTreeNode("more", "", null, null));
					results.add(new ParseTreeNode("on", "", null, null));
				} else {
					results.add(new ParseTreeNode("Do", "", null, null));
					results.add(new ParseTreeNode("you", "", null, null));
					results.add(new ParseTreeNode("know", "", null, null));
					results.add(new ParseTreeNode("about", "", null, null));
				}
				dupes.add(ParseTreeNode.toWordString(ps));
				results.addAll(ps); 
				return results;
			}
		}

		for( List<ParseTreeNode> ps: list){
			if (ps.get(0).getPhraseType().startsWith("WH") && ps.size()>2 )
				return ps;
			//'what':WP
			//[<10>SBAR'what':WP, <11>SBAR'I':PRP, <12>SBAR'like':VBP, 
			//<13>SBAR'to':TO, <14>SBAR'write':VB],
			if (ps.get(0).getPos().startsWith("WH") && ps.size()> 3 )
				return ps;

			//[<4>SBAR'what':WP, <5>SBAR'he':PRP, <6>SBAR'argues':VBZ]
			if (ps.get(0).getPos().startsWith("WP") && ps.size()>3 && ps.size()<10)
				return ps;
			//<36>VP'to':TO
			if (ps.get(0).getPos().equals("TO") && ps.size()>5 && ps.size()< 8){

				results.add(new ParseTreeNode("Do", "", null, null));
				results.add(new ParseTreeNode("you", "", null, null));
				results.add(new ParseTreeNode("want", "", null, null));
				results.addAll(ps); 
				return results;
			}
			//[<12>VP'being':VBG, <13>VP'responsible':JJ, <14>VP'for':IN, <15>VP'shooting':VBG, <16>VP'down':RB, <17>VP'plane':NN]
			if (ps.size()>2  && ps.get(0).getPos().equals("VBG") && ( ps.get(1).getPos().startsWith("N") || ps.get(1).getPos().startsWith("J")) &&
					ps.size()< 8){

				results.add(new ParseTreeNode("Who", "", null, null));
				results.add(new ParseTreeNode("is", "", null, null));
				ps.remove(0);
				results.addAll(ps); 
				return results;
			}
			//<1>PP'At':IN, <2>PP'the':DT, <3>PP'beginning':NN
			if (ps.get(0).getPos().equals("IN") && ps.size()>2 && ps.get(1).getPos().equals("DT")  && ps.size()<= 8
					&& ps.size()>3){

				results.add(new ParseTreeNode("What", "", null, null));
				results.add(new ParseTreeNode("happens", "", null, null));

				results.addAll(ps); 
				return results;
			}
			//<3>SBARQ'it':PRP, <4>SBARQ'been':VBN, <5>SBARQ'so':RB, <6>SBARQ'difficult':JJ, <7>SBARQ'?':.],		
			if (ps.get(0).getPos().equals("PRP") && ps.size()>3 && ps.get(1).getPos().equals("VBN") &&   ps.size()<= 8){
				results.add(new ParseTreeNode("Why", "", null, null));
				results.addAll(ps); 
				return results;
			}

			//[<13>PP'of':IN, <14>PP'synaptic':JJ, <15>PP'connections':NNS, <16>PP'or':CC, <17>PP'field':NN, <18>PP'properties':NNS]
			if (ps.get(0).getPos().equals("IN") && ps.size()>3 && ps.get(1).getPos().equals("JJ") &&  ps.size()<= 8){
				results.add(new ParseTreeNode("How", "", null, null));
				results.add(new ParseTreeNode("about", "", null, null));
				results.addAll(ps); 
				return results;
			}
			
			//[<1>VP'To':TO, VP'create':VB, <4>VP'service':NN, <5>VP':'::]
			if (ps.get(0).getPos().equals("TO") && ps.size()>=3 && ps.get(1).getPos().equals("VB") && 
					(ps.get(2).getPos().equals("NN") || ps.get(2).getPos().equals("JJ") )&& 
					ps.size()<= 8){
				results.add(new ParseTreeNode("How", "", null, null));
				
				results.addAll(ps); 
				return results;
			}
			//[<10>SBAR'it':PRP, <11>SBAR'should':MD, <12>SBAR'always':RB, <13>SBAR'be':VB, 
			if (ps.get(0).getPos().equals("PRP") && ps.size()>=4 && ps.get(1).getPos().equals("MD") && 
					(ps.get(2).getOriginalWord().equals("be") || ps.get(3).getOriginalWord().equals("be")  )&& 
					ps.size()<= 8){
				results.add(new ParseTreeNode("Why", "", null, null));
				ps.remove(0);
				results.addAll(ps); 
				return results;
			}
		}

		if (results.isEmpty()){
			for( List<ParseTreeNode> ps: list){
				if (ps.get(0).getPos().equals("VBP") && ps.size()>2 && ps.size()< 8){
					results.add(new ParseTreeNode("What", "", null, null));
					results.add(new ParseTreeNode("do", "", null, null));
					results.add(new ParseTreeNode("you", "", null, null));
					results.addAll(ps);
					return results;
				}
				if (ps.get(0).getPos().equals("VBD") && ps.size()>2 && ps.size()< 8){
					results.add(new ParseTreeNode("Who", "", null, null));
					results.addAll(ps);
					return results;
				}
				//<3>VP'is':VBZ, <4>VP'mostly':RB, <5>VP'interested':JJ
				if (ps.get(0).getPos().equals("VBZ") && ps.size()>2 && ( ps.get(1).getPos().equals("RB") ||  ps.get(1).getPos().equals("JJ") ) && ps.size()< 8){
					results.add(new ParseTreeNode("Who", "", null, null));
					results.addAll(ps);
					return results;
				}

				//[<5>NP'the':DT, <6>NP'White':NNP, <7>NP'House':NNP]
				if (ps.get(0).getPos().equals("DT") && ps.size()>2 &&  ps.get(1).getPos().equals("NNP")  
						&& ps.size()< 8 && ps.get(0).getPhraseType().startsWith("NP")){
					results.add(new ParseTreeNode("What", "", null, null));
					results.add(new ParseTreeNode("about", "", null, null));
					results.addAll(ps);
					return results;	
				}
				//[<1>VP'Expect':VB, <2>VP'a':DT, <3>VP'charge':NN, <4>VP'of':IN, <5>VP'$':$, VP'35':CD, <8>VP'so':RB]
				if (ps.get(0).getPos().equals("VB") && ps.size()>3 &&  ps.get(1).getPos().equals("DT")  
						&&  ps.get(2).getPos().equals("NN")  
						&& ps.size()< 10){
					results.add(new ParseTreeNode("When", "", null, null));
					results.add(new ParseTreeNode("can", "", null, null));
					results.add(new ParseTreeNode("I", "", null, null));
					results.addAll(ps);
					return results;	
				}
				//[<8>NP'your':PRP$, <9>NP'best':JJS, <10>NP'bet':NN]
				if (ps.get(0).getPos().startsWith("PRP") && ps.size()>=3 &&  ps.get(1).getPos().startsWith("JJ")  
						&&  ps.get(2).getPos().startsWith("NN")  
						&& ps.size()< 8 ){
					results.add(new ParseTreeNode("What", "", null, null));
					results.add(new ParseTreeNode("is", "", null, null));
					results.addAll(ps);
					return results;	
				}
			}
		}
		return null;
	}
	

	public List<ParseTreeNode>  correctQuestionViaWebMining(List<ParseTreeNode> questionPhrase){
		if (questionPhrase.size()<4)
			return null;

		String origQuestion = ParseTreeNode.toWordString(questionPhrase);
		List<ParseTreeNode> questionResult = new ArrayList<ParseTreeNode>();

		String foundInCache = this.queryConfirmed.get(origQuestion);
		if (foundInCache != null && foundInCache.length()>3){
			String[] bestExtrQsplit =  foundInCache.split(" ");
			for(String w: bestExtrQsplit){
				questionResult.add(new ParseTreeNode(w, "", null, null));
			}
			return questionResult;
		} else if (foundInCache != null)
			return null;

		List<HitBase> searchRes = brunner.runSearch(origQuestion);
		System.out.print(".");

		double maxScore = -1; String bestExtrQ = null;

		for(HitBase h: searchRes){
			String extractedQuestion = extractQuestionFromSearchResultTitle(h.getTitle());
			if (extractedQuestion.indexOf('|')>-1){
				extractedQuestion = extractedQuestion.substring(0, extractedQuestion.indexOf('|'));
			} else if (extractedQuestion.indexOf('-')>-1)
				extractedQuestion = extractedQuestion.substring(0, extractedQuestion.indexOf('-'));

			if (extractedQuestion.split(" ").length*1.5 < origQuestion.split(" ").length)
				continue;

			List<List<ParseTreeChunk>> res = matcher.assessRelevanceCache(origQuestion, extractedQuestion);
			double score = //meas.measureStringDistance(origQuestion, extractedQuestion);
					scorer.getParseTreeChunkListScore(res);
			if (score>maxScore){
				maxScore = score;
				bestExtrQ = extractedQuestion;
			}
		}
		if (maxScore<1.5){ //0.6)
			this.queryConfirmed.put(origQuestion,  "" );
			try {
				serializer.writeObject(queryConfirmed);
			} catch (Exception e) {
				System.err.println("Error serializing");
			}
			return null;
		}
		if (!bestExtrQ.endsWith("?"))
			bestExtrQ+=" ?";
		String[] bestExtrQsplit =  bestExtrQ.split(" ");
		for(String w: bestExtrQsplit){
			questionResult.add(new ParseTreeNode(w, "", null, null));
		}

		this.queryConfirmed.put(origQuestion,  bestExtrQ );
		// each time new query comes => write the results
		try {
			serializer.writeObject(queryConfirmed);
		} catch (Exception e) {
			System.err.println("Error serializing");
		}

		return questionResult;
	}

	// query: what do you not like to do today?
	// result: 
	//What Do You Like To Do? Song | Hobbies Song for Kids

	//How to deal with people you don't like - Business Insider
	private String extractQuestionFromSearchResultTitle(String title) {
		int indexEnd = title.indexOf('?');
		if (indexEnd<0)
			indexEnd = title.indexOf('-');
		if (indexEnd<0)
			indexEnd = title.indexOf(':');
		if (indexEnd<0)
			indexEnd = title.indexOf('|');
		if (indexEnd<0)
			indexEnd = title.indexOf('/');
		if (indexEnd<0)
			indexEnd = title.indexOf('.');
		if (indexEnd<0)
			indexEnd = title.indexOf('(');

		if (indexEnd>0)
			return title.substring(0, indexEnd);


		return title;
	}



	public static void main(String[] args){
		Doc2DialogueBuilder builder = new Doc2DialogueBuilder();

		String texts[] = new String[]{

				"I thought I d tell you a little about what I like to write. And I like to immerse myself in my topics. I just like to dive right in and become sort of a human guinea pig. And I see my life as a series of experiments. So , I work for Esquire magazine , and a couple of years ago I wrote an article called  My Outsourced Life ,  where I hired a team of people in Bangalore , India , to live my life for me. "
						+ "So they answered my emails. They answered my phone. ",
						"Dutch accident investigators say that evidence points to pro-Russian rebels as being responsible for shooting down plane. The report indicates where the missile was fired from and identifies who was in control of the territory and pins the downing of the plane on the pro-Russian rebels. "+
								"However, the Investigative Committee of the Russian Federation believes that the plane was hit by a missile from the air which was not produced in Russia. "+
								"At the same time, rebels deny that they controlled the territory from which the missile was supposedly fired."

		,"I want to start with a very basic question. At the beginning of AI, people were extremely optimistic about the field's progress, but it hasn't turned out that way. Why has it been so difficult? If you ask neuroscientists why understanding the brain is so difficult, they give you very intellectually unsatisfying answers, "
				+ "like that the brain has billions of cells, and we can't record from all of them, and so on.",
				"It turns out the ants do pretty complicated things, like path integration, for example. If you look at bees, bee navigation involves quite complicated computations, involving position of the sun, and so on and so forth. But in general what he argues is that if you take a look at animal cognition, human too, it's computational systems. Therefore, you want to look the units of computation. "
						+ "Think about a Turing machine, say, which is the simplest form of computation, you have to find units that have properties like read, write, and address."
						+ " That's the minimal computational unit, so you got to look in the brain for those. You're never going to find them if you look for strengthening of synaptic connections or field properties, and so on. You've got to start by looking for what's there and what's working and you see that from Marr's highest level.",

						"Well, like strengthening synaptic connections. Scientists have been arguing for years that if you want to study the brain properly you should begin"
								+ " by asking what tasks is it performing. So he is mostly interested in insects. So if you want to study, say, the neurology of an ant, you ask what does the ant do? ",

								"As the Trump administration scrambles to find a replacement for outgoing advisor John Kelly, officials announced Monday that a high-level White House ficus would leave for the State Arboretum of Virginia after declining the president’s offer to be chief of staff. "
										+ "The ficus has been honored to serve President Trump and the American people these last several months and plans to continue advancing the MAGA cause as a member of the private sector, read a statement drafted by an aide for the ficus, noting that the potted shrub was one of the longest-tenured and most-trusted members of the Trump administration, spending countless hours working alongside the president from a sunny spot inside the Oval Office.",

		"Rumors that the ficus was forced out following a heated argument with Jared Kushner are simply untrue. The ficus will spend the next few weeks helping with the transition of its replacement, a large fern, before departing to work in the tropical plant section of the arboretum. At press time, the White House was reportedly thrown into chaos after the large fern confirmed it would not accept the new job." };

		for(String t: texts){
			builder.buildDialogueFromParagraph(t); 
			System.out.println("\n\n");
		}



	}
}
