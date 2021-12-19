
package opennlp.tools.chatbot.text_searcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import opennlp.tools.chatbot.ChatIterationResult;
import opennlp.tools.jsmlearning.ProfileReaderWriter;
import opennlp.tools.parse_thicket.matching.Matcher;
import opennlp.tools.parse_thicket.opinion_processor.EntityExtractionResult;
import opennlp.tools.similarity.apps.HitBase;
import opennlp.tools.similarity.apps.utils.CountItemsList;
import opennlp.tools.similarity.apps.utils.ValueSortMap;
import opennlp.tools.stemmer.PStemmer;
import opennlp.tools.textsimilarity.TextProcessor;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.json.JSONObject;

import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.highlight.TokenSources;

public class DocSearcherWithHighlighter extends DocSearcher{
	public static final Log logger = LogFactory.getLog(DocSearcherWithHighlighter.class);

	private static final String INDEX_PATH = resourceDir
			+ DocsAnalyzerIndexerDocPerParagraph.INDEX_PATH;
	
	public DocSearcherWithHighlighter (){
		Directory indexDirectory = null;
		stopListCached = Arrays.asList(stopWords);

		try {
			indexDirectory = FSDirectory.open(FileSystems.getDefault().getPath(INDEX_PATH, ""));
		} catch (IOException e2) {
			logger.error("problem opening index " + e2);
		}
		try {
			indexReader = DirectoryReader.open(indexDirectory);
			indexSearcher = new IndexSearcher(indexReader);
		} catch (IOException e2) {
			logger.error("problem reading index \n" + e2);
		}
		parserText.setDefaultOperator(QueryParser.Operator.AND);
		parserAnsw.setDefaultOperator(QueryParser.Operator.AND);
		parserEdu.setDefaultOperator(QueryParser.Operator.AND);
		parserPhrase.setDefaultOperator(QueryParser.Operator.AND);
		parserTitle.setDefaultOperator(QueryParser.Operator.AND);
		parserAnswOR.setDefaultOperator(QueryParser.Operator.OR);
		parserTextOR.setDefaultOperator(QueryParser.Operator.OR);	
	}
	
