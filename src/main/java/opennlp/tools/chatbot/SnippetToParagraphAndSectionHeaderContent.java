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

package opennlp.tools.chatbot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import opennlp.tools.parse_thicket.Pair;
import opennlp.tools.similarity.apps.UtteranceFilter;
import opennlp.tools.similarity.apps.HitBase;
import opennlp.tools.similarity.apps.utils.PageFetcher;
import opennlp.tools.similarity.apps.utils.StringDistanceMeasurer;
import opennlp.tools.similarity.apps.utils.Utils;
import opennlp.tools.textsimilarity.TextProcessor;

public class SnippetToParagraphAndSectionHeaderContent  {
	private PageFetcher pFetcher = new PageFetcher();
	private static Logger LOG = Logger
			.getLogger("opennlp.tools.chatbot.SnippetToParagraphAndSectionHeaderContent");
	private PageByTagsParser pageParser = new PageByTagsParser(); 
	private static StringDistanceMeasurer meas = new StringDistanceMeasurer();
	

	private Pair<String[], Map<String, String>> extractSentencesAndSectionMapFromPage(String url)
	{
		String downloadedPage = pFetcher.fetchPage(url, 10000);
		if (downloadedPage == null || downloadedPage.length() < 100)
		{
			return null;
		}
		
		List<String> acceptedSectionHeaders= pageParser.extractSectionTitles(url);
		Map<String, String> headerContent = new HashMap<String, String>();
		for(String h:acceptedSectionHeaders ){
			try {
	            int pos = downloadedPage.indexOf(h);
	            if (pos<0)
	            	continue;
	            String areaContent = downloadedPage.substring(pos+h.length(), Math.min(pos+h.length()+500, downloadedPage.length()))+"._.";
	            if (areaContent!=null) 
	            	headerContent.put(h, areaContent);
            } catch (Exception e) {
	            e.printStackTrace();
            }
		}
		
		//String pageOrigHTML = pFetcher.fetchOrigHTML(url);

		downloadedPage= downloadedPage.replace("     ", "&");
		downloadedPage = downloadedPage.replaceAll("(?:&)+", "#");
		String[] sents = downloadedPage.split("#");

		for(int i=0; i< sents.length; i++){
			String s = sents[i];
			s = s.trim().replace("  ", ". ").replace("..", ".").replace(". . .", " ")
					.replace(": ", ". ").replace("- ", ". ").
					replace (". .",".").trim();
			sents[i] = s;
			
		}
		sents = cleanAndSplitListOfSents(sents);

		return new Pair<String[], Map<String, String>> ( sents, headerContent );
	}

	public HitBase formTextFromOriginalPageGivenSnippet(HitBase item) {

		Pair<String[], Map<String, String>> sentsMap = extractSentencesAndSectionMapFromPage(item.getUrl());
		if (sentsMap==null)
			return item;
		
		String[] sents = sentsMap.getFirst();
		item.setSectionHeaderContent(sentsMap.getSecond());

		//String title = item.getTitle().replace("<b>", " ").replace("</b>", " ")
		//		.replace("  ", " ").replace("  ", " ");
		// generation results for this sentence
		List<String> result = new ArrayList<String>();
		// form plain text from snippet
		String snapshot = item.getAbstractText().replace("<b>", " ")
				.replace("</b>", " ").replace("  ", " ").replace("  ", " ").replace("\"", "");

		String snapshotMarked = snapshot.replace(" ...", ".");
		List<String> fragments = TextProcessor.splitToSentences(snapshotMarked);
		if (fragments.size()<3 && StringUtils.countMatches(snapshotMarked, ".")>1){
			snapshotMarked = snapshotMarked.replace("..", "&").replace(".", "&");
			String[] fragmSents = snapshotMarked.split("&");
			fragments = Arrays.asList(fragmSents);
		}

		for (String f : fragments) {
			String followSent = null;
			if (f.length() < 50)
				continue;
			String pageSentence = "";
			// try to find original sentence from webpage

			try {
				String[] mainAndFollowSent = getFullOriginalSentenceFromWebpageBySnippetFragment(
						f, sents);
				pageSentence = mainAndFollowSent[0];
				followSent = mainAndFollowSent[1];
				if (pageSentence!=null && followSent!=null)
					result.add(pageSentence + "\n" + followSent);
				else if (pageSentence!=null){
					result.add(pageSentence);
				}
				else {
					result.add(f);
					//LOG.info("Could not find the original sentence \n"+f +"\n in the page " );
				}

			} catch (Exception e) {

				e.printStackTrace();
			}
		}
		item.setOriginalSentences(result);
		return item;
	}

