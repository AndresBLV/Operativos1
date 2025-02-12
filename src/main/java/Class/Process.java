/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Class;

import ENV.ProcessState;

/**
 *
 * @author Indatech
 */
public class Process extends Thread {
    private int id;
    private String name;
    private ProcessState state;
    private int programCounter;
    private int memoryAddressRegister;
    private int totalInstructions;
    private boolean isIOBound;
    private int cyclesUntilInterrupt;
    private int cyclesForIO;
    private int remainingInstructions;
    private volatile boolean pause;
    private int currentIOCycleCount;
     int ioWaitTime;
    private volatile boolean isRunning;

    public Process(int id, String name, int totalInstructions, boolean isIOBound, int cyclesUntilInterrupt, int cyclesForIO) {
        this.id = id;
        this.name = name;
        this.state = ProcessState.READY;
        this.programCounter = 0;
        this.memoryAddressRegister = 0;
        this.totalInstructions = totalInstructions;
        this.isIOBound = isIOBound;
        this.cyclesUntilInterrupt = cyclesUntilInterrupt;
        this.cyclesForIO = cyclesForIO;
        this.remainingInstructions = totalInstructions;
        this.pause = false;
        this.currentIOCycleCount = 0;
        this.ioWaitTime = 0;
        this.isRunning = true;
    }

    public void executeNextInstruction() {
        if (!pause && programCounter < totalInstructions) {
            // Verificar si necesita E/S
            if (isIOBound) {
                currentIOCycleCount++;
                if (currentIOCycleCount >= cyclesUntilInterrupt) {
                    System.out.println("Proceso " + id + " requiere E/S - Iniciando interrupción");
                    pausar();
                    return;
                }
            }

            // Ejecutar instrucción
            programCounter++;
            remainingInstructions--;
            System.out.println("Proceso " + id + " ejecutando instrucción " + programCounter);

            // Verificar finalización
            if (programCounter >= totalInstructions) {
                this.setStateProcess(ProcessState.FINISHED);
                System.out.println("Proceso " + id + " ha finalizado");
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    public void pausar() {
        pause = true;
        this.setStateProcess(ProcessState.BLOCKED);
        System.out.println("Proceso " + id + " bloqueado por E/S");
    }

    public void reanudar() {
        pause = false;
        ioWaitTime = 0;
        currentIOCycleCount = 0;
        this.setStateProcess(ProcessState.READY);
        System.out.println("Proceso " + id + " desbloqueado");
    }

    public boolean isBlocked() {
        return pause;
    }

    public void setIoWaitTime(int ioWaitTime) {
        this.ioWaitTime = ioWaitTime;
    }

    
    public boolean needsToBeUnblocked() {
        return pause && ioWaitTime >= cyclesUntilInterrupt;
    }

    public int getIOProgress() {
        return ioWaitTime;
    }

    // Getters and setters
    public int getIdProcess() {
        return id;
    }

    public String getNameProcess() {
        return name;
    }

    public ProcessState getStateProcess() {
        return state;
    }

    public void setStateProcess(ProcessState state) {
        this.state = state;
    }

    public int getProgramCounter() {
        return programCounter;
    }

    public void setProgramCounter(int pc) {
        this.programCounter = pc;
    }

    public int getMAR() {
        return memoryAddressRegister;
    }

    public void setMAR(int mar) {
        this.memoryAddressRegister = mar;
    }

    public boolean isIOBound() {
        return isIOBound;
    }

    public int getCyclesUntilInterrupt() {
        return cyclesUntilInterrupt;
    }

    public int getCyclesForIO() {
        return cyclesForIO;
    }

    public int getRemainingInstructions() {
        return remainingInstructions;
    }

    public void decrementRemainingInstructions() {
        if (remainingInstructions > 0) {
            remainingInstructions--;
        }
    }

    @Override
    public String toString() {
        return "Process{id=" + id + ", name='" + name + "', state=" + state + "}";
    }
}
