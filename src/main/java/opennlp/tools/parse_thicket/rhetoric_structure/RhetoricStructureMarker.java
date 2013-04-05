package opennlp.tools.parse_thicket.rhetoric_structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import opennlp.tools.parse_thicket.IGeneralizer;
import opennlp.tools.parse_thicket.Pair;
import opennlp.tools.parse_thicket.ParseTreeNode;


public class RhetoricStructureMarker implements IGeneralizer<Integer[]>  {
	private static String rstRelations[] = {"antithesis", "concession", "contrast", "elaboration"};
	List<Pair<String, ParseTreeNode[]>> rstMarkers = new ArrayList<Pair<String, ParseTreeNode[]>>();

	public  RhetoricStructureMarker(){

		rstMarkers.add(new Pair<String, ParseTreeNode[]>("contrast", new ParseTreeNode[]{new ParseTreeNode(",",","),  new ParseTreeNode("than",",")  }));
		rstMarkers.add(new Pair<String, ParseTreeNode[]>( "antithesis", new ParseTreeNode[]{new ParseTreeNode("although",","),  new ParseTreeNode("*","*")  }));
		rstMarkers.add(new Pair<String, ParseTreeNode[]>( "contrast", new ParseTreeNode[]{new ParseTreeNode(",",","),  new ParseTreeNode("however","*")  }));
		rstMarkers.add(new Pair<String, ParseTreeNode[]>( "contrast", new ParseTreeNode[]{new ParseTreeNode("however","*"), new ParseTreeNode(",",","),
					new ParseTreeNode("*","prp"),   }));
		rstMarkers.add(new Pair<String, ParseTreeNode[]>( "elaboration", new ParseTreeNode[]{new ParseTreeNode(",",","),  new ParseTreeNode("*","NN")  }));
	
		rstMarkers.add(new Pair<String, ParseTreeNode[]>("explanation", new ParseTreeNode[]{new ParseTreeNode(",",","),  new ParseTreeNode("because",",")  }));
		rstMarkers.add(new Pair<String, ParseTreeNode[]>( "example", new ParseTreeNode[]{new ParseTreeNode("for","IN"),  new ParseTreeNode("example","NN")  }));
		rstMarkers.add(new Pair<String, ParseTreeNode[]>( "contrast", new ParseTreeNode[]{new ParseTreeNode(",",","),  new ParseTreeNode("ye","*")  }));
		rstMarkers.add(new Pair<String, ParseTreeNode[]>( "contrast", new ParseTreeNode[]{new ParseTreeNode("yet","*"), new ParseTreeNode(",",","),
					new ParseTreeNode("*","prp"),   }));
		rstMarkers.add(new Pair<String, ParseTreeNode[]>( "explanation", new ParseTreeNode[]{new ParseTreeNode(",",","),  new ParseTreeNode("where","*")  }));

	}

	public List<Pair<String, Integer[]>> extractRSTrelationInSentenceGetBoundarySpan(List<ParseTreeNode> sentence){
		List<Pair<String, Integer[]>> results = new ArrayList<Pair<String, Integer[]>> ();
		
		for(Pair<String, ParseTreeNode[]> template: rstMarkers){
			List<Integer[]> spanList = generalize(sentence,template.getSecond() );
			if (!spanList.isEmpty())
				results.add(new Pair<String, Integer[]>(template.getFirst(), spanList.get(0)));
		}
		return results;
	}

	/* rule application in the form of generalization
	 * 
	 * @see com.become.parse_thicket.IGeneralizer#generalize(java.lang.Object, java.lang.Object)
	 * returns the span Integer[] 
	 */
	@Override
	public List<Integer[]> generalize(Object o1, Object o2) {
		List<Integer[]> result = new ArrayList<Integer[]>();

		List<ParseTreeNode> sentence = (List<ParseTreeNode> )o1;
		ParseTreeNode[] template = (ParseTreeNode[]) o2;

		boolean bBeingMatched = false;
		for(int wordIndexInSentence=0; wordIndexInSentence<sentence.size(); wordIndexInSentence++){
			ParseTreeNode word = sentence.get(wordIndexInSentence);
			int wordIndexInSentenceEnd = wordIndexInSentence; //init iterators for internal loop
			int templateIterator=0;
			while (wordIndexInSentenceEnd<sentence.size() && templateIterator< template.length){
				ParseTreeNode tword = template[templateIterator];
				ParseTreeNode currWord=sentence.get(wordIndexInSentenceEnd);
				List<ParseTreeNode> gRes = tword.generalize(tword, currWord);
				if (gRes.isEmpty()|| gRes.get(0)==null || ( gRes.get(0).getWord().equals("*") 
						&& gRes.get(0).getPos().equals("*") )){
					bBeingMatched = false;
					break;
				} else {
					bBeingMatched = true;
				}
				wordIndexInSentenceEnd++;
				templateIterator++;
			}
			// template iteration is done
			// the only condition for successful match is that we are at the end of template
			if (templateIterator == template.length){
				result.add(new Integer[]{wordIndexInSentence, wordIndexInSentenceEnd-1});
				return result;
			}

			// no match for current sentence word: proceed to the next
		}
		return result; //[]

	}

	public static void main(String[] args){
		ParseTreeNode[] sent = 	
		new ParseTreeNode[]{new ParseTreeNode("he","prn"), new ParseTreeNode("was","vbz"), new ParseTreeNode("more","jj"), 
				new ParseTreeNode(",",","),  new ParseTreeNode("than",","), new ParseTreeNode("little","jj"), new ParseTreeNode("boy","nn"),
				new ParseTreeNode(",",","), new ParseTreeNode("however","*"), new ParseTreeNode(",",","),
				new ParseTreeNode("he","prp"), new ParseTreeNode("was","vbz"), new ParseTreeNode("adult","jj")
		};
		
		List<Pair<String, Integer[]>> res = new RhetoricStructureMarker().extractRSTrelationInSentenceGetBoundarySpan(Arrays.asList(sent));
		System.out.println(res);
	}
}
