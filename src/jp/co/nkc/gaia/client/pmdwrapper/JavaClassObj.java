package jp.co.nkc.gaia.client.pmdwrapper;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Map;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.MethodDeclaration;

public class JavaClassObj implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String className = "";
	
	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
	
//	Map<methodName,modifier>
//	private Map<String,EnumSet<Modifier>> methodMap;
	private Map<MethodDeclaration,EnumSet<Modifier>> methodMap;

	public Map<MethodDeclaration, EnumSet<Modifier>> getMethodMap() {
		return methodMap;
	}
	public void setMethodMap(Map<MethodDeclaration, EnumSet<Modifier>> methodMap) {
		this.methodMap = methodMap;
	} 
	
}
