package opennlp.tools.apps.ultimate_deception;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import opennlp.tools.jsmlearning.ProfileReaderWriter;
import opennlp.tools.parse_thicket.VerbNetProcessor;

public class ComplaintClassifierRunner extends ComplaintClassifierDataPreparer{
	private ComplaintClassifier classifier = new ComplaintClassifier();
	private static final String modelName3 = "model.IntenceArgumentation.isComplaintValid_cell3.txt",
			modelName5 = "model.UltimateDeception.isMisrepresenting_cell5.txt";

	public 	 ComplaintClassifierRunner(){
		classifier.setKernelPath(resourceDir + "/tree_kernel/");
	}

	public void runComplaintClassifier(){

		try {
			FileUtils.deleteDirectory(new File(outputDir));
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		List<String[]> complaints = ProfileReaderWriter.readProfiles(examplesDir + "/ultimateDeception.csv"),
		autoTaggedComplaints = new ArrayList<String[]>();
		int count = 0;
		for(String[] complLine: complaints ){
			String[] newComplLine = new String[complLine.length];
			for(int j=0; j<complLine.length; j++ ){
				newComplLine[j] = complLine[j];
			}
			String text = complLine[1];
			if (text==null || text.length()<100)
				continue;

			Boolean bPos = (complLine[3].equalsIgnoreCase("Y")),
					bNeg = (complLine[3].equalsIgnoreCase("N"));
			if (!bPos && !bNeg ){ //unassigned => needs to be classified
				classifier.setModelFile(modelName3 );
				classifier.setThreshold(new Float[] { - 0.35f, - 0.50f});
				
				SVMClassifResult result = classifier.classifyTextFromString(text);
				if (result.getBoolResult()!=null){
					if (result.getBoolResult())
						newComplLine[3] = "Y";
					else if (!result.getBoolResult())
						newComplLine[3] = "N";
				}
			}

			Boolean bPos5 = (complLine[5].equalsIgnoreCase("Y")),
					bNeg5 = (complLine[5].equalsIgnoreCase("N"));
			if (!bPos5 && !bNeg5 ){ //unassigned => needs to be classified
				classifier.setModelFile(modelName5 );
				
				classifier.setThreshold(new Float[] { -0.65f, -0.72f});
				SVMClassifResult result = classifier.classifyTextFromString(text);
				
				if (result.getBoolResult()!=null){
					if (result.getBoolResult())
						newComplLine[5] = "Y";
					else if (!result.getBoolResult())
						newComplLine[5] = "N";
				}
				//otherwise null, do not set result
			}
			count++;

			autoTaggedComplaints .add(newComplLine);
			ProfileReaderWriter.writeReport(autoTaggedComplaints, "ultimateDeceptionAutoTagged.csv");
		}
	}


	public static void main(String[] args){

		System.out.println("This is SVM TK text classifier. Please specify a doc to classify [argument0], the model file [argument1], "
				+ "and the kernel path\n"
				+ "java -jar supervisedLearner.jar unknown.doc model_design_doc.txt /Users/bgalitsky/Documents/DLP/KamanjaMaltegoIntegration/NLPProcessor/src/test/resources/tree_kernel/\n");
		/*if (args.length!=3){
			System.err.println("Wrong arguments!");
			System.exit(-1);
		}*/
		
		VerbNetProcessor p = VerbNetProcessor.getInstance(resourceDir); 
		ComplaintClassifierRunner runner = new ComplaintClassifierRunner();
		runner.runComplaintClassifier();

		
	}
}
