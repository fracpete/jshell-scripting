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
 * Copyright (C) 2018 FracPete
 */

package com.github.fracpete.jshell;

import nz.ac.waikato.cms.core.FileUtils;
import nz.ac.waikato.cms.gui.core.BaseFileChooser;
import nz.ac.waikato.cms.gui.core.BaseFrame;
import nz.ac.waikato.cms.gui.core.BasePanel;
import nz.ac.waikato.cms.gui.core.ExtensionFileFilter;
import nz.ac.waikato.cms.gui.core.GUIHelper;
import org.apache.commons.lang3.JavaVersion;
import org.apache.commons.lang3.SystemUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * Panel for performing scripting via jshell. Requires Java 9.
 *
 * See https://docs.oracle.com/javase/9/jshell/
 *
 * @author FracPete (fracpete at gmail dot com)
 */
public class JShellPanel
  extends BasePanel {

  /** whether scripting is available. */
  protected Boolean m_Available;

  /** for splitting code and output. */
  protected JSplitPane m_SplitPane;

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
  }

  /**
   * Initializes the widgets.
   */
  protected void initGUI() {
    JPanel panel;
    JPanel panelRight;
    JPanel panelButtons;
    JPanel panelText;
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
    panel.add(new RTextScrollPane(m_TextCode), BorderLayout.CENTER);
    panelText = new JPanel(new FlowLayout(FlowLayout.LEFT));
    panel.add(panelText, BorderLayout.NORTH);
    label = new JLabel("JShell");
    panelText.add(label);
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
   * Updates the state of the buttons.
   */
  protected void updateButtons() {
    boolean     running;

    running = false;  // TODO

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
    // TODO

    updateButtons();
  }

  /**
   * Lets the user save the script to a file.
   */
  public void saveScript() {
    // TODO

    updateButtons();
  }

  public void runScript() {
    // TODO

    updateButtons();
  }

  public void stopScript() {
    // TODO

    updateButtons();
  }

  /**
   * Clears the output of the script.
   */
  public void clearScriptOutput() {
    m_TextOutput.setText("");
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
    if (msg != null)
      GUIHelper.showErrorMessage(this, msg, "Failed saving output");

    updateButtons();
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
	&& new File(getExecutable()).exists();
    }
    return m_Available;
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
