package opennlp.tools.apps.ultimate_deception;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;

import opennlp.tools.jsmlearning.ProfileReaderWriter;
import opennlp.tools.parse_thicket.VerbNetProcessor;

public class ComplaintClassifierDataPreparer {
	public static String resourceDir = System.getProperty("user.dir") + File.separator + "src/test/resources",
	 outputDir = resourceDir + File.separator + "complaints",
	examplesDir = System.getProperty("user.dir") + File.separator + "examples";
	@SuppressWarnings("deprecation")
    public static void main(String[] args){
		VerbNetProcessor p = VerbNetProcessor.getInstance(resourceDir); 
		try {
	        FileUtils.deleteDirectory(new File(outputDir));
        } catch (IOException e1) {
	        e1.printStackTrace();
        }
		
		List<String[]> complaints = ProfileReaderWriter.readProfiles(examplesDir + "/ultimateDeception.csv");
		int count = 0;
		for(String[] complLine: complaints ){
			String text = complLine[1];
			Boolean bPos = (complLine[5].equalsIgnoreCase("Y")),
					bNeg = (complLine[5].equalsIgnoreCase("N"));
			String filename = complLine[0];
			if (filename==null || filename.length()<5 && text.length()>20)
				filename = text.substring(0, 15);
			
			filename = count+"_"+filename + ".txt";
			count++;
			
			if (bPos==null) // no tagging
				continue;
			if (bPos){
	            try {
	                FileUtils.writeStringToFile(new File(outputDir+"/pos/"+filename), text, "UTF-8");
                } catch (IOException e) {
	                e.printStackTrace();
                }
			} else if (bNeg){
				try {
	                FileUtils.writeStringToFile(new File(outputDir+"/neg/"+filename), text, "UTF-8");
                } catch (IOException e) {
	                e.printStackTrace();
                }
			}
		}
	}
	
}
