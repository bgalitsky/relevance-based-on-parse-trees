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
package opennlp.tools.apps.ultimate_deception;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;

import opennlp.tools.jsmlearning.ProfileReaderWriter;
import opennlp.tools.parse_thicket.ParseThicket;
import opennlp.tools.parse_thicket.VerbNetProcessor;
import opennlp.tools.parse_thicket.apps.MultiSentenceSearchResultsProcessor;
import opennlp.tools.parse_thicket.kernel_interface.DescriptiveParagraphFromDocExtractor;
import opennlp.tools.parse_thicket.kernel_interface.TreeKernelBasedClassifier;
import opennlp.tools.parse_thicket.matching.Matcher;

public class ComplaintClassifier extends TreeKernelBasedClassifier{
	boolean bShortRun = false;
	public static final Log logger = LogFactory.getLog(ComplaintClassifier.class);
	private static int MIN_PARA_LENGTH = 200, //120, 
			MIN_NUM_WORDS=15, 
			MAX_PARA_LENGTH = 500, //200 
			TEXT_PORTION_FOR_ANALYSIS = 20000, 
			MAX_PARA_OUTPUT=20;
	public void setShortRun(){
		bShortRun = true;
	}
	
	private static float MIN_SVM_SCORE_TOBE_IN_POS = -0.35f, MIN_SVM_SCORE_TOBE_IN_NEG = -0.50f;
	
	
	public void setThreshold(Float[] threshs){
		MIN_SVM_SCORE_TOBE_IN_POS = threshs[0];
		MIN_SVM_SCORE_TOBE_IN_NEG = threshs[1];
	}

	public void setModelFile(String name) {
		try {
			// to check if exists
	        modelFileName = name;
        } catch (Exception e) {
	        System.err.println("Specified model file does not exist.  Using the default one");
	        e.printStackTrace();
        }
    }
	
	
	private static String normalizePara(String p){
		p = p.replaceAll("\\n", " ").replaceAll("\\.\\.", " ").replaceAll("  ", " ");
		p = p.replaceAll("[^A-Za-z0-9 _\\.,\\!]", "");
		return p;
	}
	
