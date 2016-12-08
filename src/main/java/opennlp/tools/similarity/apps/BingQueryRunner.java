package opennlp.tools.similarity.apps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import opennlp.tools.similarity.apps.HitBase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BingQueryRunner {
	// This is Boris' personal account key. Should be replaced by Ligadata's
	private String accountKey = "623bf56c8cc9403290abb2aa1b5b8ced";
	
	// request string
	final String bingUrlPattern ="https://api.cognitive.microsoft.com/bing/v5.0/search?q=", 
			bingImageUrlPattern ="https://api.cognitive.microsoft.com/bing/v5.0/images/search?q=";

	// Main entry point. The same function from ver 3 but now new authentication and new reqiest string
	public List<HitBase> runSearch(String queryOrig){
		List<HitBase> sresults = new ArrayList<HitBase>();
		String query = "";
        try {
	        query = URLEncoder.encode(queryOrig, Charset.defaultCharset().name());
        } catch (UnsupportedEncodingException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
		String bingUrl = String.format(bingUrlPattern, query);
		URL url;
		URLConnection connection = null;
        try {
	        url = new URL(bingUrl+query);
            connection = url.openConnection();
        } catch (Exception e) {
	        e.printStackTrace();
        }
	      connection.setRequestProperty("Ocp-Apim-Subscription-Key", accountKey);
		try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
			String inputLine;
			StringBuilder response = new StringBuilder();
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			try {
	            JSONObject json = new JSONObject(response.toString());
	            JSONObject d = json.getJSONObject("webPages");
	            JSONArray results = d.getJSONArray("value");
	            int resultsLength = results.length();
	            for (int i = 0; i < resultsLength; i++) {
	            	final JSONObject aResult = results.getJSONObject(i);
	            	HitBase sr = new HitBase();
	            	 sr.setUrl(aResult.getString("displayUrl"));
	            	 sr.setAbstractText(aResult.getString("snippet"));
	            	 sr.setTitle(aResult.getString("name"));
	            	 sresults.add(sr);
	            }
            } catch (JSONException e) {
	            e.printStackTrace();
            }
		} catch (IOException e) {
	        e.printStackTrace();
        }
		return sresults;
	}
	
	public List<HitBase> runSearch(String queryOrig, int count){
		List<HitBase> sresults = new ArrayList<HitBase>();
		String query = "";
        try {
	        query = URLEncoder.encode(queryOrig, Charset.defaultCharset().name());
        } catch (UnsupportedEncodingException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
		String bingUrl = String.format(bingUrlPattern, query);
		URL url;
		URLConnection connection = null;
        try {
	        url = new URL(bingUrl+query);
            connection = url.openConnection();
        } catch (Exception e) {
	        e.printStackTrace();
        }
	      connection.setRequestProperty("Ocp-Apim-Subscription-Key", accountKey);
		try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
			String inputLine;
			StringBuilder response = new StringBuilder();
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			try {
	            JSONObject json = new JSONObject(response.toString());
	            JSONObject d = json.getJSONObject("webPages");
	            JSONArray results = d.getJSONArray("value");
	            int resultsLength = results.length();
	            for (int i = 0; i < resultsLength && i< count ; i++) {
	            	final JSONObject aResult = results.getJSONObject(i);
	            	HitBase sr = new HitBase();
	            	 sr.setUrl(aResult.getString("displayUrl"));
	            	 sr.setAbstractText(aResult.getString("snippet"));
	            	 sr.setTitle(aResult.getString("name"));
	            	 sresults.add(sr);
	            }
            } catch (JSONException e) {
	            e.printStackTrace();
            }
		} catch (IOException e) {
	        e.printStackTrace();
        }
		return sresults;
	}
	
	public List<HitBase> runImageSearch(String queryOrig){
		List<HitBase> sresults = new ArrayList<HitBase>();
		String query = "";
        try {
	        query = URLEncoder.encode(queryOrig, Charset.defaultCharset().name());
        } catch (UnsupportedEncodingException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
		String bingUrl = String.format(bingImageUrlPattern, query);
		URL url;
		URLConnection connection = null;
        try {
	        url = new URL(bingUrl+query);
            connection = url.openConnection();
        } catch (Exception e) {
	        e.printStackTrace();
        }
	      connection.setRequestProperty("Ocp-Apim-Subscription-Key", accountKey);
		try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
			String inputLine;
			StringBuilder response = new StringBuilder();
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			try {
	            JSONObject json = new JSONObject(response.toString());
	            //JSONObject d = json.getJSONObject("instrumentation");
	            JSONArray results = json.getJSONArray("value");
	            int resultsLength = results.length();
	            for (int i = 0; i < resultsLength; i++) {
	            	final JSONObject aResult = results.getJSONObject(i);
	            	HitBase sr = new HitBase();
	            	 sr.setUrl(aResult.getString("contentUrl"));
	            	 sr.setAbstractText(aResult.getString("hostPageUrl"));
	            	 sr.setTitle(aResult.getString("name"));
	            	 sresults.add(sr);
	            }
            } catch (JSONException e) {
	            e.printStackTrace();
            }
		} catch (IOException e) {
	        e.printStackTrace();
        }
		return sresults;
	}
	
	public void setKey(String key){
		accountKey=key;
	}
	public void setLang(String lang) {
	    // TODO Auto-generated method stub   
    }
	
	public static void main(String[] args){
		// run search and print all results in a  line
//		System.out.println(new BingQueryRunner().runSearch("latest iphone"));
		System.out.println(new BingQueryRunner().runImageSearch("latest iphone"));
	}


}
