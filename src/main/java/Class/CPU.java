package Class;

public class CPU {

    private int id;
    private Process currentProcess;
    private boolean busy;
    private int clockCycle;

    public CPU(int id) {
        this.id = id;
        this.busy = false;
        this.clockCycle = 0;
    }

    public void assignProcess(Process process) {
        if (this.currentProcess == null || !this.busy) {
        this.currentProcess = process;
        this.busy = true;
        System.out.println("CPU " + id + " ahora está ocupado con proceso " + process.getIdProcess());

    
    }}

    public void releaseProcess() {
       if (this.currentProcess != null) {
        System.out.println("CPU " + id + " liberado del proceso " + currentProcess.getIdProcess());
        this.currentProcess = null;
        this.busy = false;
    
  
       }
    }

    public boolean isBusy() {
        return busy;
    }

    public int getId() {
        return id;
    }

    public Process getCurrentProcess() {
        return currentProcess;
    }

    public int getClockCycle() {
        return clockCycle;
    }

    public void incrementClockCycle() {
        clockCycle++;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
    }

}
