package jp.co.nkc.gaia.client.pmdwrapper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class JavaClassInfo implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	List<JavaClassObj> classObjs = new ArrayList<JavaClassObj>();
	
	public JavaClassInfo(List<JavaClassObj> classObjs) {
		
		this.classObjs = classObjs;
	}
	public JavaClassObj getJavaClassObj(String className) {
		JavaClassObj classObj = null;
		
		for(JavaClassObj obj:classObjs){
			if(obj.getClassName().equals(className)) {
				classObj = obj;
			}
		}
		return classObj;
	}
}
