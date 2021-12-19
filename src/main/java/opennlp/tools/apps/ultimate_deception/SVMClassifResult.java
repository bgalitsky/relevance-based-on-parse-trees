package opennlp.tools.apps.ultimate_deception;

import java.util.ArrayList;
import java.util.List;

public class SVMClassifResult {
	Boolean boolResult = null;
	List<Float> arr;
	Float avg;
	

	public SVMClassifResult(Boolean boolResult,
	List<Float> arr,
	Float avg) {
		this.boolResult=false;
		this.arr=new ArrayList<Float>();
		this.avg=0f;
	}


	public SVMClassifResult() {
		// TODO Auto-generated constructor stub
	}


	public Boolean getBoolResult() {
		return boolResult;
	}


	public List<Float> getArr() {
		return arr;
	}

	public void setArr(List<Float> arr) {
		this.arr = arr;
	}

	public Float getAvg() {
		return avg;
	}

	public void setAvg(Float avg) {
		this.avg = avg;
	}

	public void setBool(Boolean b) {
		boolResult = b;
		
	}


	public void setArray(List<Float> vals) {
		arr=vals;		
	}

}
