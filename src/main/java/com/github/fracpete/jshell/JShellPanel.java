/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * JShellPanel.java
 * Copyright (C) 2018-2019 FracPete
 */

package com.github.fracpete.jshell;

import com.github.fracpete.jshell.event.JShellEvent;
import com.github.fracpete.jshell.event.JShellEvent.EventType;
import com.github.fracpete.jshell.event.JShellListener;
import com.github.fracpete.processoutput4j.core.StreamingProcessOutputType;
import com.github.fracpete.processoutput4j.core.StreamingProcessOwner;
import com.github.fracpete.processoutput4j.output.StreamingProcessOutput;
import nz.ac.waikato.cms.core.FileUtils;
import nz.ac.waikato.cms.core.Utils;
import nz.ac.waikato.cms.gui.core.BaseFileChooser;
import nz.ac.waikato.cms.gui.core.BaseFrame;
import nz.ac.waikato.cms.gui.core.BasePanel;
import nz.ac.waikato.cms.gui.core.ExtensionFileFilter;
import nz.ac.waikato.cms.gui.core.GUIHelper;
import org.apache.commons.lang3.JavaVersion;
import org.apache.commons.lang3.SystemUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Panel for performing scripting via jshell. Requires Java 9.
 *
 * See https://docs.oracle.com/javase/9/jshell/
 *
 * @author FracPete (fracpete at gmail dot com)
 */
