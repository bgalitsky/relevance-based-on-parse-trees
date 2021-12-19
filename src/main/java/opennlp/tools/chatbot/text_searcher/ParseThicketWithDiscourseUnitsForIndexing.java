package opennlp.tools.chatbot.text_searcher;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.trees.Tree;
import opennlp.tools.parse_thicket.ParseThicket;
import opennlp.tools.parse_thicket.WordWordInterSentenceRelationArc;
import opennlp.tools.parse_thicket.external_rst.ParseThicketWithDiscourseTree;

public class ParseThicketWithDiscourseUnitsForIndexing extends ParseThicketWithDiscourseTree {
	public ParseThicketWithDiscourseUnitsForIndexing(List<Tree> ptTrees, List<WordWordInterSentenceRelationArc> barcs) {
		super(ptTrees, barcs);
		eDUs = new ArrayList<String>();
	}

	public List<String> eDUs;
	
}
