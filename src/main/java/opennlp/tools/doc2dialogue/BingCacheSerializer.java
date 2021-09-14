package opennlp.tools.doc2dialogue;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class BingCacheSerializer {
  private static Logger LOG = Logger
      .getLogger("opennlp.tools.doc2dialogue.ParserCacheSerializer");

  private static String RESOURCE_DIR = ""; //"src/test/resources/";
  public String parseCacheFileName = "bingQueries.dat";
 
  public  void setCacheFileName(String fileName){
	  parseCacheFileName = fileName;
  }

  public void writeObject(Object objectToSerialize) {
      String filename = RESOURCE_DIR + parseCacheFileName;
      FileOutputStream fos = null;
      ObjectOutputStream out = null;
      try {
        fos = new FileOutputStream(filename);
        out = new ObjectOutputStream(fos);
        out.writeObject(objectToSerialize);
        out.close();
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    } 


  public Object readObject() {
      String filename = RESOURCE_DIR + parseCacheFileName;
      Object data = null;
      FileInputStream fis = null;
      ObjectInputStream in = null;
      try {
        fis = new FileInputStream(filename);
        in = new ObjectInputStream(fis);
        data = (Object) in.readObject();
        in.close();
      } catch (IOException ex) {
        System.out.println("Cant find cache file ");
      } catch (ClassNotFoundException ex) {
        ex.printStackTrace();
      }
      return data;
    } 

  }
