package opennlp.tools.parse_thicket.matching;

import java.util.ArrayList;
import java.util.List;


import opennlp.tools.parse_thicket.ParseCorefsBuilder;
import opennlp.tools.parse_thicket.ParseThicket;
import opennlp.tools.parse_thicket.ParseTreeNode;
import opennlp.tools.textsimilarity.LemmaPair;
import opennlp.tools.textsimilarity.ParseTreeChunk;
import opennlp.tools.textsimilarity.ParseTreeMatcherDeterministic;
import opennlp.tools.textsimilarity.SentencePairMatchResult;

public class Matcher {
	ParseTreeMatcherDeterministic md = new ParseTreeMatcherDeterministic();
	ParseCorefsBuilder ptBuilder = ParseCorefsBuilder.getInstance();
	PT2ThicketPhraseBuilder phraseBuilder = new PT2ThicketPhraseBuilder();
	/**	   * The key function of similarity component which takes two portions of text
	 * and does similarity assessment by finding the set of all maximum common
	 * subtrees of the set of parse trees for each portion of text
	 * 
	 * @param input
	 *          text 1
	 * @param input
	 *          text 2
	 * @return the matching results structure, which includes the similarity score
	 */
	public List<List<ParseTreeChunk>> assessRelevance(String para1, String para2) {
		// first build PTs for each text
		ParseThicket pt1 = ptBuilder.buildParseThicket(para1);
		ParseThicket pt2 = ptBuilder.buildParseThicket(para2);
		// then build phrases and rst arcs
		List<List<ParseTreeNode>> phrs1 = phraseBuilder.buildPT2ptPhrases(pt1);
		List<List<ParseTreeNode>> phrs2 = phraseBuilder.buildPT2ptPhrases(pt2);
		// group phrases by type
		List<List<ParseTreeChunk>> sent1GrpLst = formGroupedPhrasesFromChunksForPara(phrs1), 
				sent2GrpLst = formGroupedPhrasesFromChunksForPara(phrs2);

		
		List<List<ParseTreeChunk>> res = md
				.matchTwoSentencesGroupedChunksDeterministic(sent1GrpLst, sent2GrpLst);
		return res;

	}
	private List<List<ParseTreeChunk>> formGroupedPhrasesFromChunksForPara(
			List<List<ParseTreeNode>> phrs) {
		List<List<ParseTreeChunk>> results = new ArrayList<List<ParseTreeChunk>>();
		List<ParseTreeChunk> nps = new ArrayList<ParseTreeChunk>(), vps = new ArrayList<ParseTreeChunk>(), 
				pps = new ArrayList<ParseTreeChunk>();
		for(List<ParseTreeNode> ps:phrs){
			ParseTreeChunk ch = convertNodeListIntoChunk(ps);
			String ptype = ps.get(0).getPhraseType();
			if (ptype.equals("NP")){
				nps.add(ch);
			} else if (ptype.equals("VP")){
				vps.add(ch);
			} else if (ptype.equals("PP")){
				pps.add(ch);
			}
		}
		results.add(nps); results.add(vps); results.add(pps);
		return results;
	}

	private ParseTreeChunk convertNodeListIntoChunk(List<ParseTreeNode> ps) {
		List<String> lemmas = new ArrayList<String>(),  poss = new ArrayList<String>();
		for(ParseTreeNode n: ps){
			lemmas.add(n.getWord());
			poss.add(n.getPos());
		}
		ParseTreeChunk ch = new ParseTreeChunk(lemmas, poss, 0, 0);
		ch.setMainPOS(ps.get(0).getPhraseType());
		return ch;
	}
	
	// this function is the main entry point into the PT builder if rst arcs are required
	public ParseThicket buildParseThicketFromTextWithRST(String para){
		ParseThicket pt = ptBuilder.buildParseThicket(para);
		phraseBuilder.buildPT2ptPhrases(pt);
		return pt;	
	}
	
	public static void main(String[] args){
		Matcher m = new Matcher();
		List<List<ParseTreeChunk>> res = m.assessRelevance("Iran refuses to accept the UN proposal to end its dispute over its work on nuclear weapons."+
				"UN nuclear watchdog passes a resolution condemning Iran for developing its second uranium enrichment site in secret. " +
				"A recent IAEA report presented diagrams that suggested Iran was secretly working on nuclear weapons. " +
				"Iran envoy says its nuclear development is for peaceful purpose, and the material evidence against it has been fabricated by the US. "

				, "Iran refuses the UN offer to end a conflict over its nuclear weapons."+
						"UN passes a resolution prohibiting Iran from developing its uranium enrichment site. " +
						"A recent UN report presented charts saying Iran was working on nuclear weapons. " +
				"Iran envoy to UN states its nuclear development is for peaceful purpose, and the evidence against its claim is fabricated by the US. ");
		System.out.print(res);
	}
}
