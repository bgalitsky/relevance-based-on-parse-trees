package opennlp.tools.doc2dialogue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;

public class DescriptiveParagraphFromDocExtractor {
	// it needs its own tika transaction, reads the file from 
	// filesystem and extracts text altogether, not by lines
	protected static  Tika tika = new Tika();
	private static int MIN_PARA_LENGTH = 200, 
			MIN_NUM_WORDS=15, 
			MAX_PARA_LENGTH = 500, 
			TEXT_PORTION_FOR_ANALYSIS = 20000, 
			MAX_PARA_OUTPUT=20;
	public static final Log logger = LogFactory.getLog(DescriptiveParagraphFromDocExtractor.class);

	public static List<String> getLongParagraphsFromFile(File f) {
		List<String> results = new ArrayList<String>();
		String text = "";
		try {
			try { // we already verified, calling thic func, that file size is below a few megs
				text = tika.parseToString(f);
			} catch (TikaException e) {
				logger.error(" Failed to process file by Tika  "+e.getMessage());
			} 
			if (text.length()>TEXT_PORTION_FOR_ANALYSIS)
				text = text.substring(0, TEXT_PORTION_FOR_ANALYSIS);
			float avgSentSizeThr = (float)MIN_PARA_LENGTH/4f; //2f
			// we try to split text into paragraphs in various ways, till we succeed
			String[] portions = text.split("\\.\\n");
			if (portions.length<2)
				portions = text.split("\\n\\n");
			if (portions.length<2)
				portions = text.split("\\n \\n");
			for(String p: portions){
				float avgSentSize = (float)p.length()/(float)p.split("\\n\\n").length;

				if (p.length()> MIN_PARA_LENGTH && p.split(" ").length>MIN_NUM_WORDS &&
						avgSentSize > avgSentSizeThr &&  p.length() < MAX_PARA_LENGTH){
					results.add(normalizePara(p));
					if (results.size()>MAX_PARA_OUTPUT)
						break;
				}
			}
		} catch (IOException e) {
			logger.error(" Failed to process file by Tika and extract paragraphs for extended parse trees "+e.getMessage());
		}
		if (results.size()<1){
			logger.error("Failed to extract text from "+f.getName());
		}

		return results;
	}

	// for training we need to extract init part of a doc
	public static String getFirstParagraphFromFile(File f) {

		String text = "";
		try {
			try {
				text = tika.parseToString(f);
			} catch (TikaException e) {
				logger.error(" Failed to process file by Tika @ getFirstParagraphFromFile  "+e.getMessage());
			} 
			//text = FileUtils.readFileToString(f, null);
			if (text.length()>TEXT_PORTION_FOR_ANALYSIS)
				text = text.substring(0, TEXT_PORTION_FOR_ANALYSIS);
			float avgSentSizeThr = (float)MIN_PARA_LENGTH/4f; //2f
			String[] portions = text.split("\\.\\n");
			for(String p: portions){
				float avgSentSize = (float)p.length()/(float)p.split("\\n\\n").length;

				if (p.length()> MIN_PARA_LENGTH && p.split(" ").length>MIN_NUM_WORDS &&
						avgSentSize > avgSentSizeThr &&  p.length() < MAX_PARA_LENGTH){
					return normalizePara(p);
				}
			}
		} catch (Exception e) {
			logger.error(" Failed to process file by Tika and extract paragraphs for extended parse trees @ getFirstParagraphFromFile"+e.getMessage());
		}
		// if not a suitable paragraph found, return the whole text
		if (text.length()>150)
			text = text.substring(0, 150);
		return text;
	}

	private static String normalizePara(String p){
		p = p.replaceAll("\\n", " ").replaceAll("\\.\\.", " ").replaceAll("  ", " ");
		p = p.replaceAll("[^A-Za-z0-9 _\\.,\\!]", "");
		return p;
	}
}
