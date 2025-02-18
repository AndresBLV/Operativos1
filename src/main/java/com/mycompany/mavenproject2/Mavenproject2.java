/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package com.mycompany.mavenproject2;

import Class.Scheduler;
import ENV.SchedulingAlgorithm;
import SimuladorUI.SimulatorGUI;
import javax.swing.SwingUtilities;

/**
 *
 * @author Indatech
 */
public class Mavenproject2 {

    public static void main(String[] args) {
        Scheduler s = new Scheduler(2);
        s.setAlgorithm(SchedulingAlgorithm.HRRN);
        s.createProcess("P1", 10, false, 3, 2);
        s.createProcess("P2", 15, true, 5, 3);
        s.createProcess("P3", 8, false, 2, 1);
        s.createProcess("P4", 12, true, 4, 2);
        s.createProcess("P5", 20, false, 6, 3);
        s.createProcess("P6", 3, false, 4, 2);
        s.start();

    }
}
