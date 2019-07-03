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
 * JShellExec.java
 * Copyright (C) 2019 University of Waikato, Hamilton, NZ
 */

package com.github.fracpete.jshell;

import com.github.fracpete.jshell.event.JShellErrorEvent;
import com.github.fracpete.jshell.event.JShellErrorListener;
import com.github.fracpete.jshell.event.JShellExecEvent;
import com.github.fracpete.jshell.event.JShellExecEvent.EventType;
import com.github.fracpete.jshell.event.JShellExecListener;
import com.github.fracpete.processoutput4j.core.StreamingProcessOutputType;
import com.github.fracpete.processoutput4j.core.StreamingProcessOwner;
import com.github.fracpete.processoutput4j.output.StreamingProcessOutput;
import nz.ac.waikato.cms.core.FileUtils;
import org.apache.commons.lang3.JavaVersion;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * For executing code via JShell.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class JShellExec
  implements Serializable, StreamingProcessOwner {

  /** whether debugging is on. */
  protected boolean m_Debug;

  /** whether scripting is available. */
  protected Boolean m_Available;

  /** executes the script. */
  protected transient StreamingProcessOutput m_Execution;

  /** the listeners that listen for changes. */
  protected Set<JShellExecListener> m_JShellExecListeners;

  /** the listeners for errors. */
  protected Set<JShellErrorListener> m_JShellErrorListeners;

  /** the streaming process owner to forward the process output to. */
  protected StreamingProcessOwner m_StreamingProcessOwner;

  /**
   * Initializes the execution.
   */
  public JShellExec() {
    m_Available             = null;
    m_StreamingProcessOwner = null;
    m_JShellExecListeners   = new HashSet<>();
    m_JShellErrorListeners  = new HashSet<>();
    m_Execution             = null;
    m_Debug                 = false;
  }

  /**
   * Sets the debugging flag.
   *
   * @param value	true if to turn debugging output on
   */
  public void setDebug(boolean value) {
    m_Debug = value;
  }

  /**
   * Returns the debugging flag.
   *
   * @return		true if debugging output on
   */
  public boolean getDebug() {
    return m_Debug;
  }

  /**
   * Outputs the debugging message if debugging is enabled.
   *
   * @param msg		the message output
   */
  protected void debugMsg(String msg) {
    if (m_Debug)
      System.out.println("[DEBUG] " + msg);
  }

  /**
   * Sets the object to stream the output of the process to.
   * If none set, output is output on stdout/stderr.
   *
   * @param value	the receiver of the output
   */
  public void setStreamingProcessOwner(StreamingProcessOwner value) {
    m_StreamingProcessOwner = value;
  }

  /**
   * Returns the current recipient of the process output.
   * If none set, output is output on stdout/stderr.
   *
   * @return		the receiver, null if none set
   */
  public StreamingProcessOwner getStreamingProcessOwner() {
    return m_StreamingProcessOwner;
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
   * Executes the script with no flags.
   *
   * @param code 	the script code to execute
   */
  public void runScript(String code) {
    runScript(code, null, null, null);
  }

  /**
   * Executes the script.
   *
   * @param code 	the script code to execute
   * @param runtimeFlags 	optional runtime flags to pass through (-J gets prefixed automatically) - for JShell (eg -verbose)
   * @param remoteRuntimeFlags 	optional runtime flags to pass through (-R gets prefixed automatically) - for JVM that runs code (eg -javaagent:...)
   * @param compilerFlags 	optional runtime flags to pass through (-C gets prefixed automatically)
   */
  public void runScript(String code, List<String> runtimeFlags, List<String> remoteRuntimeFlags, List<String> compilerFlags) {
    List<String> 	cmd;
    final File 		tmpFile;
    String		msg;
    ProcessBuilder 	builder;
    Runnable		run;

    stopScript();

    // create tmp file name
    try {
      tmpFile = File.createTempFile("jshell-", ".jsh");
      debugMsg("tmpfile: " + tmpFile);
    }
    catch (Exception e) {
      showErrorMessage("Failed to create temporary file for script!\nCannot execute script!", e);
      notifyJShellExecListeners(new JShellExecEvent(this, EventType.SCRIPT_RUN_SETUP_FAILURE));
      return;
    }

    // ensure that "/exit is in code"
    if (!code.toLowerCase().contains("/exit"))
      code += "\n/exit\n";

    // save script to tmp file
    msg = FileUtils.writeToFileMsg(tmpFile.getAbsolutePath(), code, false, null);
    if (msg != null) {
      tmpFile.delete();
      showErrorMessage("Failed to write script to temporary file: " + tmpFile + "\n" + msg);
      notifyJShellExecListeners(new JShellExecEvent(this, EventType.SCRIPT_RUN_SETUP_FAILURE));
      return;
    }

    // build commandline for jshell
    cmd = new ArrayList<>();
    cmd.add(getExecutable());
    cmd.add("--class-path");
    cmd.add(System.getProperty("java.class.path"));
    if (runtimeFlags != null) {
      for (String runtimeFlag: runtimeFlags)
        cmd.add("-J" + runtimeFlag);
    }
    if (remoteRuntimeFlags != null) {
      for (String remoteRuntimeFlag: remoteRuntimeFlags)
        cmd.add("-R" + remoteRuntimeFlag);
    }
    if (compilerFlags != null) {
      for (String compilerFlag: compilerFlags)
        cmd.add("-C" + compilerFlag);
    }
    cmd.add(tmpFile.getAbsolutePath());
    debugMsg("Command: " + cmd);

    builder = new ProcessBuilder();
    builder.command(cmd);
    m_Execution = new StreamingProcessOutput(this);

    run = new Runnable() {
      @Override
      public void run() {
	try {
	  m_Execution.monitor(builder);
	  if (m_Execution.getExitCode() != 0)
	    notifyJShellExecListeners(new JShellExecEvent(JShellExec.this, EventType.SCRIPT_RUN_FAILURE));
	  else
	    notifyJShellExecListeners(new JShellExecEvent(JShellExec.this, EventType.SCRIPT_RUN_SUCCESS));
	}
	catch (Throwable t) {
	  showErrorMessage("Failed to execute script!", t);
	  notifyJShellExecListeners(new JShellExecEvent(JShellExec.this, EventType.SCRIPT_RUN_FAILURE));
	}
	m_Execution = null;
	notifyJShellExecListeners(new JShellExecEvent(JShellExec.this, EventType.SCRIPT_FINISHED));
	tmpFile.delete();
      }
    };
    new Thread(run).start();
    notifyJShellExecListeners(new JShellExecEvent(this, EventType.SCRIPT_RUN));
  }

  /**
   * Stops a running script.
   */
  public void stopScript() {
    if (m_Execution != null) {
      m_Execution.destroy();
      m_Execution = null;
      notifyJShellExecListeners(new JShellExecEvent(this, EventType.SCRIPT_STOP));
    }
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
    if (m_StreamingProcessOwner != null)
      return m_StreamingProcessOwner.getOutputType();
    else
      return StreamingProcessOutputType.BOTH;
  }

  /**
   * Processes the incoming line.
   *
   * @param line	the line to process
   * @param stdout	whether stdout or stderr
   */
  public void processOutput(String line, boolean stdout) {
    if (m_StreamingProcessOwner != null) {
      m_StreamingProcessOwner.processOutput(line, stdout);
    }
    else {
      if (stdout)
	System.out.println(line);
      else
	System.err.println(line);
    }
  }

  /**
   * Outputs the error message.
   *
   * @param msg		the message to output
   */
  public void showErrorMessage(String msg) {
    showErrorMessage(msg, null);
  }

  /**
   * Outputs the error message.
   *
   * @param msg		the message to output
   * @param t 		the (optional) associated exception
   */
  public void showErrorMessage(String msg, Throwable t) {
    if (m_JShellErrorListeners.isEmpty()) {
      System.err.println(msg);
      if (t != null)
        t.printStackTrace();
    }
    else {
      notifyJShellErrorListeners(new JShellErrorEvent(this, msg, t));
    }
  }

  /**
   * Adds the exec listener to the internal list.
   *
   * @param l		the listener to add
   */
  public void addJShellExecListener(JShellExecListener l) {
    m_JShellExecListeners.add(l);
  }

  /**
   * Removes the exec listener to the internal list.
   *
   * @param l		the listener to remove
   */
  public void removeJShellExecListener(JShellExecListener l) {
    m_JShellExecListeners.remove(l);
  }

  /**
   * Notifies all the exec listeners with the specified event.
   *
   * @param e		the event to send
   */
  public synchronized void notifyJShellExecListeners(JShellExecEvent e) {
    debugMsg("ExecEvent: " + e.toString());
    for (JShellExecListener l: m_JShellExecListeners)
      l.jshellExecEventOccurred(e);
  }

  /**
   * Adds the error listener to the internal list.
   *
   * @param l		the listener to add
   */
  public void addJShellErrorListener(JShellErrorListener l) {
    m_JShellErrorListeners.add(l);
  }

  /**
   * Removes the listener to the internal list.
   *
   * @param l		the listener to remove
   */
  public void removeJShellErrorListener(JShellErrorListener l) {
    m_JShellErrorListeners.remove(l);
  }

  /**
   * Notifies all the listeners with the specified event.
   *
   * @param e		the event to send
   */
  public synchronized void notifyJShellErrorListeners(JShellErrorEvent e) {
    debugMsg("Error: " + e.getMessage() + (e.hasException() ? "" : "\n" + e.getException()));
    for (JShellErrorListener l: m_JShellErrorListeners)
      l.jshellErrorOccurred(e);
  }
}
