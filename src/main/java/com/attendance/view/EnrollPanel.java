package com.attendance.view;

import com.attendance.config.DatabaseConfig;
import com.attendance.service.ArduinoCommService;
import com.attendance.util.SerialPortManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel de Enrolamiento - VERSIÓN OPTIMIZADA
 * Mejoras en flujo, validaciones y experiencia de usuario
 * 
 * @author Sistema Biométrico
 * @version 2.0 - Optimizado
 */
public class EnrollPanel extends JPanel {
    
    private static final Logger logger = LoggerFactory.getLogger(EnrollPanel.class);
    
    private ArduinoCommService arduinoService;
    
    // Componentes de conexión
    private JComboBox<String> cmbPorts;
    private JButton btnConnect;
    private JButton btnRefreshPorts;
    private JLabel lblConnectionStatus;
    private JLabel lblSensorInfo;
    
    // Componentes de selección
    private JComboBox<UserItem> cmbUsers;
    private JTextField txtFingerprintId;
    private JButton btnAutoAssignId;
    private JButton btnStartEnroll;
    private JButton btnCancelEnroll;
    
    // Componentes de progreso
    private JTextArea txtLog;
    private JProgressBar progressBar;
    private JLabel lblCurrentStep;
    private JLabel lblStepCount;
    
    // Estado
    private boolean enrolling = false;
    private int nextAvailableId = 1;
    
    public EnrollPanel() {
        this.arduinoService = new ArduinoCommService();
        initComponents();
        refreshPorts();
        loadUsers();
        calculateNextAvailableId();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(20, 20));
        setBackground(new Color(250, 250, 250));
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        
        // Header
        JPanel headerPanel = createHeaderPanel();
        
        // Main content
        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        mainPanel.setOpaque(false);
        
        mainPanel.add(createLeftPanel());
        mainPanel.add(createRightPanel());
        
        add(headerPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        
        JLabel lblTitle = new JLabel("👆 Enrolar Huella Dactilar");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(new Color(44, 62, 80));
        lblTitle.setAlignmentX(LEFT_ALIGNMENT);
        
        JLabel lblSubtitle = new JLabel("Registro biométrico de usuarios en el sensor");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblSubtitle.setForeground(new Color(127, 140, 141));
        lblSubtitle.setAlignmentX(LEFT_ALIGNMENT);
        
        titlePanel.add(lblTitle);
        titlePanel.add(Box.createVerticalStrut(5));
        titlePanel.add(lblSubtitle);
        
        panel.add(titlePanel, BorderLayout.WEST);
        
        return panel;
    }
    
    private JPanel createLeftPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        // SECCIÓN 1: Conexión Arduino
        JPanel connectionSection = createSection("① Conexión con Sensor");
        
        JPanel portPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        portPanel.setOpaque(false);
        
        JLabel lblPort = new JLabel("Puerto COM:");
        lblPort.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        cmbPorts = new JComboBox<>();
        cmbPorts.setPreferredSize(new Dimension(130, 32));
        cmbPorts.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        btnRefreshPorts = new JButton("↻");
        btnRefreshPorts.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnRefreshPorts.setPreferredSize(new Dimension(40, 32));
        btnRefreshPorts.setToolTipText("Actualizar puertos");
        btnRefreshPorts.setFocusPainted(false);
        btnRefreshPorts.addActionListener(e -> refreshPorts());
        
        btnConnect = new JButton("CONECTAR");
        btnConnect.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnConnect.setBackground(new Color(46, 204, 113));
        btnConnect.setForeground(Color.WHITE);
        btnConnect.setFocusPainted(false);
        btnConnect.setBorderPainted(false);
        btnConnect.setPreferredSize(new Dimension(130, 32));
        btnConnect.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnConnect.addActionListener(e -> toggleConnection());
        
        portPanel.add(lblPort);
        portPanel.add(cmbPorts);
        portPanel.add(btnRefreshPorts);
        portPanel.add(btnConnect);
        
        lblConnectionStatus = new JLabel("● DESCONECTADO");
        lblConnectionStatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblConnectionStatus.setForeground(new Color(231, 76, 60));
        lblConnectionStatus.setAlignmentX(LEFT_ALIGNMENT);
        
