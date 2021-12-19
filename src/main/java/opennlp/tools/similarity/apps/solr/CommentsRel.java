package opennlp.tools.similarity.apps.solr;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;

import javax.xml.bind.JAXBException;

import org.docx4j.XmlUtils;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.CommentsPart;
import org.docx4j.openpackaging.parts.WordprocessingML.EndnotesPart;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.wml.CTEndnotes;
import org.docx4j.wml.CTFtnEdn;
import org.docx4j.wml.Comments;

public class CommentsRel {
	   
	   private WordprocessingMLPackage wordMlPackage;
	   private MainDocumentPart mainPart;
	   private boolean relSet = false;
	   private org.docx4j.wml.ObjectFactory wmlObjectFactory;
	   private CommentsPart cmPart;

	   
	   public CommentsRel(WordprocessingMLPackage wordMLPack) {
	      this.wordMlPackage = wordMLPack;
	      wmlObjectFactory = new org.docx4j.wml.ObjectFactory();
	      setCommentRel();
	      cmPart = wordMlPackage.getMainDocumentPart().getCommentsPart();
	      mainPart = wordMLPack.getMainDocumentPart();
	   }

	   private void setCommentRel() {
	      if (!commentRelSet()) {
	         CommentsPart cp;
	         try {
	            cp = new CommentsPart();
	            // Part must have minimal contents
	            Comments comments = wmlObjectFactory.createComments();
	               cp.setJaxbElement(comments);            
	            
	            wordMlPackage.getMainDocumentPart().addTargetPart(cp);
	         } catch (InvalidFormatException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	         }
	      }
	   }

	   private boolean commentRelSet() {
	      return !(wordMlPackage.getMainDocumentPart().getCommentsPart() == null);
	   }

	   public void addNewComment(String author, String text) {
	      
//	      XMLGregorianCalendar xmlCal = new XMLGregorianCalendarImpl();  // You'll need to fix this!
	      
//	      CommentRangeEnd cRangeEnde = wmlObjectFactory.createCommentRangeEnd();
//	      CommentRangeStart cRangeStart = wmlObjectFactory
//	            .createCommentRangeStart();
	      Comments comment = wmlObjectFactory.createComments();
//	      Comments.Comment myCom = wm

	      org.docx4j.wml.Comments.Comment c = Context.getWmlObjectFactory().createCommentsComment();
	      System.out.println("test");
//	      comment.setParent(cmPart);
	      c.setAuthor(author);
//	      c.setDate(xmlCal);
	      cmPart.getJaxbElement().getComment().add(c);

	      System.out.println("test ende");
	   }

	//   WordprocessingMLPackage wordML;

	   public static void main(String args[]) throws IOException {

	      File document = new File("C:/workspace/TestSolr/mydoc.docx");

	      MainDocumentPart mDocPart;
	      try {
	        /* mlPackage = new WordprocessingMLPackage().load(new File(document.getCanonicalPath()));

	         mDocPart = mlPackage.getMainDocumentPart();

	         CommentsRel myComment = new CommentsRel(mlPackage);

	         myComment.addNewComment("MC","Text");
	         */
	      // Add an endnote
	         
	         WordprocessingMLPackage mlPackage = WordprocessingMLPackage.createPackage();
	         
	         // Setup endnotes part
	         EndnotesPart ep = new EndnotesPart();
	         CTEndnotes endnotes = Context.getWmlObjectFactory().createCTEndnotes();
	         ep.setJaxbElement(endnotes);
	         mlPackage.getMainDocumentPart().addTargetPart(ep);
	         
	         CTFtnEdn endnote = Context.getWmlObjectFactory().createCTFtnEdn();
	         endnotes.getEndnote().add(endnote);
	         
	         endnote.setId(BigInteger.ONE.add(BigInteger.ONE));
	         String endnoteBody = "<w:p xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" ><w:pPr><w:pStyle w:val=\"EndnoteText\"/></w:pPr><w:r><w:rPr><w:rStyle w:val=\"EndnoteReference\"/></w:rPr><w:endnoteRef/></w:r><w:r><w:t xml:space=\"preserve\"> An endnote</w:t></w:r></w:p>";
	         try {
				endnote.getEGBlockLevelElts().add( XmlUtils.unmarshalString(endnoteBody));
			} catch (JAXBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	         
	         // Add the body text referencing it
	         String docBody = "<w:p xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" ><w:r><w:t>the quick brown</w:t></w:r><w:r><w:rPr><w:rStyle w:val=\"EndnoteReference\"/></w:rPr><w:endnoteReference w:id=\"2\"/></w:r></w:p>";
	         
	         try {
				mlPackage.getMainDocumentPart().addParagraph(docBody);
			} catch (JAXBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	         
	         
	         
	         
	         
	         
	          mlPackage.save(new File("C:/workspace/TestSolr/mydoc.docx-OUT.docx"));
	      } catch (Docx4JException e) {
	         // TODO Auto-generated catch block
	         e.printStackTrace();
	      }

	   }
	}
