package opennlp.tools.chatbot.sequence_learner;
import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.io.FileOutputStream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import opennlp.tools.jsmlearning.ProfileReaderWriter;

public class WebScraper {

    private static final int MAX_FILES = 50000;
    private static final int MIN_RESPONSES = 2;
    List<String[]> report = new ArrayList<String[]>();
    public static String resourceDir = System.getProperty("user.dir") + File.separator + "src/test/resources/carPros/";

    // boolean indicates if operation was successful
    public boolean writeResultToFile(Element result, int fileNo) {
        String asker = result.child(1).child(0).text().toLowerCase();
        Document question = null;
        try {
            question = Jsoup.connect(result.child(0).absUrl("href")).get();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(resourceDir+"example" + fileNo + ".txt"), "utf-8"))) {
            String text = "";
            for (Element user : question.body().getElementsByClass("user")) {
                String response = user.parent().child(1).child(0).text();
                String responder = user.child(1).child(0).text().toLowerCase();
                if (responder.equals(asker)) {
                    response = "User: " + response;
                } else response = "Bot: " + response;
                System.out.println(response);
                text += response + "\n";
            }
            writer.write(text);
          
           
			report.add(new String[] { text });
			ProfileReaderWriter.writeReport(report, "carPros.csv");
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
        WebScraper queryRunner = new WebScraper();
        Document doc = null;
        String url = "https://www.2carpros.com";
        int fileNo = 1;
        while (true) {
            System.out.println("Connecting to " + url);
            try {
                doc = Jsoup.connect(url).get();

            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
            Elements results = doc.body().getElementById("results").getElementsByClass("p-user");
            for (Element result : results) {
                Element askedBy = result.child(1);
                Elements askedByElements = askedBy.getElementsByClass("badgebg");

                // Make sure question has answers, and at least MIN_RESPONSES of them
                if (askedByElements.isEmpty() || !askedByElements.get(0).text().contains("ANSWER")) continue;
                if (Character.getNumericValue(askedByElements.get(0).text().charAt(0)) >= MIN_RESPONSES) {
                    if (queryRunner.writeResultToFile(result, fileNo)) fileNo++; //successful write
                    if (fileNo > MAX_FILES) return;
                    System.out.println("\n");
                }
            }
            System.out.println("\n");
            url = doc.getElementsByClass("next_page").get(0).child(0).absUrl("href");
            if (url.isEmpty()) break; //reached last page
        }
    }
}
