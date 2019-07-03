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

import com.github.fracpete.jshell.JShellPanel;
import com.github.fracpete.jshell.event.JShellExecEvent;
import com.github.fracpete.jshell.event.JShellPanelEvent;
import nz.ac.waikato.cms.gui.core.BaseFrame;

import javax.swing.JFrame;
import java.awt.BorderLayout;

/**
 * Shows how to display the JShellPanel.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class GUI {

  public static void main(String[] args) {
    JShellPanel panel = new JShellPanel();
    panel.addJShellExecListener((JShellExecEvent e) -> System.out.println("exec: " + e.getType()));
    panel.addJShellPanelListener((JShellPanelEvent e) -> System.out.println("panel: " + e.getType()));
    JFrame frame = new JFrame("JShell");
    frame.getContentPane().setLayout(new BorderLayout());
    frame.getContentPane().add(panel, BorderLayout.CENTER);
    frame.setSize(1200, 900);
    frame.setDefaultCloseOperation(BaseFrame.EXIT_ON_CLOSE);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }
}
