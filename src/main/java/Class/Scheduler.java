/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Class;

import EDD.CustomQueue;
import ENV.ProcessState;
import ENV.SchedulingAlgorithm;
import static ENV.SchedulingAlgorithm.FCFS;
import static ENV.SchedulingAlgorithm.FEEDBACK;
import static ENV.SchedulingAlgorithm.HRRN;
import static ENV.SchedulingAlgorithm.ROUND_ROBIN;
import static ENV.SchedulingAlgorithm.SJF;
import static ENV.SchedulingAlgorithm.SRT;
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
    private int quantum = 6;
    private CustomQueue<Process>[] feedbackQueues; // Arreglo de colas con diferentes prioridades
    private int[] quantumPerLevel; // Quantum para cada nivel
    private static final int NUM_LEVELS = 3; // Número de niveles de prioridad

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
        this.feedbackQueues = new CustomQueue[NUM_LEVELS];
        for (int i = 0; i < NUM_LEVELS; i++) {
            feedbackQueues[i] = new CustomQueue<>();
        }

        // Configurar quantum para cada nivel (aumenta exponencialmente)
        this.quantumPerLevel = new int[NUM_LEVELS];
        for (int i = 0; i < NUM_LEVELS; i++) {
            quantumPerLevel[i] = (int) Math.pow(2, i) * 2; // 2, 4, 8 unidades de tiempo
        }
    }

    public void createProcess(String name, int instructions, boolean isIOBound, int cyclesUntilInterrupt, int cyclesForIO) {
        Process process = new Process(nextProcessId++, name, instructions, isIOBound, cyclesUntilInterrupt, cyclesForIO);
        readyQueue.enqueue(process);
        allQueue.enqueue(process);
    }

    public void printQueues() {
        System.out.println("Ready Queue:");
        readyQueue.printQueue();

        System.out.println("\nAll Queue:");
        allQueue.printQueue();

        System.out.println("\nBloqueados");
        this.blockedQueue.printQueue();
    }

    public void addCPU(int newCount) {
        for (int i = 0; i < 5; i++) {
            if (this.cpus[i] != null) {
                System.out.println("CPUS" + this.cpus[i].getId());
            }
        }
        for (int i = newCount-1; i < 5; i++) {
            System.out.println("iteraciones");
            if (this.cpus[i] == null) {
                this.cpus[i] = new CPU(i + 1);
                System.out.println("salio1");
            }
            break;
        }
        cpuSemaphore.release();
    }

    private double calculatePriorityScore(Process process) {
        double score = 0;

        // Factor 1: Instrucciones restantes (menor es mejor)
        score += process.getRemainingInstructions();

        // Factor 2: Tiene interrupciones (los procesos sin interrupciones tienen prioridad)
        if (!process.isIOBound()) {
            score -= 1000; // Bonificación para procesos sin interrupciones
        }

        // Factor 3: Tiempo de espera por IO (más tiempo de espera tiene prioridad)
        if (process.isIOBound()) {
            score += process.getCyclesForIO() * 100; // Penalizar tiempos IO más largos
            score -= process.cyclesForIO * 50; // Priorizar procesos que han esperado más
        }

        return score;
    }

    public void sortReadyQueueByPriority() {
        if (readyQueue.isEmpty() || readyQueue.size() == 1) {
            return;
        }

        CustomQueue<Process> sortedQueue = new CustomQueue<>();
        CustomQueue<Process> tempQueue = new CustomQueue<>();

        // Copiar todos los procesos a tempQueue
        while (!readyQueue.isEmpty()) {
            Process axu = readyQueue.dequeue();
            tempQueue.enqueue(axu);
        }

        // Ordenar los procesos por prioridad
        while (!tempQueue.isEmpty()) {
            Process highestPriorityProcess = null;
            double highestPriorityScore = Double.MAX_VALUE;
            CustomQueue<Process> auxiliaryQueue = new CustomQueue<>();

            // Buscar el proceso con la mayor prioridad
            while (!tempQueue.isEmpty()) {
                Process currentProcess = tempQueue.dequeue();
                double currentScore = calculatePriorityScore(currentProcess);

                // Si el proceso tiene mayor prioridad, lo ponemos en sortedQueue
                if (currentScore < highestPriorityScore) {
                    if (highestPriorityProcess != null) {
                        auxiliaryQueue.enqueue(highestPriorityProcess);
                    }
                    highestPriorityProcess = currentProcess;
                    highestPriorityScore = currentScore;
                } else {
                    auxiliaryQueue.enqueue(currentProcess);
                }
            }

            // Agregar el proceso con mayor prioridad a la cola ordenada
            if (highestPriorityProcess != null) {
                sortedQueue.enqueue(highestPriorityProcess);
            }

            // Volver a colocar los procesos restantes en tempQueue
            while (!auxiliaryQueue.isEmpty()) {
                tempQueue.enqueue(auxiliaryQueue.dequeue());
            }
        }

        // Actualizar la readyQueue con la cola ordenada
        this.readyQueue = sortedQueue;
        this.readyQueue.printQueue();
    }

    public void deleteCPU(int newCount) throws InterruptedException {

        for (int i = newCount; i >= 2; i--) {
            System.out.println("iteraciones2");
            if (this.cpus[i] != null) {
                if(this.cpus[i].getCurrentProcess()!=null){
                    if (this.cpus[i+1].getCurrentProcess().getStateProcess().equals(ProcessState.BLOCKED)) {
                    // Si el proceso está bloqueado, liberar CPU y mover a cola de bloqueados
                    System.out.println("CPU " + this.cpus[i].getId() + " liberado del proceso " + this.cpus[i].getCurrentProcess().getIdProcess());
                    blockedQueue.enqueue(this.cpus[i+1].getCurrentProcess());
                } else if (this.cpus[i+1].getCurrentProcess().getStateProcess().equals(ProcessState.FINISHED)) {
                    // Si el proceso terminó, mover a cola de finalizados
                    finishedQueue.enqueue(this.cpus[i+1].getCurrentProcess());
                    System.out.println("Proceso " + this.cpus[i].getCurrentProcess().getIdProcess() + " movido a cola de finalizados");
                    System.out.println("CPU " + this.cpus[i+1].getId() + " liberado del proceso " + this.cpus[i+1].getCurrentProcess().getIdProcess());

                }
                    else if (this.cpus[i].getCurrentProcess().getStateProcess().equals(ProcessState.READY)) {
                    // Si el proceso terminó, mover a cola de finalizados
                    readyQueue.enqueue(this.cpus[i+1].getCurrentProcess());
                    System.out.println("Proceso " + this.cpus[i].getCurrentProcess().getIdProcess() + " movido a cola de finalizados");
                    System.out.println("CPU " + this.cpus[i].getId() + " liberado del proceso " + this.cpus[i].getCurrentProcess().getIdProcess());
                }
    
                }
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
            case SRT:
                selectedProcess = selectShortestJob(true); // true indica que es preemptivo
                if (selectedProcess != null) {
                    handlePreemption(selectedProcess);
                }
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
                break;

            case FEEDBACK:
                selectedProcess = getNextProcessMLFQ();
                break;
            case HRRN:
                double highestRatio = -1;
                while (!readyQueue.isEmpty()) {
                    Process process = readyQueue.dequeue();
                    double ratio = calculateResponseRatio(process);

                    if (ratio > highestRatio) {
                        if (selectedProcess != null) {
                            tempQueue.enqueue(selectedProcess);
                        }
                        selectedProcess = process;
                        highestRatio = ratio;
                    } else {
                        tempQueue.enqueue(process);
                    }
                }
                // Restaurar los procesos no seleccionados a la cola
                while (!tempQueue.isEmpty()) {
                    readyQueue.enqueue(tempQueue.dequeue());
                }
                break;
        }

        return selectedProcess;
    }

    private void handlePreemption(Process newProcess) {
        if (newProcess == null) {
            return;
        }

        // Verificar todos los CPUs para encontrar un proceso con un tiempo restante mayor
        for (CPU cpu : cpus) {
            if (cpu != null && cpu.isBusy()) {
                Process runningProcess = cpu.getCurrentProcess();
                if (runningProcess != null
                        && runningProcess.getRemainingInstructions() > newProcess.getRemainingInstructions()) {
                    // Preemptar el proceso en ejecución
                    cpu.releaseProcess();
                    readyQueue.enqueue(runningProcess);
                    runningProcess.setStateProcess(ProcessState.READY);
                    System.out.println("Proceso " + runningProcess.getIdProcess() + " preemptado por SRT");
                    break;
                }
            }
        }
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

    private double calculateResponseRatio(Process process) {
        double waitingTime = process.getWaitTime();
        double serviceTime = process.getRemainingInstructions();
        return (waitingTime + serviceTime) / serviceTime;
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

    private boolean shouldPreempt(Process currentProcess) {
        if (readyQueue.isEmpty()) {
            return false; // No hay procesos en la cola de listos para preemptar
        }

        // Obtener el proceso con el tiempo restante más corto de la cola de listos
        Process shortestJob = null;
        try {
            shortestJob = selectShortestJob(true);
        } catch (NullPointerException e) {
            // Manejar la excepción si tempQueue es null o si dequeue() falla
            System.err.println("Error: tempQueue es null o no se pudo desencolar.");
            e.printStackTrace(); // Imprimir el stack trace para depuración
        }
        // Verificar si el proceso en ejecución debe ser preemptado
        if (shortestJob != null && shortestJob.getRemainingInstructions() < currentProcess.getRemainingInstructions()) {
            return true; // Hay un proceso con tiempo restante menor
        }

        return false; // No es necesario preemptar
    }

    private void executeMFLQ(CPU cpu, Process process) {
        cpu.assignProcess(process);
        process.setStateProcess(ProcessState.RUNNING);
        System.out.println("CPU " + cpu.getId() + " ejecutando proceso " + process.getIdProcess() + " en nivel " + process.getPriorityLevel());

        Thread executionThread = new Thread(() -> {
            int currentQuantum = quantumPerLevel[process.getPriorityLevel()];
            int programCounter = 0;

            while (!process.getStateProcess().equals(ProcessState.FINISHED)
                    && !process.getStateProcess().equals(ProcessState.BLOCKED)) {

                process.executeNextInstruction();
                programCounter++;

                if (programCounter >= currentQuantum && process.getStateProcess() == ProcessState.RUNNING) {
                    System.out.println("Quantum agotado para proceso " + process.getIdProcess() + " en nivel " + process.getPriorityLevel());

                    // Degradar prioridad si no es el último nivel
                    if (process.getPriorityLevel() < NUM_LEVELS - 1) {
                        process.setPriorityLevel(process.getPriorityLevel() + 1);
                        System.out.println("Proceso " + process.getIdProcess() + " degradado al nivel " + process.getPriorityLevel());
                    }

                    process.setStateProcess(ProcessState.READY);
                    cpu.releaseProcess();
                    cpuSemaphore.release();
                    feedbackQueues[process.getPriorityLevel()].enqueue(process);
                    break;
                }
            }

            if (process.getStateProcess().equals(ProcessState.BLOCKED)) {
                cpu.releaseProcess();
                cpuSemaphore.release();
                blockedQueue.enqueue(process);
            } else if (process.getStateProcess().equals(ProcessState.FINISHED)) {
                finishedQueue.enqueue(process);
                cpu.releaseProcess();
                cpuSemaphore.release();
            }
        });

        executionThread.start();
    }

    private void executeProcess(CPU cpu, Process process) {
        if (currentAlgorithm == SchedulingAlgorithm.FEEDBACK) {
            executeMFLQ(cpu, process);
        } else {
            cpu.assignProcess(process);
            process.setStateProcess(ProcessState.RUNNING);
            System.out.println("CPU " + cpu.getId() + " ahora está ocupado con proceso " + process.getIdProcess());

            Thread executionThread = new Thread(() -> {
                int programcounter = 0;
                while (!process.getStateProcess().equals(ProcessState.FINISHED)
                        && !process.getStateProcess().equals(ProcessState.BLOCKED)) {

                    process.executeNextInstruction();
                    programcounter++;
                    if (programcounter == this.quantum && this.getCurrentAlgorithm() == SchedulingAlgorithm.ROUND_ROBIN && process.getStateProcess() == ProcessState.RUNNING) {
                        System.out.println("Se acabo el tiempo");
                        process.setStateProcess(ProcessState.READY);
                        cpu.releaseProcess();
                        cpuSemaphore.release();
                        this.readyQueue.enqueue(process);
                        break;

                    }
                    if (this.getCurrentAlgorithm() == SchedulingAlgorithm.SRT && shouldPreempt(process) && process.getStateProcess() == ProcessState.RUNNING) {
                        System.out.println("Proceso " + process.getIdProcess() + " preemptado por SRT");
                        process.setStateProcess(ProcessState.READY);
                        cpu.releaseProcess();
                        cpuSemaphore.release();
                        this.readyQueue.enqueue(process);
                        break; // Salir del bucle de ejecución
                    }
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
    }

    private Process getNextProcessMLFQ() {
        // Buscar proceso en orden de prioridad
        for (int i = 0; i < NUM_LEVELS; i++) {
            if (!feedbackQueues[i].isEmpty()) {
                return feedbackQueues[i].dequeue();
            }
        }
        return null;
    }

    private Process selectShortestJob(boolean preemptive) {
        if (readyQueue.isEmpty()) {
            return null;
        }

        CustomQueue<Process> tempQueue = new CustomQueue<>();
        Process shortestJob = null;
        int shortestTime = Integer.MAX_VALUE;

        while (!readyQueue.isEmpty()) {
            Process current = readyQueue.dequeue();
            int currentTime = current.getRemainingInstructions();

            if (currentTime < shortestTime) {
                if (shortestJob != null) {
                    tempQueue.enqueue(shortestJob);
                }
                shortestJob = current;
                shortestTime = currentTime;
            } else {
                tempQueue.enqueue(current);
            }
        }

        // Restaurar los procesos que no fueron seleccionados en la cola de listos
        while (!tempQueue.isEmpty()) {

            try {

                // Intentar desencolar un elemento de tempQueue y encolarlo en readyQueue
                Process process = tempQueue.dequeue(); // Desencolar
                if (process != null) { // Verificar si el proceso no es nulo
                    readyQueue.enqueue(process); // Encolar en readyQueue
                }
            } catch (NullPointerException e) {
                // Manejar la excepción si tempQueue es null o si dequeue() falla
                System.err.println("Error: tempQueue es null o no se pudo desencolar.");
                e.printStackTrace(); // Imprimir el stack trace para depuración
            }
        }

        return shortestJob;
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
