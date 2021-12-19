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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.tika.Tika;

public class DocsAnalyzerIndexerWithHighlighter extends DocsAnalyzerIndexer {
	// TODO
	// does not work: cannot create docs with highlighter fields. 
	//But turned out it is not necesary for highlighter feature to work

	public void indexFileOrDirectory(String fileName) throws IOException {
		addFiles(new File(fileName));
		List<File> files = new ArrayList<File>(queue);
		
	    FieldType type = new FieldType();
	    type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
	    type.setStored(true);
	    type.setStoreTermVectors(true);
	    type.setTokenized(true);
	    type.setStoreTermVectorOffsets(true);
	    
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
						doc.add(new Field("text_highl", contentStemmed, type));
					} catch (Exception e1) {
						e1.printStackTrace();
					}

					// TODO make cleaning/splitting code universal
					String contentJSON = content.replace("u00a0","");
					content = StringUtils.substringBetween(contentJSON, "{\"body\": \"", "\"");
					String title = StringUtils.substringBetween(contentJSON,"\"title\":", "\"}");
					doc.add(new TextField("title", title, Field.Store.YES));
					
					String[] paragraphs = content.split("<br><br>");
					for(String p: paragraphs ){
						try {
							doc.add(new TextField("answer", p.toLowerCase(), Field.Store.YES));
							doc.add(new TextField("answer", formStemmedStrQuery(p), Field.Store.YES));
							doc.add(new Field("answer_highl", p.toLowerCase(), type));
							doc.add(new Field("answer_highl", formStemmedStrQuery(p), type));

							List<String> phrases = getPhrasesForIndexing(p);
							for(String phr: phrases){
								doc.add(new TextField("phrase", phr, Field.Store.YES));
								doc.add(new TextField("phrase", formStemmedStrQuery(phr), Field.Store.YES));
								// no need to use highlighter here	
							}
							try {
								List<String> edus = getEDUsForIndexing(p);
								if (edus==null)
									continue;
								for(String ed: edus)
									if (ed!=null){
										doc.add(new TextField("edu", ed, Field.Store.YES));
										doc.add(new TextField("edu", formStemmedStrQuery(ed), Field.Store.YES));
										doc.add(new Field("edu", ed, type));
										doc.add(new Field("edu", formStemmedStrQuery(ed), type));
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

	


	public static void main(String[] args) {
		DocsAnalyzerIndexerWithHighlighter indexer =  new DocsAnalyzerIndexerWithHighlighter();
		indexer.setDocDir("/Users/bgalitsk/Downloads/manualdocs");

		try {
			indexer.indexTrainingSet();
		} catch (Exception e) {
			e.printStackTrace();
		}
		indexer.close();
	}

}
