import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

public class Editor extends JFrame implements ActionListener {

	private static final long serialVersionUID = 7995390736844924440L;
	private RSyntaxTextArea scriptArea;
	private JTextArea consoleArea;
	private JTextField entryField;
	private JMenuItem menuConnect;
	private JMenuItem menuLoad;
	private JMenuItem menuSave;
	private JMenuItem menuSaveAs;
	private JMenuItem menuRun;
	private SocketClient socketClient;
	private File openFile = null;
	
	public Editor()  {
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

		JPanel cp = new JPanel(new BorderLayout());
		scriptArea = new RSyntaxTextArea(20, 60);
		scriptArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
		scriptArea.setCodeFoldingEnabled(true);
		scriptArea.setText("# Your Python code will be typed here\n\n");
		RTextScrollPane sp = new RTextScrollPane(scriptArea);
		cp.add(sp);

		JPanel bp = new JPanel(new BorderLayout());
		consoleArea = new JTextArea("-- Please connect to server first --");
		consoleArea.setBackground(Color.black);
		consoleArea.setForeground(Color.white);
		consoleArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
		consoleArea.setEditable(false);
		consoleArea.setLineWrap(true);
		consoleArea.setWrapStyleWord(true);
		DefaultCaret caret = (DefaultCaret) consoleArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		JScrollPane jsp = new JScrollPane(consoleArea);
		bp.add(jsp, BorderLayout.CENTER);

		entryField = new JTextField();
		entryField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
		entryField.addActionListener(this);
		bp.add(entryField, BorderLayout.SOUTH);

		splitPane.add(cp);
		splitPane.add(bp);
		splitPane.setDividerLocation(200);

		JMenuBar menuBar = new JMenuBar();
		menuConnect = new JMenuItem("Connect");
		menuLoad = new JMenuItem("Open");
		menuSave = new JMenuItem("Save");
		menuSaveAs = new JMenuItem("Save As");
		menuRun = new JMenuItem("Run");

		menuConnect.setAccelerator(KeyStroke.getKeyStroke('I', 
				Toolkit.getDefaultToolkit ().getMenuShortcutKeyMask()));
		menuLoad.setAccelerator(KeyStroke.getKeyStroke('O', 
				Toolkit.getDefaultToolkit ().getMenuShortcutKeyMask()));
		menuSave.setAccelerator(KeyStroke.getKeyStroke('S', 
				Toolkit.getDefaultToolkit ().getMenuShortcutKeyMask()));
		menuRun.setAccelerator(KeyStroke.getKeyStroke('R', 
				Toolkit.getDefaultToolkit ().getMenuShortcutKeyMask()));
		
		menuBar.add(menuConnect);
		menuBar.add(menuLoad);
		menuBar.add(menuSave);
		menuBar.add(menuSaveAs);
		menuBar.add(menuRun);

		menuConnect.addActionListener(this);
		menuLoad.addActionListener(this);
		menuSave.addActionListener(this);
		menuSaveAs.addActionListener(this);
		menuRun.addActionListener(this);

		this.setJMenuBar(menuBar);

		setContentPane(splitPane);
		setTitle("Untitled");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
		setLocationRelativeTo(null);
		
	}
	
	public void executeCode(String code) {
		if (code == null) return;
		if (socketClient == null) return;
		String[] lines = code.split("\n");
		for (String line : lines) {
			if (line.trim().equals(""))
				line = "";
			socketClient.send(line);
			consoleArea.append(line+"\n");
			consoleArea.append(socketClient.getAllOutput());
		}
		if (lines.length == 0) {
			socketClient.send("");
			consoleArea.append("\n");
			consoleArea.append(socketClient.getAllOutput());
		}
		Document d = consoleArea.getDocument();
		consoleArea.select(d.getLength(), d.getLength());
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == entryField) {
			executeCode(entryField.getText());
			entryField.setText("");
		} else if(e.getSource() == menuRun) {
			String text = scriptArea.getSelectedText();
			if (text == null || text.equals("")) {
				try {
					text = scriptArea.getText(
							scriptArea.getLineStartOffsetOfCurrentLine(),
							scriptArea.getLineEndOffsetOfCurrentLine()
								- scriptArea.getLineStartOffsetOfCurrentLine());
					scriptArea.setCaretPosition(scriptArea.getLineEndOffsetOfCurrentLine());
				} catch (BadLocationException e1) {
					return;
				}
			}
			executeCode(text);
		} else if(e.getSource() == menuConnect) {
			final JFrame frame = new JFrame("Connect to server");
			final JTextField txthost = new JTextField("localhost");
			final JTextField txtport = new JTextField("44444");
			final JPasswordField txtpass = new JPasswordField("swordfish");
		    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		    frame.getContentPane().setLayout(new GridLayout(0, 2));
		    frame.getContentPane().add(new JLabel("Host:"));
		    frame.getContentPane().add(txthost);
		    frame.getContentPane().add(new JLabel("Port:"));
		    frame.getContentPane().add(txtport);
		    frame.getContentPane().add(new JLabel("Password:"));
		    frame.getContentPane().add(txtpass);
		    JButton btn = new JButton("Connect");
		    btn.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					connect(txthost.getText(), Integer.parseInt(txtport.getText()), 
							new String(txtpass.getPassword()));
					frame.setVisible(false);
				}
		    });
		    frame.getContentPane().add(btn);
		    frame.pack();
		    frame.setVisible(true);
		} else if(e.getSource() == menuLoad) {
			JFileChooser fileChooser = new JFileChooser();
			if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				openFile = fileChooser.getSelectedFile();
				setTitle(openFile.getName());
				Path path = Paths.get(openFile.getAbsolutePath());
			    scriptArea.setText("");
			    List<String> lines;
				try {
					lines = Files.readAllLines(path);
					for (String line : lines){
				    	scriptArea.append(line+"\n");
				    }
				} catch (IOException e1) {}
			}
		} else if(e.getSource() == menuSaveAs || (openFile == null && e.getSource() == menuSave)) {
			JFileChooser fileChooser = new JFileChooser();
			if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
				openFile = fileChooser.getSelectedFile();
				setTitle(openFile.getName());
				Path path = Paths.get(openFile.getAbsolutePath());
				List<String> lines = new ArrayList<String>();
				lines.add(scriptArea.getText());
				try {
					Files.write(path, lines);
				} catch (IOException e1) {}
			}
		} else if(e.getSource() == menuSave) {
			Path path = Paths.get(openFile.getAbsolutePath());
			List<String> lines = new ArrayList<String>();
			lines.add(scriptArea.getText());
			try {
				Files.write(path, lines);
			} catch (IOException e1) {}
		}
	}
	
	public void connect(String host, int port, String pass) {
		socketClient = new SocketClient(host, port, pass);
		boolean result = socketClient.connect();
		if (result)
			consoleArea.setText("Connected");
		else
			consoleArea.setText("Connection failed");
		consoleArea.append("\n>>> ");
		
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new Editor().setVisible(true);
			}
		});
	}

}