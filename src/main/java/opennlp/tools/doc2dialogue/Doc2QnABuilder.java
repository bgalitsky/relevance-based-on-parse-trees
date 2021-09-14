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

public class Doc2QnABuilder {
	protected MatcherExternalRST matcher = new MatcherExternalRST();
	protected ParseCorefBuilderWithNERandRST ptBuilder = matcher.ptBuilderRST;
	protected PT2ThicketPhraseBuilderExtrnlRST phraseBuilder = matcher.phraseBuilder;
	protected Set<String> dupes = new HashSet<String> ();
	protected BingQueryRunner brunner = new BingQueryRunner();
	protected ParseTreeChunkListScorer scorer = new ParseTreeChunkListScorer();

	//private Map<String, List<HitBase>> query_listOfSearchResults = new HashMap<String, List<HitBase>>();
	private Map<String, String> queryConfirmed = new HashMap<String, String>();
	private BingCacheSerializer serializer = new BingCacheSerializer();


	public Doc2QnABuilder(){
		queryConfirmed =  (Map<String, String>) serializer.readObject();
		if (queryConfirmed ==null) 
			queryConfirmed = new HashMap<String, String>();
	}

	public List<List<ParseTreeNode>> buildQuestionForParagraph(String text){
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
		return phrases;
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
				//other way around doc2dialogue
				if (!typeAbove.equals("Satellite")){
					List<ParseTreeNode> phraseQuestionAdd = new ArrayList<ParseTreeNode>();
					//phraseQuestionAdd.add(new ParseTreeNode(" - ", "", null, null));

					List<ParseTreeNode> bestPhrase = tryToFindBestPhrase(sentNumPhrases.get(dt.lastSentence()));
					if (bestPhrase != null) {
						if (dupes.contains(ParseTreeNode.toWordString(bestPhrase))){
							bestPhrase = null;
						} else 
							dupes.add(ParseTreeNode.toWordString(bestPhrase));
					}
					if (bestPhrase!=null) {
						for(ParseTreeNode n: bestPhrase){
							if (n.getPos().equals("RB") || n.getPos().equals("CD") || n.getPos().equals("-RRB-")){
								continue;
							}
							/*		if (n.getWord().equals("I") || n.getWord().equals("me")){
								phraseQuestionAdd.add(new ParseTreeNode( "you", "PRP", null, 0));
								continue;
							} 
							if (n.getWord().equals("my")){
								phraseQuestionAdd.add(new ParseTreeNode( "your", "PRP", null, 0));
								continue;
							} */
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
							// remove all till non-alpha    except -, '
							if (!StringUtils.isAlphanumeric(phraseQuestionAdd.get(j).getWord().replace("-", "a").replace("'", "a").replace("`", "a"))){
								for(int c = 0; c<=count ; c++)
									phraseQuestionAdd.remove(phraseQuestionAdd.size()-1);

								break;
							}
							count++;
						}

						ParseTreeNode endNodePos = phraseQuestionAdd.get(phraseQuestionAdd.size()-1);
						if ( endNodePos.getWord().equals("a") || endNodePos.getWord().equals("the") || endNodePos.getPos().startsWith("IN") ||
								endNodePos.getPos().startsWith("TO") ){
							phraseQuestionAdd.remove(phraseQuestionAdd.size()-1);
							// once again
							endNodePos = phraseQuestionAdd.get(phraseQuestionAdd.size()-1);
							if ( endNodePos.getWord().equals("a") || endNodePos.getWord().equals("the") 
									||  endNodePos.getPos().startsWith("TO") ||endNodePos.getPos().startsWith("IN") ||
									endNodePos.getPos().startsWith("PRP")){
								phraseQuestionAdd.remove(phraseQuestionAdd.size()-1);
							}
						}

						// phrase interruption control
						String endNodeWord = phraseQuestionAdd.get(phraseQuestionAdd.size()-1).getWord();
						if (endNodeWord.equals("more") || endNodeWord.equals("about") || endNodeWord.equals("on") || endNodeWord.equals("be")){
							System.err.println("Wrong synthesized query cut!");
							phraseQuestionAdd = null; // so that it is not added
						} else {



							phraseQuestionAdd.add(new ParseTreeNode( "?", "QQ", null, 0));

							//System.out.println(ParseTreeNode.toWordString(phraseQuestionAdd));

							//System.out.println( sentNumPhrases.get(dt.lastSentence()));

							List<ParseTreeNode> minedQPhrase = //correctQuestionViaWebMining(
									(phraseQuestionAdd);




							if (minedQPhrase!=null && !minedQPhrase.isEmpty())
								phrases.add(minedQPhrase);
							else 
								phrases.add(phraseQuestionAdd);
						}
					}			
				}

				// no need to add 
				//phrases.add(phraseEdu);

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
					ps.size()< 10){ //8
				double flag = Math.random();
				if (flag>0.80){
					results.add(new ParseTreeNode("Tell", "", null, null));
					results.add(new ParseTreeNode("me", "", null, null));
				} else if (flag>0.60){
					results.add(new ParseTreeNode("Can", "", null, null));
					results.add(new ParseTreeNode("you", "", null, null));
					results.add(new ParseTreeNode("share", "", null, null));
				} else if (flag>0.45){
					results.add(new ParseTreeNode("How", "", null, null));
					results.add(new ParseTreeNode("can", "", null, null));
					results.add(new ParseTreeNode("I", "", null, null));
					results.add(new ParseTreeNode("learn", "", null, null));
				} else if (flag>0.25){
					results.add(new ParseTreeNode("Inform", "", null, null));
					results.add(new ParseTreeNode("me", "", null, null));
				}else if (flag>0.10){
					results.add(new ParseTreeNode("Let", "", null, null));
					results.add(new ParseTreeNode("me", "", null, null));
					results.add(new ParseTreeNode("know", "", null, null));
				}


				results.add(new ParseTreeNode("more", "", null, null));
				results.add(new ParseTreeNode("about", "", null, null));

				dupes.add(ParseTreeNode.toWordString(ps));

				ps.remove(0); // remove preposition
				results.addAll(ps); 
				return results;
			}
			//[<18>NP'naturalistic':JJ, <19>NP'conversations':NNS]
			//[<14>NP'TruMark':NNP, <15>NP'Financial':NNP, <16>NP'Credit':NNP, <17>NP'Union':NNP
			if (ps.size()>=2 &&
					( ps.get(0).getPos().equals("JJ") || ps.get(0).getPos().startsWith("NN") ) &&
					( ps.get(1).getPos().equals("JJ") || ps.get(1).getPos().startsWith("NN") ) &&
					//			( ps.get(2).getPos().equals("JJ") || ps.get(2).getPos().equals("NN") ) &&
					ps.size()< 8){
				double flag = Math.random();
				if (flag>0.80){
					results.add(new ParseTreeNode("Provide", "", null, null));
					results.add(new ParseTreeNode("details", "", null, null));
					results.add(new ParseTreeNode("on", "", null, null));
				} else if (flag>0.60){
					results.add(new ParseTreeNode("Mind", "", null, null));
					results.add(new ParseTreeNode("telling", "", null, null));
					results.add(new ParseTreeNode("more", "", null, null));
					results.add(new ParseTreeNode("on", "", null, null));
				} else if (flag>0.40){
					results.add(new ParseTreeNode("inform", "", null, null));
					results.add(new ParseTreeNode("me", "", null, null));
					results.add(new ParseTreeNode("about", "", null, null));
				} else if (flag>0.20){
					results.add(new ParseTreeNode("I", "", null, null));
					results.add(new ParseTreeNode("need", "", null, null));
					results.add(new ParseTreeNode("to", "", null, null));
					results.add(new ParseTreeNode("know", "", null, null));
					results.add(new ParseTreeNode("more", "", null, null));
					results.add(new ParseTreeNode("about", "", null, null));
				}else {
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
			if (dupes.contains(ParseTreeNode.toWordString(ps))) 
				continue;
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
			//<8>PP'of':IN, <9>PP'the':DT, <10>PP'letter':NN, <11>PP'to':TO, <12>PP'you':PRP, <13>PP'indicating':VBG, <12>PP'you':PRP]
			if (ps.get(0).getPos().equals("IN") && ps.size()>2 && ps.get(1).getPos().equals("DT")  && ps.size()<= 8
					&& ps.size()>3){
				if (ps.get(0).getWord().equals("of")){
					results.add(new ParseTreeNode("What", "", null, null));
					results.add(new ParseTreeNode("is", "", null, null));
					results.add(new ParseTreeNode("the", "", null, null));
					results.add(new ParseTreeNode("purpose", "", null, null));
				} else {
					results.add(new ParseTreeNode("What", "", null, null));
					results.add(new ParseTreeNode("happens", "", null, null));
				}

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
					(ps.get(2).getWord().equals("is") || ps.get(3).getWord().equals("is") ||
							ps.get(2).getWord().equals("are") || ps.get(3).getWord().equals("are"))&& 
					ps.size()<= 8){
				results.add(new ParseTreeNode("Why", "", null, null));
				ps.remove(0);
				results.addAll(ps); 
				return results;
			}
		}

		if (results.isEmpty()){ 
			for( List<ParseTreeNode> ps: list){ 
				if (dupes.contains(ParseTreeNode.toWordString(ps))) 
					continue;
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
				//[<3>NP'your':PRP$, <4>NP'Credit':NN, <5>NP'Card':NNP, <6>NP'Agreement':NNP]
				if ((ps.get(0).getPos().equals("DT") || ps.get(0).getPos().startsWith("PRP"))
						&& ps.size()>2 &&  ps.get(1).getPos().equals("NNP")  
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
		Doc2QnABuilder builder = new Doc2QnABuilder();

		String texts[] = new String[]{
				"Your Community Day is your day to spend away from work, donating your time and services to the IRS, tax-exempt charitable organization of your choice. This could also include events sponsored by Pioneers, United Way, Junior Achievement, or a 501(c)(3) Employee Resource Group."+
						"Your Community Day is separate from paid time off and not considered time worked towards overtime. Volunteer time outside of work will not be paid. "
						,
						"You can use the day as a full day or as 2 half-days with supervisor approval; the number of hours you get to use is equal to your normal workday (i.e., 8 hours or 10 hours). Non-exempt employees have the option to use the time in 2-hour increments. You cannot carry over unused Your Community Day time to the next year, and you are not paid out for unused time if you leave the company. Supervisors are responsible for ensuring each employee receives only the equivalent of one normal work day per year (and notifying the new supervisor how much time has been used to date if either the employee or the supervisor changes organizations during the year)."
						,
						"You cannot use Your Community Day to participate in political activities such as get-out-the-vote efforts, campaigning for candidates, or voter registration drives. "+
								"You and your supervisor must be mindful of E-rate and the Rural Health Care (RHC) program requirements when seeking authorization to use their Your Community Day to volunteer at a K-12 school, library or at a healthcare facility. Volunteer activities cannot be provided with the intention or appearance of influencing procurement decisions, or of circumventing the competitive bidding or other E-rate or RHC program rules. "+
								"Managers in the GEM organization must obtain written supervisor and legal approval if they desire to volunteer at a K-12 school, library, or a healthcare facility. "
		};


		for(String t: texts){
			List<List<ParseTreeNode>> phrases = builder.buildQuestionForParagraph(t); 
			for(List<ParseTreeNode> p: phrases){
				System.out.println(ParseTreeNode.toWordString(p));
			}
			System.out.println("\n\n");
		}

	}
}
