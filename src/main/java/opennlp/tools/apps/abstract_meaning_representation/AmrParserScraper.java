package opennlp.tools.apps.abstract_meaning_representation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import opennlp.tools.similarity.apps.utils.PageFetcher;

public class AmrParserScraper {
	private static final String url = "http://cohort.inf.ed.ac.uk/amreager.html?lang=en&sent=Nghe+An+police+have+collaborated+with+central+authorities+to+collect+related+DNA+samples+for+examination+and+further+investigation+Cau+said";
	private static PageFetcher fetcher = new PageFetcher();
	  private static int DEFAULT_TIMEOUT = 3500;

	public List<String>  getAmrParseGraph(String sent){
		List<String> results = new ArrayList<String>();
		String content=null;
		StringBuffer buf = new StringBuffer();
		try {
			//content = FileUtils.readFileToString(new File(productListPath));
			
		    try {
		      URLConnection connection = new URL(url).openConnection();
		      connection.setReadTimeout(DEFAULT_TIMEOUT);
		      connection
		          .setRequestProperty(
		              "User-Agent",
		              "Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US; rv:1.9.0.3) Gecko/2008092417 Firefox/3.0.3");
		      String line;
		      BufferedReader reader = null;

		        reader = new BufferedReader(new InputStreamReader(
		            connection.getInputStream()));


		      while ((line = reader.readLine()) != null) {
		        buf.append(line);
		      }

		} catch (Exception e) {
			e.printStackTrace();
		}
		FileUtils.writeStringToFile(new File("amreager.txt"), buf.toString(), "UTF-8");
		    
		String[] prods = StringUtils.substringsBetween(content, "data-pdp-url=\"", "\">");
		
		
	} catch (Exception ee) {
		ee.printStackTrace();
	}
		return null;
	}
	
	public static void main(String[] args){
		//new AmrParserScraper().getAmrParseGraph(null);
		
		 URL oracle=null;
		try {
			oracle = new URL(url);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	        BufferedReader in=null;
			try {
				in = new BufferedReader(
				new InputStreamReader(oracle.openStream()));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	        String inputLine = null;
	        try {
				while ((inputLine = in.readLine()) != null)
				    System.out.println(inputLine);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        try {
				in.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
}