public class JShellPanel
  extends BasePanel
  implements StreamingProcessOwner {

  /** the available themes. */
  public final static String[] THEMES = new String[]{
    "dark",
    "default",
    "default-alt",
    "eclipse",
    "idea",
    "monokai",
    "vs",
  };

  /** the default theme. */
  public final static String DEFAULT_THEME = "default";

  /** whether scripting is available. */
  protected Boolean m_Available;

  /** for splitting code and output. */
  protected JSplitPane m_SplitPane;

  /** the panel with the themes. */
  protected JComboBox<String> m_ComboBoxThemes;

  /** the text area for the script. */
  protected RSyntaxTextArea m_TextCode;

  /** the filechooser for scripts. */
  protected BaseFileChooser m_FileChooserScript;

  /** the button for loading a script. */
  protected JButton m_ButtonScriptLoad;

  /** the button for saving as script. */
  protected JButton m_ButtonScriptSave;

  /** the button for executing a script. */
  protected JButton m_ButtonScriptRun;

  /** the button for stopping a script. */
  protected JButton m_ButtonScriptStop;

  /** the filechooser for the output. */
  protected BaseFileChooser m_FileChooserOutput;

  /** the button for clearing the output. */
  protected JButton m_ButtonOutputClear;

  /** the button for saving the output. */
  protected JButton m_ButtonOutputSave;

  /** for the jshell output. */
  protected JTextArea m_TextOutput;

  /** executes the script. */
  protected transient StreamingProcessOutput m_Execution;

  /** the listeners that listen for changes. */
  protected Set<JShellListener> m_JShellListeners;

  /**
   * Initializes the members.
   */
  protected void initialize() {
    m_FileChooserScript = new BaseFileChooser();
    m_FileChooserScript.addChoosableFileFilter(new ExtensionFileFilter("JShell script", new String[]{"jsh", "jshell"}));
    m_FileChooserScript.setAcceptAllFileFilterUsed(true);

    m_FileChooserOutput = new BaseFileChooser();
    m_FileChooserOutput.addChoosableFileFilter(new ExtensionFileFilter("Text file", "txt"));
    m_FileChooserOutput.setAcceptAllFileFilterUsed(true);

    m_JShellListeners = new HashSet<>();
  }

  /**
   * Initializes the widgets.
   */
  protected void initGUI() {
    JPanel panel;
    JPanel panelRight;
    JPanel panelButtons;
    JPanel panelText;
    JPanel panelTop;
    JPanel panelThemes;
    JLabel label;

    setLayout(new BorderLayout());

    if (!isAvailable()) {
      panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
      add(panel, BorderLayout.CENTER);
      label = new JLabel("jshell executable not found (Java 9+ only) - scripting disabled!");
      panel.add(label);
      return;
    }

    m_SplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    m_SplitPane.setOneTouchExpandable(true);
    m_SplitPane.setResizeWeight(1.0);
    add(m_SplitPane, BorderLayout.CENTER);

    // code
    panel = new JPanel(new BorderLayout());
    m_SplitPane.setTopComponent(panel);
    m_TextCode = new RSyntaxTextArea(10, 80);
    m_TextCode.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
    m_TextCode.setLineWrap(false);
    m_TextCode.setAutoIndentEnabled(true);
    m_TextCode.setAntiAliasingEnabled(true);
    m_TextCode.setCodeFoldingEnabled(true);
    m_TextCode.setBracketMatchingEnabled(false);
    m_TextCode.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        updateButtons();
      }
      @Override
      public void removeUpdate(DocumentEvent e) {
        updateButtons();
      }
      @Override
      public void changedUpdate(DocumentEvent e) {
        updateButtons();
      }
    });
    panel.add(new RTextScrollPane(m_TextCode), BorderLayout.CENTER);
    panelTop = new JPanel(new BorderLayout());
    panel.add(panelTop, BorderLayout.NORTH);
    panelText = new JPanel(new FlowLayout(FlowLayout.LEFT));
    panelTop.add(panelText, BorderLayout.WEST);
    label = new JLabel("JShell");
    panelText.add(label);
    panelThemes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    panelTop.add(panelThemes, BorderLayout.EAST);
    m_ComboBoxThemes = new JComboBox<>(THEMES);
    m_ComboBoxThemes.setSelectedItem(DEFAULT_THEME);
    m_ComboBoxThemes.addActionListener((ActionEvent e) -> updateTheme());
    label = new JLabel("Theme");
    label.setDisplayedMnemonic('T');
    label.setLabelFor(m_ComboBoxThemes);
    panelThemes.add(label);
    panelThemes.add(m_ComboBoxThemes);
    panelRight = new JPanel(new BorderLayout());
    panel.add(panelRight, BorderLayout.EAST);
    panelButtons = new JPanel(new GridLayout(0, 1));
    panelRight.add(panelButtons, BorderLayout.NORTH);
    m_ButtonScriptLoad = new JButton(GUIHelper.getIcon("open.gif"));
    m_ButtonScriptLoad.setToolTipText("Load script from file");
    m_ButtonScriptLoad.addActionListener((ActionEvent e) -> loadScript());
    panelButtons.add(m_ButtonScriptLoad);
    m_ButtonScriptSave = new JButton(GUIHelper.getIcon("save.gif"));
    m_ButtonScriptSave.setToolTipText("Save script to file");
    m_ButtonScriptSave.addActionListener((ActionEvent e) -> saveScript());
    panelButtons.add(m_ButtonScriptSave);
    m_ButtonScriptRun = new JButton(GUIHelper.getIcon("run.gif"));
    m_ButtonScriptRun.setToolTipText("Execute script");
    m_ButtonScriptRun.addActionListener((ActionEvent e) -> runScript());
    panelButtons.add(m_ButtonScriptRun);
    m_ButtonScriptStop = new JButton(GUIHelper.getIcon("stop.gif"));
    m_ButtonScriptStop.setToolTipText("Stop script");
    m_ButtonScriptStop.addActionListener((ActionEvent e) -> stopScript());
    panelButtons.add(m_ButtonScriptStop);

    // output
    panel = new JPanel(new BorderLayout());
    m_SplitPane.setBottomComponent(panel);
    m_TextOutput = new JTextArea(20, 80);
    m_TextOutput.setFont(new Font("monospaced", Font.PLAIN, 10));
    panel.add(new JScrollPane(m_TextOutput), BorderLayout.CENTER);
    panelText = new JPanel(new FlowLayout(FlowLayout.LEFT));
    panel.add(panelText, BorderLayout.NORTH);
    label = new JLabel("Output");
    panelText.add(label);
    panelRight = new JPanel(new BorderLayout());
    panel.add(panelRight, BorderLayout.EAST);
    panelButtons = new JPanel(new GridLayout(0, 1));
    panelRight.add(panelButtons, BorderLayout.NORTH);
    m_ButtonOutputClear = new JButton(GUIHelper.getIcon("new.gif"));
    m_ButtonOutputClear.setToolTipText("Clear output");
    m_ButtonOutputClear.addActionListener((ActionEvent e) -> clearScriptOutput());
    panelButtons.add(m_ButtonOutputClear);
    m_ButtonOutputSave = new JButton(GUIHelper.getIcon("save.gif"));
    m_ButtonOutputSave.setToolTipText("Save output to file");
    m_ButtonOutputSave.addActionListener((ActionEvent e) -> saveScriptOutput());
    panelButtons.add(m_ButtonOutputSave);
  }

  /**
   * Finishes the initialization.
   */
  protected void finishInit() {
    updateButtons();
  }

  /**
   * Updates the theme.
   *
   * @return		true if successfully applied
   */
  protected boolean updateTheme() {
    String	themeURL;
    InputStream	in;
    Theme	theme;

    themeURL = "org/fife/ui/rsyntaxtextarea/themes/" + m_ComboBoxThemes.getSelectedItem() + ".xml";
    in       = ClassLoader.getSystemResourceAsStream(themeURL);
    try {
      theme = Theme.load(in);
      theme.apply(m_TextCode);
      return true;
    }
    catch (Exception e) {
      return false;
    }
    finally {
      FileUtils.closeQuietly(in);
    }
  }

  /**
   * Sets the current theme.
   *
   * @param value	the theme to use
   * @see		#THEMES
   */
  public void setCurrentTheme(String value) {
    for (String theme: THEMES) {
      if (value.equalsIgnoreCase(theme)) {
        m_ComboBoxThemes.setSelectedItem(theme);
        break;
      }
    }
  }

  /**
   * Returns the currently selected theme.
   *
   * @return		the current theme
   */
  public String getCurrentTheme() {
    return (String) m_ComboBoxThemes.getSelectedItem();
  }

  /**
   * Returns whether a script is currently running.
   *
   * @return		true if a script is running
   */
  public boolean isRunning() {
    return (m_Execution != null);
  }

  /**
   * Updates the state of the buttons.
   */
  protected void updateButtons() {
    boolean     running;

    if (!isAvailable())
      return;

    running = isRunning();

    // script
    m_ButtonScriptLoad.setEnabled(!running);
    m_ButtonScriptSave.setEnabled(!running);
    m_ButtonScriptRun.setEnabled(!running && (m_TextCode.getDocument().getLength() > 0));
    m_ButtonScriptStop.setEnabled(running);

    // output
    m_ButtonOutputClear.setEnabled(m_TextOutput.getDocument().getLength() > 0);
    m_ButtonOutputSave.setEnabled(m_TextOutput.getDocument().getLength() > 0);
  }

  /**
   * Lets the user select a script to load.
   */
  public void loadScript() {
    int		retVal;

    retVal = m_FileChooserScript.showOpenDialog(this);
    if (retVal != BaseFileChooser.APPROVE_OPTION)
      return;

    loadScript(m_FileChooserScript.getSelectedFile());
  }

  /**
   * Loads the specified file.
   *
   * @param script	the script to load
   */
  public void loadScript(File script) {
    List<String> 	lines;

    try {
      lines = Files.readAllLines(script.toPath());
      m_TextCode.setText(Utils.flatten(lines, "\n"));
      notifyJShellListeners(new JShellEvent(this, EventType.SCRIPT_LOAD_SUCCESS));
    }
    catch (Exception e) {
      GUIHelper.showErrorMessage(this, "Failed to load script from: " + script, e);
      notifyJShellListeners(new JShellEvent(this, EventType.SCRIPT_LOAD_FAILURE));
    }

    updateButtons();
  }

  /**
   * Lets the user save the script to a file.
   */
  public void saveScript() {
    int		retVal;
    File	script;
    String	msg;

    retVal = m_FileChooserScript.showSaveDialog(this);
    if (retVal != BaseFileChooser.APPROVE_OPTION)
      return;

    script = m_FileChooserScript.getSelectedFile();
    msg    = FileUtils.writeToFileMsg(script.getAbsolutePath(), m_TextCode.getText(), false, null);
    if (msg != null) {
      GUIHelper.showErrorMessage(this, "Failed to save script to : " + script + "\n" + msg);
      notifyJShellListeners(new JShellEvent(this, EventType.SCRIPT_SAVE_FAILURE));
    }
    else {
      notifyJShellListeners(new JShellEvent(this, EventType.SCRIPT_SAVE_SUCCESS));
    }

    updateButtons();
  }

  /**
   * Executes the script.
   */
  public void runScript() {
    List<String>	cmd;
    final File		tmpFile;
    String		code;
    String		msg;
    ProcessBuilder 	builder;
    Runnable		run;

    stopScript();
    clearScriptOutput();

    // create tmp file name
    try {
      tmpFile = File.createTempFile("jshell-", ".jsh");
    }
    catch (Exception e) {
      GUIHelper.showErrorMessage(this, "Failed to create temporary file for script!\nCannot execute script!", e);
      notifyJShellListeners(new JShellEvent(this, EventType.SCRIPT_RUN_SETUP_FAILURE));
      return;
    }

    // ensure that "/exit is in code"
    code = m_TextCode.getText();
    if (!code.toLowerCase().contains("/exit"))
      code += "\n/exit\n";

    // save script to tmp file
    msg = FileUtils.writeToFileMsg(tmpFile.getAbsolutePath(), code, false, null);
    if (msg != null) {
      tmpFile.delete();
      GUIHelper.showErrorMessage(this, "Failed to write script to temporary file: " + tmpFile + "\n" + msg);
      notifyJShellListeners(new JShellEvent(this, EventType.SCRIPT_RUN_SETUP_FAILURE));
      return;
    }

    // build commandline for jshell
    cmd = new ArrayList<>();
    cmd.add(getExecutable());
    cmd.add("--class-path");
    cmd.add(System.getProperty("java.class.path"));
    cmd.add(tmpFile.getAbsolutePath());

    builder = new ProcessBuilder();
    builder.command(cmd);
    m_Execution = new StreamingProcessOutput(this);

    run = new Runnable() {
      @Override
      public void run() {
	try {
	  m_Execution.monitor(builder);
	  if (m_Execution.getExitCode() != 0)
	    notifyJShellListeners(new JShellEvent(JShellPanel.this, EventType.SCRIPT_RUN_FAILURE));
	  else
	    notifyJShellListeners(new JShellEvent(JShellPanel.this, EventType.SCRIPT_RUN_SUCCESS));
	}
	catch (Exception e) {
	  GUIHelper.showErrorMessage(JShellPanel.this, "Failed to execute script!", e);
	  notifyJShellListeners(new JShellEvent(JShellPanel.this, EventType.SCRIPT_RUN_FAILURE));
	}
	notifyJShellListeners(new JShellEvent(JShellPanel.this, EventType.SCRIPT_FINISHED));
	m_Execution = null;
	tmpFile.delete();
	updateButtons();
      }
    };
    new Thread(run).start();
    notifyJShellListeners(new JShellEvent(this, EventType.SCRIPT_RUN));

    updateButtons();
  }

  /**
   * Stops a running script.
   */
  public void stopScript() {
    if (m_Execution != null) {
      m_Execution.destroy();
      m_Execution = null;
      notifyJShellListeners(new JShellEvent(this, EventType.SCRIPT_STOP));
    }

    updateButtons();
  }

  /**
   * Clears the output of the script.
   */
  public void clearScriptOutput() {
    m_TextOutput.setText("");
    notifyJShellListeners(new JShellEvent(this, EventType.OUTPUT_CLEARED));
    updateButtons();
  }

  /**
   * Lets the user save the script output to a file.
   */
  public void saveScriptOutput() {
    int     retVal;
    String msg;

    retVal = m_FileChooserOutput.showSaveDialog(this);
    if (retVal != BaseFileChooser.APPROVE_OPTION)
      return;

    msg = FileUtils.writeToFileMsg(m_FileChooserOutput.getSelectedFile().getAbsolutePath(), m_TextOutput.getText(), false, null);
    if (msg != null) {
      GUIHelper.showErrorMessage(this, msg, "Failed saving output");
      notifyJShellListeners(new JShellEvent(this, EventType.OUTPUT_SAVE_FAILURE));
    }
    else {
      notifyJShellListeners(new JShellEvent(this, EventType.OUTPUT_SAVE_SUCESS));
    }

    updateButtons();
  }

  /**
   * Returns the current code.
   *
   * @return		the code
   */
  public String getCode() {
    return m_TextCode.getText();
  }

  /**
   * Returns the current output.
   *
   * @return		the output
   */
  public String getOutput() {
    return m_TextOutput.getText();
  }

  /**
   * Returns the jshell executable.
   *
   * @return the executable path
   */
  public String getExecutable() {
    String result;
    String home;

    home = System.getProperty("java.home");
    result = home + File.separator + "bin" + File.separator + "jshell";
    if (SystemUtils.IS_OS_WINDOWS)
      result += ".exe";

    return result;
  }

  /**
   * Checks whether jshell executable is available.
   *
   * @return true if available
   */
  public boolean isAvailable() {
    if (m_Available == null) {
      m_Available = JavaVersion.JAVA_RECENT.atLeast(JavaVersion.JAVA_9)
	&& new File(getExecutable()).exists()
	&& !System.getProperty("java.class.path").isEmpty();
    }
    return m_Available;
  }

  /**
   * Returns what output from the process to forward.
   *
   * @return 		the output type
   */
  public StreamingProcessOutputType getOutputType() {
    return StreamingProcessOutputType.BOTH;
  }

  /**
   * Processes the incoming line.
   *
   * @param line	the line to process
   * @param stdout	whether stdout or stderr
   */
  public void processOutput(String line, boolean stdout) {
    boolean	moveToEnd;

    moveToEnd = (m_TextOutput.getDocument().getLength() == m_TextOutput.getCaretPosition());
    m_TextOutput.append((stdout ? "[OUT] " : "[ERR] ") + line + "\n");
    if (moveToEnd)
      m_TextOutput.setCaretPosition(m_TextOutput.getDocument().getLength());
  }

  /**
   * Adds the listener to the internal list.
   *
   * @param l		the listener to add
   */
  public void addJShellListener(JShellListener l) {
    m_JShellListeners.add(l);
  }

  /**
   * Removes the listener to the internal list.
   *
   * @param l		the listener to remove
   */
  public void removeJShellListener(JShellListener l) {
    m_JShellListeners.remove(l);
  }

  /**
   * Notifies all the listeners with the specified event.
   *
   * @param e		the event to send
   */
  protected synchronized void notifyJShellListeners(JShellEvent e) {
    for (JShellListener l: m_JShellListeners)
      l.jshellEventOccurred(e);
  }

  /**
   * For testing only.
   *
   * @param args first argument is interpreted as script
   */
  public static void main(String[] args) {
    BaseFrame	frame;
    JShellPanel	panel;

    panel = new JShellPanel();
    panel.addJShellListener((JShellEvent e) -> System.out.println(e.getType()));
    frame = new BaseFrame("JShell");
    frame.setIconImage(GUIHelper.getIcon("jshell.gif").getImage());
    frame.getContentPane().setLayout(new BorderLayout());
    frame.getContentPane().add(panel, BorderLayout.CENTER);
    frame.setSize(1200, 900);
    frame.setDefaultCloseOperation(BaseFrame.EXIT_ON_CLOSE);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }
}
