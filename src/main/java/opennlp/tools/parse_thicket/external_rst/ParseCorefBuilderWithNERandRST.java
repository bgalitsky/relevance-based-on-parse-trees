/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package opennlp.tools.parse_thicket.external_rst;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.arizona.sista.discourse.rstparser.DiscourseTree;
import edu.arizona.sista.processors.CorefMention;
import edu.arizona.sista.processors.Document;
import edu.arizona.sista.processors.Processor;
import edu.arizona.sista.processors.Sentence;
import edu.arizona.sista.processors.corenlp.CoreNLPProcessor;
import edu.arizona.sista.struct.DirectedGraphEdgeIterator;
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
import scala.Option;

public class ParseCorefBuilderWithNERandRST {	
	public Processor proc = null;
	CommunicativeActionsArcBuilder caFinder = new CommunicativeActionsArcBuilder();

	AbstractSequenceClassifier<CoreLabel> classifier = null;

	ParseCorefBuilderWithNERandRST() {
		super();
		classifier = CRFClassifier.getDefaultClassifier();
		proc = new CoreNLPProcessor(true, true, 100);
	}

	public ParseThicket buildParseThicket(String text){
		List<Tree> ptTrees = new ArrayList<Tree>();
		// all numbering from 1, not 0
		List<WordWordInterSentenceRelationArc> arcs = new ArrayList<WordWordInterSentenceRelationArc>();
		List<List<ParseTreeNode>> nodesThicket = new ArrayList<List<ParseTreeNode>>();

		Document doc = proc.annotate(text, false);
		int sentenceCount = 0;
		for (Sentence sentence: doc.sentences()) {
			List<ParseTreeNode> sentenceNodes = new ArrayList<ParseTreeNode>();
			String[] tokens= sentence.words();
			for(int i=0; i< tokens.length; i++){
				//sentence.startOffsets(), " "));
				//sentence.endOffsets(), " "));
				ParseTreeNode p = new ParseTreeNode(sentence.lemmas().get()[i], sentence.tags().get()[i]);
				p.setId(i+1);
				if(sentence.entities().isDefined()){
					p.setNe(sentence.entities().get()[i]);
				}
				if(sentence.norms().isDefined()){
					p.setNormalizedWord(sentence.norms().get()[i]);
				}
				sentenceNodes.add(p);
			}

			if(sentence.dependencies().isDefined()) {
				int i=1;
				DirectedGraphEdgeIterator<String> iterator = new
						DirectedGraphEdgeIterator<String>(sentence.dependencies().get());
				while(iterator.hasNext()) {
					scala.Tuple3<Object, Object, String> dep = iterator.next();
					//System.out.println(" head:" + dep._1() + " modifier:" + dep._2() + " label:" + dep._3());
					ParseTreeNode p = sentenceNodes.get(i-1);
					p.setHead(dep._1().toString());
					p.setModifier(dep._2().toString());
					p.setLabel(dep._3());
					sentenceNodes.set(i, p);
				}
			}
			if(sentence.syntacticTree().isDefined()) {
				Tree tree = Tree.valueOf(sentence.syntacticTree().get().toString());
				ptTrees.add(tree);
				//tree.pennPrint();
			}

			sentenceCount += 1;

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
										nodesThicket.get(niSentence[i]).get(startOffset[i]).getWord()+"..."+nodesThicket.get(niSentence[i]).get(endOffset[i]).getWord(),
										nodesThicket.get(niSentence[i+1]).get(startOffset[i+1]).getWord()+"..."+nodesThicket.get(niSentence[i+1]).get(endOffset[i+1]).getWord(),           	    					  arcType);
						arcs.add(arc);
					}
				}
			}
		}


		List<WordWordInterSentenceRelationArc> arcsCA = buildCAarcs(nodesThicket);
		arcs.addAll(arcsCA);

		if(doc.discourseTree().isDefined()) {
			Option<DiscourseTree> discourseTree = doc.discourseTree();

			//scala.collection.immutable.List<DiscourseTree> scList = discourseTree.toList();
			scala.collection.Iterator<DiscourseTree> iterator = discourseTree.iterator();
			while(iterator.hasNext()) {
				DiscourseTree dt = iterator.next();
				List<WordWordInterSentenceRelationArc> rstArcs = new ArrayList<WordWordInterSentenceRelationArc>();
				navigateDiscourseTree(dt, rstArcs);
				arcs.addAll(rstArcs);
				/*System.out.println(dt);
				System.out.println("first EDU = "+dt.firstEDU() + "| dt.firstSentence() = "+ dt.firstSentence() + 
						" \n| last EDU = "+dt.lastEDU() + "| dt.lastSentence() = "+ dt.lastSentence() + 
						" \n| dt.tokenCount() = " + dt.tokenCount() + "| dt.firstToken " + dt.firstToken() + 
						" | dt.lastToken() "+ dt.lastToken() + "\n kind =" + dt.kind() + " | text = "+ dt.rawText());
						*/

			}
		}

		ParseThicket result = new ParseThicket(ptTrees, arcs);
		result.setNodesThicket(nodesThicket);
		return result;
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

	private void navigateDiscourseTree(DiscourseTree dt, List<WordWordInterSentenceRelationArc> arcs ) {
		if (dt.isTerminal()) {
			return;
		} else {
			ArcType arcType = new ArcType("rst", 
					dt.relationLabel()+ "=>" + dt.kind(), Boolean.compare(dt.relationDirection().equals("LeftToRight"), true),0);
			WordWordInterSentenceRelationArc arc = 
					new WordWordInterSentenceRelationArc(new Pair<Integer, Integer>(dt.firstToken().copy$default$1(), dt.firstToken().copy$default$2()), 
							new Pair<Integer, Integer>(dt.lastToken().copy$default$1(), dt.lastToken().copy$default$2()), 
							"","", 
							arcType);
			System.out.println(arc);
			arcs.add(arc);
			DiscourseTree[] kids = dt.children();
			if (kids != null) {
				for (DiscourseTree kid : kids) {
					navigateDiscourseTree(kid, arcs);
				}
			}
			return ;
		}
	}

	public static void main(String[] args){
		new ParseCorefBuilderWithNERandRST ().buildParseThicket(
				"Dutch accident investigators say that evidence points to pro-Russian rebels as being responsible for shooting down plane. The report indicates where the missile was fired from and identifies who was in control of the territory and pins the downing of the plane on the pro-Russian rebels. "+
						 "However, the Investigative Committee of the Russian Federation believes that the plane was hit by a missile from the air which was not produced in Russia. "+
						 "At the same time, rebels deny that they controlled the territory from which the missile was supposedly fired."
			/*	
				"No one knows yet what General Prayuth's real intentions are. He has good reason to worry about resistance. "
				+ "The pro-government Red-Shirt movement is far better organised than eight years ago, and could still be financed by former Prime Minister Thaksin Shinawatra's deep pockets."
				*/		
				);
	}

}