        lblSensorInfo = new JLabel("Sensor: Esperando conexión...");
        lblSensorInfo.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblSensorInfo.setForeground(new Color(127, 140, 141));
        lblSensorInfo.setAlignmentX(LEFT_ALIGNMENT);
        
        connectionSection.add(portPanel);
        connectionSection.add(Box.createVerticalStrut(10));
        connectionSection.add(lblConnectionStatus);
        connectionSection.add(Box.createVerticalStrut(5));
        connectionSection.add(lblSensorInfo);
        
        // SECCIÓN 2: Selección de Usuario
        JPanel userSection = createSection("② Seleccionar Usuario");
        
        JLabel lblUser = new JLabel("Usuario:");
        lblUser.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblUser.setAlignmentX(LEFT_ALIGNMENT);
        
        cmbUsers = new JComboBox<>();
        cmbUsers.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbUsers.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        cmbUsers.setAlignmentX(LEFT_ALIGNMENT);
        cmbUsers.addActionListener(e -> onUserSelected());
        
        JButton btnReloadUsers = new JButton("🔄 Recargar Lista");
        btnReloadUsers.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnReloadUsers.setAlignmentX(LEFT_ALIGNMENT);
        btnReloadUsers.setFocusPainted(false);
        btnReloadUsers.addActionListener(e -> {
            loadUsers();
            calculateNextAvailableId();
        });
        
        userSection.add(lblUser);
        userSection.add(Box.createVerticalStrut(5));
        userSection.add(cmbUsers);
        userSection.add(Box.createVerticalStrut(10));
        userSection.add(btnReloadUsers);
        
        // SECCIÓN 3: ID de Huella
        JPanel idSection = createSection("③ Asignar ID de Huella");
        
        JLabel lblId = new JLabel("ID en el sensor (1-255):");
        lblId.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblId.setAlignmentX(LEFT_ALIGNMENT);
        
        JPanel idInputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        idInputPanel.setOpaque(false);
        idInputPanel.setAlignmentX(LEFT_ALIGNMENT);
        idInputPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        
        txtFingerprintId = new JTextField(10);
        txtFingerprintId.setFont(new Font("Segoe UI", Font.BOLD, 14));
        txtFingerprintId.setPreferredSize(new Dimension(100, 35));
        
        btnAutoAssignId = new JButton("Auto-Asignar");
        btnAutoAssignId.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnAutoAssignId.setPreferredSize(new Dimension(100, 35));
        btnAutoAssignId.setFocusPainted(false);
        btnAutoAssignId.setToolTipText("Asignar próximo ID disponible");
        btnAutoAssignId.addActionListener(e -> autoAssignId());
        
        idInputPanel.add(txtFingerprintId);
        idInputPanel.add(btnAutoAssignId);
        
        JLabel lblIdHelp = new JLabel(
            "<html><small>💡 Sugerencia: Usar DNI o número correlativo</small></html>");
        lblIdHelp.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblIdHelp.setForeground(new Color(127, 140, 141));
        lblIdHelp.setAlignmentX(LEFT_ALIGNMENT);
        
        JLabel lblNextId = new JLabel("Próximo ID disponible: " + nextAvailableId);
        lblNextId.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblNextId.setForeground(new Color(52, 152, 219));
        lblNextId.setAlignmentX(LEFT_ALIGNMENT);
        
        idSection.add(lblId);
        idSection.add(Box.createVerticalStrut(5));
        idSection.add(idInputPanel);
        idSection.add(Box.createVerticalStrut(8));
        idSection.add(lblIdHelp);
        idSection.add(Box.createVerticalStrut(3));
        idSection.add(lblNextId);
        
        // Botones de acción
        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 0, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        buttonPanel.setAlignmentX(LEFT_ALIGNMENT);
        
        btnStartEnroll = new JButton("▶ INICIAR ENROLAMIENTO");
        btnStartEnroll.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnStartEnroll.setBackground(new Color(41, 128, 185));
        btnStartEnroll.setForeground(Color.WHITE);
        btnStartEnroll.setFocusPainted(false);
        btnStartEnroll.setBorderPainted(false);
        btnStartEnroll.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnStartEnroll.setEnabled(false);
        btnStartEnroll.addActionListener(e -> startEnrollment());
        
