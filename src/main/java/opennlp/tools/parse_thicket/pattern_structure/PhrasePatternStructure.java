package opennlp.tools.parse_thicket.pattern_structure;

import java.util.*;
import java.io.*;

import opennlp.tools.parse_thicket.ParseCorefsBuilder;
import opennlp.tools.parse_thicket.ParseThicket;
import opennlp.tools.parse_thicket.ParseTreeNode;
import opennlp.tools.parse_thicket.matching.PT2ThicketPhraseBuilder;
import opennlp.tools.textsimilarity.ParseTreeChunk;
import opennlp.tools.textsimilarity.ParseTreeMatcherDeterministic;


public class PhrasePatternStructure {
	int objectCount;
	int attributeCount;
	ArrayList<PhraseConcept> conceptList;
	ParseTreeMatcherDeterministic md; 
	public PhrasePatternStructure(int objectCounts, int attributeCounts) {
		objectCount = objectCounts;
		attributeCount = attributeCounts;
		conceptList = new ArrayList<PhraseConcept>();
		PhraseConcept bottom = new PhraseConcept();
		md = new ParseTreeMatcherDeterministic();
		/*Set<Integer> b_intent = new HashSet<Integer>();
		for (int index = 0; index < attributeCount; ++index) {
			b_intent.add(index);
		}
		bottom.setIntent(b_intent);*/
		bottom.setPosition(0);
		conceptList.add(bottom);
	}
	public int GetMaximalConcept(List<List<ParseTreeChunk>> intent, int Generator) {
		boolean parentIsMaximal = true;
		while(parentIsMaximal) {
			parentIsMaximal = false;
			for (int parent : conceptList.get(Generator).parents) {
				if (conceptList.get(parent).intent.containsAll(intent)) {
					Generator = parent;
					parentIsMaximal = true;
					break;
				}
			}
		}
		return Generator;
	}
	public int AddIntent(List<List<ParseTreeChunk>> intent, int generator) {
		System.out.println("debug");
		System.out.println("called for " + intent);
		//printLattice();
		int generator_tmp = GetMaximalConcept(intent, generator);
		generator = generator_tmp;
		if (conceptList.get(generator).intent.equals(intent)) {
			System.out.println("at generator:" + conceptList.get(generator).intent);
			System.out.println("to add:" + intent);

			System.out.println("already generated");
			return generator;
		}
		Set<Integer> generatorParents = conceptList.get(generator).parents;
		Set<Integer> newParents = new HashSet<Integer>();
		for (int candidate : generatorParents) {
			if (!intent.containsAll(conceptList.get(candidate).intent)) {
			//if (!conceptList.get(candidate).intent.containsAll(intent)) {
				//Set<Integer> intersection = new HashSet<Integer>(conceptList.get(candidate).intent);
				//List<List<ParseTreeChunk>> intersection = new ArrayList<List<ParseTreeChunk>>(conceptList.get(candidate).intent);
				//intersection.retainAll(intent);
				List<List<ParseTreeChunk>> intersection = md
				.matchTwoSentencesGroupedChunksDeterministic(intent, conceptList.get(candidate).intent);
				System.out.println("recursive call (inclusion)");
				candidate = AddIntent(intersection, candidate);
			}
			boolean addParents = true;
			System.out.println("now iterating over parents");
			Iterator<Integer> iterator = newParents.iterator();
			while (iterator.hasNext()) {
				Integer parent = iterator.next();
				if (conceptList.get(parent).intent.containsAll(conceptList.get(candidate).intent)) {
					addParents = false;
					break;
				}
				else {
					if (conceptList.get(candidate).intent.containsAll(conceptList.get(parent).intent)) {
						iterator.remove();
					}
				}
			}
			/*for (int parent : newParents) {
				System.out.println("parent = " + parent);
				System.out.println("candidate intent:"+conceptList.get(candidate).intent);
				System.out.println("parent intent:"+conceptList.get(parent).intent);
				
				if (conceptList.get(parent).intent.containsAll(conceptList.get(candidate).intent)) {
					addParents = false;
					break;
				}
				else {
					if (conceptList.get(candidate).intent.containsAll(conceptList.get(parent).intent)) {
						newParents.remove(parent);
					}
				}
			}*/
			if (addParents) {
				newParents.add(candidate);
			}
		}
		System.out.println("size of lattice: " + conceptList.size());
		PhraseConcept newConcept = new PhraseConcept();
		newConcept.setIntent(intent);
		newConcept.setPosition(conceptList.size());
		conceptList.add(newConcept);
		conceptList.get(generator).parents.add(newConcept.position);
		for (int newParent: newParents) {
			if (conceptList.get(generator).parents.contains(newParent)) {
				conceptList.get(generator).parents.remove(newParent);
			}
			conceptList.get(newConcept.position).parents.add(newParent);
		}
		return newConcept.position;
	}
	public void printLatticeStats() {
		System.out.println("Lattice stats");
		System.out.println("max_object_index = " + objectCount);
		System.out.println("max_attribute_index = " + attributeCount);
		System.out.println("Current concept count = " + conceptList.size());
	}
	public void printLattice() {
		for (int i = 0; i < conceptList.size(); ++i) {
			printConceptByPosition(i);
		}
	}
	public void printConceptByPosition(int index) {
		System.out.println("Concept at position " + index);
		conceptList.get(index).printConcept();
	}
	public List<List<ParseTreeChunk>> formGroupedPhrasesFromChunksForPara(
			List<List<ParseTreeNode>> phrs) {
		List<List<ParseTreeChunk>> results = new ArrayList<List<ParseTreeChunk>>();
		List<ParseTreeChunk> nps = new ArrayList<ParseTreeChunk>(), vps = new ArrayList<ParseTreeChunk>(), 
				pps = new ArrayList<ParseTreeChunk>();
		for(List<ParseTreeNode> ps:phrs){
			ParseTreeChunk ch = convertNodeListIntoChunk(ps);
			String ptype = ps.get(0).getPhraseType();
			if (ptype.equals("NP")){
				nps.add(ch);
			} else if (ptype.equals("VP")){
				vps.add(ch);
			} else if (ptype.equals("PP")){
				pps.add(ch);
			}
		}
		results.add(nps); results.add(vps); results.add(pps);
		return results;
	}
	private ParseTreeChunk convertNodeListIntoChunk(List<ParseTreeNode> ps) {
		List<String> lemmas = new ArrayList<String>(),  poss = new ArrayList<String>();
		for(ParseTreeNode n: ps){
			lemmas.add(n.getWord());
			poss.add(n.getPos());
		}
		ParseTreeChunk ch = new ParseTreeChunk(lemmas, poss, 0, 0);
		ch.setMainPOS(ps.get(0).getPhraseType());
		return ch;
	}
	public static void main(String []args) {
		PhrasePatternStructure lat = new PhrasePatternStructure(3,1);
		
		ParseTreeMatcherDeterministic md = new ParseTreeMatcherDeterministic();
		ParseCorefsBuilder ptBuilder = ParseCorefsBuilder.getInstance();
		PT2ThicketPhraseBuilder phraseBuilder = new PT2ThicketPhraseBuilder();
		String Description;
		ParseThicket pt1;
		List<List<ParseTreeNode>> phrs1;
		List<List<ParseTreeChunk>> sent1GrpLst;
		//Example 1
		/*Description = "Eh bien, mon prince, so Genoa and Lucca are now no more than family estates of the Bonapartes. No, I warn you, if you don’t say that this means war, if you still permit yourself to condone all the infamies, all the atrocities, of this Antichrist—and that’s what I really believe he is—I will have nothing more to do with you, you are no longer my friend, my faithful slave, as you say. But how do you do, how do you do? I see that I am frightening you. Sit down and tell me all about it.";
		pt1 = ptBuilder.buildParseThicket(Description);	
		phrs1 = phraseBuilder.buildPT2ptPhrases(pt1);
		sent1GrpLst = lat.formGroupedPhrasesFromChunksForPara(phrs1);
		lat.AddIntent(sent1GrpLst, 0);
		*/
		/*Description = "Well, Prince, so Genoa and Lucca are now just family estates of the Buonapartes. But I warn you, if you don't tell me that this means war, if you still try to defend the infamies and horrors perpetrated by that Antichrist—I really believe he is Antichrist—I will have nothing more to do with you and you are no longer my friend, no longer my 'faithful slave,' as you call yourself! But how do you do? I see I have frightened you—sit down and tell me all the news";		
		pt1 = ptBuilder.buildParseThicket(Description);	
		phrs1 = phraseBuilder.buildPT2ptPhrases(pt1);
		sent1GrpLst = lat.formGroupedPhrasesFromChunksForPara(phrs1);
		lat.AddIntent(sent1GrpLst, 0);
		
		
		Description = "Well, Prince, Genoa and Lucca are now nothing more than estates taken over by the Buonaparte family.1 No, I give you fair warning. If you won’t say this means war, if you will allow yourself to condone all the ghastly atrocities perpetrated by that Antichrist – yes, that’s what I think he is – I shall disown you. You’re no friend of mine – not the “faithful slave” you claim to be . . . But how are you? How are you keeping? I can see I’m intimidating you. Do sit down and talk to me.";
		pt1 = ptBuilder.buildParseThicket(Description);	
		phrs1 = phraseBuilder.buildPT2ptPhrases(pt1);
		sent1GrpLst = lat.formGroupedPhrasesFromChunksForPara(phrs1);
		lat.AddIntent(sent1GrpLst, 0);
		
		Description = "Well, prince, Genoa and Lucca are now nothing more than the apanages, than the private property of the Bonaparte family. I warn you that if you do not tell me we are going to have war, if you still allow yourself to condone all the infamies, all the atrocities of this Antichrist - on my word I believe he is Antichrist - that is the end of our acquaintance; you are no longer my friend, you are no longer my faithful slave, as you call yourself. Now, be of good courage, I see I frighten you. Come, sit down and tell me all about it.";
		pt1 = ptBuilder.buildParseThicket(Description);	
		phrs1 = phraseBuilder.buildPT2ptPhrases(pt1);
		sent1GrpLst = lat.formGroupedPhrasesFromChunksForPara(phrs1);
		lat.AddIntent(sent1GrpLst, 0); */
		
		
		//Example 2
		/*List<List<ParseTreeChunk>> res = m.assessRelevance("At least 9 people were killed and 43 others wounded in shootings and bomb attacks, including four car bombings, in central and western Iraq on Thursday, the police said. A car bomb parked near the entrance of the local government compound in Anbar's provincial capital of Ramadi, some 110 km west of Baghdad, detonated in the morning near a convoy of vehicles carrying the provincial governor Qassim al-Fahdawi, a provincial police source told Xinhua on condition of anonymity.",
				"Officials say a car bomb in northeast Baghdad killed four people, while another bombing at a market in the central part of the capital killed at least two and wounded many more. Security officials also say at least two policemen were killed by a suicide car bomb attack in the northern city of Mosul. No group has claimed responsibility for the attacks, which occurred in both Sunni and Shi'ite neighborhoods."
				);*/
		Description = "At least 9 people were killed and 43 others wounded in shootings and bomb attacks, including four car bombings, in central and western Iraq on Thursday, the police said. A car bomb parked near the entrance of the local government compound in Anbar's provincial capital of Ramadi, some 110 km west of Baghdad, detonated in the morning near a convoy of vehicles carrying the provincial governor Qassim al-Fahdawi, a provincial police source told Xinhua on condition of anonymity.";
		pt1 = ptBuilder.buildParseThicket(Description);	
		phrs1 = phraseBuilder.buildPT2ptPhrases(pt1);
		sent1GrpLst = lat.formGroupedPhrasesFromChunksForPara(phrs1);
		lat.AddIntent(sent1GrpLst, 0);
		
		Description = "Officials say a car bomb in northeast Baghdad killed four people, while another bombing at a market in the central part of the capital killed at least two and wounded many more. Security officials also say at least two policemen were killed by a suicide car bomb attack in the northern city of Mosul. No group has claimed responsibility for the attacks, which occurred in both Sunni and Shi'ite neighborhoods.";
		pt1 = ptBuilder.buildParseThicket(Description);	
		phrs1 = phraseBuilder.buildPT2ptPhrases(pt1);
		sent1GrpLst = lat.formGroupedPhrasesFromChunksForPara(phrs1);
		lat.AddIntent(sent1GrpLst, 0);
		
		Description = "Two car bombs killed at least four people and wounded dozens of others on Monday in one of the bloodiest attacks this year in Dagestan, a turbulent province in Russia's North Caucasus region where armed groups are waging an Islamist insurgency. Car bombs, suicide bombings and firefights are common in Dagestan, at the centre of an insurgency rooted in two post-Soviet wars against separatist rebels in neighbouring Chechnya. Such attacks are rare in other parts of Russia, but in a separate incident in a suburb of Moscow on Monday, security forces killed two suspected militants alleged to have been plotting an attack in the capital and arrested a third suspect after a gunbattle";
	//	Description = "AMMAN, Jordan (AP) — A Syrian government official says a car bomb has exploded in a suburb of the capital Damascus, killing three people and wounding several others. The Britain-based Syrian Observatory for Human Rights confirmed the Sunday explosion in Jouber, which it said has seen heavy clashes recently between rebels and the Syrian army. It did not have any immediate word on casualties. It said the blast targeted a police station and was carried out by the Jabhat al-Nusra, a militant group linked to al-Qaida, did not elaborate.";
	//	Description = "A car bombing in Damascus has killed at least nine security forces, with aid groups urging the evacuation of civilians trapped in the embattled Syrian town of Qusayr. The Syrian Observatory for Human Rights said on Sunday the explosion, in the east of the capital, appeared to have been carried out by the extremist Al-Nusra Front, which is allied to al-Qaeda, although there was no immediate confirmation. In Lebanon, security sources said two rockets fired from Syria landed in a border area, and Israeli war planes could be heard flying low over several parts of the country.";
		pt1 = ptBuilder.buildParseThicket(Description);	
		phrs1 = phraseBuilder.buildPT2ptPhrases(pt1);
		sent1GrpLst = lat.formGroupedPhrasesFromChunksForPara(phrs1);
		lat.AddIntent(sent1GrpLst, 0);
		
		lat.printLattice();
		lat.printLatticeStats();
		/*PhrasePatternStructure lat = new PhrasePatternStructure(3,4);
		lat.printLattice();
		lat.printConceptByPosition(0);
		Set<Integer> intent = new HashSet<Integer>();
		intent.add(0);
		intent.add(1);
		int gen = lat.GetMaximalConcept(intent,0);
		System.out.println("generator: " + gen);
		intent.clear();
		intent.add(0);
		intent.add(3);
		
		lat.AddIntent(intent, 0);
		//System.out.println("after first addintent");
		//lat.printConceptByPosition(0);
		//lat.printConceptByPosition(1);
		intent.clear();
		intent.add(0);
		intent.add(2);
		lat.AddIntent(intent, 0);

		intent.clear();
		intent.add(1);
		intent.add(2);

		lat.AddIntent(intent, 0);
		intent.clear();
		intent.add(1);
		intent.add(2);
		intent.add(3);
		lat.AddIntent(intent, 0);
		lat.printLattice();
		lat.printLatticeStats();*/
	}
}