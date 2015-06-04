package opennlp.tools.fca;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class FcaReader {
	
	ArrayList<String> obNames = null;
	ArrayList<String> atNames = null;
	int [][] binContext = null;
	int objectsNumber = 0;
	int attributesNumber = 0;
	
	public void ReadContextFromCxt(String filename) throws FileNotFoundException, IOException{

		obNames = new ArrayList<String>();
		atNames = new ArrayList<String>();
					
		BufferedReader br = new BufferedReader(new FileReader(filename));
		try	{
		    String line;		    
		    br.readLine(); //B
		    br.readLine();
		    objectsNumber = Integer.parseInt(br.readLine());
		    attributesNumber = Integer.parseInt(br.readLine());
		    br.readLine();	
	    
		    binContext = new int [objectsNumber][attributesNumber];
		    
		    for (int i=0;i<objectsNumber;i++){
		    	obNames.add(br.readLine());
		    }
		    
		    for (int i=0;i<attributesNumber;i++){
		    	atNames.add(br.readLine());
		    }
		    
		    int i=0;
		    while ((line = br.readLine()) != null) {
		    	for (int j = 0; j<line.length();j++){
		    		if (line.charAt(j)=='.'){
		    			binContext[i][j] = 0;
		    		}		    			
		    		else  
			    		binContext[i][j] = 1;
			    }
		    	i+=1;	    	
		    }	    
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public int getAttributesCount(){
		return attributesNumber;
	}
	
	public int getObjectsCount(){
		return objectsNumber;
	}
	
	public int[][] getBinaryContext(){
		return binContext;
	}
	
	
	public static void main(String []args) throws FileNotFoundException, IOException{
		
		FcaReader loader = new FcaReader();
		loader.ReadContextFromCxt("C://Users/Tanya/Desktop/�����/1 �������/������������� ��������� � ������� ������/�������/sports.cxt");
		
	}
	

}
