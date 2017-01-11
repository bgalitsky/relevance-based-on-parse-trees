package opennlp.tools.chatbot;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
public class PageByTagsParser {
   private static Logger LOG = Logger
			.getLogger("opennlp.tools.chatbot.PageByTagsParser");

   private static void print(String msg, Object... args) {
       LOG.info(String.format(msg, args));
    }

    private static String trim(String s, int width) {
        if (s.length() > width)
            return s.substring(0, width-1) + ".";
        else
            return s;
    }
    
    public List<String>  extractSectionTitles(String url){
    	List<String> results = new ArrayList<String>();
    	  Document doc=null;
        try {
	        doc = Jsoup.connect(url).get();
        } catch (IOException e) {
	       // e.printStackTrace();
	        return results;
        }
    	  Elements h1links = doc.body().getElementsByTag("h1");
          for (Element link : h1links) {
          	print("h1 :  %s", link);
          	if (isAcceptable(link.toString()))
          		results.add(link.text());
          }
          
          Elements h2links = doc.body().getElementsByTag("h2");
          for (Element link : h2links) {
          	print("h2 :  %s", link);
          	if (isAcceptable(link.toString()))
          		results.add(link.text());
          }
          
          Elements h3links = doc.body().getElementsByTag("h3");
          for (Element link : h3links) {
          	print("h3 :  %s", link);
          	if (isAcceptable(link.toString()))
          		results.add(link.text());
          }
          Elements h4links = doc.body().getElementsByTag("h4");
          for (Element link : h4links) {
          	print("h4 :  %s", link);
          	if (isAcceptable(link.toString()))
          		results.add(link.text());
          }
          return results;
    }
    
    
	private boolean isAcceptable(String text) {
		text = text.toLowerCase();
		if (text.indexOf("class")>-1)
	    	return false;
		if (text.indexOf("related")>-1)
	    	return false;
		String title = StringUtils.substringBetween(text, "\">", "<");
		if (title!=null && title.split(" ").length<3)
			return false;
		
		return true;
    }

	public static void main(String[] args) throws IOException {
		 String url = 
	        		//"https://loanscanada.ca/credit/paying-off-one-credit-card-with-another-credit-card/";
	        		//"https://www.nerdwallet.com/blog/credit-cards/can-i-use-credit-card-to-pay-another-credit-card/";
	        		//"http://www.thesimpledollar.com/can-you-pay-a-credit-card-with-a-credit-card/";
	        			//"https://loanscanada.ca/credit/paying-off-one-credit-card-with-another-credit-card/";
		// "http://www.schwab.com/public/schwab/investing/retirement_and_planning/understanding_iras/"
		// + "withdrawals_and_distributions/age_70_and_a_half_and_over";
		//		 "http://time.com/money/4068087/ira-withdrawals-rmd/";
		//		 "http://blogs.wsj.com/totalreturn/2015/03/18/no-fooling-deadline-for-first-ira-withdrawal-is-almost-here/";
	    // "https://www.irs.gov/uac/newsroom/many-retirees-face-april-1-deadline-to-take-required-retirement-plan-distributions";
		"https://www.fidelity.com/retirement-planning/learn-about-iras/ira-withdrawal";		
		PageByTagsParser pageParser = new PageByTagsParser();
		 List<String> results = pageParser.extractSectionTitles(url);
		 System.out.println(results);
		
		System.exit(0);
          print("Fetching %s...", url);

        Document doc = Jsoup.connect(url).get();
        Elements links = doc.select("a[href]");
        Elements media = doc.select("[src]");
        Elements imports = doc.select("link[href]");
        
        Elements h1links = doc.body().getElementsByTag("h1");
        for (Element link : h1links) {
        	print("h1 :  %s", link);
        }
        
        Elements h2links = doc.body().getElementsByTag("h2");
        for (Element link : h2links) {
        	print("h2 :  %s", link);
        }
        
        Elements h3links = doc.body().getElementsByTag("h3");
        for (Element link : h3links) {
        	print("h3 :  %s", link);
        }

 /*
        print("\nMedia: (%d)", media.size());
        for (Element src : media) {
            if (src.tagName().equals("img"))
                print(" * %s: <%s> %sx%s (%s)",
                        src.tagName(), src.attr("abs:src"), src.attr("width"), src.attr("height"),
                        trim(src.attr("alt"), 20));
            else
                print(" * %s: <%s>", src.tagName(), src.attr("abs:src"));
        }

        print("\nImports: (%d)", imports.size());
        for (Element link : imports) {
            print(" * %s <%s> (%s)", link.tagName(),link.attr("abs:href"), link.attr("rel"));
        }

        print("\nLinks: (%d)", links.size());
        for (Element link : links) {
            print(" * a: <%s>  (%s)", link.attr("abs:href"), trim(link.text(), 35));
        }
        */
    }

}