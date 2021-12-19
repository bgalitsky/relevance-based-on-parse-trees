package opennlp.tools.enron_email_recognizer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class EmailNormalizer {
	protected ArrayList<File> queue = new ArrayList<File>();
	public static String subjectLinesFileName = "subjectLines.txt";
	
	protected List<String> emailSubjectsList = new ArrayList<String>();
	
	protected void addFilesPos(File file) {

		if (!file.exists()) {
			System.out.println(file + " does not exist.");
		}
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				addFilesPos(f);
				//System.out.println(f.getName());
			}
		} else {
			queue.add(file);
		}
	}
	
	public static final String[] headers = new String[] {
		"Message-ID:",
	"Date:",
	"From:",
	"To:",
	//"Subject:",
	"Mime-Version:",
	"Content-T",
	"X-From:",
	"X-To:",
	"X-cc:",
	"X-bcc:",
	"X-Folder:",
	"X-Origin:",
	"X-FileName",
	"cc:",
	"----",
	};
	
	public static final String[] prohibitedStrings = new String[] {
		"@", "<", ">"
	};

	private String OrigFolder = "maildir", NewFolder = "data";

	
	
	public void normalizeAndWriteIntoANewFile(File f){
		String content="";
        try {
	        content = FileUtils.readFileToString(f);
        } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
		String[] lines = content.split("\n");
		StringBuffer buf = new StringBuffer();
		for(String l: lines){
			boolean bAccept = true;
			for(String h: headers){
				if (l.startsWith(h)){
					bAccept = false;
				}
				if (l.startsWith("Subject:")){
					l = l.substring(7);
					emailSubjectsList.add(l);
				}
			}
			for(String h: prohibitedStrings){
				if (l.indexOf(h)>0){
					bAccept = false;
				}
			}
			if (bAccept)
				buf.append(l+"\n");
		}
		String directoryNew = f.getAbsolutePath().replace(OrigFolder, NewFolder);
		try {
			String fullFileNameNew = directoryNew +"txt";
	        FileUtils.writeStringToFile(new File(fullFileNameNew), buf.toString());
        } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
	}
	
	public void normalizeDirectory(File f){
		addFilesPos(f);
		for(File e: queue){
			normalizeAndWriteIntoANewFile(e);
		}
		try {
			FileUtils.writeLines(new File(subjectLinesFileName="subjectLines.txt"), emailSubjectsList, true);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		EmailNormalizer nrm = new EmailNormalizer();
		nrm.normalizeDirectory(new File("/Users/bgalitsk/Downloads/maildir"));
	}
}
