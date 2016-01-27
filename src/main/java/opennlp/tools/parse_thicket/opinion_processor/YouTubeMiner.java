package opennlp.tools.parse_thicket.opinion_processor;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import opennlp.tools.similarity.apps.utils.PageFetcher;

public class YouTubeMiner {
	private PageFetcher fetcher = new PageFetcher();
	public YouTubeMinerResult getData(String url){
		YouTubeMinerResult result = new YouTubeMinerResult();
		String content = fetcher.fetchOrigHTML(url);
		try {
			FileUtils.writeStringToFile(new File(url.replace(':', '_').replace('/', '_')), content);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (url.indexOf("channel")>-1){
			try { //subscriber-count" title="30" 
				String subscribersStr = StringUtils.substringBetween(content,"yt-subscriber-count", "aria-label");
						//"subscriber-count\" title=\"","\"");
				subscribersStr = subscribersStr.replace(",", "");
				int subscribers = Integer.parseInt(subscribersStr );
				result.subscribers = subscribers;
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			try {
				String viewsStr = StringUtils.substringBetween(content,
						//"div class=\"watch-view-count\">"," views</div>");
						"watch-view-count"," views");
				int views = Integer.parseInt(viewsStr );
				result.views = views;
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return result;
	}
	
	
	public static void main(String[] args){
		YouTubeMiner  miner = new YouTubeMiner();
		System.out.println(miner.getData("https://www.youtube.com/channel/UC-maQbG5eUS5c1wmaTnLwTA"));
		System.out.println(miner.getData("https://www.youtube.com/watch?v=U6X4VT9dVr8"));
	}
}