        btnCancelEnroll = new JButton("⏹ CANCELAR");
        btnCancelEnroll.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnCancelEnroll.setBackground(new Color(149, 165, 166));
        btnCancelEnroll.setForeground(Color.WHITE);
        btnCancelEnroll.setFocusPainted(false);
        btnCancelEnroll.setBorderPainted(false);
        btnCancelEnroll.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancelEnroll.setEnabled(false);
        btnCancelEnroll.addActionListener(e -> cancelEnrollment());
        
        buttonPanel.add(btnStartEnroll);
        buttonPanel.add(btnCancelEnroll);
        
        // Agregar todo
        panel.add(connectionSection);
        panel.add(Box.createVerticalStrut(20));
        panel.add(userSection);
        panel.add(Box.createVerticalStrut(20));
        panel.add(idSection);
        panel.add(Box.createVerticalStrut(30));
        panel.add(buttonPanel);
        panel.add(Box.createVerticalGlue());
        
        return panel;
    }
    
    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        // Header del proceso
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);
        
        JLabel lblLogTitle = new JLabel("📋 Proceso de Enrolamiento");
        lblLogTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblLogTitle.setForeground(new Color(44, 62, 80));
        lblLogTitle.setAlignmentX(LEFT_ALIGNMENT);
        
        lblStepCount = new JLabel("Paso 0 de 4");
        lblStepCount.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStepCount.setForeground(new Color(127, 140, 141));
        lblStepCount.setAlignmentX(LEFT_ALIGNMENT);
        
        lblCurrentStep = new JLabel("Esperando inicio del proceso...");
        lblCurrentStep.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblCurrentStep.setForeground(new Color(52, 152, 219));
        lblCurrentStep.setAlignmentX(LEFT_ALIGNMENT);
        
        headerPanel.add(lblLogTitle);
        headerPanel.add(Box.createVerticalStrut(8));
        headerPanel.add(lblStepCount);
        headerPanel.add(Box.createVerticalStrut(3));
        headerPanel.add(lblCurrentStep);
        
        // Área de log
        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtLog.setBackground(new Color(250, 250, 250));
        txtLog.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        txtLog.setLineWrap(true);
        txtLog.setWrapStyleWord(true);
        txtLog.setText("═══════════════════════════════════════\n" +
                       "  SISTEMA DE ENROLAMIENTO BIOMÉTRICO  \n" +
                       "═══════════════════════════════════════\n\n" +
                       "✓ Sistema inicializado correctamente\n" +
                       "→ Conecte con Arduino para comenzar\n\n" +
                       "PASOS DEL PROCESO:\n" +
                       "1. Conectar sensor\n" +
                       "2. Seleccionar usuario\n" +
                       "3. Asignar ID de huella\n" +
                       "4. Capturar primera huella\n" +
                       "5. Capturar segunda huella\n" +
                       "6. Validar y guardar\n");
        
        JScrollPane scrollPane = new JScrollPane(txtLog);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        
        // Progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setForeground(new Color(52, 152, 219));
        progressBar.setValue(0);
        progressBar.setString("Listo para comenzar");
        
        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(progressBar, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createSection(String title) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setOpaque(false);
        section.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            title,
            0, 0,
            new Font("Segoe UI", Font.BOLD, 13),
            new Color(52, 73, 94)
        ));
        section.setAlignmentX(LEFT_ALIGNMENT);
        return section;
    }
    
    // ============================================
    // LÓGICA DE CONEXIÓN
    // ============================================
    
    private void refreshPorts() {
        cmbPorts.removeAllItems();
        List<String> ports = SerialPortManager.getAvailablePorts();
        
        if (ports.isEmpty()) {
            cmbPorts.addItem("Sin puertos disponibles");
            btnConnect.setEnabled(false);
            addLog("⚠ No se encontraron puertos COM");
        } else {
            for (String port : ports) {
                cmbPorts.addItem(port);
            }
            btnConnect.setEnabled(true);
            addLog("✓ Puertos actualizados: " + ports.size() + " disponibles");
        }
    }
    
    private void toggleConnection() {
        if (arduinoService.isConnected()) {
            arduinoService.disconnect();
            updateConnectionStatus(false);
            addLog("⊗ Desconectado de Arduino");
        } else {
            String selectedPort = (String) cmbPorts.getSelectedItem();
            if (selectedPort == null || selectedPort.equals("Sin puertos disponibles")) {
                JOptionPane.showMessageDialog(this,
                    "Por favor seleccione un puerto COM válido",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            addLog("⟳ Conectando con " + selectedPort + "...");
            btnConnect.setEnabled(false);
            btnConnect.setText("Conectando...");
            
            SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    return arduinoService.connect(selectedPort);
                }
                
                @Override
                protected void done() {
                    try {
                        boolean connected = get();
                        updateConnectionStatus(connected);
                        
                        if (connected) {
                            addLog("✓ Conexión establecida con " + selectedPort);
                            addLog("✓ Sensor biométrico responde correctamente");
                            
                            // Obtener info del sensor
                            int templateCount = arduinoService.getTemplateCount();
                            if (templateCount >= 0) {
                                addLog("ℹ Huellas en sensor: " + templateCount);
                                lblSensorInfo.setText("Sensor: " + templateCount + " huellas registradas");
                            }
                        } else {
                            addLog("✗ Error al conectar con Arduino");
                            JOptionPane.showMessageDialog(EnrollPanel.this,
                                "No se pudo conectar con Arduino.\n\n" +
                                "Verifique:\n" +
                                "• Arduino está conectado al puerto\n" +
                                "• El firmware está cargado correctamente\n" +
                                "• No hay otra aplicación usando el puerto",
                                "Error de Conexión",
                                JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception e) {
                        logger.error("Error al conectar", e);
                        updateConnectionStatus(false);
                        addLog("✗ Error: " + e.getMessage());
                    }
                    
                    btnConnect.setEnabled(true);
                    btnConnect.setText(arduinoService.isConnected() ? "DESCONECTAR" : "CONECTAR");
                }
            };
            
            worker.execute();
        }
    }
    
    private void updateConnectionStatus(boolean connected) {
        if (connected) {
            lblConnectionStatus.setText("● CONECTADO");
            lblConnectionStatus.setForeground(new Color(46, 204, 113));
            btnConnect.setText("DESCONECTAR");
            btnConnect.setBackground(new Color(231, 76, 60));
            btnStartEnroll.setEnabled(true);
            cmbPorts.setEnabled(false);
            btnRefreshPorts.setEnabled(false);
        } else {
            lblConnectionStatus.setText("● DESCONECTADO");
            lblConnectionStatus.setForeground(new Color(231, 76, 60));
            btnConnect.setText("CONECTAR");
            btnConnect.setBackground(new Color(46, 204, 113));
            btnStartEnroll.setEnabled(false);
            cmbPorts.setEnabled(true);
            btnRefreshPorts.setEnabled(true);
            lblSensorInfo.setText("Sensor: Esperando conexión...");
        }
    }
    
    // ============================================
    // LÓGICA DE USUARIOS
    // ============================================
    
    private void loadUsers() {
        cmbUsers.removeAllItems();
        
        String sql = "SELECT id_usuario, dni, nombres, apellidos, fingerprint_id " +
                     "FROM usuarios WHERE activo = TRUE ORDER BY apellidos, nombres";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            int count = 0;
            while (rs.next()) {
                UserItem item = new UserItem(
                    rs.getInt("id_usuario"),
                    rs.getString("dni"),
                    rs.getString("nombres"),
                    rs.getString("apellidos"),
                    (Integer) rs.getObject("fingerprint_id")
                );
                cmbUsers.addItem(item);
                count++;
            }
            
            addLog("✓ Cargados " + count + " usuarios activos");
            
        } catch (Exception e) {
            logger.error("Error al cargar usuarios", e);
            addLog("✗ Error al cargar usuarios: " + e.getMessage());
        }
    }
    
    private void onUserSelected() {
        UserItem selected = (UserItem) cmbUsers.getSelectedItem();
        if (selected != null && selected.getFingerprintId() != null) {
            addLog("ℹ Usuario seleccionado ya tiene huella ID: " + selected.getFingerprintId());
            int confirm = JOptionPane.showConfirmDialog(this,
                "Este usuario ya tiene una huella registrada (ID: " + selected.getFingerprintId() + ")\n\n" +
                "¿Desea reemplazarla?",
                "Huella Existente",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            
            if (confirm == JOptionPane.YES_OPTION) {
                txtFingerprintId.setText(String.valueOf(selected.getFingerprintId()));
            }
        }
    }
    
    private void calculateNextAvailableId() {
        String sql = "SELECT COALESCE(MAX(fingerprint_id), 0) + 1 AS next_id FROM usuarios";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                nextAvailableId = rs.getInt("next_id");
                logger.info("Próximo ID disponible: {}", nextAvailableId);
            }
            
        } catch (Exception e) {
            logger.error("Error al calcular próximo ID", e);
            nextAvailableId = 1;
        }
    }
    
    private void autoAssignId() {
        txtFingerprintId.setText(String.valueOf(nextAvailableId));
        addLog("✓ ID auto-asignado: " + nextAvailableId);
    }
    
    // ============================================
    // PROCESO DE ENROLAMIENTO
    // ============================================
    
    private void startEnrollment() {
        if (enrolling) {
            JOptionPane.showMessageDialog(this,
                "Ya hay un proceso de enrolamiento en curso",
                "Advertencia",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Validaciones
        UserItem selectedUser = (UserItem) cmbUsers.getSelectedItem();
        if (selectedUser == null) {
            JOptionPane.showMessageDialog(this,
                "Por favor seleccione un usuario",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String fingerprintIdStr = txtFingerprintId.getText().trim();
        if (fingerprintIdStr.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Por favor ingrese un ID de huella",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int fingerprintId;
        try {
            fingerprintId = Integer.parseInt(fingerprintIdStr);
            if (fingerprintId < 1 || fingerprintId > 255) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                "El ID debe ser un número entre 1 y 255",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Confirmar
        int confirm = JOptionPane.showConfirmDialog(this,
            "¿Iniciar enrolamiento?\n\n" +
            "Usuario: " + selectedUser.toString() + "\n" +
            "ID Huella: " + fingerprintId + "\n\n" +
            "El proceso tomará aproximadamente 30 segundos.",
            "Confirmar Enrolamiento",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        
        // Iniciar proceso
        enrolling = true;
        setEnrollingState(true);
        
        txtLog.setText("");
        addLog("═══════════════════════════════════════");
        addLog("  INICIANDO PROCESO DE ENROLAMIENTO");
        addLog("═══════════════════════════════════════");
        addLog("");
        addLog("Usuario: " + selectedUser.toString());
        addLog("ID Huella: " + fingerprintId);
        addLog("");
        
        progressBar.setValue(0);
        progressBar.setIndeterminate(true);
        progressBar.setString("Preparando sensor...");
        
        lblStepCount.setText("Paso 1 de 4");
        lblCurrentStep.setText("Preparando sensor...");
        
        // Ejecutar enrolamiento
        arduinoService.startEnroll(fingerprintId, new ArduinoCommService.EnrollCallback() {
            @Override
            public void onProgress(String message) {
                SwingUtilities.invokeLater(() -> {
                    addLog("▶ " + message);
                    lblCurrentStep.setText(message);
                    
                    // Actualizar progreso
                    if (message.contains("Coloque el dedo")) {
                        progressBar.setIndeterminate(false);
                        progressBar.setValue(25);
                        progressBar.setString("Paso 2/4: Primera captura");
                        lblStepCount.setText("Paso 2 de 4");
                    } else if (message.contains("Retire el dedo")) {
                        progressBar.setValue(50);
                        progressBar.setString("Paso 3/4: Validando...");
                        lblStepCount.setText("Paso 3 de 4");
                    } else if (message.contains("mismo dedo nuevamente")) {
                        progressBar.setValue(75);
                        progressBar.setString("Paso 4/4: Segunda captura");
                        lblStepCount.setText("Paso 4 de 4");
                    }
                });
            }
            
            @Override
            public void onSuccess(int enrolledId) {
                SwingUtilities.invokeLater(() -> {
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(100);
                    progressBar.setString("✓ Completado exitosamente");
                    lblCurrentStep.setText("✓ Enrolamiento completado");
                    lblStepCount.setText("Paso 4 de 4");
                    
                    addLog("");
                    addLog("═══════════════════════════════════════");
                    addLog("  ✓ ENROLAMIENTO EXITOSO");
                    addLog("═══════════════════════════════════════");
                    addLog("");
                    
                    // Actualizar BD
                    updateUserFingerprint(selectedUser.getId(), fingerprintId);
                    
                    enrolling = false;
                    setEnrollingState(false);
                    
                    // Notificar éxito
                    JOptionPane.showMessageDialog(EnrollPanel.this,
                        "Huella registrada exitosamente!\n\n" +
                        "Usuario: " + selectedUser.toString() + "\n" +
                        "ID Huella: " + fingerprintId,
                        "Éxito",
                        JOptionPane.INFORMATION_MESSAGE);
                    
                    // Recargar datos
                    loadUsers();
                    calculateNextAvailableId();
                    txtFingerprintId.setText("");
                });
            }
            
            @Override
            public void onError(String error) {
                SwingUtilities.invokeLater(() -> {
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(0);
                    progressBar.setString("✗ Error en el proceso");
                    lblCurrentStep.setText("✗ Error: " + error);
                    
                    addLog("");
                    addLog("═══════════════════════════════════════");
                    addLog("  ✗ ERROR EN ENROLAMIENTO");
                    addLog("═══════════════════════════════════════");
                    addLog("Error: " + error);
                    addLog("");
                    
                    enrolling = false;
                    setEnrollingState(false);
                    
                    JOptionPane.showMessageDialog(EnrollPanel.this,
                        "Error durante el enrolamiento:\n\n" + error +
                        "\n\nIntente nuevamente.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                });
            }
        });
    }
    
    private void cancelEnrollment() {
        if (!enrolling) return;
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "¿Está seguro de cancelar el proceso?\n\n" +
            "El enrolamiento en curso se perderá.",
            "Confirmar Cancelación",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            addLog("");
            addLog("⊗ Proceso cancelado por el usuario");
            enrolling = false;
            setEnrollingState(false);
            
            progressBar.setIndeterminate(false);
            progressBar.setValue(0);
            progressBar.setString("Cancelado");
            lblCurrentStep.setText("Proceso cancelado");
        }
    }
    
    private void setEnrollingState(boolean isEnrolling) {
        btnStartEnroll.setEnabled(!isEnrolling);
        btnCancelEnroll.setEnabled(isEnrolling);
        btnConnect.setEnabled(!isEnrolling);
        cmbUsers.setEnabled(!isEnrolling);
        txtFingerprintId.setEnabled(!isEnrolling);
        btnAutoAssignId.setEnabled(!isEnrolling);
    }
    
    private void updateUserFingerprint(int userId, int fingerprintId) {
        String sql = "UPDATE usuarios SET fingerprint_id = ? WHERE id_usuario = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, fingerprintId);
            pstmt.setInt(2, userId);
            int updated = pstmt.executeUpdate();
            
            if (updated > 0) {
                addLog("✓ Base de datos actualizada correctamente");
                logger.info("Fingerprint ID {} asignado al usuario {}", fingerprintId, userId);
            }
            
        } catch (Exception e) {
            logger.error("Error al actualizar usuario", e);
            addLog("✗ Error al actualizar BD: " + e.getMessage());
        }
    }
    
    private void addLog(String message) {
        SwingUtilities.invokeLater(() -> {
            txtLog.append(message + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }
    
    // ============================================
    // CLEANUP
    // ============================================
    
    public void cleanup() {
        if (arduinoService != null && arduinoService.isConnected()) {
            arduinoService.disconnect();
            logger.info("Recursos de EnrollPanel liberados");
        }
    }
    
    // ============================================
    // CLASE AUXILIAR
    // ============================================
    
    private static class UserItem {
        private final int id;
        private final String dni;
        private final String nombres;
        private final String apellidos;
        private final Integer fingerprintId;
        
        public UserItem(int id, String dni, String nombres, String apellidos, Integer fingerprintId) {
            this.id = id;
            this.dni = dni;
            this.nombres = nombres;
            this.apellidos = apellidos;
            this.fingerprintId = fingerprintId;
        }
        
        public int getId() {
            return id;
        }
        
        public Integer getFingerprintId() {
            return fingerprintId;
        }
        
        @Override
        public String toString() {
            String name = apellidos + ", " + nombres + " (" + dni + ")";
            if (fingerprintId != null) {
                return name + " [Huella: " + fingerprintId + "]";
            }
            return name + " [Sin huella]";
        }
    }
}
