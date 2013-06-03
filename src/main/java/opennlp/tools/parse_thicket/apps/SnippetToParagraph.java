package opennlp.tools.parse_thicket.apps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;


import opennlp.tools.similarity.apps.Fragment;
import opennlp.tools.similarity.apps.GeneratedSentenceProcessor;
import opennlp.tools.similarity.apps.HitBase;
import opennlp.tools.similarity.apps.RelatedSentenceFinder;
import opennlp.tools.similarity.apps.utils.PageFetcher;
import opennlp.tools.similarity.apps.utils.StringDistanceMeasurer;
import opennlp.tools.similarity.apps.utils.Utils;
import opennlp.tools.textsimilarity.TextProcessor;


public class SnippetToParagraph extends RelatedSentenceFinder {
	private PageFetcher pFetcher = new PageFetcher();
	private static Logger LOG = Logger
			.getLogger("com.become.parse_thicket.apps.SnippetToParagraph");

	public HitBase formTextFromOriginalPageGivenSnippetDirect(HitBase item) {

		// put orig sentence in structure
		List<String> origs = new ArrayList<String>();

		item.setOriginalSentences(origs);
		String title = item.getTitle().replace("<b>", " ").replace("</b>", " ")
				.replace("  ", " ").replace("  ", " ");
		// generation results for this sentence
		List<Fragment> result = new ArrayList<Fragment>();
		// form plain text from snippet
		String snapshot = item.getAbstractText().replace("<b>", " ")
				.replace("</b>", " ").replace("  ", " ").replace("  ", " ");

		String snapshotMarked = snapshot.replace("...",
				" _should_find_orig_ . _should_find_orig_");
		List<String> fragments = TextProcessor.splitToSentences(snapshotMarked);
		List<String> allFragms = new ArrayList<String>();
		allFragms.addAll(fragments);

		List<String> sents = new ArrayList<String>();
		String downloadedPage;
		try {
			if (snapshotMarked.length() != snapshot.length()) {
				downloadedPage = pFetcher.fetchPage(item.getUrl());
				if (downloadedPage != null && downloadedPage.length() > 100) {
					item.setPageContent(downloadedPage);
					String pageContent = Utils.fullStripHTML(item.getPageContent());
					pageContent = GeneratedSentenceProcessor
							.normalizeForSentenceSplitting(pageContent);
					pageContent = pageContent.trim().replaceAll("  [A-Z]", ". $0")// .replace("  ",
							// ". ")
							.replace("..", ".").replace(". . .", " ").trim(); // sometimes
					// html breaks
					// are converted
					// into ' ' (two
					// spaces), so
					// we need to
					// put '.'
					sents = TextProcessor.splitToSentences(pageContent);

				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			System.err
			.println("Problem downloading  the page and splitting into sentences");
			return item;
		}

		for (String fragment : allFragms) {
			String followSent = null;
			if (fragment.length() < 50)
				continue;
			String pageSentence = "";
			// try to find original sentence from webpage
			if (fragment.indexOf("_should_find_orig_") > -1 && sents != null
					&& sents.size() > 0)
				try {
					String[] mainAndFollowSent = getFullOriginalSentenceFromWebpageBySnippetFragment(
							fragment.replace("_should_find_orig_", ""), (String[])sents.toArray(new String[]{}));
					pageSentence = mainAndFollowSent[0];
					followSent = mainAndFollowSent[1];

				} catch (Exception e) {

					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			else
				// or get original snippet
				pageSentence = fragment;
			if (pageSentence != null)
				pageSentence = pageSentence.replace("_should_find_orig_", "");
			String pageSentenceProc = GeneratedSentenceProcessor
					.acceptableMinedSentence(pageSentence);
			if (pageSentenceProc != null) {
				pageSentenceProc = GeneratedSentenceProcessor
						.processSentence(pageSentenceProc);
				if (followSent != null) {
					pageSentenceProc += " "
							+ GeneratedSentenceProcessor.processSentence(followSent);
				}

				pageSentenceProc = Utils.convertToASCII(pageSentenceProc);
				Fragment f = new Fragment(pageSentenceProc, 1);
				f.setSourceURL(item.getUrl());
				f.fragment = fragment;
				result.add(f);
				System.out.println("Accepted sentence: " + pageSentenceProc
						+ "| with title= " + title);
				System.out.println("For fragment = " + fragment);
			} else
				System.out
				.println("Rejected sentence due to wrong area at webpage: "
						+ pageSentence);
		} 


		item.setFragments(result);
		return item;
	}

	public HitBase formTextFromOriginalPageGivenSnippet(HitBase item) {

		String[] sents = extractSentencesFromPage(item.getUrl());

		String title = item.getTitle().replace("<b>", " ").replace("</b>", " ")
				.replace("  ", " ").replace("  ", " ");
		// generation results for this sentence
		List<String> result = new ArrayList<String>();
		// form plain text from snippet
		String snapshot = item.getAbstractText().replace("<b>", " ")
				.replace("</b>", " ").replace("  ", " ").replace("  ", " ").replace("\"", "").replace("“", "");

		String snapshotMarked = snapshot.replace(" ...", ".");
		List<String> fragments = TextProcessor.splitToSentences(snapshotMarked);
		if (fragments.size()<3 && StringUtils.countMatches(snapshotMarked, ".")>1){
			snapshotMarked = snapshotMarked.replace("..", "&").replace(".", "&");
			String[] fragmSents = snapshotMarked.split("&");
			fragments = Arrays.asList(fragmSents);
		}

		for (String f : fragments) {
			String followSent = null;
			if (f.length() < 50)
				continue;
			String pageSentence = "";
			// try to find original sentence from webpage

			try {
				String[] mainAndFollowSent = getFullOriginalSentenceFromWebpageBySnippetFragment(
						f, sents);
				pageSentence = mainAndFollowSent[0];
				followSent = mainAndFollowSent[1];
				if (pageSentence!=null)
					result.add(pageSentence);
				else {
					result.add(f);
					LOG.info("Could not find the original sentence \n"+f +"\n in the page " );
				}
				//if (followSent !=null)
				//	result.add(followSent);
			} catch (Exception e) {

				e.printStackTrace();
			}
		}
		item.setOriginalSentences(result);
		return item;
	}

	public  List<String> cleanListOfSents(List<String> sents) {
		List<String> sentsClean = new ArrayList<String>();
		for (String s : sents) {
			if (s == null || s.trim().length() < 30 || s.length() < 20)
				continue;
			sentsClean.add(s);
		}
		return sentsClean;
	}



	private String[] removeDuplicates(String[] hits)
	{
		StringDistanceMeasurer meas = new StringDistanceMeasurer();

		List<Integer> idsToRemove = new ArrayList<Integer>();
		List<String> hitsDedup = new ArrayList<String>();
		try
		{
			for (int i = 0; i < hits.length; i++)
				for (int j = i + 1; j < hits.length; j++)
				{
					String title1 = hits[i];
					String title2 = hits[j];
					if (StringUtils.isEmpty(title1) || StringUtils.isEmpty(title2))
						continue;
					if (meas.measureStringDistance(title1, title2) > 0.7)
					{
						idsToRemove.add(j); // dupes found, later list member to
						// be deleted
					}
				}
			for (int i = 0; i < hits.length; i++)
				if (!idsToRemove.contains(i))
					hitsDedup.add(hits[i]);
			if (hitsDedup.size() < hits.length)
			{
				System.out.println("Removed duplicates from relevant search results, including "
						+ hits[idsToRemove.get(0)]);
			}
		}
		catch (Exception e)
		{
			System.out.println("Problem removing duplicates from relevant images");
		}

		return hitsDedup.toArray(new String[0]);

	}

	private String[] extractSentencesFromPage(String url)
	{

		int maxSentsFromPage= 100;
		List<String[]> results = new ArrayList<String[]>();

		String downloadedPage = pFetcher.fetchPage(url, 20000);
		if (downloadedPage == null || downloadedPage.length() < 100)
		{
			return null;
		}

		String pageOrigHTML = pFetcher.fetchOrigHTML(url);

		downloadedPage= downloadedPage.replace("     ", "&");
		downloadedPage = downloadedPage.replaceAll("(?:&)+", "#");
		String[] sents = downloadedPage.split("#");
		List<TextChunk> sentsList = new ArrayList<TextChunk>();
		for(String s: sents){
			s = s.trim().replace("  ", ". ").replace("..", ".").replace(". . .", " ")
					.replace(": ", ". ").replace("- ", ". ").
					replace (". .",".").trim();
			sentsList.add(new TextChunk(s, s.length()));
		}

		Collections.sort(sentsList, new TextChunkComparable());
		String[] longestSents = new String[maxSentsFromPage];
		int j=0;
		int initIndex = sentsList.size()-1 -maxSentsFromPage;
		if (initIndex<0)
			initIndex = 0;
		for(int i=initIndex; i< sentsList.size() && j<maxSentsFromPage ; i++){
			longestSents[j] = sentsList.get(i).text;
			j++;
		}

		sents = cleanSplitListOfSents(longestSents);

		//sents = removeDuplicates(sents);
		//sents = verifyEnforceStartsUpperCase(sents);

		return sents;
	}
	
	protected String[] cleanSplitListOfSents(String[] longestSents){
	float minFragmentLength = 40, minFragmentLengthSpace=4;

	List<String> sentsClean = new ArrayList<String>();
	for (String sentenceOrMultSent : longestSents)
	{
		if (sentenceOrMultSent==null || sentenceOrMultSent.length()<20)
			continue;
		if (MinedSentenceProcessor.acceptableMinedSentence(sentenceOrMultSent)==null){
			System.out.println("Rejected sentence by GeneratedSentenceProcessor.acceptableMinedSentence = "+sentenceOrMultSent);
			continue;
		}
		// aaa. hhh hhh.  kkk . kkk ll hhh. lll kkk n.
		int numOfDots = sentenceOrMultSent.replace('.','&').split("&").length;
		float avgSentenceLengthInTextPortion = (float)sentenceOrMultSent.length() /(float) numOfDots;
		if ( avgSentenceLengthInTextPortion<minFragmentLength)
			continue;
		// o oo o ooo o o o ooo oo ooo o o oo
		numOfDots = sentenceOrMultSent.replace(' ','&').split("&").length;
		avgSentenceLengthInTextPortion = (float)sentenceOrMultSent.length() /(float) numOfDots;
		if ( avgSentenceLengthInTextPortion<minFragmentLengthSpace)
			continue;

		List<String> furtherSplit = TextProcessor.splitToSentences(sentenceOrMultSent);
		
		// forced split by ',' somewhere in the middle of sentence
		// disused - Feb 26 13
		//furtherSplit = furtherMakeSentencesShorter(furtherSplit);
		furtherSplit.remove(furtherSplit.size()-1);
		for(String s : furtherSplit){
			if (s.indexOf('|')>-1)
				continue;
			s = s.replace("<em>"," ").replace("</em>"," ");
			s = Utils.convertToASCII(s);
			sentsClean.add(s);
		}
	}

	return (String[]) sentsClean.toArray(new String[0]);
}
	private String[] verifyEnforceStartsUpperCase(String[] sents) {
		for(int i=0; i<sents.length; i++){
			String s = sents[i];
			s = StringUtils.trim(s);
			String sFirstChar = s.substring(0, 1);
			if (!sFirstChar.toUpperCase().equals(sFirstChar)){
				s = sFirstChar.toUpperCase()+s.substring(1);
			}
			sents[i] = s;
		}
		return sents;
	}

	private List<String> cleanProductFeatures(List<String> productFeaturesList) {
		List<String> results = new ArrayList<String>();
		for(String feature: productFeaturesList){
			if (feature.startsWith("Unlimited Free") || feature.startsWith("View Larger") || feature.startsWith("View Larger") || feature.indexOf("shipping")>0)
				continue;
			results.add(feature);
		}
		return results;
	}
	public class TextChunk {
		public TextChunk(String s, int length) {
			this.text = s;
			this.len = length;
		}
		public String text;
		public int len;
	}

	public class TextChunkComparable implements Comparator<TextChunk>
	{
		public int compare(TextChunk ch1, TextChunk ch2)
		{
			if (ch1.len>ch2.len)
				return 1;
			else if (ch1.len<ch2.len)
				return  -1;
			else return 0;

		}
	}

}

/*

Besides, the hotel's host of recreational ... non smoking rooms, balcony/terrace, internet ... all tastefully furnished to provide comforts such as internet access ...
null
null
null
[Besides, the hotel's host of recreational. non smoking rooms, balcony/terrace, internet. all tastefully furnished to provide comforts such as internet access.]
null
[]
                                                                                                                                                                                                                                               AGODA                                 smarter hotel booking                                                                                                                                               New to Agoda? Register Now!                                                                                                                  English                                                                                                                                                                                                  My Account                                                          (sign in)                                                                                                                                                                                                                                                                                                                                                                                                                           Home                     Hotels:                                                                >                                                                                                                                                    World                     290,654                                                                >                                                                                                                                                    Asia                     47,857                                                                >                                                                                                                                                    Philippines                     2,132                                                                >                                                                                                                                                    Puerto Galera                     72                                                                >                                                                                                                                                                         Shark Cave                                                                                                                                                                                                                                                                                                                                Search hotels near Shark Cave                                                                                                     Check-in:                                                                                                                      Depart:                                                         Calendar                     Nights:          -12345678910111213141516171819202122232425262728293030+                                     Check-out:                                                                                                                          agoda price guarantee                                                      lowest rate guarantee       We want you to pay the lowest possible rate for your room. If you have reserved a hotel room through Agoda, and then show us that you could book the same room, for the same dates, at a lower rate, that is viewable and bookable on another website, we will either match that rate or beat it.  Click this link to find out more.                                          We want you to pay the lowest possible rate for your room. If you have reserved a hotel room through Agoda, and then show us that you could book the same room, for the same dates, at a lower rate, that is viewable and bookable on another website, we will either match that rate or beat it.  Click this link to find out more.                                                                                                                                                                                                                          In and around Puerto Galera                                                                                                                                         Banks and Currency Exchange                                                                                                Villa Sabang Money ChangerAllied Savings BankMax BankRural Bank of Puerto Galera                                                                                                                                                  Show 4 most popular                                                                                                                                                        Bars                                                                                                Hang Out BarTipzy Naku Bar                                                                                                                                                                                                                                                                                                          Beaches                                                                                                White BeachSabang BeachsBig La LagunaSmall La Laguna BeachCoco BeachTalipanan BeachHaligi BeachLong BeachAninuan BeachBalatero BeachDulangan BeachEncenada BeachHundura BeachSinandigan BeachTabinay Beach                                                                                                                                                  Show 15 most popular                                                                                                                                                        Botanical Gardens and Zoos                                                                                                Coral GardenParadise Reptile ZooVilla Masalimbo Forest Park                                                                                                                                                  Show 3 most popular                                                                                                                                                        Colleges and Universities                                                                                                Holy Child MontessoriMangyan School and VillagePrince of Peace CollegePuerto Galera AcademyPuerto Galera CentralSabang Elementary School                                                                                                                                                  Show 6 most popular                                                                                                                                                        Entertainment                                                                                                Fish Bowl                                                                                                                                                                                                                                                                                                          Golf Courses                                                                                                The Ponderosa Golf and Country Club                                                                                                                                                                                                                                                                                                          Islands                                                                                                Alibatan IslandBoquete IslandMedio IslandSibalat Island                                                                                                                                                  Show 4 most popular                                                                                                                                                        Marina                                                                                                Muelle Cross                                                                                                                                                                                                                                                                                                           Monuments and Landmarks                                                                                                Manila ChannelLambingan FallsMt BalaticMt HalconMt MalasimboMt TalipananPaspasin FallsPlaza Iluminada San Jose                                                                                                                                                  Show 8 most popular                                                                                                                                                        Places of Worship                                                                                                Church of Immaculate ConcepcionIglesia ni Cristo                                                                                                                                                                                                                                                                                              ...
null
Shark Cave Puerto Galera Hotels - BEST HOTEL RATES Near Puerto ...
http://www.agoda.com/asia/philippines/puerto_galera/attractions/hotels_near_shark_cave.html

 */