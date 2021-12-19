
package opennlp.tools.chatbot.text_searcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import opennlp.tools.chatbot.ChatIterationResult;
import opennlp.tools.jsmlearning.ProfileReaderWriter;
import opennlp.tools.parse_thicket.Pair;
import opennlp.tools.parse_thicket.matching.Matcher;
import opennlp.tools.parse_thicket.opinion_processor.EntityExtractionResult;
import opennlp.tools.similarity.apps.HitBase;
import opennlp.tools.similarity.apps.taxo_builder.TaxoQuerySnapshotMatcher;
import opennlp.tools.similarity.apps.utils.CountItemsList;
import opennlp.tools.similarity.apps.utils.ValueSortMap;
import opennlp.tools.stemmer.PStemmer;
import opennlp.tools.textsimilarity.ParseTreeChunk;
import opennlp.tools.textsimilarity.ParseTreeChunkListScorer;
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

public class DocSearcherWithSyntacticAndTaxoRelevance extends DocSearcherWithHighlighter{
	public static final Log logger = LogFactory.getLog(DocSearcherWithSyntacticAndTaxoRelevance.class);
	protected TaxoQuerySnapshotMatcher matcherTaxo = new TaxoQuerySnapshotMatcher(
	        "src/test/resources/taxonomies/irs_domTaxo.dat");
	protected ParseTreeChunkListScorer parseTreeChunkListScorer = new ParseTreeChunkListScorer();
	protected Matcher matcher = Matcher.getInstance();
	    


	public List<ChatAnswer> runSearchChatIterFormat(String queryOrig){
		List<ChatAnswer> origResults = super.runSearchChatIterFormat(queryOrig), orderedResults = new 
				ArrayList<ChatAnswer>() ;
     
        for (ChatAnswer ans: origResults ) {
            List<List<ParseTreeChunk>> res = matcher.assessRelevanceCache(queryOrig, ans.getAbstractText());
            double syntacticScore = parseTreeChunkListScorer.getParseTreeChunkListScoreAggregPhraseType(res);
            double taxoScore = matcherTaxo.getTaxoScore(queryOrig, ans.getAbstractText());
            double generWithQueryScore = syntacticScore+taxoScore;
            ans.setGenerWithQueryScore(generWithQueryScore);
            orderedResults.add(ans);
        }
        Collections.sort(orderedResults, new ChatIterationResultComparable());

		return orderedResults;
  }
	

	public static void main(String[] args) {
		DocSearcherWithSyntacticAndTaxoRelevance searcher = new DocSearcherWithSyntacticAndTaxoRelevance();

		List<ChatAnswer> resCIR = 
				searcher.runSearchChatIterFormat(//"Ford Customer Relationship Center");
						//"repair flat tire"
						"sidewall between tread shoulder and maximum section width"
						);
		//"issued an international transfer"
		System.out.println(resCIR);
		for(ChatIterationResult sr: resCIR){
			System.out.println(sr.getParagraph().toString());
		}
	}
}

