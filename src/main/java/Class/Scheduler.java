/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Class;

import EDD.CustomQueue;
import ENV.ProcessState;
import ENV.SchedulingAlgorithm;
import java.util.concurrent.Semaphore;

/**
 *
 * @author Indatech
 */
public class Scheduler extends Thread {

    private CPU[] cpus;
    private CustomQueue<Process> allQueue;
    private CustomQueue<Process> readyQueue;
    private CustomQueue<Process> blockedQueue;
    private CustomQueue<Process> finishedQueue;
    private int cycleDuration;
    private Semaphore cpuSemaphore;
    private Semaphore processSemaphoer;
    private int nextProcessId;
    private SchedulingAlgorithm currentAlgorithm = SchedulingAlgorithm.FCFS;

    public Scheduler(int numCPUs) {
        this.allQueue = new CustomQueue<>();
        this.readyQueue = new CustomQueue<>();
        this.blockedQueue = new CustomQueue<>();
        this.finishedQueue = new CustomQueue<>();
        this.cycleDuration = 1000;
        this.processSemaphoer = new Semaphore(0);
        this.cpuSemaphore = new Semaphore(numCPUs);
        this.nextProcessId = 1;
        this.cpus = new CPU[5];
        for (int i = 0; i < numCPUs; i++) {
            this.cpus[i] = new CPU(i + 1);
        }
    }

    public void createProcess(String name, int instructions, boolean isIOBound, int cyclesUntilInterrupt, int cyclesForIO) {
        Process process = new Process(nextProcessId++, name, instructions, isIOBound, cyclesUntilInterrupt, cyclesForIO);
        readyQueue.enqueue(process);
        allQueue.enqueue(process);
        printQueues();
    }

    public void printQueues() {
        System.out.println("Ready Queue:");
        readyQueue.printQueue();

        System.out.println("\nAll Queue:");
        allQueue.printQueue();

        System.out.println("\nBloqueados");
        this.blockedQueue.printQueue();
    }

    public void addCPU() {
        for (int i = 0; i < 5; i++) {
            if (this.cpus[i] != null) {
                System.out.println("CPUS" + this.cpus[i].getId());
            }
        }
        for (int i = 2; i < 5; i++) {
            System.out.println("iteraciones");
            if (this.cpus[i] == null) {
                this.cpus[i] = new CPU(i + 1);
                System.out.println("salio1");
                break;

            }
        }
        cpuSemaphore.release();
    }

    public void deleteCPU() throws InterruptedException {

        for (int i = 4; i >= 2; i--) {
            System.out.println("iteraciones2");
            if (this.cpus[i] != null) {
                this.cpus[i] = null;
                System.out.println("salio2");
                break;
            }
        }
        for (int i = 0; i < 5; i++) {
            if (this.cpus[i] != null) {
                System.out.println(this.cpus[i].getId());
            }
        }
        cpuSemaphore.acquire();
    }

