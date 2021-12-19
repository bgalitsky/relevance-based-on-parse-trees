
package opennlp.tools.chatbot.qna_pairs_adapter;

import java.io.File;
import java.io.IOException;
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
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.json.JSONObject;

public class QnAPairsIndexSearcher {

	public static String resourceDir = ExtractedQnAPairsIndexer.resourceDir;

	public static final Log logger = LogFactory.getLog(QnAPairsIndexSearcher.class);
	
	private static final String[] stopWords = new String[]{"I", "i", "have", "has", "had", "how",
			"what", "when", "which", "where", "can", "could", "a", "the", "this", "that", "do"};
	protected  List<String> stopListCached = null;

	private static final String INDEX_PATH = resourceDir
			+ ExtractedQnAPairsIndexer.INDEX_PATH;

	protected static IndexReader indexReader = null;
	protected static IndexSearcher indexSearcher = null;


	private Analyzer std = new StopAnalyzer();
	private QueryParser parser = new QueryParser("question", std), 
			parserAnsw = new QueryParser("answer", std);


	public QnAPairsIndexSearcher (){
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
		parser.setDefaultOperator(QueryParser.Operator.AND);
		parserAnsw.setDefaultOperator(QueryParser.Operator.AND);
	}

	public List<HitBase> runSearch(String queryStrOrig) {
		
		String queryStr = removeStopWords(queryStrOrig);
		List<HitBase> results = new ArrayList<HitBase>();
		Query query = null, queryAnsw = null;
		try {
			query = parser.parse(queryStr);
			queryAnsw = parserAnsw.parse(queryStr);

		} catch (ParseException e2) {
			return results;
		}
		TopDocs topDocs = null; 
		// Finds the top n hits for query.
		try {
			topDocs  = indexSearcher.search(query, 2);
			// search answers only if searching questions gives 0 results
			if (topDocs.totalHits<1)
				topDocs = indexSearcher.search(queryAnsw, 2);
		} catch (IOException e1) {
			logger.error("problem searching index \n" + e1);
		}
		ScoreDoc[] hits = topDocs .scoreDocs;
		for(int i=0;i<hits.length;++i) {
			int docId = hits[i].doc;
			Document d=null;
			try {
				d = indexSearcher.doc(docId);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			HitBase hb = new HitBase();
			hb.setTitle(d.get("question"));
			hb.setAbstractText(d.get("answer"));
			results.add(hb);
		}
		return results;
	}

	protected String removeStopWords(String queryStrOrig) {
		
		List<String> tokens = TextProcessor.fastTokenize(queryStrOrig, false);
		StringBuffer newQuery = new StringBuffer();
		for(String s: tokens ){
			if (!stopListCached.contains(s)){
				newQuery.append(s+ " ");
			}
		}
		return newQuery.toString().trim();
	}

	public void close() {
		try {
			indexReader.close();
		} catch (IOException e) {
			logger.error("Problem closing index \n" + e);
		}
	}	

	public List<ChatIterationResult> runSearchChatIterFormat(String queryOrig) {
		String queryStr = removeStopWords(queryOrig.toLowerCase());
		
		
		List<ChatIterationResult> results = new ArrayList<ChatIterationResult>();
		Query query = null, queryAnsw = null;
		try {
			query = parser.parse(queryStr);
			queryAnsw = parserAnsw.parse(queryStr);
		} catch (ParseException e2) {
			return results;
		}
		TopDocs topDocs = null; 
		// Finds the top n hits for query.
		try {
			topDocs  = indexSearcher.search(query, 20);
		} catch (IOException e1) {
			logger.error("problem searching index \n" + e1);
		}

		ScoreDoc[] hits = topDocs .scoreDocs;
		if (hits.length<1){
			 String stemmedQuery = ExtractedQnAPairsIndexer.formStemmedStrQuery(queryOrig);
			try {
				query = parser.parse(stemmedQuery);

			} catch (ParseException e2) {
				return results;
			}
			topDocs = null; 
			// Finds the top n hits for query.
			try {
				topDocs  = indexSearcher.search(query, 20);
			} catch (IOException e1) {
				logger.error("problem searching index \n" + e1);
			}

			hits = topDocs .scoreDocs;
		}
		// if tokenized query did not match try answer
		if (topDocs.totalHits<1)
			try {
				topDocs = indexSearcher.search(queryAnsw, 20);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		// if answer did not match try OR query
		if (topDocs.totalHits<1){
			parser.setDefaultOperator(QueryParser.Operator.OR);
			try {
				query = parser.parse(queryOrig);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			try {
				topDocs = indexSearcher.search(query, 20);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		for(int i=0;i<hits.length;++i) {
			int docId = hits[i].doc;
			Document d=null;
			try {
				d = indexSearcher.doc(docId);
			} catch (IOException e) {
				e.printStackTrace();
			}
			ChatIterationResult hb = new ChatIterationResult();
			hb.setTitle(d.get("question"));
			hb.setAbstractText(d.get("answer"));
			hb.setUrl("url");
			hb.setParagraph(d.get("answer"));

			String[] categories = d.getValues("categories");
			hb.setSelectedClarificationPhrase(Arrays.asList(categories).toString());
			
			String[] categories_paths =d.getValues( "category_paths" );
			hb.setFirstClarificationPhrase(Arrays.asList(categories_paths).toString());

			results.add(hb);
		}
		return results;
	}

	public static void main(String[] args) {
		QnAPairsIndexSearcher searcher = new QnAPairsIndexSearcher();
		//List<HitBase> results  = searcher.runSearch(
		//		"issued an international transfer"
		//		);		
		//System.out.println(results);

		List<ChatIterationResult> resCIR = 
				searcher.runSearchChatIterFormat("automation");
		//"issued an international transfer"
		System.out.println(resCIR);

	}


}
