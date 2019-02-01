package jp.co.nkc.gaia.client.pmdwrapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.github.javaparser.JavaParser;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.SimpleName;
import com.sun.org.apache.xerces.internal.parsers.DOMParser;

import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.cli.PMDCommandLineInterface;
import net.sourceforge.pmd.cli.PMDParameters;

public class PMDWrapperModel implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static final String ROOT_DIR = System.getProperty("java.io.tmpdir");
	
	private static final String TMP_DIR = ROOT_DIR + "\\PMD_tmp\\";
	private static final String PMD_RULESET_DIR = TMP_DIR + "ruleSet\\";
	private static final String TARGET_HOME_DIR = "target\\";

	String[] arg;
	List<JavaClassObj> classInfos;
	private List<String> pathList;
	private static final String FILE_LIST_ARG = "-filelist";
	private static final String FILE_LIST = TMP_DIR + "target\\target_src_file.txt";
	
	private static final String OUT_PUT_FILE_FORMAT_ARG = "-f";
	private static final String OUT_PUT_FILE_FORMAT = "xml";
	
	private static final String ROULE_SET_XML_ARG = "-R";
	private static final String ROULE_SET_XML = PMD_RULESET_DIR + "PrimeGaia_PMD_ruleset.xml";
	
	private static final String ROULE_SET_LOCATION = "/jp/co/nkc/gaia/client/pmdwrapper/config/PrimeGaia_PMD_ruleset.xml";
	
	private static final String ANALYZE_LANGUAGE_ARG = "-l";
	private static final String ANALYZE_LANGUAGE = "java";
	
	private static final String TEMP_FILE_LOCATION_ARG = "-r";
	private static final String TEMP_FILE_LOCATION = TMP_DIR +  "temp.xml";
	
	private static final String TARGET_FILE_LOCATION = TMP_DIR + "target\\target_src_file.txt";
	private static final String RESULT_FILE_LOCATION = "PMD_Result_";
	
	private static final Logger logger = Logger.getLogger(PMDWrapperModel.class);
	
	public void analyze(List<File> fileList){
		logger.info("解析開始");
		boolean ret = false;
		File dir = new File(TMP_DIR);
		if(!dir.exists()) {
			dir.mkdir();
		}
		File ruleSetDir = new File(PMD_RULESET_DIR);
		if(!ruleSetDir.exists()) {
			ruleSetDir.mkdir();
		}
		File ruleXML = new File(ROULE_SET_XML);
		if(!ruleXML.exists()) {
			try {
				URL url = this.getClass().getResource(ROULE_SET_LOCATION);
				URLConnection connection = url.openConnection();
				InputStream is = connection.getInputStream();
				Files.copy(is, ruleXML.toPath());
			} catch (IOException e) {
				logger.info(e.getCause() + "ファイル操作エラー" + ROULE_SET_LOCATION);
			}
		}

		pathList = new ArrayList<String>();
		for(File file:fileList) {
			pathList.add(file.getPath());
		}
		
		createFileList(fileList);
		initArg();
		execPMD();
		ret =  xPathConvert();
		File file = new File(TEMP_FILE_LOCATION);
		try {
			Files.delete(file.toPath());
		} catch (IOException e) {
			logger.info(e.getCause() + TEMP_FILE_LOCATION + "ファイル削除エラー");
		}
		if(ret) {
			JOptionPane.showMessageDialog(null, "Analysis succeeded :) ");
		}else {
			JOptionPane.showMessageDialog(null, "Analysis failed :( ");
		}
	}

	private void initArg() {
		logger.info("argの作成");
		arg = new String[] {
				 FILE_LIST_ARG,
				 FILE_LIST,
				 OUT_PUT_FILE_FORMAT_ARG,
				 OUT_PUT_FILE_FORMAT,
				 ROULE_SET_XML_ARG,
				 ROULE_SET_XML,
				 ANALYZE_LANGUAGE_ARG,
				 ANALYZE_LANGUAGE,
				 TEMP_FILE_LOCATION_ARG,
				 TEMP_FILE_LOCATION,
		};
	}
	
	private void execPMD(){
		logger.info("PMDの実行");
		final PMDParameters params = PMDCommandLineInterface.extractParameters(new PMDParameters(), arg, "pmd");
		final PMDConfiguration configuration = params.toConfiguration();
		PMD.doPMD(configuration);
	}
	
	private static final String DELETE_ATTR1 = "begincolumn";
	private static final String DELETE_ATTR2 = "beginline";
	private static final String DELETE_ATTR3 = "endcolumn";
	private static final String DELETE_ATTR4 = "endline";
	private static final String DELETE_ATTR5 = "externalInfoUrl";
	private static final String DELETE_ATTR6 = "priority";
	private static final String DELETE_ATTR7 = "ruleset";
	
	private boolean xPathConvert(){
		logger.info("XML再構築");
		boolean ret = false;
		Document doc = null;
		//ASTでクラスのツリー解析
		JavaClassInfo classInfo = analyzeClassInfo();
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);

			DOMParser parser = new DOMParser();
			parser.parse(TEMP_FILE_LOCATION);
			doc = parser.getDocument();
			XPathFactory xpathFactory = XPathFactory.newInstance();
			XPath xpath = xpathFactory.newXPath();
			xpath.setNamespaceContext(new PMDNameSpaceContext());
			NodeList violationNodelist = (NodeList)xpath.evaluate("//xsi:pmd/xsi:file/xsi:violation",doc,XPathConstants.NODESET);

			for(int i =0;i < violationNodelist.getLength();i ++) {
				Node violation = violationNodelist.item(i);
				
				Node item = violation.getAttributes().getNamedItem("beginline");
				int beginline = Integer.parseInt(item.getTextContent());
				
				//				type|method|value
				String textContent = violation.getTextContent();
				textContent = textContent.replace("\n", "");
				String[] content = textContent.split("\\|",0);

				Node type = doc.createElement("type");
				type.setTextContent(content[0]);

				if(content[1].equals("getSpaceName")) {
					System.out.println("");
				}
				
				Node method = doc.createElement("method");
				method.setTextContent(content[1]);

				Node value = doc.createElement("value");
				String[] valueStr = content[2].split(" ");
				value.setTextContent(valueStr[0]);

				Node scope = doc.createElement("scope");
				if(type.getTextContent().contentEquals("method")) {
					NamedNodeMap nodeMap = violation.getAttributes();
					Node classNode = nodeMap.getNamedItem("class");
					String className = classNode.getTextContent();
					if(className.contains("$")) {
						className = className.split("\\$", 0)[1];
					}
					JavaClassObj jcobj = classInfo.getJavaClassObj(className);
					if(jcobj != null) {
						String key = method.getTextContent();
						Node methodAttr = violation.getAttributes().getNamedItem("method");
						methodAttr.setTextContent(key);
						String[] keysss = key.split("\\(", 0);
						Map<MethodDeclaration,EnumSet<Modifier>> map = jcobj.getMethodMap(); 
						for(Entry<MethodDeclaration,EnumSet<Modifier>> entry:map.entrySet()) {
							Optional<Range> range = entry.getKey().getRange();
							MethodDeclaration mDeclaration = entry.getKey();
							if(range.isPresent() && 
									keysss[0].contentEquals(mDeclaration.getName().toString()) && 
									range.get().begin.line == beginline) {
								EnumSet<Modifier> val = entry.getValue();
								scope.setTextContent(getScopeString(val.toString()));
							}
						}
					}
				}
				
				
				violation.setTextContent("");
				NamedNodeMap namedMap = violation.getAttributes();
				namedMap.removeNamedItem(DELETE_ATTR1);
				namedMap.removeNamedItem(DELETE_ATTR2);
				namedMap.removeNamedItem(DELETE_ATTR3);
				namedMap.removeNamedItem(DELETE_ATTR4);
				namedMap.removeNamedItem(DELETE_ATTR5);
				namedMap.removeNamedItem(DELETE_ATTR6);
				namedMap.removeNamedItem(DELETE_ATTR7);
				
				Node result = violation.cloneNode(true);
				result.appendChild(scope);
				result.appendChild(type);
				result.appendChild(value);

				Node fileNode = violation.getParentNode();
				fileNode.replaceChild(result,violation);
			}

			doc.getChildNodes();
			
			ret = write(doc);

		} catch (SAXException | IOException | XPathExpressionException e) {
			logger.info(e.getCause());
		}
		return ret;
	}
	
	private boolean write(Document document) {
		logger.info("解析結果を書きだす");
		boolean ret = false;
		FileWriter fileWriter = null;
		try {
			File resultFile = selectSaveDir();
			if(resultFile != null) {
				Transformer transformer = null;
				
				if(!resultFile.exists()) {
					ret = resultFile.createNewFile();
				}
				// Transformerインスタンスの生成

				fileWriter = new FileWriter(resultFile);
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

				transformer = transformerFactory.newTransformer();

				// Transformerの設定
				transformer.setOutputProperty("indent", "yes"); //改行指定
				transformer.setOutputProperty("encoding", "UTF-8"); // エンコーディング

				transformer.transform(new DOMSource(document), new StreamResult(fileWriter));
				transformer = null;
			}else {
				int option = JOptionPane.showConfirmDialog(null,"解析結果は保存されませんがよろしいですか？","保存確認",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
				if(option == JOptionPane.YES_OPTION) {
					return false;
				}else if(option == JOptionPane.NO_OPTION){
					write(document);
				}
			}
		} catch (IOException | TransformerException e ) {
			logger.info(e.getCause() + "解析結果ファイル出力エラー"); 
		}finally {
			if(fileWriter != null) {
				try {
					fileWriter.close();
				} catch (IOException e) {
					logger.info(e.getCause() + "ファイルクローズエラー");
				}
			}
		}
		return ret;
	}

	private static final String RESULT_DIR = "\\result\\";
	private File selectSaveDir() {
		logger.info("ファイル選択ダイアログ開く");
		String currentDir = new File(".").getAbsoluteFile().getParent();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		File dir = new File(currentDir + RESULT_DIR);
		if(!dir.exists()) {
			dir.mkdir();
		}
		
		File file = new File(currentDir + RESULT_DIR + RESULT_FILE_LOCATION + sdf.format(Calendar.getInstance().getTimeInMillis()) + ".xml");
		
		
		File selectedFile = null;
		JFileChooser fileChooser = new JFileChooser(dir);
		fileChooser.setSelectedFile(file);
		int selected = fileChooser.showSaveDialog(null);
		if (selected == JFileChooser.APPROVE_OPTION){
			selectedFile = fileChooser.getSelectedFile();
	      }else if (selected == JFileChooser.CANCEL_OPTION){
	    	  selectedFile = null;
	      }else if (selected == JFileChooser.ERROR_OPTION){
	    	  selectedFile =null;
	      }
		
		return selectedFile;
	}
	
	private String getScopeString(String str) {
		String scope = "";
		
		if(str.contentEquals("[]")) {
			scope = "protected";
		}else if(str.contains("PRIVATE")) {
			scope = "private";
		}else if(str.contains("PROTECTED")) {
			scope = "protected";
		}else if(str.contains("PUBLIC")) {
			scope = "public";
		}
		
		return scope;
	}
	
	private JavaClassInfo analyzeClassInfo() {
		classInfos = new ArrayList<JavaClassObj>();
			for(String path:pathList) {
				classInfos.add(createClassObj(path,null));
			}
		return new JavaClassInfo(classInfos);
	}
	
	private JavaClassObj createClassObj(String path,com.github.javaparser.ast.Node node) {
		logger.info("ASTでTree解析" + path);
		JavaClassObj classObj = new JavaClassObj();
		Map<MethodDeclaration, EnumSet<Modifier>> methodMap = new HashMap<MethodDeclaration, EnumSet<Modifier>>(); 
			Path source = Paths.get(path);
	        try {
	        	List<com.github.javaparser.ast.Node> classStructureNodeList = null;
	            if(path.length() > 0) {
	            	CompilationUnit unit = JavaParser.parse(source);
	            	classStructureNodeList = unit.getChildNodes();
	            }else if(node != null){
	            	classStructureNodeList = new ArrayList<com.github.javaparser.ast.Node>();
	            	classStructureNodeList.add(node);
	            }
	            for(com.github.javaparser.ast.Node csnl : classStructureNodeList) {
	            	analyzeNode(csnl,classObj,methodMap);
	            }
	        } catch (IOException e) {
	            logger.info(path + "ファイル操作エラー");
	        }
	        classObj.setMethodMap(methodMap);
		return classObj;
	}
	
	private void analyzeNode(com.github.javaparser.ast.Node csnl,JavaClassObj classObj,Map<MethodDeclaration, EnumSet<Modifier>> methodMap) {
		if(csnl instanceof ClassOrInterfaceDeclaration || csnl instanceof EnumDeclaration) {
			com.github.javaparser.ast.Node classOrInterfaceDeclaration =  csnl;
    		List<com.github.javaparser.ast.Node> classStructureNodeList2 = classOrInterfaceDeclaration.getChildNodes();
    		for(com.github.javaparser.ast.Node csnl2:classStructureNodeList2) {
    			if(csnl2 instanceof MethodDeclaration) {
    				MethodDeclaration method = (MethodDeclaration) csnl2;
    				EnumSet<Modifier> methodModi = method.getModifiers();
    				methodMap.put(method,methodModi);
    			}else if(csnl2 instanceof SimpleName){
    				SimpleName name = (SimpleName)csnl2;
    				classObj.setClassName(name.getIdentifier());
    			}else if(csnl2 instanceof ClassOrInterfaceDeclaration || csnl2 instanceof EnumDeclaration) {
    				classInfos.add(createClassObj("",csnl2));
    			}
    		}
    	}
	}
	
	private static final String PMD_URL = "http://pmd.sourceforge.net/report/2.0.0";
	private class PMDNameSpaceContext implements NamespaceContext{
		@Override
		public Iterator<String> getPrefixes(String prefixes) {
			if(prefixes == null) {
				throw new IllegalArgumentException();
			}else if(prefixes.equals(PMD_URL)){
				return Arrays.asList("xsi").iterator();
			}

			return null;
		}

		@Override
		public String getPrefix(String prefix) {
			if(prefix == null) {
				throw new IllegalArgumentException();
			}else if(prefix.equals(PMD_URL)){
				return "xsi";
			}
			return null;
		}

		@Override
		public String getNamespaceURI(String namespaceURI) {
			if(namespaceURI == null) {
				throw new IllegalArgumentException();
			}else if(namespaceURI.contentEquals("xsi")){
				return PMD_URL;
			}
			return null;
		}
	}

	private  boolean createFileList(List<File> fileList) {
		logger.info("ファイルリスト作るよ");
		File targetDir = new File(TMP_DIR + TARGET_HOME_DIR);
		if(!targetDir.exists()) {
			targetDir.mkdir();
		}
		
		
		boolean ret = false;
		File fl = new File(TARGET_FILE_LOCATION);
		//		該当ファイルパスにファイルが存在していた場合は削除
		//		ファイルリストの作成
		FileWriter fileWriter = null;
		//		ファイルの作成
		try {
			if(fl.exists()) {
				Files.delete(fl.toPath());
			}
			ret = fl.createNewFile();
			fileWriter = new FileWriter(fl, true);
			for(File file:fileList) {
				fileWriter.write(file.toString() + "\n");	
			}
		} catch (IOException e) {
			logger.info(e.getCause() + "ターゲットファイルリスト操作エラー");
		} finally {
			if(fileWriter != null) {
				try {
					fileWriter.close();
				} catch (IOException e) {
					logger.info(e.getCause() + "ファイルクローズエラー");
				}
			}
		}
		return ret;
	}
}
