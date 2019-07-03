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
 * RunningGUI.java
 * Copyright (C) 2019 University of Waikato, Hamilton, NZ
 */

package com.github.fracpete.jshell.examples;

import com.github.fracpete.jshell.JShellExec;
import com.github.fracpete.jshell.event.JShellErrorEvent;
import com.github.fracpete.jshell.event.JShellErrorListener;

/**
 * Shows how to execute code in the background and listen to errors.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class ExecErrors {

  public static void main(String[] args) {
    String code = "for (int i = 0; i < 10; i++) System.out.println(j)";
    JShellExec exec = new JShellExec();
    exec.addJShellErrorListener(new JShellErrorListener() {
      public void jshellErrorOccurred(JShellErrorEvent e) {
        System.err.println(e.getMessage());
        if (e.hasException())
          e.getException().printStackTrace();
      }
    });
    exec.runScript(code);
  }
}
