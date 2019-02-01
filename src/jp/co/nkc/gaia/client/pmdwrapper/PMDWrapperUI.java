package jp.co.nkc.gaia.client.pmdwrapper;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.TransferHandler;

import org.apache.log4j.Logger;

public class PMDWrapperUI extends JFrame implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private JTextArea area;
	private List<File> files;
	private JButton startAnalyzeButton = new JButton("Analyze Start");
	private JButton clearButton = new JButton("Clear List");

	private PMDWrapperModel model ;

	private Rectangle bounceRect = new Rectangle(100, 100, 510, 260);
	private Dimension textAreaDim = new Dimension(500, 250);

	private static final Logger logger = Logger.getLogger(PMDWrapperUI.class);
	
	public void setModel(PMDWrapperModel model) {
		this.model = model;
	}

	public PMDWrapperUI() {
	}
	
	public PMDWrapperUI(String title) {
		super(title);
		init();
	}

	private static final String ICON_PATH = "/jp/co/nkc/gaia/client/pmdwrapper/image/pmdicon.png";
	
	private String labelStr = "解析したいクラスをD&Dしてください。";
	private void init(){
		BufferedImage bi;
		try {
			bi = ImageIO.read(this.getClass().getResource(ICON_PATH));
			ImageIcon image = new ImageIcon(bi); 
			setIconImage(image.getImage());
		} catch (IOException e) {
			logger.info(e.getCause() + "アイコンが取得できませんでした。");
		}
		
		files = new ArrayList<File>();

		area = new JTextArea();
		area.setPreferredSize(textAreaDim);
		area.setTransferHandler(new DropFileHandler());

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout(0, 0));

		JLabel label = new JLabel(labelStr);

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		add(mainPanel);
		setBounds(bounceRect);

		JPanel footerPanel = new JPanel();
		footerPanel.add(startAnalyzeButton);
		footerPanel.add(clearButton);

		mainPanel.add(footerPanel,BorderLayout.SOUTH);
		mainPanel.add(label,BorderLayout.NORTH);
		mainPanel.add(area,BorderLayout.CENTER);
		startAnalyzeButton.addMouseListener(new StartAnalyzeActionListener());
		clearButton.addMouseListener(new ClearActionListener());
	}

	private class StartAnalyzeActionListener extends MouseAdapter{
		@Override
		public void mouseClicked(MouseEvent e) {
			fire(files);
		}
		
		private void fire(List<File> fileList) {
			setTitle("Programming Mistake Detector(PMD) 解析中...");
			model.analyze(fileList);
			setTitle("Programming Mistake Detector(PMD)");
		}
		
	}

	private class ClearActionListener extends MouseAdapter{
		@Override
		public void mouseClicked(MouseEvent e) {
			logger.info("ファイルリストのクリア");
			files.clear();
			files = new ArrayList<File>();
			area.setText("");
		}
	}

	/**
	 * ドロップ操作の処理を行うクラス
	 */
	private class DropFileHandler extends TransferHandler {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * ドロップされたものを受け取るか判断 (ファイルのときだけ受け取る)
		 */
		@Override
		public boolean canImport(TransferSupport support) {
			boolean ret = true;
			if (!support.isDrop() || !support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
				// ドロップ操作でない場合は受け取らない
				ret =  false;
			}
			return ret ;
		}

		/**
		 * ドロップされたファイルを受け取る
		 */
		@Override
		public boolean importData(TransferSupport support) {
			// 受け取っていいものか確認する
			if (!canImport(support)) {
				return false;
			}

			// ドロップ処理
			Transferable t = support.getTransferable();
			try {
				// ファイルを受け取る

				@SuppressWarnings("unchecked")
				List<File> list = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);

				// テキストエリアに表示するファイル名リストを作成する

				StringBuilder builder = new StringBuilder();
				if(area.getText() != null && area.getText().length() > 0) {
					builder.append(area.getText());
				}
				for (File file : list){
					builder.append(file.getName());
					builder.append("\n");
					files.add(file);
				}

				// テキストエリアにファイル名のリストを表示する
				area.removeAll();
				area.setText(builder.toString());
			} catch (UnsupportedFlavorException | IOException e) {
				logger.info(e.getCause() + "ファイルリスト操作エラー");
			}
			return true;
		}
	}

	public void setFiles(List<File> files) {
		this.files = files;
	}
	private Rectangle bounceRectDandD = new Rectangle(100, 100, 200, 50);
	JLabel labelDandD = new JLabel();
	public void createDandDAnalyzeGUI() {
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		labelDandD = new JLabel();
		labelDandD.setText("PMD解析中");
		setContentPane(labelDandD);
		setTitle("Programming Mistake Detector(PMD) D&Dモード");
		setBounds(bounceRectDandD);
	}
	public void setEndLabel() {
		labelDandD.setText("PMD解析完了");
	}
}
