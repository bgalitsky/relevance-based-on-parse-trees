package opennlp.tools.enron_email_recognizer;

import opennlp.tools.jsmlearning.ProfileReaderWriter;
import opennlp.tools.parse_thicket.ParseCorefsBuilder;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.*;

public class EmailSubjectLineSentimentAssessor {
	private ParseCorefsBuilder sentimentAssessor = ParseCorefsBuilder.getInstance();
	
	
	String currentDirWithFile = System.getProperty("user.dir")+"/"+EmailNormalizer.subjectLinesFileName;
	private List<String[]> report = new ArrayList<String[]>();
	public void processEmailSubjectLine(){
		List<String> lines = null;
		try {
			 lines = FileUtils.readLines(new File(currentDirWithFile) );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for(String line:lines){
			line = line.substring(1);
			if (line.length()<20)
				continue;
			int sent = sentimentAssessor.getSentiment(line);
			String[] repLine = new String[]{line, sent+""};
			report.add(repLine);
			ProfileReaderWriter.writeReport(report, "enronSubjLinesSentiments.csv");
		}
		
		ProfileReaderWriter.writeReport(report, "enronSubjLinesSentiments.csv");
	}
	
	public static void main(String[] args){
		EmailSubjectLineSentimentAssessor assessor = new EmailSubjectLineSentimentAssessor();
		assessor.processEmailSubjectLine();
	}

}
