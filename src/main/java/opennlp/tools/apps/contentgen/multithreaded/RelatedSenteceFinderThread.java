package opennlp.tools.apps.contentgen.multithreaded;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tika.Tika;

import opennlp.tools.parser.Parse;
import opennlp.tools.similarity.apps.HitBase;
import opennlp.tools.similarity.apps.RelatedSentenceFinder;


public class RelatedSenteceFinderThread extends RelatedSentenceFinder implements Runnable{
	private static Logger log = Logger.getLogger(RelatedSenteceFinderThread.class);
	private String sentence;
	private List<String> sents;
	List<HitBase> result;
	
	 public RelatedSenteceFinderThread(String sentence, List<String> sents) {
		super();
		this.sentence = sentence;
		this.sents = sents;
	}

	public String getSentence() {
		return sentence;
	}

	public void setSentence(String sentence) {
		this.sentence = sentence;
	}

	public List<String> getSents() {
		return sents;
	}

	public void setSents(List<String> sents) {
		this.sents = sents;
	}

	public List<HitBase> getResult() {
		return result;
	}



	// Create the listener list
    protected javax.swing.event.EventListenerList listenerList = new javax.swing.event.EventListenerList();
    // This methods allows classes to register for MyEvents

    public void addMyEventListener(MyEventListener listener) {
        listenerList.add(MyEventListener.class, listener);
    }
    // This methods allows classes to unregister for MyEvents

    public void removeMyEventListener(MyEventListener listener) {
        listenerList.remove(MyEventListener.class, listener);
    }

    void fireMyEvent(MyEvent evt) {
        Object[] listeners = listenerList.getListenerList();
        // Each listener occupies two elements - the first is the listener class
        // and the second is the listener instance
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == MyEventListener.class) {
                ((MyEventListener) listeners[i + 1]).MyEvent(evt);
            }
        }
    }
	
	@Override
	public void run() {
		try {
			result=	findRelatedOpinionsForSentence(sentence,sents);
		} catch (Exception e) {
			log.error(e);
			e.printStackTrace();
		}finally{
			fireMyEvent(new MyEvent(this));
		}
		
	}

}
