package opennlp.tools.parse_thicket.pattern_structure;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import opennlp.tools.fca.ConceptLattice;
import opennlp.tools.fca.FormalConcept;

public class PatternStructureWriter {
	
	public void WriteStatsToTxt(String filename, PhrasePatternStructure ps){
			
		String formatStr = "[%5.2f; %5.2f]  %s   %s%n";
		Writer writer = null;

		try {
		    writer = new BufferedWriter(new OutputStreamWriter(
		          new FileOutputStream(filename), "utf-8"));
		    writer.write("PatternStructure size: " + ps.conceptList.size()+ " with " + ps.objectCount + "objects\n");
		    
		    for (PhraseConcept c : ps.conceptList){
		    	writer.write(String.format(formatStr,c.intLogStabilityBottom, c.intLogStabilityUp, c.extent, c.intent));
		    }
		    writer.close();
		    
		} catch (IOException ex) {
			System.err.println(ex.getMessage());
		} finally {
		   try {writer.close();} catch (Exception ex) {}
		}
	}
	

		public static void main(String[] args) {
			
		}
}
			