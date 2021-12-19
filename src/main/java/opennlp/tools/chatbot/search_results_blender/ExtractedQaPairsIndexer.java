
package opennlp.tools.chatbot.search_results_blender;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import opennlp.tools.jsmlearning.ProfileReaderWriter;

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

public class ExtractedQaPairsIndexer {
	public static String resourceDir = System.getProperty("user.dir") + "/src/test/resources";
    public static String INDEX_PATH = "/qaPairs";

    protected ArrayList<File> queue = new ArrayList<File>();
    protected Tika tika = new Tika();

    protected IndexWriter indexWriter = null;
   
	private String absolutePathTrainingSet=null;

    public ExtractedQaPairsIndexer() {
    	try {
			initIndexWriter();
		} catch (Exception e) {
			e.printStackTrace();
		}
    } 


    public void putQuestionAnswerPairIntoIndex(String question, String answer) {

        try {
	        Document doc = new Document();

	        doc.add(new TextField("question", question.toLowerCase(), Field.Store.YES));
	        doc.add(new TextField("answer", answer, Field.Store.YES));

	        try {
	        	indexWriter.addDocument(doc);
	        } catch (IOException e) {
	        	e.printStackTrace();
	        }
        } catch (Exception e) {
	        e.printStackTrace();
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
            indexDir = FSDirectory.open(path.toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
        IndexWriterConfig luceneConfig = new IndexWriterConfig(null, new StopAnalyzer());
        luceneConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

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
    	ExtractedQaPairsIndexer indexer = new ExtractedQaPairsIndexer();
    	indexer.putQuestionAnswerPairIntoIndex("I want to get money from my account", "........");
    }
    	

}
