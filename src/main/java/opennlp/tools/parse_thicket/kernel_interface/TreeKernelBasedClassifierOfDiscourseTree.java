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
package opennlp.tools.parse_thicket.kernel_interface;


import java.util.ArrayList;
import java.util.List;

import opennlp.tools.parse_thicket.ParseThicket;
import opennlp.tools.parse_thicket.VerbNetProcessor;
import opennlp.tools.parse_thicket.external_rst.MatcherExternalRST;
import opennlp.tools.parse_thicket.external_rst.ParseThicketWithDiscourseTree;

/*
 * This class performs TK learning based on parse thicket which includes RST relations only 
 * based on Surdeanu at al RST parser. It does sentence parsing and NLP pipeline of 
 * Surdeanu's wrapper of Stanford NLP
 */
public class TreeKernelBasedClassifierOfDiscourseTree extends TreeKernelBasedClassifierMultiplePara{

	private MatcherExternalRST matcherRST = new MatcherExternalRST();

	protected List<String> formTreeKernelStructuresMultiplePara(List<String> texts, String flag) {
		//TODO
		this.setShortRun();	
		List<String> extendedTreesDumpTotal = new ArrayList<String>();
		try {

			for(String text: texts){
				// get the parses from original documents, and form the training dataset
				try {
					System.out.print("About to build pt with external rst from "+text + "\n...");
					ParseThicket pt = matcherRST.buildParseThicketFromTextWithRST(text);
					if (pt == null)
						continue;
					System.out.print("About to build extended forest with external rst...");
					List<String> extendedTreesDump =  // use direct option (true
							buildReptresentationForDiscourseTreeAndExtensions((ParseThicketWithDiscourseTree)pt, true);
					for(String line: extendedTreesDump)
						extendedTreesDumpTotal.add(flag + " |BT| "+line + " |ET| ");
					System.out.println("DONE");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return extendedTreesDumpTotal;
	}

	private List<String> buildReptresentationForDiscourseTreeAndExtensions(ParseThicketWithDiscourseTree pt, boolean bDirectDT){
		List<String> extendedTreesDump = new ArrayList<String>();
		if (!bDirectDT)
			// option 1: use RST relation for extended trees 
			extendedTreesDump = treeExtender.buildForestForRSTArcs(pt);
		else {
			// option 2: use DT directly
			extendedTreesDump.add(pt.getDtDump());
		    extendedTreesDump.add(pt.getDtDumpWithPOS());
		    extendedTreesDump.add(pt.getDtDumpWithEmbeddedTrees());
		    extendedTreesDump.add(pt.getDtDumpWithVerbNet());
		}		
		return extendedTreesDump;
	}
	/*
	public void dt(){
		1 |BT| (elaboration
		  (joint
		    (elaboration 
		      (It that)
		    (joint (Journalism readers)))
		  (elaboration ( elaboration ( joint (elaboration (I who))) and)
		      manner-means (by they))
		    elaboration (rural become)))|ET|

		    (elaboration (joint (elaboration (It that) (joint (Journalism readers))) 
		    (elaboration ( elaboration ( joint ( elaboration (I who))) and) (manner-means (by they)) 
		    elaboration (rural become))))

		    1 |BT| (elaboration (joint (elaboration (It that) (joint (Journalism readers)))))|ET|

	} 
	 
	
	(elaboration (joint (attribution ((I PRP)(thought VBD)) 
			((I PRP)(d NN)(tell VBP)(you PRP)(a DT)(little JJ)(about IN)(what WP)(I PRP)(like VBP)(to TO)(write VB)(. .))) 
			(joint ((And CC)(I PRP)(like VBP)(to TO)(immerse VB)(myself PRP)(in IN)(my PRP$)(topics NNS)(. .)) 
			(joint ((I PRP)(just RB)(like VBP)(to TO)(dive NN)(right NN)(in IN)) ((and CC)(become VB)(sort NN)(of IN)(a DT)(human JJ)(guinea NN)(pig NN)(. .))))) 
			(elaboration (joint ((And CC)(I PRP)(see VBP)(my PRP$)(life NN)(as IN)(a DT)(series NN)(of IN)(experiments NNS)(. .)) (joint ((So RB)(, ,)(I PRP)(work VBP)(for IN)(Esquire NNP)
					(magazine NN)(, ,)) (elaboration (elaboration ((and CC)(a DT)(couple NN)(of IN)(years NNS)(ago IN)(I PRP)(wrote VBD)(an DT)(article NN)) ((called VBN)(My PRP$)(Outsourced JJ)(Life NNP)(, ,))) 
							(enablement ((where WRB)(I PRP)(hired VBD)(a DT)(team NN)(of IN)(people NNS)(in IN)(Bangalore NNP)(, ,)(India NNP)(, ,)) ((to TO)(live VB)(my PRP$)(life NN)(for IN)(me PRP)(. .)))))) 
							(elaboration ((So IN)(they PRP)(answered VBD)(my PRP$)(emails NNS)(. .)) 
			((They PRP)(answered VBD)(my PRP$)(phone NN)(. .))))) 

	(elaboration (elaboration (joint (elaboration (joint (elaboration (And CC)(this DT) (be  (beg-582 beg-582 beg-582 ) 
			(NP V for NP S_INF NP V NP PP-proposition NP V NP S_INF NP V PP-proposition NP V NP that S NP V S_INF ) 
			(FOR-TO-INF NP-PPfor-PP NP-TO-INF-OC PPfor-PP S-SUBJUNCT TO-INF-SCwith-PP ) ) (
					the DT)(United NNP)(States NNPS) (move  (slide-112 slide-112 slide-112 slide-112 ) 
							(NP V NP V PP-source NP V PP-destination NP V PP-source PP-destination NP-agent V NP NP V NP PP-source NP V NP PP-destination NP V NP PP-source             PP-destination ) 
							(Intransitive PPpath-PP PPpath-PP PPpath-PP Basic TransitiveCausative NP-PPpath-PP NP-PPpath-PP NP-PPpath-PP ) ) 
							(away RB)(here RB) (earn  (get-1351-1 ) (NP V NP PP-source ) (NP-PPfor/on Source (abstract) ) 
									(edu.mit.jverbnet.data.ThematicRole@43a267d2 edu.mit.jverbnet.data.ThematicRole@2db6b034 
											edu.mit.jverbnet.data.ThematicRole@38f90b2d edu.mit.jverbnet.data.ThematicRole@67bd3d10 edu.mit.jverbnet.data.ThematicRole@37715ac2 ))
											(more JJR)(money NN)) (And CC)(we PRP)(will MD)(soon RB) (see  (see-301 see-301 see-301 ) (NP V NP NP V that S NP V NP-ATTR-POS PP-oblique NP V how S NP V what S ) (Basic Transitive S Attribute Object Possessor-Attribute Factoring Alternation HOW-S WHAT-S ) ) (China NNP)(coming VBG)(up RB)(in IN)(the DT)(very RB)(far RB)(end VB)(corner NN)(here RB)) (elaboration (cause (And CC)(it PRP) (move  (slide-112 slide-112 slide-112 slide-112 ) (NP V NP V PP-source NP V PP-destination NP V PP-source PP-destination NP-agent V NP NP V NP PP-source NP V NP PP-destination NP V NP PP-source             PP-destination ) (Intransitive PPpath-PP PPpath-PP PPpath-PP Basic TransitiveCausative NP-PPpath-PP NP-PPpath-PP NP-PPpath-PP ) ) (up RP)(with IN)(Mao NNP)(TseTung NNP) (get  (get-1351 get-1351 get-1351 get-1351 get-1351 ) (NP V NP NP V NP PP-source NP V NP PP-beneficiary NP V NP-beneficiary NP NP V NP PP-asset NP-asset V NP NP V NP PP-source NP-asset ) (Basic Transitive NP-PPfrom-PP NP-PPBeneficiary Object Benefactive Alternationdouble object NP-PPAsset Subject NPAsset Subject NP-PP-PPSource-PP  Asset-PP ) ) (health NN) (not RB) (get  (get-1351 get-1351 get-1351 get-1351 get-1351 ) (NP V NP NP V NP PP-source NP V NP PP-beneficiary NP V NP-beneficiary NP NP V NP PP-asset NP-asset V NP NP V NP PP-source NP-asset ) (Basic Transitive NP-PPfrom-PP NP-PPBeneficiary Object Benefactive Alternationdouble object NP-PPAsset Subject NPAsset Subject NP-PP-PPSource-PP  Asset-PP ) ) (so RB)(rich JJ)) (temporal (There EX)(he PRP) (die  (disappearance-482 disappearance-482 ) (NP V There V PP NP ) (Basic Intransitive PP-NPExpletive-there Subject ) ) (attribution (then RB)(Deng NNP)(Xiaoping NNP) (bring  (bring-113 bring-113 bring-113 bring-113 ) (NP V NP NP V NP PP-destination NP V PP-destination NP NP V NP PP-source NP V NP PP-source PP-destination NP V NP ADVP ) (Basic Transitive NP-PPGoal-PP PP-NPGoal-PP NP-PPSource-PP NP-PP-PPSource-PP Goal-PP NP-ADVP-PREDhere/there ) ) (money NN) (it PRP) (move  (slide-112 slide-112 slide-112 slide-112 ) (NP V NP V PP-source NP V PP-destination NP V PP-source PP-destination NP-agent V NP NP V NP PP-source NP V NP PP-destination NP V NP PP-source             PP-destination ) (Intransitive PPpath-PP PPpath-PP PPpath-PP Basic TransitiveCausative NP-PPpath-PP NP-PPpath-PP NP-PPpath-PP ) ) (this DT)(way NN)(over IN)(here RB))))) (joint (And CC)(the DT)(bubbles NNS) (keep  (keep-152 keep-152 keep-152 ) (NP V NP PP-location NP V NP ) (NP-PPlocative-PP Basic Transitive ) )  (move  (slide-112 slide-112 slide-112 slide-112 ) (NP V NP V PP-source NP V PP-destination NP V PP-source PP-destination NP-agent V NP NP V NP PP-source NP V NP PP-destination NP V NP PP-source             PP-destination ) (Intransitive PPpath-PP PPpath-PP PPpath-PP Basic TransitiveCausative NP-PPpath-PP NP-PPpath-PP NP-PPpath-PP ) ) (up RB)(there RB) (and CC)(this DT) (be  (beg-582 beg-582 beg-582 ) (NP V for NP S_INF NP V NP PP-proposition NP V NP S_INF NP V PP-proposition NP V NP that S NP V S_INF ) (FOR-TO-INF NP-PPfor-PP NP-TO-INF-OC PPfor-PP S-SUBJUNCT TO-INF-SCwith-PP ) ) (what WP)(the DT)(world NN)(looks VBZ)(like IN)(today NN))) (Applause NN)(Let VB)(us PRP) (have  (own-100 own-100 ) (NP V NP ) (NP ) ) (a DT)(look NN)(at IN)(the DT)(United NNP)(States NNPS)) (elaboration (We PRP) (have  (own-100 own-100 ) (NP V NP ) (NP ) ) (a DT)(function NN)(here RB)(I PRP)(can MD) (tell  (tell-372 tell-372 tell-372 ) (NP V NP NP V NP PP-topic NP V NP S ) (NP NP-PPof-PP NP-S ) ) (the DT)(world NN)(Stay NNP) (where WRB)(you PRP) (be  (beg-582 beg-582 beg-582 ) (NP V for NP S_INF NP V NP PP-proposition NP V NP S_INF NP V PP-proposition NP V NP that S NP V S_INF ) (FOR-TO-INF NP-PPfor-PP NP-TO-INF-OC PPfor-PP S-SUBJUNCT TO-INF-SCwith-PP ) ))) 

	
	(elaboration (elaboration (attribution (And CC)(we PRP)(can MD) (see  (see-301 see-301 see-301 ) (NP V NP NP V that S NP V NP-ATTR-POS 
			PP-oblique NP V how S NP V what S ) (Basic Transitive S Attribute Object Possessor-Attribute Factoring Alternation HOW-S WHAT-S ) 
			) (that IN)(the DT)(United NNP)(States NNPS) (go  (gobble-393 ) () () ) (to TO)(the DT)(right NN)(of IN)(the DT)(mainstream NN))
			(joint (They PRP) (be  (beg-582 beg-582 beg-582 ) (NP V for NP S_INF NP V NP PP-proposition NP V NP S_INF NP V PP-proposition NP
					V NP that S NP V S_INF ) (FOR-TO-INF NP-PPfor-PP NP-TO-INF-OC PPfor-PP S-SUBJUNCT TO-INF-SCwith-PP ) ) 
					(on IN)(the DT)(money NN)(side NN)(all PDT)(the DT)(time NN) (joint (And CC)(down RB)(in IN)(1915 CD)(the DT)
							(United NNP)(States NNPS) (be  (beg-582 beg-582 beg-582 ) (NP V for NP S_INF NP V NP PP-proposition NP V NP 
									S_INF NP V PP-proposition NP V NP that S NP V S_INF ) (FOR-TO-INF NP-PPfor-PP NP-TO-INF-OC PPfor-PP 
											S-SUBJUNCT TO-INF-SCwith-PP ) ) (a DT)(neighbor NN)(of IN)(India NNP)(present JJ)(contemporary JJ)
											(India NNP) (joint (contrast (And CC)(that DT) (mean  (meander-477 meander-477 ) (NP V PP-location PP-location V NP There V PP NP There V NP PP ) 
													(PPpath-PP Locative Inversion PP-NPExpletive-there Subject NP-PPExpletive-there Subject ) ) (United NNP)(States NNPS) 
													(be  (beg-582 beg-582 beg-582 ) (NP V for NP S_INF NP V NP PP-proposition NP V NP S_INF NP V PP-proposition NP V NP that S NP 
															V S_INF ) (FOR-TO-INF NP-PPfor-PP NP-TO-INF-OC PPfor-PP S-SUBJUNCT TO-INF-SCwith-PP ) ) (richer JJR) (elaboration 
																	(but CC) (lose  (contribute-132 contribute-132 contribute-132 ) (NP V NP PP-recipient NP V NP-theme (PP) 
																			NP V PP-recipient NP ) (NP-PPRecipient-PP TransitiveTheme Object PP-NPRecipient-PP ) ) 
																			(more JJR)(kids NNS) (than IN)(India NNP) (be  (beg-582 beg-582 beg-582 ) (NP V for NP S_INF NP V NP 
																					PP-proposition NP V NP S_INF NP V PP-proposition NP V NP that S NP V S_INF ) 
																					(FOR-TO-INF NP-PPfor-PP NP-TO-INF-OC PPfor-PP S-SUBJUNCT TO-INF-SCwith-PP ) ) (doing VBG)(today NN)(proportionally RB))) 
															(And CC)(look VB)(here RB)(compare VB)(to TO)(the DT)(Philippines NNPS)(of IN)(today NN))))) 
															)
															*/
	public static void main(String[] args){
		VerbNetProcessor p = VerbNetProcessor.
				getInstance("/Users/bgalitsky/Documents/relevance-based-on-parse-trees/src/test/resources"); 

		TreeKernelBasedClassifierOfDiscourseTree proc = new TreeKernelBasedClassifierOfDiscourseTree();
		proc.setKernelPath("/Users/bgalitsky/Documents/relevance-based-on-parse-trees/src/test/resources/tree_kernel/");
		proc.trainClassifier(

				"/Users/bgalitsky/Documents/relevance-based-on-parse-trees/src/test/resources/style_recognizer/txt/ted",
				"/Users/bgalitsky/Documents/relevance-based-on-parse-trees/src/test/resources/style_recognizer/txt/Tedi");
	}

}
/*
 * 
RST - based run
Number of examples: 6980, linear space size: 10
ted vs Tedi

estimating ...
Setting default regularization parameter C=1.0000
Optimizing............................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................
 Checking optimality of inactive variables...done.
 Number of inactive variables = 1931
done. (3597 iterations)
Optimization finished (78 misclassified, maxdiff=0.00100).
Runtime in cpu-seconds: 198.37
Number of SV: 3830 (including 652 at upper bound)
L1 loss: loss=261.78883
Norm of weight vector: |w|=41.37067
Norm of longest example vector: |x|=1.00000
Estimated VCdim of classifier: VCdim<=1712.53247
Computing XiAlpha-estimates...done
Runtime for XiAlpha-estimates in cpu-seconds: 0.05
XiAlpha-estimate of the error: error<=11.53% (rho=1.00,depth=0)
XiAlpha-estimate of the recall: recall=>97.01% (rho=1.00,depth=0)
XiAlpha-estimate of the precision: precision=>89.47% (rho=1.00,depth=0)
Number of kernel evaluations: 73092240

GENERAL RUN (the same set of texts)
Number of examples: 21146, linear space size: 10

estimating ...
Setting default regularization parameter C=1.0000
Optimizing.........................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................
 Checking optimality of inactive variables...done.
 Number of inactive variables = 8849
done. (5770 iterations)
Optimization finished (231 misclassified, maxdiff=0.00098).
Runtime in cpu-seconds: 1486.33
Number of SV: 5368 (including 940 at upper bound)
L1 loss: loss=582.99311
Norm of weight vector: |w|=46.91885
Norm of longest example vector: |x|=1.00000
Estimated VCdim of classifier: VCdim<=2202.37876
Computing XiAlpha-estimates...done
Runtime for XiAlpha-estimates in cpu-seconds: 0.13
XiAlpha-estimate of the error: error<=5.57% (rho=1.00,depth=0)
XiAlpha-estimate of the recall: recall=>98.42% (rho=1.00,depth=0)
XiAlpha-estimate of the precision: precision=>95.18% (rho=1.00,depth=0)
Number of kernel evaluations: 550748695
Writing model file...done


Number of examples: 7461, linear space size: 10

estimating ...
Setting default regularization parameter C=1.0000
Optimizing............................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................................
 Checking optimality of inactive variables...done.
 Number of inactive variables = 2091
done. (3773 iterations)
Optimization finished (87 misclassified, maxdiff=0.00096).
Runtime in cpu-seconds: 231.42
Number of SV: 4092 (including 680 at upper bound)
L1 loss: loss=280.03696
Norm of weight vector: |w|=42.82963
Norm of longest example vector: |x|=1.00000
Estimated VCdim of classifier: VCdim<=1835.37688
Computing XiAlpha-estimates...done
Runtime for XiAlpha-estimates in cpu-seconds: 0.05
XiAlpha-estimate of the error: error<=11.54% (rho=1.00,depth=0)
XiAlpha-estimate of the recall: recall=>96.75% (rho=1.00,depth=0)
XiAlpha-estimate of the precision: precision=>89.59% (rho=1.00,depth=0)
Number of kernel evaluations: 94432306
Writing model file...done



SMALL SET

Number of examples: 172, linear space size: 10

estimating ...
Setting default regularization parameter C=1.0000
Optimizing.......................................................done. (56 iterations)
Optimization finished (0 misclassified, maxdiff=0.00076).
Runtime in cpu-seconds: 0.01
Number of SV: 172 (including 59 at upper bound)
L1 loss: loss=7.38525
Norm of weight vector: |w|=12.46777
Norm of longest example vector: |x|=1.00000
Estimated VCdim of classifier: VCdim<=156.44537
Computing XiAlpha-estimates...done
Runtime for XiAlpha-estimates in cpu-seconds: 0.00
XiAlpha-estimate of the error: error<=44.77% (rho=1.00,depth=0)
XiAlpha-estimate of the recall: recall=>79.55% (rho=1.00,depth=0)
XiAlpha-estimate of the precision: precision=>54.26% (rho=1.00,depth=0)
Number of kernel evaluations: 20139
Writing model file...done


LONGER RUN, DTs only
Number of examples: 720, linear space size: 10

estimating ...
Setting default regularization parameter C=1.0000
Optimizing............................................................................................................................................................................................................................................................................
 Checking optimality of inactive variables...done.
 Number of inactive variables = 114
done. (269 iterations)
Optimization finished (11 misclassified, maxdiff=0.00096).
Runtime in cpu-seconds: 0.17
Number of SV: 712 (including 140 at upper bound)
L1 loss: loss=117.83422
Norm of weight vector: |w|=12.73402
Norm of longest example vector: |x|=1.00000
Estimated VCdim of classifier: VCdim<=163.15526
Computing XiAlpha-estimates...done
Runtime for XiAlpha-estimates in cpu-seconds: 0.00
XiAlpha-estimate of the error: error<=20.14% (rho=1.00,depth=0)
XiAlpha-estimate of the recall: recall=>99.14% (rho=1.00,depth=0)
XiAlpha-estimate of the precision: precision=>80.42% (rho=1.00,depth=0)
Number of kernel evaluations: 283615
Writing model file...done

HYBRID RUN
Number of examples: 8301, linear space size: 10

estimating ...
Setting default regularization parameter C=1.0000
Optimizing................................
 Checking optimality of inactive variables...done.
 Number of inactive variables = 2323
done. (4206 iterations)
Optimization finished (98 misclassified, maxdiff=0.00099).
Runtime in cpu-seconds: 299.94
Number of SV: 4870 (including 846 at upper bound)
L1 loss: loss=398.61389
Norm of weight vector: |w|=44.95124
Norm of longest example vector: |x|=1.00000
Estimated VCdim of classifier: VCdim<=2021.61414
Computing XiAlpha-estimates...done
Runtime for XiAlpha-estimates in cpu-seconds: 0.05
XiAlpha-estimate of the error: error<=12.32% (rho=1.00,depth=0)
XiAlpha-estimate of the recall: recall=>97.15% (rho=1.00,depth=0)
XiAlpha-estimate of the precision: precision=>88.53% (rho=1.00,depth=0)
Number of kernel evaluations: 138447398
Writing model file...done
 */