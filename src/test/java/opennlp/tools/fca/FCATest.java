package opennlp.tools.fca;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;

public class FCATest {
	
	
	public static void main(String []args) throws FileNotFoundException, IOException {

		ConceptLattice cl = new ConceptLattice("sports.cxt",true);
		cl.printLatticeStats();
		cl.printLatticeFull();
		cl.printBinContext();
		
		FcaWriter wt = new FcaWriter();
		wt.WriteStatsToCvs("stats.csv", cl, 0);

		FcaConverter converter = new FcaConverter();
		int [][] binCon = converter.latticeToContext(cl);
		
/*		if (binCon!=null){
			ConceptLattice new_cl = new ConceptLattice(binCon.length, binCon[0].length, binCon, false);	
			new_cl.printLatticeStats();
			new_cl.printLatticeFull();
			new_cl.printBinContext();
			FcaWriter wt = new FcaWriter();
			wt.WriteStats("stats.txt", cl, 0);
			//wt.WriteAsCxt("cl.cxt", cl);
			wt.WriteAsCxt("cl_new.cxt", new_cl);
		}		
*/		
/*		RandomNoiseGenerator rng = new RandomNoiseGenerator();
		
		//int[][] bc = rng.AddObjectsAttributesWithProbability(10, 0.5, cl.binaryContext);
		int[][] bc = rng.AlterCellsWithProbability(0.2, cl.binaryContext);
		ConceptLattice new_cl = new ConceptLattice(bc.length, bc[0].length, bc, false);	
		new_cl.printLatticeStats();
		new_cl.printLattice();
		//FcaWriter wt = new FcaWriter();
		//wt.WriteAsCxt("cl1.cxt", new_cl);
		 * 
		 * 
		 */
	}
	
}

