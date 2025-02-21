package SimuladorUI;

import Class.CPU;
import Class.Data;
import Class.Scheduler;
import Class.Process;
import ENV.ProcessState;
import ENV.SchedulingAlgorithm;
import EDD.CustomQueue;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


public class SimulatorGUI extends JFrame {

    private static final int UPDATE_INTERVAL = 100;
    private static final int MIN_CPU_COUNT = 2;
    private static final int MAX_CPU_COUNT = 4;
    private static final int DEFAULT_CPU_COUNT = 2;

    private final Scheduler scheduler;

    private final JPanel cpuPanel;
    private final JPanel queuePanel;
    private final JPanel controlPanel;
    private final Timer updateTimer;
    private boolean isSimulationRunning = false;
    private Thread schedulerThread;

    public SimulatorGUI() {
        super("OS Process Scheduler Simulator");

        // Initialize core components
        this.scheduler = new Scheduler(DEFAULT_CPU_COUNT);

        // Initialize GUI components
        this.cpuPanel = createCpuPanel();
        this.queuePanel = createQueuePanel();
        this.controlPanel = createControlPanel();
        this.updateTimer = new Timer(UPDATE_INTERVAL, e -> updateGUI());

        setupMainWindow();

    }

    private void setupMainWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setPreferredSize(new Dimension(800, 600));

