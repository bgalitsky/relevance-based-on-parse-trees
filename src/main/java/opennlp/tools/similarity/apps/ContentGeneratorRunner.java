package opennlp.tools.similarity.apps;

import java.util.List;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import opennlp.tools.apps.utils.email.EmailSender;
import opennlp.tools.textsimilarity.chunker2matcher.ParserChunker2MatcherProcessor;

public class ContentGeneratorRunner {
	public static void main(String[] args) {
		ParserChunker2MatcherProcessor sm = null;
	    	    
	    try {
			String resourceDir = args[2];
			if (resourceDir!=null)
				sm = ParserChunker2MatcherProcessor.getInstance(resourceDir);
			else
				sm = ParserChunker2MatcherProcessor.getInstance();
	
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	    RelatedSentenceFinder f = null;
	    if (args.length>4 && args[4]!=null)
	    	f = new RelatedSentenceFinder(Integer.parseInt(args[3]), Integer.parseInt(args[4]), Float.parseFloat(args[5]));
	    else
	    	f = new RelatedSentenceFinder();
	    
	    List<HitBase> hits = null;
	    try {
	      
	      hits = f.generateContentAbout(args[0].replace('+', ' ').replace('"', ' ').trim());
	      System.out.println(HitBase.toString(hits));
	      String generatedContent = HitBase.toResultantString(hits);
	      
	      opennlp.tools.apps.utils.email.EmailSender s = new opennlp.tools.apps.utils.email.EmailSender();
			
			try {
				s.sendMail("smtp.live.com", "bgalitsky@hotmail.com", "borgalor", new InternetAddress("bgalitsky@hotmail.com"), new InternetAddress[]{new InternetAddress(args[1])}, new InternetAddress[]{}, new InternetAddress[]{}, 
						"Generated content for you on '"+args[0].replace('+', ' ')+"'", generatedContent, null);
			} catch (AddressException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
		
				e.printStackTrace();
				try {
					s.sendMail("smtp.live.com", "bgalitsky@hotmail.com", "borgalor", new InternetAddress("bgalitsky@hotmail.com"), new InternetAddress[]{new InternetAddress(args[1])}, new InternetAddress[]{}, new InternetAddress[]{}, 
							"Generated content for you on '"+args[0].replace('+', ' ')+"'", generatedContent, null);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
	      
	      
	    } catch (Exception e) {
	      e.printStackTrace();
	    }

	  }
}

/*
 * C:\stanford-corenlp>java -Xmx1g -jar pt.jar albert+einstein bgalitsky@hotmail.com C:/stanford-corenlp/src/test/resources
 */
