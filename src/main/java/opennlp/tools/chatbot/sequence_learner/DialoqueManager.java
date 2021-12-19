package opennlp.tools.chatbot.sequence_learner;

import java.util.List;

public class DialoqueManager {
	List<DialogueScenario> trainingSet;
	
	DialoqueManager(){
		importTrainingSet();
	}
	
	private void importTrainingSet(){
		//TODO
	}
	
	public String respond(DialogueScenario currentSession){
		DialogueScenario theClosestSession = null;
		// find closest scenario to the given one
		// for( ...)
		
		// assuming sequence is original
		String newChatBotUtterance = theClosestSession.utterances.get(currentSession.utterances.size());
		// TODO - build a map between currentSession and theClosestSession
		
		return newChatBotUtterance;
	}
	
}
