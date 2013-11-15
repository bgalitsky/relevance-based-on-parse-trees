package opennlp.tools.similarity.apps.solr;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.util.logging.Logger;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import opennlp.tools.similarity.apps.HitBase;
import opennlp.tools.similarity.apps.RelatedSentenceFinder;
import opennlp.tools.similarity.apps.utils.Pair;
import opennlp.tools.textsimilarity.ParseTreeChunk;
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
import org.apache.lucene.search.CachingWrapperFilter;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.ShardParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.SearchHandler;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;



public class ContentGeneratorRequestHandler extends SearchHandler {
	private static Logger LOG = Logger
			.getLogger("com.become.search.requestHandlers.SearchResultsReRankerRequestHandler");
	private final static int MAX_SEARCH_RESULTS = 100;
	private ParseTreeChunkListScorer parseTreeChunkListScorer = new ParseTreeChunkListScorer();
	private ParserChunker2MatcherProcessor sm = null;
	private int MAX_QUERY_LENGTH_NOT_TO_RERANK=3;
	//private static String resourceDir = "/data1/solr/example/src/test/resources";
	//private RelatedSentenceFinder contgen = new RelatedSentenceFinder();
	//private opennlp.tools.apps.utils.email.EmailSender s = null; //r();

	public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp){
		
		String query = req.getParams().get("q");
		LOG.info(query);
				
		String[] runCommand = new String[10];
		runCommand[0] = "java";
		runCommand[1] = "-Xmx1g";
		runCommand[2] = "-jar";
		runCommand[3] = "pt.jar";
		runCommand[4] = "\""+query+"\"";
		runCommand[5] = req.getParams().get("email");
		runCommand[6] = req.getParams().get("resourceDir");
		runCommand[7] = req.getParams().get("stepsNum");
		runCommand[8] = req.getParams().get("searchResultsNum");
		runCommand[9] = req.getParams().get("relevanceThreshold");
		
		
		Runtime r = Runtime.getRuntime();
		Process mStartProcess = null;
		String workDir = req.getParams().get("workDir"); 
		if (workDir == null)
			System.err.println("workDir = null");
				
		try {
			mStartProcess = r.exec(runCommand, null, new File(workDir));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		StreamLogger outputGobbler = new StreamLogger(mStartProcess.getInputStream());
		outputGobbler.start();
	
		NamedList<Object> values = rsp.getValues();
		values.remove("response");
		values.add("response", "We received your request to write an essay on '"+query+"'");
		rsp.setAllValues(values);
		
	}

	
	class StreamLogger extends Thread{

		private InputStream mInputStream;

		public StreamLogger(InputStream is) {
			this.mInputStream = is;
		}

		public void run() {
			try {
				InputStreamReader isr = new InputStreamReader(mInputStream);
				BufferedReader br = new BufferedReader(isr);
				String line = null;
				while ((line = br.readLine()) != null) {
					System.out.println(line);
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

}

/*
http://173.255.254.250:8983/solr/contentgen/?q=human+body+anatomy&email=bgalitsky@hotmail.com&resourceDir=/home/solr/solr-4.4.0/example/src/test/resources&workDir=/home/solr/solr-4.4.0/example/solr-webapp/webapp/WEB-INF/lib&stepsNum=20&searchResultsNum=10&relevanceThreshold=1.5

*/