package opennlp.tools.chatbot.text_searcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.clulab.discourse.rstparser.DiscourseTree;
import org.clulab.processors.Processor;
import org.clulab.processors.corenlp.CoreNLPProcessor;
import org.clulab.struct.CorefMention;
import org.clulab.struct.DirectedGraphEdgeIterator;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.Tree;
import opennlp.tools.parse_thicket.ArcType;
import opennlp.tools.parse_thicket.Pair;
import opennlp.tools.parse_thicket.ParseThicket;
import opennlp.tools.parse_thicket.ParseTreeNode;
import opennlp.tools.parse_thicket.WordWordInterSentenceRelationArc;
import opennlp.tools.parse_thicket.communicative_actions.CommunicativeActionsArcBuilder;
import opennlp.tools.parse_thicket.external_rst.ParseThicketWithDiscourseTree;
import scala.Option;
import org.clulab.processors.Document;
import org.clulab.processors.Sentence;

public class ElementaryDiscourseUnitsBuilderForIndexing {	
	public Processor proc = null;
	CommunicativeActionsArcBuilder caFinder = new CommunicativeActionsArcBuilder();
	private static Logger log = Logger
			.getLogger("oracle.cloud.bots.document_searcher.ElementaryDiscourseUnitsBuilderForIndexing");


	AbstractSequenceClassifier<CoreLabel> classifier = null;

	ElementaryDiscourseUnitsBuilderForIndexing() {
		classifier = CRFClassifier.getDefaultClassifier();
		proc = new CoreNLPProcessor(true, true, 100, 0);
	}

