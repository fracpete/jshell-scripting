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
 * JShellEvent.java
 * Copyright (C) 2018 University of Waikato, Hamilton, NZ
 */

package com.github.fracpete.jshell.event;

import com.github.fracpete.jshell.JShellPanel;

import java.util.EventObject;

/**
 * Events sent by the {@link JShellPanel}.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class JShellEvent
  extends EventObject {

  /** the type of event. */
  public enum EventType {
    SCRIPT_LOAD_SUCCESS,
    SCRIPT_LOAD_FAILURE,
    SCRIPT_SAVE_SUCCESS,
    SCRIPT_SAVE_FAILURE,
    SCRIPT_RUN_SETUP_FAILURE,
    SCRIPT_RUN,
    SCRIPT_RUN_FAILURE,
    SCRIPT_RUN_SUCCESS,
    SCRIPT_STOP,
    SCRIPT_FINISHED,
    OUTPUT_CLEARED,
    OUTPUT_SAVE_SUCESS,
    OUTPUT_SAVE_FAILURE,
  }

  /** the event type. */
  protected EventType m_Type;

  /**
   * Constructs a prototypical Event.
   *
   * @param source the panel on which the Event initially occurred
   * @throws IllegalArgumentException if source is null
   */
  public JShellEvent(JShellPanel source, EventType type) {
    super(source);
    m_Type = type;
  }

  /**
   * Returns the panel that triggered the event.
   *
   * @return the source panel
   */
  public JShellPanel getPanel() {
    return (JShellPanel) getSource();
  }

  /**
   * Returns the event type.
   *
   * @return the event type
   */
  public EventType getType() {
    return m_Type;
  }
}
