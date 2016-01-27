package opennlp.tools.parse_thicket.opinion_processor;

public class YouTubeMinerResult {
	public int likes;
	public int subscribers;
	public int views;
	
	boolean isPromisingYoungIndividual(){
		if (subscribers>0)
			if (subscribers>10 && subscribers< 20000)
				return true;
		if (views>0)
			if (views>10 && views< 20000)
				return true;
		return false;

	}
	
	public String toString(){
		return "views :"+ views + "| subscribers = "+ subscribers;
	}
}
