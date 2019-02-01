package jp.co.nkc.gaia.client.pmdwrapper;

import org.apache.log4j.Logger;

public class PMDWrapperMain {
	
	private static final Logger logger = Logger.getLogger(PMDWrapperModel.class);
	
	public static void main(String[] args) {
		PMDWrapperController analyzerController = new PMDWrapperController(args);
		if(args.length == 0) {
			logger.info("GUIモードで起動");
			analyzerController.startGUI();
		}else {
			logger.info("Drug&Dropモードで起動");
			analyzerController.startAnalyze();
		}
	}
}