	protected String[] cleanAndSplitListOfSents(String[] longestSents){
		float minFragmentLength = 40, minFragmentLengthSpace=4;

		List<String> sentsClean = new ArrayList<String>();
		for (String sentenceOrMultSent : longestSents)
		{
			if (sentenceOrMultSent==null || sentenceOrMultSent.length()<20)
				continue;
			if (UtteranceFilter.acceptableMinedSentence(sentenceOrMultSent)==null){
				//System.out.println("Rejected sentence by GeneratedSentenceProcessor.acceptableMinedSentence = "+sentenceOrMultSent);
				continue;
			}
			// aaa. hhh hhh.  kkk . kkk ll hhh. lll kkk n.
			int numOfDots = sentenceOrMultSent.replace('.','&').split("&").length;
			float avgSentenceLengthInTextPortion = (float)sentenceOrMultSent.length() /(float) numOfDots;
			if ( avgSentenceLengthInTextPortion<minFragmentLength)
				continue;
			// o oo o ooo o o o ooo oo ooo o o oo
			numOfDots = sentenceOrMultSent.replace(' ','&').split("&").length;
			avgSentenceLengthInTextPortion = (float)sentenceOrMultSent.length() /(float) numOfDots;
			if ( avgSentenceLengthInTextPortion<minFragmentLengthSpace)
				continue;

			List<String> furtherSplit = TextProcessor.splitToSentences(sentenceOrMultSent);
			
			// forced split by ',' somewhere in the middle of sentence
			// disused - Feb 26 13
			//furtherSplit = furtherMakeSentencesShorter(furtherSplit);
			furtherSplit.remove(furtherSplit.size()-1);
			for(String s : furtherSplit){
				if (s.indexOf('|')>-1)
					continue;
				s = s.replace("<em>"," ").replace("</em>"," ");
				s = Utils.convertToASCII(s);
				sentsClean.add(s);
			}
		}

		return (String[]) sentsClean.toArray(new String[0]);
	}
	
	// given a fragment from snippet, finds an original sentence at a webpage by
		// optimizing alignmemt score
		public static String[] getFullOriginalSentenceFromWebpageBySnippetFragment(
				String fragment, String[] sents) {
			//if (fragment.trim().length() < 15)
			//	return null;

			//StringDistanceMeasurer meas = new StringDistanceMeasurer();
			Double dist = 0.0;
			String result = null, followSent = "";
			for (int i = 0; i < sents.length; i++) {
				String s = sents[i];
				if (s == null || s.length() < 30)
					continue;
				Double distCurr = meas.measureStringDistance(s, fragment);
				if (distCurr > dist && distCurr > 0.4) {
					result = s;
					dist = distCurr;
					try {
						if (i < sents.length - 1 && sents[i + 1].length() > 60) { 
							String f1 = UtteranceFilter.acceptableMinedSentence(sents[i+1]);
							if (f1!=null){
								followSent = f1;
							}
						}

						if (i < sents.length - 2 && sents[i + 2].length() > 60) {
							String f2 = UtteranceFilter.acceptableMinedSentence(sents[i+2]);
							if (f2!=null){
								followSent += " "+f2;
							}
							
						}
						if (i < sents.length - 3 && sents[i + 3].length() > 60) {
							String f3 = UtteranceFilter.acceptableMinedSentence(sents[i+3]);
							if (f3!=null){
								followSent += " "+f3;
							}
						}
						if (i < sents.length - 4 && sents[i + 4].length() > 60) {
							String f4 = UtteranceFilter.acceptableMinedSentence(sents[i+4]);
							if (f4!=null){
								followSent += " "+f4;
							}
						}
						if (i < sents.length - 5 && sents[i + 5].length() > 60) {
							String f5 = UtteranceFilter.acceptableMinedSentence(sents[i+5]);
							if (f5!=null){
								followSent += " "+f5;
							}
						}
						if (i < sents.length - 6 && sents[i + 6].length() > 60) {
							String f6 = UtteranceFilter.acceptableMinedSentence(sents[i+6]);
							if (f6!=null){
								followSent += " "+f6;
							}
						}
						
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			return new String[] { result, followSent };
		}
		
		public static void main(String[] args){
			SnippetToParagraphAndSectionHeaderContent exractor = new SnippetToParagraphAndSectionHeaderContent ();
			Pair<String[], Map<String, String>> p = exractor.extractSentencesAndSectionMapFromPage("https://en.wikipedia.org/wiki/Blind_date");
			System.out.println(p.getFirst());
			System.out.println(p.getSecond());
			
		}
}

