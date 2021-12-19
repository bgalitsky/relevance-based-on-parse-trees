package opennlp.tools.chatbot.text_searcher;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import opennlp.tools.chatbot.TopicExtractorFromSearchResult;
import opennlp.tools.jsmlearning.ProfileReaderWriter;
import opennlp.tools.parse_thicket.ParseThicket;
import opennlp.tools.parse_thicket.matching.Matcher;
import opennlp.tools.parse_thicket.opinion_processor.EntityExtractionResult;
import opennlp.tools.stemmer.PStemmer;
import opennlp.tools.textsimilarity.TextProcessor;
import oracle.cloud.bots.search_results_blender.ExtractedQaPairsIndexer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.tika.Tika;

public class DocsAnalyzerIndexer {
	public static String resourceDir = null;
	private static PStemmer stemmer = new PStemmer();
	public static String INDEX_PATH = "/docIndex";
	protected ArrayList<File> queue = new ArrayList<File>();
	protected Tika tika = new Tika();
	protected Matcher matcher = Matcher.getInstance();
	protected TopicExtractorFromSearchResult phraseExtractorForIndexing = new   TopicExtractorFromSearchResult();
	protected ElementaryDiscourseUnitsBuilderForIndexing eduBuilder = new ElementaryDiscourseUnitsBuilderForIndexing();

	IndexWriter indexWriter = null;

	private String absolutePathTrainingSet=null;
	public void setDocDir(String dir){
		absolutePathTrainingSet = dir;
	}

	public DocsAnalyzerIndexer() {

		try {
			initIndexWriter();
		} catch (Exception e) {
			e.printStackTrace();
		}
	} 


	protected List<String> getPhrasesForIndexing(String para){
		EntityExtractionResult res = phraseExtractorForIndexing.extractEntities(para);
		List<String> results = res.getExtractedNerPhrasesStr();
		results.addAll(res.getExtractedNONSentimentPhrasesStr());
		return results;
	}

	protected List<String> getEDUsForIndexing(String para){
		List<String> results = new ArrayList<String>();
		try {
			ParseThicketWithDiscourseUnitsForIndexing parseThicket = eduBuilder.buildParseThicket(para);
			results = parseThicket.eDUs;
		} catch (Exception e) {
			System.out.println("failed to parse text");
		}
		return results;
	}
	public void indexTrainingSet() {

		try {
			indexFileOrDirectory(
					absolutePathTrainingSet);        		
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			indexWriter.commit();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void addFiles(File file) {
		if (!file.exists()) {
			System.out.println(file + " does not exist.");
		}
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				if (f.getName().startsWith("."))
					continue;
				addFiles(f);
				System.out.println(f.getName());
			}
		} else {
			queue.add(file);
		}
	}

	// index last folder name, before filename itself

	public void indexFileOrDirectory(String fileName) throws IOException {
		addFiles(new File(fileName));

		List<File> files = new ArrayList<File>(queue);
		for (File f : files) {
			// TODO add other file extensions
			if (f.getName().endsWith(".json")) {

				try {
					Document doc = new Document();
					String content = FileUtils.readFileToString(f);

					// index in both original and stemmed format
					try {
						doc.add(new TextField("text", tika.parse(f)));
						String contentStemmed = formStemmedStrQuery(content.toLowerCase());
						doc.add(new TextField("text", contentStemmed, Field.Store.YES));
					} catch (Exception e1) {
						e1.printStackTrace();
					}

					// TODO make cleaning/splitting code universal
					// now it is specific to watson auto dataset
					String contentJSON = content.replace("u00a0","");
					content = StringUtils.substringBetween(contentJSON, "{\"body\": \"", "\"");
					String title = StringUtils.substringBetween(contentJSON,"\"title\":", "\"}");
					doc.add(new TextField("title", title.toLowerCase(), Field.Store.YES));
					doc.add(new TextField("title", formStemmedStrQuery(title.toLowerCase()), Field.Store.YES));

					String[] paragraphs = content.split("<br><br>");
					for(String p: paragraphs ){
						try {
							doc.add(new TextField("answer", p.toLowerCase(), Field.Store.YES));
							doc.add(new TextField("answer", formStemmedStrQuery(p), Field.Store.YES));
							List<String> phrases = getPhrasesForIndexing(p);
							for(String phr: phrases){
								doc.add(new TextField("phrase", phr, Field.Store.YES));
								doc.add(new TextField("phrase", formStemmedStrQuery(phr), Field.Store.YES));
							}
							try {
								List<String> edus = getEDUsForIndexing(p);
								if (edus==null)
									continue;
								for(String ed: edus)
									if (ed!=null){
										doc.add(new TextField("edu", ed, Field.Store.YES));
										doc.add(new TextField("edu", formStemmedStrQuery(ed), Field.Store.YES));
									}

							} catch (Exception e) {
								e.printStackTrace();
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}


					try {

						indexWriter.addDocument(doc);

					} catch (Exception e) {
						e.printStackTrace();
						System.out.println("Could not add: " + f);
					}
				} catch (Exception ee) {
					ee.printStackTrace();
				}
			} 

			queue.clear();
		}
	}

	public static String getIndexDir() {
		try {
			return new File(".").getCanonicalPath() + INDEX_PATH;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	private void initIndexWriter() throws Exception {

		Directory indexDir = null;

		try {
			Path path = FileSystems.getDefault().getPath(resourceDir + INDEX_PATH, "");
			indexDir = FSDirectory.open(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
		IndexWriterConfig luceneConfig = new IndexWriterConfig(new StopAnalyzer());
		luceneConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

		indexWriter = new IndexWriter(indexDir, luceneConfig);
	}

	private void initIndexWriter(boolean bCreate) throws Exception {

		Directory indexDir = null;

		try {
			Path path = FileSystems.getDefault().getPath(resourceDir + INDEX_PATH, "");
			indexDir = FSDirectory.open(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
		IndexWriterConfig luceneConfig = new IndexWriterConfig(new StopAnalyzer());
		if (bCreate)
			luceneConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
		else 
			luceneConfig.setOpenMode(IndexWriterConfig.OpenMode.APPEND);

		indexWriter = new IndexWriter(indexDir, luceneConfig);
	}

	public void close() {
		try {
			indexWriter.commit();
			indexWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static String formStemmedStrQuery(String queryOrig){
		List<String> words = TextProcessor.fastTokenize(queryOrig, false);
		String stemmedQuery = "";
		for(String s: words){
			stemmedQuery += stemmer.stem(s)+" ";
		}
		stemmedQuery += stemmedQuery.trim();
		return stemmedQuery;
	}


	public static void main(String[] args) {
		DocsAnalyzerIndexer indexer =  new DocsAnalyzerIndexer();
		indexer.setDocDir("/Users/bgalitsk/Downloads/manualdocs");

		try {
			indexer.indexTrainingSet();
		} catch (Exception e) {
			e.printStackTrace();
		}
		indexer.close();
	}

}
