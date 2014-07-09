package opennlp.tools.parse_thicket.matching;

import java.util.ArrayList;
import java.util.List;

import opennlp.tools.parse_thicket.IGeneralizer;

public class PartOfSpeechGeneralizer implements IGeneralizer<String>{

	@Override
	public List<String> generalize(Object o1, Object o2){
		String pos1 = (String)o1, pos2 =  (String) o2;
		List<String>  results = new ArrayList<String>();
		String res = computeSimilarity(pos1, pos2);
		if (res!=null)
			results.add(res);
		return results;

	}
	private String computeSimilarity(String pos1, String pos2){

		if ((pos1.startsWith("NN") && pos2.equals("NP") || pos2.startsWith("NN")
				&& pos1.equals("NP"))) {
			return "NN";
		}
		if ((pos1.startsWith("NN") && pos2.equals("VBG") || pos2.startsWith("VBG")
				&& pos1.equals("NN"))) {
			return "NN";
		}

		if ((pos1.startsWith("NN") && pos2.equals("ADJP") || pos2.startsWith("NN")
				&& pos1.equals("ADJP"))) {
			return "NN";
		}
		if ((pos1.equals("IN") && pos2.equals("TO") || pos1.equals("TO")
				&& pos2.equals("IN"))) {
			return "IN";
		}
		// VBx vs VBx = VB (does not matter which form for verb)
		if (pos1.startsWith("VB") && pos2.startsWith("VB")) {
			return "VB";
		}

		// ABx vs ABy always gives AB
		if (pos1.equalsIgnoreCase(pos2)) {
			return pos1;
		}
		if (pos1.length() > 2) {
			pos1 = pos1.substring(0, 2);
		}

		if (pos2.length() > 2) {
			pos2 = pos2.substring(0, 2);
		}
		if (pos1.equalsIgnoreCase(pos2)) {
			return pos1 + "*";
		}
		return null;
	}
}
