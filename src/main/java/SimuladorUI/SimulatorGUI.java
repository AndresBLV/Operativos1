package SimuladorUI;

import Class.CPU;
import Class.Scheduler;
import Class.Process;
import ENV.ProcessState;
import ENV.SchedulingAlgorithm;
import EDD.CustomQueue;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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
//        cpuCountSpinner.addChangeListener(e -> updateCPUCount((Integer) cpuCountSpinner.getValue()));

        // Process creation button
        JButton newProcessBtn = new JButton("New Process");
        newProcessBtn.addActionListener(e -> showNewProcessDialog());

        // Start simulation button
        JButton startButton = new JButton("Iniciar");
        startButton.addActionListener(e -> {
            if (!isSimulationRunning) {
                start();
            } 
        });
        
        JButton stopButton = new JButton("Detener");
        stopButton.addActionListener(e -> {
            if (isSimulationRunning) {
                stop();
            }else{
                JOptionPane.showMessageDialog(null,"No ha iniciado el simulador");
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

        return panel;
    }

//    private void updateCPUCount(int newCount) {
//        scheduler.updateCPUCount(newCount, this);
//        cpus.clear();
//        for (int i = 0; i < newCount; i++) {
//            addCPU();
//        }
//        updateGUI();
//    }
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

        // Ready queue
        JPanel readyQueuePanel = new JPanel();
        readyQueuePanel.setBorder(BorderFactory.createTitledBorder("Ready Queue"));
        updateQueueDisplay(readyQueuePanel, scheduler.getReadyQueue());
        queuePanel.add(readyQueuePanel);

        // Blocked queue
        JPanel blockedQueuePanel = new JPanel();
        blockedQueuePanel.setBorder(BorderFactory.createTitledBorder("Blocked Queue"));
        updateQueueDisplay(blockedQueuePanel, scheduler.getBlockedQueue());
        queuePanel.add(blockedQueuePanel);

        // Finished queue
        JPanel finishedQueuePanel = new JPanel();
        finishedQueuePanel.setBorder(BorderFactory.createTitledBorder("Finished Processes"));
        updateQueueDisplay(finishedQueuePanel, scheduler.getFinishedQueue());
        queuePanel.add(finishedQueuePanel);
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
        System.out.println("estamos");
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

