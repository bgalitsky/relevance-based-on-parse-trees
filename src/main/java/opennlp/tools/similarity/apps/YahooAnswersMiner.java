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

package opennlp.tools.similarity.apps;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import opennlp.tools.jsmlearning.ProfileReaderWriter;
import opennlp.tools.parse_thicket.Triple;


public class YahooAnswersMiner extends BingQueryRunner{

	private static final Logger LOG = Logger
			.getLogger("opennlp.tools.similarity.apps.YahooAnswersMiner");
	private int page = 0;
	private static final int hitsPerPage = 50;

	


	public List<HitBase> runSearch(String query, int totalPages) {
		int count=0;
		List<HitBase> results = new ArrayList<HitBase>();
		while(totalPages>page*hitsPerPage){
			List<HitBase> res = runSearch(query);
			results.addAll(res);
			if (count>10)
				break;
			count++;
		}

		return results;
	}


	public static void main(String[] args) {
		YahooAnswersMiner self = new YahooAnswersMiner();
		RelatedSentenceFinder extractor = new RelatedSentenceFinder();
		String topic = "obamacare";

		List<HitBase> resp = self
				.runSearch(topic, 150);
		System.out.print(resp.get(0));
		List<String[]> data = new ArrayList<String[]>();


		for(HitBase item: resp){	      
			Triple<List<String>, String, String[]> fragmentExtractionResults = 
					extractor.formCandidateFragmentsForPage(item, topic, null);

			List<String> allFragms = (List<String>)fragmentExtractionResults.getFirst();
			String downloadedPage = (String)fragmentExtractionResults.getSecond();
			String[] sents = (String[])fragmentExtractionResults.getThird();

			for (String fragment : allFragms) {
				String[] candidateSentences = extractor.formCandidateSentences(fragment, fragmentExtractionResults);
				System.out.println(candidateSentences);
				data.add(candidateSentences);
			}
			
		}

		ProfileReaderWriter.writeReport(data, "multi_sentence_queries.csv");

	}

}