    public void assignCPU(Process process) throws InterruptedException {
        cpuSemaphore.acquire(); // Espera un CPU disponible

        CPU freeCPU = getNotBusyCPU(); // Obtiene un CPU libre

        if (freeCPU != null) {
            freeCPU.assignProcess(process);
            process.setStateProcess(ProcessState.RUNNING);

            // Ejecutar el proceso en un nuevo hilo sin bloquear el scheduler
            new Thread(() -> {
                process.start();
                try {
                    process.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                freeCPU.releaseProcess();
                cpuSemaphore.release();
            }).start();

        } else {
            System.out.println("No hay CPU libre, encolando el proceso");
            readyQueue.enqueue(process);
        }
    }

    public Process getNextProcess() {
        CustomQueue<Process> tempQueue = new CustomQueue<>();
        Process selectedProcess = null;

        switch (currentAlgorithm) {
            case FCFS:
                selectedProcess = readyQueue.dequeue();
                break;

            case SJF:
                int shortestTime = Integer.MAX_VALUE;
                while (!readyQueue.isEmpty()) {
                    Process process = readyQueue.dequeue();
                    if (process.getRemainingInstructions() < shortestTime) {
                        if (selectedProcess != null) {
                            tempQueue.enqueue(selectedProcess);
                        }
                        selectedProcess = process;
                        shortestTime = process.getRemainingInstructions();
                    } else {
                        tempQueue.enqueue(process);
                    }
                }
                while (!tempQueue.isEmpty()) {
                    readyQueue.enqueue(tempQueue.dequeue());
                }
                break;

            case ROUND_ROBIN:
                selectedProcess = readyQueue.dequeue();
                if (selectedProcess != null) {
                    readyQueue.enqueue(selectedProcess);
                }
                break;
        }

        return selectedProcess;
    }

    public CPU getNotBusyCPU() {
        for (int i = 0; i < 5; i++) {
            if (this.cpus[i] != null) {
                if (!this.cpus[i].isBusy()) {
                    return this.cpus[i];
                }
            }
        }
        return null;
    }

   @Override
    public void run() {
        while (true) {
            try {
                // Actualizar procesos bloqueados
                checkAndUpdateBlockedProcesses();

                // Asignar procesos listos a CPUs disponibles
                while (!readyQueue.isEmpty()) {
                    Process nextProcess = getNextProcess();
                    if (nextProcess != null) {
                        if (cpuSemaphore.tryAcquire()) {
                            CPU freeCPU = getNotBusyCPU();
                            if (freeCPU != null) {
                                executeProcess(freeCPU, nextProcess);
                            } else {
                                cpuSemaphore.release();
                                readyQueue.enqueue(nextProcess);
                            }
                        } else {
                            readyQueue.enqueue(nextProcess);
                        }
                    }
                }

                Thread.sleep(cycleDuration / 2);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }
      private void checkAndUpdateBlockedProcesses() {
        if (!blockedQueue.isEmpty()) {
            CustomQueue<Process> tempQueue = new CustomQueue<>();
            
            while (!blockedQueue.isEmpty()) {
                Process blockedProcess = blockedQueue.dequeue();
                
                try {
                    Thread.sleep(100);
                    blockedProcess.ioWaitTime++;
                    
                    if (blockedProcess.getIOProgress() >= blockedProcess.getCyclesForIO()) {
                        // El proceso ha completado su E/S, mover a cola de listos
                        blockedProcess.reanudar();
                        readyQueue.enqueue(blockedProcess);
                    } else {
                        tempQueue.enqueue(blockedProcess);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            
            while (!tempQueue.isEmpty()) {
                blockedQueue.enqueue(tempQueue.dequeue());
            }
        }
    }

     private void executeProcess(CPU cpu, Process process) {
        cpu.assignProcess(process);
        process.setStateProcess(ProcessState.RUNNING);
        System.out.println("CPU " + cpu.getId() + " ahora está ocupado con proceso " + process.getIdProcess());

        Thread executionThread = new Thread(() -> {
            while (!process.getStateProcess().equals(ProcessState.FINISHED) && 
                   !process.getStateProcess().equals(ProcessState.BLOCKED)) {
                process.executeNextInstruction();
            }

            if (process.getStateProcess().equals(ProcessState.BLOCKED)) {
                // Si el proceso está bloqueado, liberar CPU y mover a cola de bloqueados
                System.out.println("CPU " + cpu.getId() + " liberado del proceso " + process.getIdProcess());
                cpu.releaseProcess();
                cpuSemaphore.release();
                blockedQueue.enqueue(process);
            } else if (process.getStateProcess().equals(ProcessState.FINISHED)) {
                // Si el proceso terminó, mover a cola de finalizados
                finishedQueue.enqueue(process);
                System.out.println("Proceso " + process.getIdProcess() + " movido a cola de finalizados");
                System.out.println("CPU " + cpu.getId() + " liberado del proceso " + process.getIdProcess());
                cpu.releaseProcess();
                cpuSemaphore.release();
            }
        });
        
        executionThread.start();
    }

    public void setCycleDuration(int duration) {
        this.cycleDuration = duration;
    }

    public void setAlgorithm(SchedulingAlgorithm algorithm) {
        this.currentAlgorithm = algorithm;
    }

    public CPU[] getCpus() {
        return cpus;
    }

    public void setCpus(CPU[] cpus) {
        this.cpus = cpus;
    }

    public CustomQueue<Process> getAllQueue() {
        return allQueue;
    }

    public void setAllQueue(CustomQueue<Process> allQueue) {
        this.allQueue = allQueue;
    }

    public CustomQueue<Process> getReadyQueue() {
        return readyQueue;
    }

    public void setReadyQueue(CustomQueue<Process> readyQueue) {
        this.readyQueue = readyQueue;
    }

    public CustomQueue<Process> getBlockedQueue() {
        return blockedQueue;
    }

    public void setBlockedQueue(CustomQueue<Process> blockedQueue) {
        this.blockedQueue = blockedQueue;
    }

    public CustomQueue<Process> getFinishedQueue() {
        return finishedQueue;
    }

    public void setFinishedQueue(CustomQueue<Process> finishedQueue) {
        this.finishedQueue = finishedQueue;
    }

    public Semaphore getCpuSemaphore() {
        return cpuSemaphore;
    }

    public void setCpuSemaphore(Semaphore cpuSemaphore) {
        this.cpuSemaphore = cpuSemaphore;
    }

    public Semaphore getProcessSemaphoer() {
        return processSemaphoer;
    }

    public void setProcessSemaphoer(Semaphore processSemaphoer) {
        this.processSemaphoer = processSemaphoer;
    }

    public int getNextProcessId() {
        return nextProcessId;
    }

    public void setNextProcessId(int nextProcessId) {
        this.nextProcessId = nextProcessId;
    }

    public SchedulingAlgorithm getCurrentAlgorithm() {
        return currentAlgorithm;
    }

    public void setCurrentAlgorithm(SchedulingAlgorithm currentAlgorithm) {
        this.currentAlgorithm = currentAlgorithm;
    }

}