	public static List<String> getLongParagraphsFromString (String text) {
		List<String> results = new ArrayList<String>();

		try {
			if (text.length()>TEXT_PORTION_FOR_ANALYSIS)
				text = text.substring(0, TEXT_PORTION_FOR_ANALYSIS);
			float avgSentSizeThr = (float)MIN_PARA_LENGTH/4f; //2f
			String[] portions = text.split("\\.\\n");
			if (portions.length<2)
				portions = text.split("\\n\\n");
			if (portions.length<2)
				portions = text.split("\\n \\n");
			if (portions.length<2){
				String[] sentences = text.replace('.','&').split(" & ");
				List<String> portionsLst = new ArrayList<String>();
				int totalChars = 0;
				String buffer = "";
				for(String sent: sentences){
					totalChars+=sent.length();
					if (totalChars>MAX_PARA_LENGTH){
						portionsLst.add(buffer);
						buffer="";
						totalChars = 0;
					} else {
						buffer+= sent + ". ";
					}
				}
				portions = portionsLst.toArray(new String[0]);
			}
			for(String p: portions){
				try {
					float avgSentSize = (float)p.length()/(float)p.split("\\n\\n").length;

					if (p.length()> MIN_PARA_LENGTH && p.split(" ").length>MIN_NUM_WORDS &&
							avgSentSize > avgSentSizeThr) {  
						if (p.length() < MAX_PARA_LENGTH){
							results.add(normalizePara(p)); 
						}
						else { // reduce length to the latest '.' in substring
							
							String pReduced = p;
							if (p.length()>= MAX_PARA_LENGTH+80)
								pReduced = p.substring(0, MAX_PARA_LENGTH+80);
							int indexPeriod = pReduced.lastIndexOf('.');
							if (indexPeriod>-1){
								pReduced = pReduced.substring(0, indexPeriod);
							}
							results.add(normalizePara(pReduced));
						}
						if (results.size()>MAX_PARA_OUTPUT)
							break;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (results.size()<1){
				if (text.length()>= MAX_PARA_LENGTH+80)
					text = text.substring(0, MAX_PARA_LENGTH+80);
				results.add(text);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		if (results.size()<1){
			System.err.println("Failed to extract text from : "+text);
		}

		return results;
	}
	
	
	
	public void trainClassifier(
			String posDirectory, String negDirectory) {

		queuePos.clear(); queueNeg.clear();
		addFilesPos(new File(posDirectory));
		addFilesNeg(new File(negDirectory));

		List<File> filesPos = new ArrayList<File>(queuePos), filesNeg = new ArrayList<File>(queueNeg);

		Collection<String> treeBankBuffer = new ArrayList<String>();
		int countPos=0, countNeg=0;

		for (File f : filesPos) {
			// get first paragraph of text
			List<String> texts=DescriptiveParagraphFromDocExtractor.getLongParagraphsFromFile(f);		
			List<String> lines = formTreeKernelStructuresMultiplePara(texts, "1");
			treeBankBuffer.addAll(lines);		
			if (bShortRun && countPos>3000)
				break;

			countPos++;
		}	
		for (File f : filesNeg) {
			// get first paragraph of text 
			List<String> texts=DescriptiveParagraphFromDocExtractor.getLongParagraphsFromFile(f);	
			List<String> lines = formTreeKernelStructuresMultiplePara(texts, "-1");
			treeBankBuffer.addAll(lines);	
			if (bShortRun && countNeg>3000)
				break;

			countNeg++;
		}	

		// write the lists of samples to a file
		try {
			FileUtils.writeLines(new File(path+trainingFileName), null, treeBankBuffer);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//	ProfileReaderWriter.writeReport(treeBankBuffer, path+trainingFileName, ' ');
		// build the model
		tkRunner.runLearner(path, trainingFileName, modelFileName);
	}

	public List<String[]> classifyFilesInDirectory(String dirFilesToBeClassified){
		Map<Integer, Integer> countObject = new HashMap<Integer, Integer>(); 
		int itemCount=0, objectCount = 0;
		List<String> treeBankBuffer = new ArrayList<String>();
		queuePos.clear();
		addFilesPos(new File( dirFilesToBeClassified));
		List<File> filesUnkn = new ArrayList<File>(queuePos);
		for (File f : filesUnkn) {	
			List<String> texts=DescriptiveParagraphFromDocExtractor.getLongParagraphsFromFile(f);
			List<String> lines = formTreeKernelStructuresMultiplePara(texts, "0");
			for(String l: lines){
				countObject.put(itemCount, objectCount);
				itemCount++;
			}
			objectCount++;
			treeBankBuffer.addAll(lines);		
		}	
		// write the lists of samples to a file
		try {
			FileUtils.writeLines(new File(path+unknownToBeClassified), null, treeBankBuffer);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		tkRunner.runClassifier(path, unknownToBeClassified, modelFileName, classifierOutput);
		// read classification results
		List<String[]> classifResults = ProfileReaderWriter.readProfiles(path+classifierOutput, ' ');
		// iterate through classification results and set them as scores for hits
		List<String[]>results = new ArrayList<String[]>();

		itemCount=0; objectCount = 0;
		int currentItemCount=0;
		float accum = 0;
		for(String[] line: classifResults){
			Float val = Float.parseFloat(line[0]);
			accum+=val;
			// last line
			Boolean bLastLine = false;
			if (itemCount==classifResults.size()-1)
				bLastLine = true;

			if (objectCount== countObject .get(itemCount) /*&& !bLastLine*/){
				itemCount++; 
				currentItemCount++;
				continue;
			}
			else while(objectCount!= countObject .get(itemCount)-1){
				objectCount++;
				String[] rline = new String[]{filesUnkn.get(objectCount).getName(), "unknown", "0",
						filesUnkn.get(objectCount).getAbsolutePath() , new Integer(itemCount).toString(), new Integer(objectCount).toString()}; 
				results.add(rline);
			}
			objectCount = countObject.get(itemCount);
			itemCount++; 

			float averaged = accum/(float)currentItemCount;
			currentItemCount=0;
			Boolean in = false;
			if (averaged> MIN_SVM_SCORE_TOBE_IN)
				in = true;

			String[] rline = new String[]{filesUnkn.get(objectCount).getName(), in.toString(), new Float(averaged).toString(),
					filesUnkn.get(objectCount).getAbsolutePath() , new Integer(itemCount).toString(), new Integer(objectCount).toString()}; 
			results.add(rline);
			accum=0;
		}
		return results;

	}

	
    /* main entry point to SVM TK classifier
     * gets a file, reads it outside of CI, extracts longer paragraphs and builds parse thickets for them.
     * Then parse thicket dump is processed by svm_classify
     */
	public  SVMClassifResult classifyTextFromString(String content){
		try {
	        FileUtils.forceDelete(new File(path+unknownToBeClassified));
        } catch (IOException e1) {
	        e1.printStackTrace();
        } 
		if (!(new File(path+modelFileName).exists())){
			logger.error("Model file '" +modelFileName + "'is absent: skip SVM classification");
			return null;
		}
		Map<Integer, Integer> countObject = new HashMap<Integer, Integer>(); 
		int itemCount=0, objectCount = 0;
		List<String> treeBankBuffer = new ArrayList<String>();	
		
		
		String[] texts=content.split("\n\n");
		List<String> textsLst = Arrays.asList(texts);
		List<String> lines = formTreeKernelStructuresMultiplePara(textsLst, "0");
		for(String l: lines){
			countObject.put(itemCount, objectCount);
			itemCount++;
		}
		objectCount++;
		treeBankBuffer.addAll(lines);		

		// write the lists of samples to a file
		try {
			FileUtils.writeLines(new File(path+unknownToBeClassified), null, treeBankBuffer);
		} catch (IOException e) {
			logger.error("Problem creating parse thicket files '"+ path+unknownToBeClassified + "' to be classified\n"+ e.getMessage() );
		}

		tkRunner.runClassifier(path, unknownToBeClassified, modelFileName, classifierOutput);
		// read classification results
		List<String[]> classifResults = ProfileReaderWriter.readProfiles(path+classifierOutput, ' ');

        SVMClassifResult result = new SVMClassifResult();
        
		itemCount=0; objectCount = 0;
		int currentItemCount=0;
		float accum = 0; List<Float> vals = new ArrayList<Float>();
		logger.debug("\nsvm scores per paragraph: " );
		for(String[] line: classifResults){
			Float val = Float.parseFloat(line[0]);
			vals.add(val);
			logger.debug(val+" ");
			accum+=val;
			currentItemCount++;
		}

		result.setArray(vals);
		
		float averaged = accum/(float)currentItemCount;
		result.setAvg(averaged);
		logger.debug("\n average SVM TK values= "+averaged);
		currentItemCount=0;
		if (averaged> MIN_SVM_SCORE_TOBE_IN_POS)
			result.setBool(true);
		else 
			if (averaged < MIN_SVM_SCORE_TOBE_IN_NEG)
 				result.setBool(false);
			else
				result.setBool(null);
			
		return result;
	}

	protected List<String> formTreeKernelStructuresMultiplePara(List<String> texts, String flag) {
		List<String> extendedTreesDumpTotal = new ArrayList<String>();
		try {
			for(String text: texts){
				// get the parses from original documents, and form the training dataset
				System.out.println("About to build pt from "+text);
				ParseThicket pt = matcher.buildParseThicketFromTextWithRST(text);
				System.out.print("About to build extended forest ");
				List<String> extendedTreesDump = treeExtender.buildForestForCorefArcs(pt);
				for(String line: extendedTreesDump)
					extendedTreesDumpTotal.add(flag + " |BT| "+line + " |ET| ");
				System.out.println("DONE");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return extendedTreesDumpTotal;
	}

	public static void main(String[] args){
		VerbNetProcessor p = VerbNetProcessor.
				getInstance(ComplaintClassifierDataPreparer.resourceDir); 

		ComplaintClassifier proc = new ComplaintClassifier();
		proc.setKernelPath(ComplaintClassifierDataPreparer.resourceDir + "/tree_kernel/");
		proc.trainClassifier(ComplaintClassifierDataPreparer.outputDir+"/pos",
				ComplaintClassifierDataPreparer.outputDir+"/neg");
		
	}

}

/*
 * complLine[4]: is complaint valid
 * 
Checking optimality of inactive variables...done.
 Number of inactive variables = 9729
done. (10429 iterations)
Optimization finished (0 misclassified, maxdiff=0.00099).
Runtime in cpu-seconds: 3549.82
Number of SV: 7460 (including 374 at upper bound)
L1 loss: loss=19.41737
Norm of weight vector: |w|=59.14082
Norm of longest example vector: |x|=1.00000
Estimated VCdim of classifier: VCdim<=3498.63609
Computing XiAlpha-estimates...done
Runtime for XiAlpha-estimates in cpu-seconds: 0.18
XiAlpha-estimate of the error: error<=3.30% (rho=1.00,depth=0)
XiAlpha-estimate of the recall: recall=>93.65% (rho=1.00,depth=0)
XiAlpha-estimate of the precision: precision=>96.90% (rho=1.00,depth=0)
Number of kernel evaluations: 1285315491

=================================================================================
Checking optimality of inactive variables...done.
 Number of inactive variables = 12941
done. (7456 iterations)
Optimization finished (3 misclassified, maxdiff=0.00098).
Runtime in cpu-seconds: 2501.01
Number of SV: 6078 (including 171 at upper bound)
L1 loss: loss=14.05874
Norm of weight vector: |w|=47.35386
Norm of longest example vector: |x|=1.00000
Estimated VCdim of classifier: VCdim<=2243.38773
Computing XiAlpha-estimates...done
Runtime for XiAlpha-estimates in cpu-seconds: 0.19
XiAlpha-estimate of the error: error<=2.05% (rho=1.00,depth=0)
XiAlpha-estimate of the recall: recall=>89.58% (rho=1.00,depth=0)
XiAlpha-estimate of the precision: precision=>98.21% (rho=1.00,depth=0)
Number of kernel evaluations: 862804536


*/
