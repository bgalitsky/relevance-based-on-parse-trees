
package opennlp.tools.chatbot.qna_pairs_adapter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import opennlp.tools.jsmlearning.ProfileReaderWriter;
import opennlp.tools.stemmer.PStemmer;
import opennlp.tools.textsimilarity.TextProcessor;

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

public class ExtractedQnAPairsIndexer{
	private static PStemmer stemmer = new PStemmer();
	public static String INDEX_PATH = "/qaPairsSiemens";
	protected IndexWriter indexWriter = null;
	public static String resourceDir = System.getProperty("user.dir") + "/src/test/resources";
	
	public ExtractedQnAPairsIndexer() {
    	try {
			initIndexWriter();
		} catch (Exception e) {
			e.printStackTrace();
		}
    } 
	
	public ExtractedQnAPairsIndexer(boolean bCreate) {
    	try {
			initIndexWriter(bCreate);
		} catch (Exception e) {
			e.printStackTrace();
		}
    } 

	public ExtractedQnAPairsIndexer(boolean b, String pathIndex) {
		INDEX_PATH = pathIndex;
		try {
			initIndexWriter(b);
		} catch (Exception e) {
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
    public void putQuestionAnswerPairIntoIndex(String question, String answer, List<String> categories) {
        try {
	        Document doc = new Document();

	        doc.add(new TextField("question", question.toLowerCase(), Field.Store.YES));
	        doc.add(new TextField("question", formStemmedStrQuery(question.toLowerCase()), Field.Store.YES));
	        doc.add(new TextField("answer", answer, Field.Store.YES));
	        for(String cat: categories){
	        	doc.add(new StringField("categories", cat, Field.Store.YES));
	        }
	        try {
	        	indexWriter.addDocument(doc);
	        } catch (IOException e) {
	        	e.printStackTrace();
	        }
        } catch (Exception e) {
	        e.printStackTrace();
        }
    }
    
    public void putQuestionAnswerPairIntoIndex(String question, String answer, List<String> categories, List<String> categoryPaths) {

        try {
	        Document doc = new Document();

	        doc.add(new TextField("question", question.toLowerCase(), Field.Store.YES));
	        doc.add(new TextField("question", formStemmedStrQuery(question.toLowerCase()), Field.Store.YES));
	        doc.add(new TextField("answer", answer, Field.Store.YES));
	        for(String cat: categories){
	        	doc.add(new StringField("categories", cat, Field.Store.YES));
	        }
	        for(String cat: categoryPaths){
	        	doc.add(new StringField("category_paths", cat, Field.Store.YES));
	        }
	        try {
	        	indexWriter.addDocument(doc);
	        } catch (IOException e) {
	        	e.printStackTrace();
	        }
        } catch (Exception e) {
	        e.printStackTrace();
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
   
    public static void main(String[] args) {
    	ExtractedQnAPairsIndexer indexer = new ExtractedQnAPairsIndexer();
    	indexer.putQuestionAnswerPairIntoIndex("I want to get money from my account", "........", 
    			new ArrayList<String>());
    }
    	

}