	public List<ChatAnswer> runSearchChatIterFormat(String queryOrig) {
		String queryStr = removeStopWords(queryOrig.toLowerCase());

		List<ChatAnswer> results = new ArrayList<ChatAnswer>();
		Query queryText = null, queryTextOR = null, queryAnsw = null, queryAnswOR = null, 
				queryAnswInDoubleQts = null, queryEdu = null, queryPhrase = null, queryTitle = null;
		List<BooleanQuery.Builder> queryPhrasePlusNERs = null, queryPhrasePlusNERsOR = null;
		// queries are instantiated in the order of decreasing strength
		try {
			queryTitle = parserTitle.parse(queryStr);
			queryPhrasePlusNERs = formSpanQueryForNERPhrases(queryOrig, "phrase", parserPhrase);
			queryEdu = parserEdu.parse(queryStr);	
			queryAnswInDoubleQts = parserAnsw.parse("\""+queryStr+"\"");
			queryPhrase = parserPhrase.parse(queryStr); // NER needs upper cases
			queryText = parserText.parse(queryStr);
			queryAnsw = parserAnsw.parse(queryStr);

			queryPhrasePlusNERsOR = formSpanQueryForNERPhrases(queryOrig, "phrase", parserPhraseOR);
			queryAnswOR =  parserAnswOR.parse(queryStr);
			queryTextOR =  parserTextOR.parse(queryStr);

		} catch (ParseException e2) {
			return results;
		}
		TopDocs topDocs = null; 
		Query queryForHighl = null;
		// Finds the top n hits for query.
		try {
			// we first search against the title
			topDocs  = indexSearcher.search(queryTitle, 10);
			if (topDocs!=null && topDocs.totalHits>0){
				queryForHighl = queryTitle;
			}

			// we start with the strongest query and proceed to less strong
			//phraseNER query combines non-NER terms as boolean with phrase query from NER
			if ((topDocs==null || topDocs.totalHits<1) && queryPhrasePlusNERs!=null && !queryPhrasePlusNERs.isEmpty()){
				for(BooleanQuery.Builder phrQ: queryPhrasePlusNERs ){
					topDocs  = indexSearcher.search(phrQ.build() , 10);	
					queryForHighl = phrQ.build();
					if (topDocs!=null && topDocs.totalHits>0) 
						break;
				}
			}
			//TODO search the same query type against edu index
			
			// edu is a collection of phrases logically related, so should be searched next
			if (topDocs==null || topDocs.totalHits<1){
				topDocs  = indexSearcher.search(queryEdu, 10);
				queryForHighl = queryEdu;
			}
			// the whole query as a phrase query, but againts the whole answer field
			if (topDocs==null || topDocs.totalHits<1){
				topDocs  = indexSearcher.search(queryAnswInDoubleQts, 10);
				queryForHighl = queryAnswInDoubleQts;
			}	
			// normal boolean query against phrase field
			if (topDocs.totalHits<1){
				topDocs = indexSearcher.search(queryPhrase, 10);
				queryForHighl = queryPhrase;
			}
			// this allows removal extra words like verb in 'repair electric power steering system'
			// remove 'repair'
			// we give up on matching ALL keywords and want  to match at least NER/phrase part for sure
			if ((topDocs==null || topDocs.totalHits<1) && queryPhrasePlusNERsOR!=null && !queryPhrasePlusNERsOR.isEmpty()){
				for(BooleanQuery.Builder phrQ: queryPhrasePlusNERsOR ){
					topDocs  = indexSearcher.search(phrQ.build() , 10);	
					queryForHighl = phrQ.build();
					if (topDocs!=null && topDocs.totalHits>0) 
						break;
				}
			}
			// boolean against the whole answer
			if (topDocs.totalHits<1){
				topDocs = indexSearcher.search(queryAnsw, 10);
				queryForHighl = queryAnsw;
			}
			// boolean against the whole text
			if (topDocs.totalHits<1){
				topDocs = indexSearcher.search(queryText, 10);
				queryForHighl = queryText;
			}
			// we cannot succeed with AND so proceed with OR
			if (topDocs.totalHits<1){
				topDocs = indexSearcher.search(queryAnswOR, 10);
				queryForHighl = queryAnswOR;
			}
			// the weakest query: OR gainst answer
			if (topDocs.totalHits<1){
				topDocs = indexSearcher.search(queryTextOR, 10);
				queryForHighl = queryTextOR;
			}
		} catch (IOException e1) {
			logger.error("problem searching index \n" + e1);
		}
		TextFragment[] hRes=null;
		try {
			if (queryForHighl!=null)
				hRes = performHighlighting(topDocs, indexReader, indexSearcher, queryForHighl, std, "answer");
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		ScoreDoc[] hits = topDocs .scoreDocs;
		for(int i=0;i<hits.length;++i) {
			int docId = hits[i].doc;
			Document d=null;
			try {
				d = indexSearcher.doc(docId);
			} catch (IOException e) {
				e.printStackTrace();
			}
			ChatAnswer hb = new ChatAnswer();
			hb.setLuceneDocId(docId);
			Map<String, String> explanationMap = new HashMap<String, String>();
			try {
				explanationMap.put("fired_field", queryForHighl.toString());
				explanationMap.put("lucene_doc_id", docId+"");
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			hb.setTitle(d.get("title"));
			hb.setDocTitle(d.get("title"));
			hb.setAbstractText(d.get("answer"));
			//?? hb.setUrl("url");
			if (hRes!=null && i<hRes.length){
				hb.setParagraph(hRes[i].toString());
			}
			hb.setPageContent(d.get("text"));

			String[] categories = d.getValues("categories");
			if (categories!=null)
				hb.setSelectedClarificationPhrase(Arrays.asList(categories).toString());

			String[] categories_paths =d.getValues( "category_paths" );
			if (categories_paths!=null)
				hb.setFirstClarificationPhrase(Arrays.asList(categories_paths).toString());
			hb.setExplanationMap(explanationMap);
			results.add(hb);
		}	
		return results;
	}

	private TextFragment[] performHighlighting(TopDocs topDocs, IndexReader reader, IndexSearcher searcher, Query query, Analyzer analyzer, String field){
		SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter();
		Highlighter highlighter = new Highlighter(htmlFormatter, new QueryScorer(query));
		TextFragment[] frag=null;
		for (int i = 0; i < reader.maxDoc() && i< topDocs.totalHits 
				&& i<topDocs.scoreDocs.length; i++) {
			int id = topDocs.scoreDocs[i].doc;
			Document doc=null;
			try {
				doc = searcher.doc(id);
			} catch (IOException e) {
				e.printStackTrace();
			}
			String text = doc.get(field);
			TokenStream tokenStream;

			try {
				tokenStream = TokenSources.getAnyTokenStream(searcher.getIndexReader(), id, field, analyzer);
				//TokenSources.getTermVectorTokenStreamOrNull(field, tvFields, 1000)
				frag = highlighter.getBestTextFragments(tokenStream, text, false, 4);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InvalidTokenOffsetsException e) {
				e.printStackTrace();
			}
			/*for (int j = 0; j < frag.length; j++) {
	            if ((frag[j] != null) && (frag[j].getScore() > 0)) {
	                System.out.println((frag[j].toString()));
	            }
	        } */
			//Term vector approach
			/*
	        text = doc.get(field);
	        try {
				tokenStream = TokenSources.getAnyTokenStream(searcher.getIndexReader(), topDocs.scoreDocs[i].doc, field, analyzer);

				frag = highlighter.getBestTextFragments(tokenStream, text, false, 10);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InvalidTokenOffsetsException e) {
				e.printStackTrace();
			}
	        for (int j = 0; j < frag.length; j++) {
	            if ((frag[j] != null) && (frag[j].getScore() > 0)) {
	                System.out.println((frag[j].toString()));
	            }
	        } 
	        System.out.println("-------------");
			 */
		}
		return frag;
	}

	public static void main(String[] args) {
		DocSearcherWithHighlighter searcher = new DocSearcherWithHighlighter();

		while(true){
			System.out.print("\nEnter your response or query. type 'q' to exit >");
			//  open up standard input
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String query = null;

			try {
				query = br.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (query.equals("q"))
				System.exit(0);

			List<ChatAnswer> resCIR = 
					searcher.runSearchChatIterFormat(query)	;



			int count = 0;
			for(ChatAnswer sr: resCIR){
				System.out.println(sr.getTitle().toString());
				if (sr.getParagraph()!=null && sr.getParagraph().length()>50)
					System.out.println(sr.getParagraph().toString());
				else 
					System.out.println(sr.getAbstractText().toString());
				
				System.out.println(sr.getExplanationMap().toString());
				

				System.out.println("-------------");
				count++;
				if (count>3)
					break;
			}
		}
	}
}

/*
 * //"Ford Customer Relationship Center");
						//"repair flat tire"
						//"sidewall between tread shoulder and maximum section width"
 * //"issued an international transfer"
 * //		"check transmission fluid"
//		"engine coolant check"	
//		"how to fix a flat tire"	
	//					"How do I check my tire pressure"
		//				"movie player"
		//				"how can I play music"
	//"what music can I play"

 */
