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
 * JShellErrorMessageListener.java
 * Copyright (C) 2019 University of Waikato, Hamilton, NZ
 */

package com.github.fracpete.jshell.event;

/**
 * Interface for classes that react to JShell error messages.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public interface JShellErrorListener {

  /**
   * Gets called when an error occurred.
   *
   * @param e		the error
   */
  public void jshellErrorOccurred(JShellErrorEvent e);
}
