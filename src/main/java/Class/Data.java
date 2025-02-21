/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Class;

import EDD.CustomQueue;

/**
 *
 * @author andre
 */
public class Data {
    
    private CPU[] cpus;
    private CustomQueue<Process> allQueue;
    private CustomQueue<Process> readyQueue;
    private CustomQueue<Process> blockedQueue;
    private CustomQueue<Process> finishedQueue;
    private int counterCPU;
    private int cycleDuration;
    
    public Data(){}
    
    public Data(CPU[] cpus,CustomQueue<Process> allQueue,CustomQueue<Process> readyQueue,CustomQueue<Process> blockedQueue,CustomQueue<Process> finishedQueue,int counterCPU,int cycleDuration) {
        this.counterCPU = counterCPU;
        this.allQueue = allQueue;
        this.readyQueue = readyQueue;
        this.blockedQueue = blockedQueue;
        this.finishedQueue = finishedQueue;
        this.cycleDuration = cycleDuration;
        this.cpus = cpus;
    }
    
    public int getCounterCPU() {
        return counterCPU;
    }

    public void setCounterCPU(int counter) {
        this.counterCPU = counter;
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
    
    public int getCycleDuration(){
        return cycleDuration;
    }
    
    public void setCycleDuration(int duration) {
        this.cycleDuration = duration;
    }
}