        add(new JScrollPane(cpuPanel), BorderLayout.WEST);
        add(new JScrollPane(queuePanel), BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    private JPanel createCpuPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("CPUs"));
        return panel;
    }

    private JPanel createQueuePanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Process Queues"));
        return panel;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Controls"));

        // Algorithm selection
        JComboBox<SchedulingAlgorithm> algorithmCombo = new JComboBox<>(SchedulingAlgorithm.values());
        algorithmCombo.addActionListener(e -> scheduler.setAlgorithm((SchedulingAlgorithm) algorithmCombo.getSelectedItem()));

        // Cycle duration control
        SpinnerNumberModel cycleDurationModel = new SpinnerNumberModel(1000, 100, 5000, 100);
        JSpinner cycleDurationSpinner = new JSpinner(cycleDurationModel);
        cycleDurationSpinner.addChangeListener(e -> scheduler.setCycleDuration((Integer) cycleDurationSpinner.getValue()));

        // CPU count control
        SpinnerNumberModel cpuCountModel = new SpinnerNumberModel(
                DEFAULT_CPU_COUNT, MIN_CPU_COUNT, MAX_CPU_COUNT, 1);
        JSpinner cpuCountSpinner = new JSpinner(cpuCountModel);
        Integer[] oldValues = {(Integer) cpuCountSpinner.getValue()};
        cpuCountSpinner.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
            int newValue = (Integer) cpuCountSpinner.getValue();
            int oldValue = oldValues[0];
            if (newValue > oldValue){
                updateCPUCount(newValue);
            }else{
                try {
                    deleteCPUCount((Integer) cpuCountSpinner.getValue());
                } catch (InterruptedException ex) {
                    Logger.getLogger(SimulatorGUI.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            oldValues[0] = newValue;
            }
            });
        
        // Process creation button
        JButton newProcessBtn = new JButton("New Process");
        newProcessBtn.addActionListener(e -> {
            showNewProcessDialog();
            updateGUI();
                });

        // Start simulation button
        JButton startButton = new JButton("Iniciar");
        startButton.addActionListener(e -> {
            if (!isSimulationRunning) {
                scheduler.setRunning(true);
                newProcessBtn.setVisible(false);
                start();
            }else{
                scheduler.setRunning(true);
                newProcessBtn.setVisible(false);                
            }
        });
        
        // Stop simulation button
        JButton stopButton = new JButton("Detener");
        stopButton.addActionListener(e -> {
            if (scheduler.getRunning()) {
                newProcessBtn.setVisible(true);
                scheduler.setRunning(false);
                JOptionPane.showMessageDialog(null,"Se ha detenido el simulador");
            }else{
                JOptionPane.showMessageDialog(null,"No ha iniciado el simulador");
            }
        });
        
        //Save simulation button
        JButton saveButton = new JButton("Guardar");
        saveButton.addActionListener(e -> {
            if (!scheduler.getRunning()) {
                String route = "src\\main\\java\\json\\data.json";
                scheduler.getAllQueue().printQueue();
                Data data = new Data(scheduler.getCpus(),scheduler.getAllQueue(),scheduler.getReadyQueue(), scheduler.getBlockedQueue(),scheduler.getFinishedQueue(),(Integer) cpuCountSpinner.getValue(),(Integer) cycleDurationSpinner.getValue());
                saveJson(data,route);
                JOptionPane.showMessageDialog(null,"Se han guardado las configuraciones");
            }
        });
        
        //Load simulation button
        JButton loadButton = new JButton("Cargar");
        loadButton.addActionListener(e -> {
            if (!scheduler.getRunning()) {
                String route = "src\\main\\java\\json\\data.json";
                Data dataRead = loadJson(route);
                scheduler.setCpus(dataRead.getCpus());
                scheduler.setAllQueue(dataRead.getAllQueue());
                scheduler.setReadyQueue(dataRead.getReadyQueue());
                scheduler.setBlockedQueue(dataRead.getBlockedQueue());
                scheduler.setFinishedQueue(dataRead.getFinishedQueue());
                cpuCountSpinner.setValue(dataRead.getCounterCPU());
                cycleDurationSpinner.setValue(dataRead.getCycleDuration());
                JOptionPane.showMessageDialog(null,"Se han cargado las configuraciones");

            }else{
                JOptionPane.showMessageDialog(null,"La simulacion ya inicio");
            }
        });
        
        //Graphic simulation button
        JButton graphicButton = new JButton("Graficar");
        graphicButton.addActionListener(e -> {
            if (!scheduler.getRunning()) {
                String route = "src\\main\\java\\json\\data.json";
                scheduler.getAllQueue().printQueue();
                Data data = new Data(scheduler.getCpus(),scheduler.getAllQueue(),scheduler.getReadyQueue(), scheduler.getBlockedQueue(),scheduler.getFinishedQueue(),(Integer) cpuCountSpinner.getValue(),(Integer) cycleDurationSpinner.getValue());
                saveJson(data,route);
                JOptionPane.showMessageDialog(null,"Se han guardado las configuraciones");
            }
        });
        
        // Add components to panel
        panel.add(new JLabel("Scheduling Algorithm:"));
        panel.add(algorithmCombo);
        panel.add(new JLabel("Cycle Duration (ms):"));
        panel.add(cycleDurationSpinner);
        panel.add(new JLabel("CPU Count:"));
        panel.add(cpuCountSpinner);
        panel.add(newProcessBtn);
        panel.add(startButton);
        panel.add(stopButton);
        panel.add(saveButton);
        panel.add(loadButton);

        return panel;
    }
    
    private static void saveJson (Data data, String nombreArchivo){
        ObjectMapper mapper = new ObjectMapper();
        try{
           mapper.writeValue(new File(nombreArchivo),data);
           System.out.println("Datos guardados en: "+nombreArchivo);
        }catch (IOException e){
            System.err.println("Error al guardar JSON: "+e.getMessage());
        }
    }
    
    private static Data loadJson(String nombreArchivo){
        ObjectMapper mapper = new ObjectMapper();
        try{
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return mapper.readValue(new File(nombreArchivo),Data.class);
        }catch (IOException e){
            System.err.println("Error al leer JSON: " + e.getMessage());
            return null;
        }
    }
    
    private void updateCPUCount(int newCount) {
        //System.out.println(Arrays.toString(scheduler.getCpus()));
        scheduler.addCPU(newCount);
        //System.out.println(Arrays.toString(scheduler.getCpus()));
        updateGUI();
    }
    
    private void deleteCPUCount(int newCount) throws InterruptedException {
        //System.out.println(Arrays.toString(scheduler.getCpus()));
        System.out.println(newCount);
        scheduler.deleteCPU(newCount);
        //System.out.println(Arrays.toString(scheduler.getCpus()));
        updateGUI();
    }
    
    @SuppressWarnings("empty-statement")
    private void showNewProcessDialog() {
        JDialog dialog = new JDialog(this, "Create New Process", true);
        dialog.setLayout(new GridLayout(0, 2, 5, 5));

        JTextField nameField = new JTextField(20);
        SpinnerNumberModel instructionsModel = new SpinnerNumberModel(10, 1, 1000, 1);
        JSpinner instructionsSpinner = new JSpinner(instructionsModel);
        JCheckBox isIOBoundCheck = new JCheckBox();
        SpinnerNumberModel interruptModel = new SpinnerNumberModel(5, 1, 100, 1);
        JSpinner cyclesUntilInterruptSpinner = new JSpinner(interruptModel);
        SpinnerNumberModel ioModel = new SpinnerNumberModel(3, 1, 50, 1);
        JSpinner cyclesForIOSpinner = new JSpinner(ioModel);

        dialog.add(new JLabel("Process Name:"));
        dialog.add(nameField);
        dialog.add(new JLabel("Instructions:"));
        dialog.add(instructionsSpinner);
        dialog.add(new JLabel("IO Bound:"));
        dialog.add(isIOBoundCheck);
        dialog.add(new JLabel("Cycles Until Interrupt:"));
        dialog.add(cyclesUntilInterruptSpinner);
        dialog.add(new JLabel("Cycles For IO:"));
        dialog.add(cyclesForIOSpinner);
        cyclesForIOSpinner.setEnabled(false);
        isIOBoundCheck.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Habilitar o deshabilitar el campo de texto según el estado de la checkbox
                cyclesForIOSpinner.setEnabled(isIOBoundCheck.isSelected());
            }
        });
       
        


        JButton createButton = new JButton("Create");
        createButton.addActionListener(e -> {
            createProcess(dialog, nameField, instructionsSpinner,
                    isIOBoundCheck, cyclesUntilInterruptSpinner, cyclesForIOSpinner);
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(createButton);
        dialog.add(buttonPanel);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void createProcess(JDialog dialog, JTextField nameField, JSpinner instructionsSpinner,
            JCheckBox isIOBoundCheck, JSpinner cyclesUntilInterruptSpinner,
            JSpinner cyclesForIOSpinner) {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(dialog, "Process name cannot be empty",
                    "Invalid Input", JOptionPane.ERROR_MESSAGE);
            return;
        }

        scheduler.createProcess(
                name,
                (Integer) instructionsSpinner.getValue(),
                isIOBoundCheck.isSelected(),
                (Integer) cyclesUntilInterruptSpinner.getValue(),
                (Integer) cyclesForIOSpinner.getValue()
        );
        dialog.dispose();
    }

    public void updateGUI() {
        updateCPUPanel();
        updateQueuePanel();
        revalidate();
        repaint();
    }

    public void updateCPUPanel() {
        cpuPanel.removeAll();
        CPU[] cpu = this.scheduler.getCpus();
        for (int i = 0; i < 5; i++) {
            if (cpu[i] != null) {
                JPanel cpuInfo = createCPUInfoPanel(cpu[i]);
                 cpuPanel.add(cpuInfo);
            }
        }
    }

    private JPanel createCPUInfoPanel(CPU cpu) {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("CPU " + cpu.getId()));

        StringBuilder info = new StringBuilder("<html>");
        info.append("Status: ").append(cpu.isBusy() ? "Busy" : "Idle").append("<br>");
         Process currentProcess =cpu.getCurrentProcess();
        if (currentProcess != null) {
            info.append("Process: ").append(currentProcess.getNameProcess()).append("<br>");
            info.append("PC: ").append(currentProcess.getProgramCounter()).append("<br>");
            info.append("MAR: ").append(currentProcess.getMAR()).append("<br>");
        } else {
            info.append("No active process<br>");
        }

        info.append("Clock Cycle: ").append(cpu.getClockCycle());
        info.append("</html>");

        panel.add(new JLabel(info.toString()));
        return panel;
    }

    private void updateQueuePanel() {
    queuePanel.removeAll();

    // Crear colas temporales para los diferentes estados
    CustomQueue<Process> readyProcesses = new CustomQueue<>();
    CustomQueue<Process> blockedProcesses = new CustomQueue<>();
    CustomQueue<Process> finishedProcesses = new CustomQueue<>();

    // Recorrer la cola allQueue y clasificar los procesos
    CustomQueue<Process> tempQueue = new CustomQueue<>(); // Para restaurar la cola después de recorrerla

    while (!scheduler.getAllQueue().isEmpty()) {
        Process process = scheduler.getAllQueue().dequeue(); // Extraer proceso de la cola

        switch (process.getStateProcess()) {
            case ProcessState.READY:
                readyProcesses.enqueue(process);
                break;
             case ProcessState.BLOCKED:
                blockedProcesses.enqueue(process);
                break;
            case   ProcessState.FINISHED:
                finishedProcesses.enqueue(process);
                break;
        }

        tempQueue.enqueue(process); // Guardar en cola temporal para restaurar después
    }

    // Restaurar allQueue con los elementos originales
    while (!tempQueue.isEmpty()) {
        scheduler.getAllQueue().enqueue(tempQueue.dequeue());
    }

    // Ready queue
    JPanel readyQueuePanel = new JPanel();
    readyQueuePanel.setBorder(BorderFactory.createTitledBorder("Ready Queue"));
    updateQueueDisplay(readyQueuePanel, readyProcesses);
    queuePanel.add(readyQueuePanel);

    // Blocked queue
    JPanel blockedQueuePanel = new JPanel();
    blockedQueuePanel.setBorder(BorderFactory.createTitledBorder("Blocked Queue"));
    updateQueueDisplay(blockedQueuePanel, blockedProcesses);
    queuePanel.add(blockedQueuePanel);

    // Finished queue
    JPanel finishedQueuePanel = new JPanel();
    finishedQueuePanel.setBorder(BorderFactory.createTitledBorder("Finished Processes"));
    updateQueueDisplay(finishedQueuePanel, finishedProcesses);
    queuePanel.add(finishedQueuePanel);

    // Refrescar la vista
    queuePanel.revalidate();
    queuePanel.repaint();
}

    private void updateQueueDisplay(JPanel panel, CustomQueue<Process> queue) {
        StringBuilder info = new StringBuilder("<html>");
        CustomQueue<Process> tempQueue = new CustomQueue<>();

        while (!queue.isEmpty()) {
            Process process = queue.dequeue();
            info.append("ID: ").append(process.getIdProcess())
                    .append(" | Name: ").append(process.getNameProcess())
                    .append(" | State: ").append(process.getStateProcess())
                    .append(" | ProgramCounter: ").append(process.getProgramCounter())
                    .append("<br>");
            tempQueue.enqueue(process);
        }

        // Restore queue
        while (!tempQueue.isEmpty()) {
            queue.enqueue(tempQueue.dequeue());
        }

        if (info.length() == 6) { // Only "<html>" was added
            info.append("Empty");
        }
        info.append("</html>");
        panel.add(new JLabel(info.toString()));
    }

    public void start() {
        isSimulationRunning = true;
        updateTimer.start();
        //System.out.println("estamos");
        this.scheduler.start();
        

        

    }

    private boolean canReplaceProcess(CPU cpu) {
        Process currentProcess = cpu.getCurrentProcess();
        return currentProcess != null
                && (currentProcess.getRemainingInstructions() <= 0
                || currentProcess.getStateProcess() == ProcessState.BLOCKED);
    }

    public void stop() {
        isSimulationRunning = false;
        if (schedulerThread != null) {
            schedulerThread.interrupt();
            try {
                schedulerThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        updateTimer.stop();
    }


}

