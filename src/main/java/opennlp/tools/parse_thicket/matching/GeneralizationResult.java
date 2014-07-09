package opennlp.tools.parse_thicket.matching;

import java.util.List;

import opennlp.tools.textsimilarity.ParseTreeChunk;

public class GeneralizationResult {
	List<List<ParseTreeChunk>> gen;
	Boolean bFire;
	String text;

	public List<List<ParseTreeChunk>> getGen() {
		return gen;
	}

	public void setGen(List<List<ParseTreeChunk>> gen) {
		this.gen = gen;
	}

	public GeneralizationResult(List<List<ParseTreeChunk>> gen) {

		this.gen = gen;
	}
	
	public String toString(){
		return this.gen.toString();
	}

	public void setIfFire(boolean fire) {
		this.bFire = fire;
		
	}

	public void setText(String text2) {
		this.text = text2;
		
	}

	public Boolean getbFire() {
		return bFire;
	}



	public String getText() {
		return text;
	}
	
}
