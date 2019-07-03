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

import com.github.fracpete.jshell.event.JShellErrorEvent;
import com.github.fracpete.jshell.event.JShellErrorListener;
import com.github.fracpete.jshell.event.JShellExecEvent;
import com.github.fracpete.jshell.event.JShellExecListener;
import com.github.fracpete.jshell.event.JShellPanelEvent;
import com.github.fracpete.jshell.event.JShellPanelEvent.EventType;
import com.github.fracpete.jshell.event.JShellPanelListener;
import com.github.fracpete.processoutput4j.core.StreamingProcessOutputType;
import com.github.fracpete.processoutput4j.core.StreamingProcessOwner;
import nz.ac.waikato.cms.core.FileUtils;
import nz.ac.waikato.cms.core.Utils;
import nz.ac.waikato.cms.gui.core.BaseFileChooser;
import nz.ac.waikato.cms.gui.core.BaseFrame;
import nz.ac.waikato.cms.gui.core.BasePanel;
import nz.ac.waikato.cms.gui.core.ExtensionFileFilter;
import nz.ac.waikato.cms.gui.core.GUIHelper;
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
  implements StreamingProcessOwner, JShellErrorListener, JShellExecListener {

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

  /** for executing the script. */
  protected JShellExec m_Exec;

  /** the listeners that listen for changes. */
  protected Set<JShellPanelListener> m_JShellPanelListeners;

  /** additional runtime flags to supply to JShell (-J). */
  protected List<String> m_RuntimeFlags;

  /** additional remote runtime flags to supply to JShell (-R). */
  protected List<String> m_RemoteRuntimeFlags;

  /** additional compiler flags to supply to JShell (-C). */
  protected List<String> m_CompilerFlags;

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

    m_Exec = new JShellExec();
    m_Exec.addJShellErrorListener(this);
    m_Exec.addJShellExecListener(this);
    m_Exec.setStreamingProcessOwner(this);

    m_JShellPanelListeners = new HashSet<>();

    m_RuntimeFlags       = new ArrayList<>();
    m_RemoteRuntimeFlags = new ArrayList<>();
    m_CompilerFlags      = new ArrayList<>();
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
   * Sets the debugging flag.
   *
   * @param value	true if to turn debugging output on
   */
  public void setDebug(boolean value) {
    m_Exec.setDebug(value);
  }

  /**
   * Returns the debugging flag.
   *
   * @return		true if debugging output on
   */
  public boolean getDebug() {
    return m_Exec.getDebug();
  }

  /**
   * Returns whether a script is currently running.
   *
   * @return		true if a script is running
   */
  public boolean isRunning() {
    return m_Exec.isRunning();
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
      notifyJShellPanelListeners(new JShellPanelEvent(this, EventType.SCRIPT_LOAD_SUCCESS));
    }
    catch (Exception e) {
      GUIHelper.showErrorMessage(this, "Failed to load script from: " + script, e);
      notifyJShellPanelListeners(new JShellPanelEvent(this, EventType.SCRIPT_LOAD_FAILURE));
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
      notifyJShellPanelListeners(new JShellPanelEvent(this, EventType.SCRIPT_SAVE_FAILURE));
    }
    else {
      notifyJShellPanelListeners(new JShellPanelEvent(this, EventType.SCRIPT_SAVE_SUCCESS));
    }

    updateButtons();
  }

  /**
   * Executes the script.
   */
  public void runScript() {
    m_Exec.runScript(m_TextCode.getText(), m_RuntimeFlags, m_RemoteRuntimeFlags, m_CompilerFlags);
    updateButtons();
  }

  /**
   * Stops a running script.
   */
  public void stopScript() {
    m_Exec.stopScript();
    updateButtons();
  }

  /**
   * Clears the output of the script.
   */
  public void clearScriptOutput() {
    m_TextOutput.setText("");
    notifyJShellPanelListeners(new JShellPanelEvent(this, EventType.OUTPUT_CLEARED));
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
      notifyJShellPanelListeners(new JShellPanelEvent(this, EventType.OUTPUT_SAVE_FAILURE));
    }
    else {
      notifyJShellPanelListeners(new JShellPanelEvent(this, EventType.OUTPUT_SAVE_SUCESS));
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
   * Checks whether jshell executable is available.
   *
   * @return true if available
   */
  public boolean isAvailable() {
    return m_Exec.isAvailable();
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
   * Adds the exec listener to the internal list.
   *
   * @param l		the listener to add
   */
  public void addJShellExecListener(JShellExecListener l) {
    m_Exec.addJShellExecListener(l);
  }

  /**
   * Removes the exec listener to the internal list.
   *
   * @param l		the listener to remove
   */
  public void removeJShellExecListener(JShellExecListener l) {
    m_Exec.removeJShellExecListener(l);
  }

  /**
   * Adds the panel listener to the internal list.
   *
   * @param l		the listener to add
   */
  public void addJShellPanelListener(JShellPanelListener l) {
    m_JShellPanelListeners.add(l);
  }

  /**
   * Removes the panel listener to the internal list.
   *
   * @param l		the listener to remove
   */
  public void removeJShellExecListener(JShellPanelListener l) {
    m_JShellPanelListeners.remove(l);
  }

  /**
   * Notifies all the listeners with the specified panel event.
   *
   * @param e		the event to send
   */
  public synchronized void notifyJShellPanelListeners(JShellPanelEvent e) {
    for (JShellPanelListener l: m_JShellPanelListeners)
      l.jshellPanelEventOccurred(e);
  }

  /**
   * Gets called when an error occurred.
   *
   * @param e		the error
   */
  public void jshellErrorOccurred(JShellErrorEvent e) {
    if (e.hasException())
      GUIHelper.showErrorMessage(this, e.getMessage(), e.getException());
    else
      GUIHelper.showErrorMessage(this, e.getMessage());
  }

  /**
   * Gets triggered with any event in the JShellPanel.
   *
   * @param e		the event
   */
  public void jshellExecEventOccurred(JShellExecEvent e) {
    updateButtons();
  }

  /**
   * Sets the runtime flags to supply to JShell (-J), used by JShell (eg -verbose).
   *
   * @param value	the flags
   */
  public void setRuntimeFlags(List<String> value) {
    m_RuntimeFlags.clear();
    if (value != null)
      m_RuntimeFlags.addAll(value);
  }

  /**
   * Returns the runtime flags to supply to JShell (-J), used by JShell (eg -verbose).
   * 
   * @return		the flags
   */
  public List<String> getRuntimeFlags() {
    return m_RuntimeFlags;
  }

  /**
   * Sets the remote runtime flags to supply to JShell (-R), used by the JVM
   * executing the code (eg -javaagent:...).
   *
   * @param value	the flags
   */
  public void setRemoteRuntimeFlags(List<String> value) {
    m_RemoteRuntimeFlags.clear();
    if (value != null)
      m_RemoteRuntimeFlags.addAll(value);
  }

  /**
   * Returns the remote runtime flags to supply to JShell (-R), used by the JVM
   * executing the code (eg -javaagent:...).
   * 
   * @return		the flags
   */
  public List<String> getRemoteRuntimeFlags() {
    return m_RemoteRuntimeFlags;
  }

  /**
   * Sets the compiler flags to supply to JShell (-C).
   *
   * @param value	the flags
   */
  public void setCompilerFlags(List<String> value) {
    m_CompilerFlags.clear();
    if (value != null)
      m_CompilerFlags.addAll(value);
  }

  /**
   * Returns the compiler flags to supply to JShell (-C).
   * 
   * @return		the flags
   */
  public List<String> getCompilerFlags() {
    return m_CompilerFlags;
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
    panel.addJShellExecListener((JShellExecEvent e) -> System.out.println("exec: " + e.getType()));
    panel.addJShellPanelListener((JShellPanelEvent e) -> System.out.println("panel: " + e.getType()));
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
