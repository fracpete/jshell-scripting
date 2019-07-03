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
 * JShellErrorMessageEvent.java
 * Copyright (C) 2019 University of Waikato, Hamilton, NZ
 */

package com.github.fracpete.jshell.event;

import com.github.fracpete.jshell.JShellExec;

import java.util.EventObject;

/**
 * Event that gets sent if an error occurred during JShell execution.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class JShellErrorEvent
  extends EventObject {

  /** the error message. */
  protected String m_Message;

  /** the optional exception. */
  protected Throwable m_Exception;

  /**
   * Initializes the error message with no exception.
   *
   * @param source	the source of the error
   * @param message	the error message
   */
  public JShellErrorEvent(JShellExec source, String message) {
    this(source, message, null);
  }

  /**
   * Initializes the error message.
   *
   * @param source	the source of the error
   * @param message	the error message
   * @param exception	the optional execption
   */
  public JShellErrorEvent(JShellExec source, String message, Throwable exception) {
    super(source);
    m_Message   = message;
    m_Exception = exception;
  }

  /**
   * Returns the message.
   *
   * @return		the message
   */
  public String getMessage() {
    return m_Message;
  }

  /**
   * Checks whether an exception is available.
   *
   * @return		true if exception available
   */
  public boolean hasException() {
    return (m_Exception != null);
  }

  /**
   * Returns the exception.
   *
   * @return		the exception, null if none available
   */
  public Throwable getException() {
    return m_Exception;
  }

  /**
   * Returns a string representation of the event.
   *
   * @return		the representation
   */
  public String toString() {
    return getSource() + ", message=" + m_Message + ", exception=" + m_Exception;
  }
}
