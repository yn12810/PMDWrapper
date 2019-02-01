package jp.co.nkc.gaia.client.pmdwrapper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

public class PMDWrapperController {

	private PMDWrapperUI analyzerUI;
	private PMDWrapperModel analyzerModel;
	private List<File> fileList;
	
	private static final String FILE_EXTENSION = ".java";
	private int count = 0;
	
	private static final Logger logger = Logger.getLogger(PMDWrapperController.class);
	
	public PMDWrapperController(String[] args) {
		if(args.length > 0) {
			List<File> files = new ArrayList<File>();
			for(int i = 0;i < args.length;i ++) {
				String arg = args[i];
				if(arg.contains(FILE_EXTENSION)) {
					files.add(new File(arg));
					count ++;
				}
			}
			
			fileList = files;
		}
	}

	public void startGUI() {
		createModel();
		createGUI();
		start();
	}

	public void startAnalyze() {
		createModel();
		int option = JOptionPane.showConfirmDialog(null, count + "個のファイルをPMD解析しますがよろしいでしょうか？","解析実行確認",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
		if(option == JOptionPane.YES_OPTION) {
			PMDWrapperUI ui = new PMDWrapperUI();
			ui.createDandDAnalyzeGUI();
			ui.setVisible(true);
			analyzerModel.analyze(fileList);
			ui.setEndLabel();
			ui.setVisible(false);
			ui.dispose();
		}else if(option == JOptionPane.NO_OPTION){
			int option2 = JOptionPane.showConfirmDialog(null,"PMDの解析を終了します。","終了確認",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
			if(option2 == JOptionPane.NO_OPTION){
				startAnalyze();
			}
		}
	}
	
	public void start() {
		logger.info("start");
		analyzerUI.setVisible(true);
	}

	private void createGUI() {
		logger.info("GUI構築");
		analyzerUI = new PMDWrapperUI("Programming Mistake Detector(PMD)");
		analyzerUI.setModel(analyzerModel);
	}

	private void createModel() {
		logger.info("model構築");
		if(analyzerModel == null) {
			analyzerModel = new PMDWrapperModel();
		}
	}
}