	public ParseThicketWithDiscourseUnitsForIndexing buildParseThicket(String text){
		List<Tree> ptTrees = new ArrayList<Tree>();
		List<WordWordInterSentenceRelationArc> arcs = new ArrayList<WordWordInterSentenceRelationArc>();
		List<List<ParseTreeNode>> nodesThicket = new ArrayList<List<ParseTreeNode>>();

		Document doc=null;
		try {
			doc = proc.annotate(text, false);
		} catch (IllegalArgumentException iae) {
			log.severe("failed to parse text: "+text);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// failed to parse - skip this text
		if (doc==null)
			return null;
		//java.lang.IllegalArgumentException
		for (Sentence sentence: doc.sentences()) {
			List<ParseTreeNode> sentenceNodes = new ArrayList<ParseTreeNode>();
			String[] tokens= sentence.words();
			for(int i=0; i< tokens.length; i++){
				//sentence.startOffsets(), " "));
				//sentence.endOffsets(), " "));
				ParseTreeNode p = new ParseTreeNode(sentence.words()[i], sentence.tags().get()[i]);
				p.setId(i+1);
				if(sentence.entities().isDefined()){
					p.setNe(sentence.entities().get()[i]);
				}
				if(sentence.norms().isDefined()){
					//p.setNormalizedWord(sentence.norms().get()[i]);
					p.setNormalizedWord(sentence.lemmas().get()[i]);
				}
				sentenceNodes.add(p);
			}

			if(sentence.dependencies().isDefined()) {
				int i=0;
				DirectedGraphEdgeIterator<String> iterator = new
						DirectedGraphEdgeIterator<String>(sentence.dependencies().get());
				while(iterator.hasNext()) {
					scala.Tuple3<Object, Object, String> dep = iterator.next();
					//System.out.println(" head:" + dep._1() + " modifier:" + dep._2() + " label:" + dep._3());
					if (i>sentenceNodes.size()-1)
						break;
					ParseTreeNode p = sentenceNodes.get(i);
					p.setHead(dep._1().toString());
					p.setModifier(dep._2().toString());
					p.setLabel(dep._3());
					sentenceNodes.set(i, p);
					i++;
				}
			}
			if(sentence.syntacticTree().isDefined()) {
				Tree tree = Tree.valueOf(sentence.syntacticTree().get().toString());
				ptTrees.add(tree);
				//tree.pennPrint();
			}
			nodesThicket.add(sentenceNodes);
		}

		if(doc.coreferenceChains().isDefined()) {
			// these are scala.collection Iterator and Iterable (not Java!)
			scala.collection.Iterator<scala.collection.Iterable<CorefMention>> chains = doc.coreferenceChains().get().getChains().iterator();
			while(chains.hasNext()) {
				scala.collection.Iterator<CorefMention> chain = chains.next().iterator();
				//System.out.println("Found one coreference chain containing the following mentions:");
				int numInChain = 0;
				int[] niSentence = new int[4], niWord = new int[4], startOffset = new int[4], endOffset = new int[4];

				while(chain.hasNext()) {
					CorefMention mention = chain.next();
					// note that all these offsets start at 0 too
					niSentence[numInChain ] = mention.sentenceIndex();
					niWord[numInChain ] = mention.headIndex();
					startOffset[numInChain ] = mention.startOffset();
					endOffset[numInChain ] = mention.endOffset();
					if (numInChain>=4-1)
						break;
					numInChain++;
					//" headIndex:" + mention.headIndex() +
					//" startTokenOffset:" + mention.startOffset() +
					//" endTokenOffset:" + mention.endOffset());
				}
				if (numInChain>0) { // more than a single mention
					for(int i=0; i<numInChain; i++){
						ArcType arcType = new ArcType("coref-", "", 0, 0);

						WordWordInterSentenceRelationArc arc = 
								new WordWordInterSentenceRelationArc(new Pair<Integer, Integer>(niSentence[i],niWord[i]), 
										new Pair<Integer, Integer>(niSentence[i+1],niWord[i+1]), 
										startOffset[i]+"", startOffset[i+1]+"",
										arcType);
						arcs.add(arc);
					}
				}
			}
		}


		List<WordWordInterSentenceRelationArc> arcsCA = buildCAarcs(nodesThicket);
		arcs.addAll(arcsCA);
		ParseThicketWithDiscourseUnitsForIndexing result = new ParseThicketWithDiscourseUnitsForIndexing(ptTrees, arcs);

		if(doc.discourseTree().isDefined()) {
			Option<DiscourseTree> discourseTree = doc.discourseTree();

			//scala.collection.immutable.List<DiscourseTree> scList = discourseTree.toList();
			scala.collection.Iterator<DiscourseTree> iterator = discourseTree.iterator();
			while(iterator.hasNext()) {
				DiscourseTree dt = iterator.next();
				result.setDt(dt);
				List<WordWordInterSentenceRelationArc> rstArcs = new ArrayList<WordWordInterSentenceRelationArc>();
				List<String> edus = new ArrayList<String>();
				//navigateDiscourseTree(dt, rstArcs, nodesThicket , edus);
				arcs.addAll(rstArcs);
				System.out.println(dt);
				String dump = dt.toString(true, true);
				String[] eduStrArrs = StringUtils.substringsBetween(dump, "TEXT:","\n");
				// TODO: now we parse dump, instead should get data directly
				String[] sectionsForNucleus = dump.split("LeftToRight");
				for(String sect : sectionsForNucleus){
					if (sect!=null && sect.length()>3){
						String nucleus = StringUtils.substringBetween(sect, "TEXT:","\n");
						if (nucleus!=null && isAcceptableEDU(nucleus))
						  result.eDUs.add(nucleus);
					}
				}

				//result.eDUs.addAll(edus); //Arrays.asList(eduStrArrs));
				//result.eDUs.add(dt.rawText());
				System.out.println("first EDU = "+dt.firstEDU() + "| dt.firstSentence() = "+ dt.firstSentence() + 
						" \n| last EDU = "+dt.lastEDU() + "| dt.lastSentence() = "+ dt.lastSentence() + 
						" \n| dt.tokenCount() = " + dt.tokenCount() + "| dt.firstToken " + dt.firstToken() + 
						" | dt.lastToken() "+ dt.lastToken() + "\n kind =" + dt.kind() + " | text = "+ dt.rawText());
				StringBuilder sb = new StringBuilder(10000);
				System.out.println(sb);
			}
		}

		result.setOrigText(text);
		result.setNodesThicket(nodesThicket);

		result.setDtDump(); // sets the DT representation for TK learning
		return result;
	}

	private boolean isAcceptableEDU(String nucleus) {
		if (nucleus.length()> 30 && nucleus.split(" ").length>3)
			return true;
		return false;
	}

	public List<WordWordInterSentenceRelationArc> buildCAarcs(
			List<List<ParseTreeNode>> nodesThicket) {
		List<WordWordInterSentenceRelationArc> arcs = new ArrayList<WordWordInterSentenceRelationArc>();

		for(int sentI=0; sentI<nodesThicket.size(); sentI++){
			for(int sentJ=sentI+1; sentJ<nodesThicket.size(); sentJ++){
				List<ParseTreeNode> sentenceI = nodesThicket.get(sentI), 
						sentenceJ = nodesThicket.get(sentJ);
				Pair<String, Integer[]> caI = caFinder.findCAInSentence(sentenceI);
				Pair<String, Integer[]> caJ = caFinder.findCAInSentence(sentenceJ);
				int indexCA1 = caFinder.findCAIndexInSentence(sentenceI);
				int indexCA2 = caFinder.findCAIndexInSentence(sentenceJ);
				if (caI==null || caJ==null)
					continue;
				Pair<String, Integer[]> caGen = caFinder.generalize(caI, caJ).get(0);

				ArcType arcType = new ArcType("ca", 
						caGen.getFirst().toString()+printNumArray(caGen.getSecond()), 0, 0);
				WordWordInterSentenceRelationArc arc = 
						new WordWordInterSentenceRelationArc(new Pair<Integer, Integer>(sentI,indexCA1), 
								new Pair<Integer, Integer>(sentJ,indexCA2), caI.getFirst(), caJ.getFirst(), 
								arcType);
				arcs.add(arc);
			}
		}

		return arcs;
	}

	private String printNumArray(Integer[] arr){
		StringBuffer buf = new StringBuffer();
		for(Integer i: arr){
			buf.append(Integer.toString(i)+ " ");
		}
		return buf.toString();
	}

	// creates a list of Arcs objects 'arcs' from the descourse tree dt, using the list of sentences 'nodesThicket' to identify words 
	// for nodes being connected with these arcs
	private void navigateDiscourseTree(DiscourseTree dt, List<WordWordInterSentenceRelationArc> arcs,  List<List<ParseTreeNode>> nodesThicket, List<String> edus ) {
		if (dt.isTerminal()) {
			return;
		} else {
			ArcType arcType = new ArcType("rst", 
					dt.relationLabel()+ "=>" + dt.kind(), Boolean.compare(dt.relationDirection().equals("LeftToRight"), true),0);
			String lemmaFrom = nodesThicket.get(dt.firstSentence()).get(dt.firstToken().copy$default$2()).getWord();
			String lemmaTo = nodesThicket.get(dt.lastSentence()).get(dt.lastToken().copy$default$2()-1).getWord();

			WordWordInterSentenceRelationArc arc = 
					new WordWordInterSentenceRelationArc(new Pair<Integer, Integer>(dt.firstToken().copy$default$1(), dt.firstToken().copy$default$2()), 
							new Pair<Integer, Integer>(dt.lastToken().copy$default$1(), dt.lastToken().copy$default$2()), 
							lemmaFrom,lemmaTo, 
							arcType);

			int s1wStartIndex = -1;
			int s1wEndIndex = -1;  
			String eduNucleus = "";
			if (dt.relationDirection().equals("LeftToRight")){
				s1wStartIndex = dt.firstToken().copy$default$2();
				s1wEndIndex =   dt.lastToken().copy$default$2();
				for(int w = s1wStartIndex; w<=s1wEndIndex; w++){
					eduNucleus += " " + nodesThicket.get(dt.firstSentence()).get(w).getWord();
				}
			} else {
				s1wStartIndex = dt.firstToken().copy$default$2();
				s1wEndIndex =   dt.lastToken().copy$default$2();	
				for(int w = s1wStartIndex; w<=s1wEndIndex; w++){
					eduNucleus += " " + nodesThicket.get(dt.lastSentence()).get(w).getWord();
				}
			}

			edus.add(eduNucleus);
			System.out.println(arc);
			arcs.add(arc);
			DiscourseTree[] kids = dt.children();
			if (kids != null) {
				for (DiscourseTree kid : kids) {
					navigateDiscourseTree(kid, arcs, nodesThicket, edus);
				}
			}
			return ;
		}
	}

	public static void main(String[] args){
		ElementaryDiscourseUnitsBuilderForIndexing builder = new ElementaryDiscourseUnitsBuilderForIndexing ();
		String text = "To help maximize your retirement savings, it is generally a good idea to consider not using the proceeds from the conversion to pay the resulting tax costs. Instead, you should consider using cash or other savings held in nonretirement accounts. Using retirement account funds to pay the taxes will reduce the amount you would have available to potentially grow tax-free in your new Roth IRA. Additionally, if you are under fifty-nine, using funds from your retirement account could result in an additional ten percent tax penalty, which may significantly reduce the potential benefit of conversion.";

		ParseThicketWithDiscourseUnitsForIndexing pt = builder.buildParseThicket(text);
		System.out.println(pt.eDUs);

	}

}
