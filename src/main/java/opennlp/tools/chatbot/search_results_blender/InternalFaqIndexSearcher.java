
package opennlp.tools.chatbot.search_results_blender;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.ArrayList;
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

public class InternalFaqIndexSearcher {

	public static String resourceDir = ExtractedQaPairsIndexer.resourceDir;

	public static final Log logger = LogFactory.getLog(InternalFaqIndexSearcher.class);

	private static final String INDEX_PATH = resourceDir
			+ ExtractedQaPairsIndexer.INDEX_PATH;

	protected static IndexReader indexReader = null;
	protected static IndexSearcher indexSearcher = null;


	private Analyzer std = new StopAnalyzer();
	private QueryParser parser = new QueryParser("question", std), 
			parserAnsw = new QueryParser("answer", std);


	public InternalFaqIndexSearcher (){
		Directory indexDirectory = null;

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

	public List<HitBase> runSearch(String queryStr) {
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
			// search answers only if searching questions gives 0 rezults
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

	public void close() {
		try {
			indexReader.close();
		} catch (IOException e) {
			logger.error("Problem closing index \n" + e);
		}
	}	


	public static void main(String[] args) {
		InternalFaqIndexSearcher searcher = new InternalFaqIndexSearcher();
		List<HitBase> results  = searcher.runSearch(//"what securities are eligible for margin");
		//		"securities eligible for margin");
				//"where is the mutual fund trading screen");
		//"place orders");
		//		"proceeds from sales");
		//		"replace an order");
		//		"stock price bands");
		//"circuit breakers");
		//"market-wide circuit breakers");
		// "cash account violations");
		 "day trade call");		
		System.out.println(results);

	}

	public List<ChatIterationResult> runSearchChatIterFormat(String queryOrig) {
		List<ChatIterationResult> results = new ArrayList<ChatIterationResult>();
		Query query = null;
		try {
			query = parser.parse(queryOrig);

		} catch (ParseException e2) {
			return results;
		}
		TopDocs topDocs = null; 
		// Finds the top n hits for query.
		try {
			topDocs  = indexSearcher.search(query, 2);
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
			ChatIterationResult hb = new ChatIterationResult();
			hb.setTitle(d.get("question"));
			hb.setAbstractText(d.get("answer"));
			hb.setUrl("url");
			hb.setParagraph(d.get("answer"));
			//hb.setEeResult(new EntityExtractionResult());
			results.add(hb);
		}
		return results;
    }

}
