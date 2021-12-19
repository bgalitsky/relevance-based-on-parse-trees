
package opennlp.tools.chatbot.text_searcher;

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

public class DocSearcher {

	public static String resourceDir = DocsAnalyzerIndexer.resourceDir;

	public static final Log logger = LogFactory.getLog(DocSearcher.class);

	protected static final String[] stopWords = new String[]{"I", "i", "have", "has", "had", "how",
			"what", "when", "which", "where", "can", "could", "a", "the", "this", "that", "do", 
			"work", "working", // for auto-domain
			"feature", "property", "parameters", "characteristics"
	};
	protected  List<String> stopListCached = null;

	PhraseExtractorForSearcher extractor = new PhraseExtractorForSearcher(Matcher.getInstance());

	private static final String INDEX_PATH = resourceDir
			+ DocsAnalyzerIndexer.INDEX_PATH;

	protected static IndexReader indexReader = null;
	protected static IndexSearcher indexSearcher = null;


	protected Analyzer std = new StopAnalyzer();
	protected QueryParser parserText = new QueryParser("text", std), parserTextOR = new QueryParser("text", std);

	protected QueryParser parserAnsw = new QueryParser("answer", std), parserAnswOR = new QueryParser("answer", std);

	protected QueryParser parserEdu = new QueryParser("edu", std);

	protected QueryParser parserPhrase = new QueryParser("phrase", std), parserPhraseOR = new QueryParser("phrase", std);

	protected QueryParser parserTitle = new QueryParser("title", std);


	public DocSearcher (){
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
		parserPhraseOR.setDefaultOperator(QueryParser.Operator.OR);
		parserTitle.setDefaultOperator(QueryParser.Operator.AND);
		parserAnswOR.setDefaultOperator(QueryParser.Operator.OR);
		parserTextOR.setDefaultOperator(QueryParser.Operator.OR);	
	}

	protected List<BooleanQuery.Builder> formSpanQueryForNERPhrases(String query, String field, QueryParser queryParser){
		List<BooleanQuery.Builder> resultantsQueries = new ArrayList<BooleanQuery.Builder>();

		EntityExtractionResult res = extractor.extractEntities(query);
		List<String> results = res.getExtractedNerPhrasesStr();

		results.addAll(res.getExtractedNONSentimentPhrasesStr());
		if (results==null || results.isEmpty() || results.get(0)== null ||results.get(0).split(" ").length<2 )
			return null;
		
		for(String phrase: results){
			BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
			try {
				booleanQuery.add(queryParser.parse(query), BooleanClause.Occur.MUST);
			} catch (ParseException e) {
				e.printStackTrace();
			}

			try {
				List<String> tokens = TextProcessor.fastTokenize(phrase.toLowerCase(), false);
				SpanNearQuery spanQ = SpanQueryBuilder.formSpanQueryFromTokens(tokens, field);
				if (spanQ==null)
					continue;
				booleanQuery.add(spanQ, BooleanClause.Occur.MUST);
			} catch (Exception e) {
				e.printStackTrace();
			}
			resultantsQueries.add(booleanQuery);
		}
		return resultantsQueries;
	}

	public List<HitBase> runSearch(String queryStrOrig) {

		String queryStr = removeStopWords(queryStrOrig);
		List<HitBase> results = new ArrayList<HitBase>();
		Query queryText = null, queryAnsw = null, queryEdu = null, queryPhrase = null;
		List<BooleanQuery.Builder> queryPhrasePlusNERs = null, queryPhrasePlusNERsOR = null;
		try {
			queryText = parserText.parse(queryStr);
			queryAnsw = parserAnsw.parse(queryStr);
			queryEdu = parserEdu.parse(queryStr);
			queryPhrase = parserPhrase.parse(queryStr);
			queryPhrasePlusNERs = formSpanQueryForNERPhrases(queryStr, "phrase", parserPhrase);
			queryPhrasePlusNERsOR = formSpanQueryForNERPhrases(queryStr, "phrase", parserPhraseOR);

		} catch (ParseException e2) {
			return results;
		}
		TopDocs topDocs = null; 
		// Finds the top n hits for query.
		try {
			// we start with the strongest query and proceed to less strong
			if (queryPhrasePlusNERs!=null && !queryPhrasePlusNERs.isEmpty())
				topDocs  = indexSearcher.search(queryPhrasePlusNERs.get(0).build() , 10);

			if (topDocs.totalHits<1)
				topDocs  = indexSearcher.search(queryEdu, 10);
			if (topDocs.totalHits<1)
				topDocs = indexSearcher.search(queryPhrase, 10);
			if (topDocs.totalHits<1)
				topDocs = indexSearcher.search(queryAnsw, 10);
			if (topDocs.totalHits<1)
				topDocs = indexSearcher.search(queryText, 10);
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
				e.printStackTrace();
			}
			HitBase hb = new HitBase();
			//hb.setTitle(d.get("question"));
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

	public List<ChatAnswer> runSearchChatIterFormat(String queryOrig) {
		String queryStr = removeStopWords(queryOrig.toLowerCase());

		List<ChatAnswer> results = new ArrayList<ChatAnswer>();
		Query queryText = null, queryAnsw = null, queryEdu = null, queryPhrase = null;
		List<BooleanQuery.Builder> queryPhrasePlusNERs = null;
		try {
			queryText = parserText.parse(queryStr);
			queryAnsw = parserAnsw.parse(queryStr);
			queryEdu = parserEdu.parse(queryStr);
			queryPhrase = parserPhrase.parse(queryStr); // NER needs upper cases
			queryPhrasePlusNERs = formSpanQueryForNERPhrases(queryOrig, "phrase", parserPhrase);

		} catch (ParseException e2) {
			return results;
		}
		TopDocs topDocs = null; 
		// Finds the top n hits for query.
		try {
			// we start with the strongest query and proceed to less strong
			if (queryPhrasePlusNERs!=null && !queryPhrasePlusNERs.isEmpty())
				topDocs  = indexSearcher.search(queryPhrasePlusNERs.get(0).build() , 10);
			if (topDocs==null || topDocs.totalHits<1)
				topDocs  = indexSearcher.search(queryEdu, 10);
			if (topDocs.totalHits<1)
				topDocs = indexSearcher.search(queryPhrase, 10);
			if (topDocs.totalHits<1)
				topDocs = indexSearcher.search(queryAnsw, 10);
			if (topDocs.totalHits<1)
				topDocs = indexSearcher.search(queryText, 10);
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
				e.printStackTrace();
			}
			ChatAnswer hb = new ChatAnswer();
			hb.setTitle(d.get("edu"));
			hb.setAbstractText(d.get("answer"));
			//?? hb.setUrl("url");
			hb.setParagraph(d.get("text"));

			String[] categories = d.getValues("categories");
			if (categories!=null)
				hb.setSelectedClarificationPhrase(Arrays.asList(categories).toString());

			String[] categories_paths =d.getValues( "category_paths" );
			if (categories_paths!=null)
				hb.setFirstClarificationPhrase(Arrays.asList(categories_paths).toString());

			results.add(hb);
		}
		return results;
	}

	public static void main(String[] args) {
		DocSearcher searcher = new DocSearcher();

		List<ChatAnswer> resCIR = 
				searcher.runSearchChatIterFormat(//"Ford Customer Relationship Center");
						"repair flat tire");
		//"issued an international transfer"
		System.out.println(resCIR);

	}


}
