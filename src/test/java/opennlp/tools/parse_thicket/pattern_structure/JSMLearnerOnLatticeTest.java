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

package opennlp.tools.parse_thicket.pattern_structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import junit.framework.TestCase;
import opennlp.tools.fca.ConceptLattice;
import opennlp.tools.fca.FcaWriter;
import opennlp.tools.fca.FormalConcept;
import opennlp.tools.similarity.apps.BingWebQueryRunner;
import opennlp.tools.similarity.apps.HitBase;
import opennlp.tools.textsimilarity.ParseTreeChunk;
import opennlp.tools.textsimilarity.ParseTreeMatcherDeterministic;
import opennlp.tools.textsimilarity.chunker2matcher.ParserChunker2MatcherProcessor;

public class JSMLearnerOnLatticeTest extends TestCase{
	ParserChunker2MatcherProcessor chunk_maker = ParserChunker2MatcherProcessor.getInstance();
	LinguisticPatternStructure psPos = new LinguisticPatternStructure(0,0), psNeg = new LinguisticPatternStructure(0,0);
	ParseTreeMatcherDeterministic md = new ParseTreeMatcherDeterministic(); 

	public void testJSMLearner() {

		String text1p = "I rent an office space. This office is for my business. I can deduct office rental expense from my business profit to calculate net income. ";
		String text2p = "To run my business, I have to rent an office. The net business profit is calculated as follows. Rental expense needs to be subtracted from revenue. ";
		String text3p = "To store goods for my retail business I rent some space. When I calculate the net income, I take revenue and subtract business expenses such as office rent. ";
		String text4p = "I rent some space for my business. To calculate my net income, I subtract from revenue my rental business expense.";


		String text1n = "I rent out a first floor unit of my house to a travel business. I need to add the rental income to my profit. However, when I repair my house, I can deduct the repair expense from my rental income. ";
		String text2n = "I receive rental income from my office. I have to claim it as a profit in my tax forms. I need to add my rental income to my profits, but subtract rental expenses such as repair from it. ";
		String text3n = "I advertised my property as a business rental. Advertisement and repair expenses can be subtracted from the rental income. Remaining rental income needs to be added to my profit and be reported as taxable profit. ";				
		String text4n = "I showed  my property to a business owner to rent. Expenses on my time spent on advertisement are subtracted from the rental income. My rental profits are added to my taxable income.  ";				

		List<List<ParseTreeChunk>> chunks1p = chunk_maker.formGroupedPhrasesFromChunksForPara(text1p);
		List<List<ParseTreeChunk>> chunks2p = chunk_maker.formGroupedPhrasesFromChunksForPara(text2p);
		List<List<ParseTreeChunk>> chunks3p = chunk_maker.formGroupedPhrasesFromChunksForPara(text3p);
		List<List<ParseTreeChunk>> chunks4p = chunk_maker.formGroupedPhrasesFromChunksForPara(text4p);
		List<List<ParseTreeChunk>> chunks1n = chunk_maker.formGroupedPhrasesFromChunksForPara(text1n);
		List<List<ParseTreeChunk>> chunks2n = chunk_maker.formGroupedPhrasesFromChunksForPara(text2n);
		List<List<ParseTreeChunk>> chunks3n = chunk_maker.formGroupedPhrasesFromChunksForPara(text3n);
		List<List<ParseTreeChunk>> chunks4n = chunk_maker.formGroupedPhrasesFromChunksForPara(text4n);


		LinkedHashSet<Integer> obj = null;
		obj = new LinkedHashSet<Integer>();
		obj.add(0);
		psPos.AddIntent(chunks1p, obj, 0);
		obj = new LinkedHashSet<Integer>();
		obj.add(1);
		psPos.AddIntent(chunks2p, obj, 0);
		obj = new LinkedHashSet<Integer>();
		obj.add(2);
		psPos.AddIntent(chunks3p, obj, 0);
		obj = new LinkedHashSet<Integer>();
		obj.add(3);
		psPos.AddIntent(chunks4p, obj, 0);
		obj = new LinkedHashSet<Integer>();
		obj.add(0);
		psNeg.AddIntent(chunks1n, obj, 0);
		obj = new LinkedHashSet<Integer>();
		obj.add(1);
		psNeg.AddIntent(chunks2n, obj, 0);
		obj = new LinkedHashSet<Integer>();
		obj.add(2);
		psNeg.AddIntent(chunks3n, obj, 0);
		obj = new LinkedHashSet<Integer>();
		obj.add(3);
		psNeg.AddIntent(chunks4n, obj, 0);

		String unknown = "I do not want to rent anything to anyone. I just want to rent a space for myself. I neither calculate deduction of individual or business tax. I subtract my tax from my income";
		List<List<ParseTreeChunk>> chunksUnknown = chunk_maker.formGroupedPhrasesFromChunksForPara(unknown);
		List<List<List<ParseTreeChunk>>> posIntersections = new ArrayList<List<List<ParseTreeChunk>>>(), 
				negIntersections = new ArrayList<List<List<ParseTreeChunk>>>();
		List<List<ParseTreeChunk>> intersection = null;
		for(int iConcept = 0; iConcept<psPos.conceptList.size(); iConcept++){
			if (psPos.conceptList.get(iConcept).intent!=null && psPos.conceptList.get(iConcept).intent.size()>0){
				intersection = md
						.matchTwoSentencesGroupedChunksDeterministic(psPos.conceptList.get(iConcept).intent, chunksUnknown);
				posIntersections.add(intersection);
			}
			if (psNeg.conceptList.get(iConcept).intent!=null && psNeg.conceptList.get(iConcept).intent.size()>0){				
				intersection = md
						.matchTwoSentencesGroupedChunksDeterministic(psNeg.conceptList.get(iConcept).intent, chunksUnknown);
				negIntersections.add(intersection);
			}
		}

		List<List<List<ParseTreeChunk>>> posIntersectionsUnderNeg = new ArrayList<List<List<ParseTreeChunk>>>(), 
				negIntersectionsUnderPos = new ArrayList<List<List<ParseTreeChunk>>>();

		for(int iConcept = 0; iConcept<psPos.conceptList.size(); iConcept++){
			for(int iConceptJ = 0; iConceptJ<psPos.conceptList.size(); iConceptJ++){
				intersection = md
						.matchTwoSentencesGroupedChunksDeterministic(psNeg.conceptList.get(iConcept).intent, negIntersections.get(iConceptJ));
				posIntersectionsUnderNeg.add(intersection);
				intersection = md
						.matchTwoSentencesGroupedChunksDeterministic(psPos.conceptList.get(iConcept).intent, posIntersections.get(iConceptJ));
				negIntersectionsUnderPos.add(intersection);
			}
		}

	}





}
