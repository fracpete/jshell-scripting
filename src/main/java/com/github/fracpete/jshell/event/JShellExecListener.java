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
 * JShellListener.java
 * Copyright (C) 2018 FracPete
 */

package com.github.fracpete.jshell.event;

/**
 * Interface for classes that listen to events of JShellExec.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public interface JShellExecListener {

  /**
   * Gets triggered with any event in the JShellPanel.
   *
   * @param e		the event
   */
  public void jshellExecEventOccurred(JShellExecEvent e);
}
