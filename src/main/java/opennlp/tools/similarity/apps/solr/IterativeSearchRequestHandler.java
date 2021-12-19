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
package opennlp.tools.similarity.apps.solr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opennlp.tools.similarity.apps.HitBaseComparable;
import opennlp.tools.similarity.apps.utils.Pair;
import opennlp.tools.textsimilarity.ParseTreeChunkListScorer;
import opennlp.tools.textsimilarity.SentencePairMatchResult;
import opennlp.tools.textsimilarity.chunker2matcher.ParserChunker2MatcherProcessor;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.ShardParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.handler.component.SearchHandler;
import org.apache.solr.handler.component.ShardHandler;
import org.apache.solr.handler.component.ShardRequest;
import org.apache.solr.handler.component.ShardResponse;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.ResultContext;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.apache.solr.search.DocSlice;
import org.apache.solr.search.QParser;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RTimer;
import org.apache.solr.util.SolrPluginUtils;

public class IterativeSearchRequestHandler extends SearchHandler {

	private ParseTreeChunkListScorer parseTreeChunkListScorer = new ParseTreeChunkListScorer();

	public SolrQueryResponse runSearchIteration(SolrQueryRequest req, SolrQueryResponse rsp, String fieldToTry){
		try {
			req = substituteField(req, fieldToTry);
			super.handleRequestBody(req, rsp);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return rsp;
	}

	public static SolrQueryRequest substituteField(SolrQueryRequest req, String newFieldName){
		SolrParams params = req.getParams();
		String query = params.get("q");
		String currField = StringUtils.substringBetween(" "+query, " ", ":");
		if ( currField !=null && newFieldName!=null)
			query = query.replace(currField, newFieldName);
		NamedList values = params.toNamedList();
		values.remove("q");
		values.add("q", query);
		params = SolrParams.toSolrParams(values);
		req.setParams(params);
		return req;

	}

	public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp){
         
		SolrQueryResponse rsp1 = new SolrQueryResponse(), rsp2=new SolrQueryResponse(), rsp3=new SolrQueryResponse();
		NamedList list = rsp.getValues();
		rsp1.setAllValues(rsp.getValues().clone());
		rsp2.setAllValues(rsp.getValues().clone());
		rsp3.setAllValues(rsp.getValues().clone());
		
		
		rsp1 = runSearchIteration(req, rsp1, "cat");
		NamedList values = rsp1.getValues();
		ResultContext c = (ResultContext) values.get("response");
		if (c!=null){			
			DocList dList = c.getDocList();
			if (dList.size()<1){
				rsp2 = runSearchIteration(req, rsp2, "name");
			}
			else {
				rsp.setAllValues(rsp1.getValues());
				return;
			}
		}

		values = rsp2.getValues();
		c = (ResultContext) values.get("response");
		if (c!=null){
			DocList dList = c.getDocList();
			if (dList.size()<1){
				rsp3 = runSearchIteration(req, rsp3, "content");
			}
			else {
				rsp.setAllValues(rsp2.getValues());
				return;
			}
		}
		
		rsp.setAllValues(rsp3.getValues());

	}

	



public DocList filterResultsBySyntMatchReduceDocSet(DocList docList,
		SolrQueryRequest req,  SolrParams params) {		
	//if (!docList.hasScores()) 
	//	return docList;

	int len = docList.size();
	if (len < 1) // do nothing
		return docList;
	ParserChunker2MatcherProcessor pos = ParserChunker2MatcherProcessor .getInstance();

	DocIterator iter = docList.iterator();
	float[] syntMatchScoreArr = new float[len];
	String requestExpression = req.getParamString();
	String[] exprParts = requestExpression.split("&");
	for(String part: exprParts){
		if (part.startsWith("q="))
			requestExpression = part;			
	}
	String fieldNameQuery = StringUtils.substringBetween(requestExpression, "=", ":");
	// extract phrase query (in double-quotes)
	String[] queryParts = requestExpression.split("\"");
	if  (queryParts.length>=2 && queryParts[1].length()>5)
		requestExpression = queryParts[1].replace('+', ' ');	
	else if (requestExpression.indexOf(":") > -1 ) {// still field-based expression
		requestExpression = requestExpression.replaceAll(fieldNameQuery+":", "").replace('+',' ').replaceAll("  ", " ").replace("q=", "");
	}

	if (fieldNameQuery ==null)
		return docList;
	if (requestExpression==null || requestExpression.length()<5  || requestExpression.split(" ").length<3)
		return docList;
	int[] docIDsHits = new int[len]; 

	IndexReader indexReader = req.getSearcher().getIndexReader();
	List<Integer> bestMatchesDocIds = new ArrayList<Integer>(); List<Float> bestMatchesScore = new ArrayList<Float>();
	List<Pair<Integer, Float>> docIdsScores = new ArrayList<Pair<Integer, Float>> ();
	try {
		for (int i=0; i<docList.size(); ++i) {
			int docId = iter.nextDoc();
			docIDsHits[i] = docId;
			Document doc = indexReader.document(docId);

			// get text for event
			String answerText = doc.get(fieldNameQuery);
			if (answerText==null)
				continue;
			SentencePairMatchResult matchResult = pos.assessRelevance( requestExpression , answerText);
			float syntMatchScore =  new Double(parseTreeChunkListScorer.getParseTreeChunkListScore(matchResult.getMatchResult())).floatValue();
			bestMatchesDocIds.add(docId);
			bestMatchesScore.add(syntMatchScore);
			syntMatchScoreArr[i] = (float)syntMatchScore; //*iter.score();
			System.out.println(" Matched query = '"+requestExpression + "' with answer = '"+answerText +"' | doc_id = '"+docId);
			System.out.println(" Match result = '"+matchResult.getMatchResult() + "' with score = '"+syntMatchScore +"';" );
			docIdsScores.add(new Pair(docId, syntMatchScore));
		}

	} catch (CorruptIndexException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
		//log.severe("Corrupt index"+e1);
	} catch (IOException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
		//log.severe("File read IO / index"+e1);
	}


	Collections.sort(docIdsScores, new PairComparable());
	for(int i = 0; i<docIdsScores.size(); i++){
		bestMatchesDocIds.set(i, docIdsScores.get(i).getFirst());
		bestMatchesScore.set(i, docIdsScores.get(i).getSecond());
	}
	System.out.println(bestMatchesScore);
	float maxScore = docList.maxScore(); // do not change
	int limit = docIdsScores.size();
	int start = 0; 
	DocSlice ds = null;

	ds = new DocSlice(start, limit, 
			ArrayUtils.toPrimitive(bestMatchesDocIds.toArray(new Integer[0])), 
			ArrayUtils.toPrimitive(bestMatchesScore.toArray(new Float[0])), 
			bestMatchesDocIds.size(), maxScore);



	return ds;
}








private void append(SolrDocumentList results, ScoreDoc[] more, 
		Set<Integer> alreadyFound, Map<String,SchemaField> fields,
		Map<String,Object> extraFields, float scoreCutoff, 
		IndexReader reader, boolean includeScore) throws IOException {
	for (ScoreDoc hit : more) {
		if (alreadyFound.contains(hit.doc)) {
			continue;
		}
		Document doc = reader.document(hit.doc);
		SolrDocument sdoc = new SolrDocument();
		for (String fieldname : fields.keySet()) {
			SchemaField sf = fields.get(fieldname);
			if (sf.stored()) {
				sdoc.addField(fieldname, doc.get(fieldname));
			}
		}
		for (String extraField : extraFields.keySet()) {
			sdoc.addField(extraField, extraFields.get(extraField));
		}
		if (includeScore) {
			sdoc.addField("score", hit.score);
		}
		results.add(sdoc);
		alreadyFound.add(hit.doc);
	}
}
public class PairComparable implements Comparator<Pair> {
	// @Override
	public int compare(Pair o1, Pair o2) {
		int b = -2;
		if ( o1.getSecond() instanceof Float && o2.getSecond() instanceof Float){

			b =  (((Float)o1.getSecond()> (Float)o2.getSecond()) ? -1
					: (((Float)o1.getSecond() == (Float)o2.getSecond()) ? 0 : 1));
		}
		return b;
	}
}

}
