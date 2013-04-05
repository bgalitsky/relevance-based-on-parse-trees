package opennlp.tools.parse_thicket.parse_thicket2graph;

import java.io.PrintWriter;
import java.util.List;

import opennlp.tools.parse_thicket.PTTree;
import opennlp.tools.parse_thicket.ParseThicket;
import opennlp.tools.parse_thicket.ParseTreeNode;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;


import edu.stanford.nlp.trees.LabeledScoredTreeNode;
import edu.stanford.nlp.trees.Tree;

public class GraphFromPTreeBuilder {
	
	
	public Graph<Object, DefaultEdge> buildGraphFromPT(ParseThicket pt){
		PrintWriter out = new PrintWriter(System.out);

		
		List<Tree> ts = pt.getSentences();
		ts.get(0).pennPrint(out);
		Graph<Object, DefaultEdge> gfragment = buildGGraphFromTree(ts.get(0));
		
		ParseTreeVisualizer applet = new ParseTreeVisualizer();
		applet.showGraph(gfragment);
		
		return null;
		
	}
	
	
	private Graph<Object, DefaultEdge> buildGGraphFromTree(Tree tree) {
		Graph<Object, DefaultEdge> g =
				new SimpleGraph<Object, DefaultEdge>(DefaultEdge.class);
		g.addVertex("S 0");
		navigate(tree, g, 0, "S 0");
	        
		return g;
	}



	private void navigate(Tree tree, Graph<Object, DefaultEdge> g, int l, String currParent) {
		//String currParent = tree.label().value()+" $"+Integer.toString(l);
		//g.addVertex(currParent);
		if (tree.getChildrenAsList().size()==1)
			navigate(tree.getChildrenAsList().get(0), g, l+1, currParent);
		else
			if (tree.getChildrenAsList().size()==0)
				return;
		
		for(Tree child: tree.getChildrenAsList()){
			String currChild = null;
			try {
				if (child.isLeaf()) 
					continue;
				if (child.label().value().startsWith("S"))
					navigate(child.getChildrenAsList().get(0), g, l+1, currParent);
				
				if (!child.isPhrasal() || child.isPreTerminal())
					currChild = child.toString()+" #"+Integer.toString(l);
				else 
					currChild = child.label().value()+" #"+Integer.toString(l);
				
				g.addVertex(currChild);
				g.addEdge(currParent, currChild);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			navigate(child, g, l+1, currChild);
		}
	}


	/*
	private static void navigateChildren(PTTree[] trChildren, int indent, boolean parentLabelNull, boolean onlyLabelValue, List<LabeledScoredTreeNode> phrases) {
	    boolean firstSibling = true;
	    boolean leftSibIsPreTerm = true;  // counts as true at beginning
	    for (PTTree currentTree : trChildren) {
	      currentTree.navigate(indent, parentLabelNull, firstSibling, leftSibIsPreTerm, false, onlyLabelValue, phrases);
	      leftSibIsPreTerm = currentTree.isPreTerminal();
	      // CC is a special case for English, but leave it in so we can exactly match PTB3 tree formatting
	      if (currentTree.value() != null && currentTree.value().startsWith("CC")) {
	        leftSibIsPreTerm = false;
	      }
	      firstSibling = false;
	    }
	  }
	
	
	  private void navigate(int indent, boolean parentLabelNull, boolean firstSibling, boolean leftSiblingPreTerminal, boolean topLevel, boolean onlyLabelValue, List<LabeledScoredTreeNode> phrases) {
	    // the condition for staying on the same line in Penn Treebank
	    boolean suppressIndent = (parentLabelNull || (firstSibling && isPreTerminal()) || (leftSiblingPreTerminal && isPreTerminal() && (label() == null || !label().value().startsWith("CC"))));
	    if (suppressIndent) {
	      //pw.print(" ");
	      // pw.flush();
	    } else {
	      if (!topLevel) {
	        //pw.println();
	      }
	      for (int i = 0; i < indent; i++) {
	        //pw.print("  ");
	        // pw.flush();
	      }
	    }
	    if (isLeaf() || isPreTerminal()) {
	      String terminalString = toStringBuilder(new StringBuilder(), onlyLabelValue).toString();
	      //pw.print(terminalString);
	      //pw.flush();
	      return;
	    }
	    //pw.print("(");
	    String nodeString = onlyLabelValue ? value() : nodeString();
	    //pw.print(nodeString);
	    // pw.flush();
	    boolean parentIsNull = label() == null || label().value() == null;
	    navigateChildren(children(), indent + 1, parentIsNull, true, phrases);
	    //pw.print(")");
	    
	  }
	  */

}
