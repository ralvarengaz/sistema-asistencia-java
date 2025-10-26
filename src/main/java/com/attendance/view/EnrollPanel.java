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
import java.util.List;

public class EnrollPanel extends JPanel {
    
    private static final Logger logger = LoggerFactory.getLogger(EnrollPanel.class);
    
    private ArduinoCommService arduinoService;
    
    private JComboBox<String> cmbPorts;
    private JButton btnConnect;
    private JButton btnRefreshPorts;
    private JLabel lblConnectionStatus;
    private JLabel lblSensorInfo;
    
    private JComboBox<UserItem> cmbUsers;
    private JTextField txtFingerprintId;
    private JButton btnAutoAssignId;
    private JButton btnStartEnroll;
    private JButton btnCancelEnroll;
    
    private JTextArea txtLog;
    private JProgressBar progressBar;
    private JLabel lblCurrentStep;
    private JLabel lblStepCount;
    
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
        
        JPanel headerPanel = createHeaderPanel();
        
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
        
        JLabel lblTitle = new JLabel("Enrolar Huella Dactilar");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(new Color(44, 62, 80));
        lblTitle.setAlignmentX(LEFT_ALIGNMENT);
        
        JLabel lblSubtitle = new JLabel("Registro biometrico de usuarios en el sensor");
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
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        JPanel connectionPanel = new JPanel();
        connectionPanel.setLayout(new BoxLayout(connectionPanel, BoxLayout.Y_AXIS));
        connectionPanel.setOpaque(false);
        connectionPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            "Conexion con Arduino",
            0, 0,
            new Font("Segoe UI", Font.BOLD, 14)
        ));
        
        JLabel lblPort = new JLabel("Puerto COM:");
        lblPort.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblPort.setAlignmentX(LEFT_ALIGNMENT);
        
        JPanel portPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        portPanel.setOpaque(false);
        portPanel.setAlignmentX(LEFT_ALIGNMENT);
        
        cmbPorts = new JComboBox<>();
        cmbPorts.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbPorts.setPreferredSize(new Dimension(150, 30));
        
        btnRefreshPorts = new JButton("↻");
        btnRefreshPorts.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnRefreshPorts.setPreferredSize(new Dimension(40, 30));
        btnRefreshPorts.setToolTipText("Actualizar puertos");
        btnRefreshPorts.addActionListener(e -> refreshPorts());
        
        portPanel.add(cmbPorts);
        portPanel.add(btnRefreshPorts);
        
        btnConnect = new JButton("CONECTAR");
        btnConnect.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnConnect.setBackground(new Color(46, 204, 113));
        btnConnect.setForeground(Color.WHITE);
        btnConnect.setFocusPainted(false);
        btnConnect.setPreferredSize(new Dimension(200, 40));
        btnConnect.setMaximumSize(new Dimension(300, 40));
        btnConnect.setAlignmentX(LEFT_ALIGNMENT);
        btnConnect.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnConnect.addActionListener(e -> toggleConnection());
        
        lblConnectionStatus = new JLabel("DESCONECTADO");
        lblConnectionStatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblConnectionStatus.setForeground(new Color(231, 76, 60));
        lblConnectionStatus.setAlignmentX(LEFT_ALIGNMENT);
        
        lblSensorInfo = new JLabel("Sensor: Esperando conexion...");
        lblSensorInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSensorInfo.setForeground(new Color(127, 140, 141));
        lblSensorInfo.setAlignmentX(LEFT_ALIGNMENT);
        
        connectionPanel.add(lblPort);
        connectionPanel.add(Box.createVerticalStrut(5));
        connectionPanel.add(portPanel);
        connectionPanel.add(Box.createVerticalStrut(12));
        connectionPanel.add(btnConnect);
        connectionPanel.add(Box.createVerticalStrut(10));
        connectionPanel.add(lblConnectionStatus);
        connectionPanel.add(Box.createVerticalStrut(5));
        connectionPanel.add(lblSensorInfo);
        
        JPanel enrollPanel = new JPanel();
        enrollPanel.setLayout(new BoxLayout(enrollPanel, BoxLayout.Y_AXIS));
        enrollPanel.setOpaque(false);
        enrollPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            "Datos de Enrolamiento",
            0, 0,
            new Font("Segoe UI", Font.BOLD, 14)
        ));
        
        JLabel lblUser = new JLabel("Seleccionar Usuario:");
        lblUser.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblUser.setAlignmentX(LEFT_ALIGNMENT);
        
        cmbUsers = new JComboBox<>();
        cmbUsers.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbUsers.setMaximumSize(new Dimension(350, 30));
        cmbUsers.setAlignmentX(LEFT_ALIGNMENT);
        cmbUsers.addActionListener(e -> onUserSelected());
        
        JLabel lblFingerprintId = new JLabel("ID de Huella (1-255):");
        lblFingerprintId.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblFingerprintId.setAlignmentX(LEFT_ALIGNMENT);
        
        JPanel idPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        idPanel.setOpaque(false);
        idPanel.setAlignmentX(LEFT_ALIGNMENT);
        
        txtFingerprintId = new JTextField(10);
        txtFingerprintId.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtFingerprintId.setPreferredSize(new Dimension(100, 30));
        
        btnAutoAssignId = new JButton("Auto-Asignar");
        btnAutoAssignId.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnAutoAssignId.setPreferredSize(new Dimension(110, 30));
        btnAutoAssignId.setToolTipText("Asignar siguiente ID disponible");
        btnAutoAssignId.addActionListener(e -> autoAssignId());
        
        idPanel.add(txtFingerprintId);
        idPanel.add(btnAutoAssignId);
        
        btnStartEnroll = new JButton("INICIAR ENROLAMIENTO");
        btnStartEnroll.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnStartEnroll.setBackground(new Color(52, 152, 219));
        btnStartEnroll.setForeground(Color.WHITE);
        btnStartEnroll.setFocusPainted(false);
        btnStartEnroll.setPreferredSize(new Dimension(200, 45));
        btnStartEnroll.setMaximumSize(new Dimension(300, 45));
        btnStartEnroll.setAlignmentX(LEFT_ALIGNMENT);
        btnStartEnroll.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnStartEnroll.setEnabled(false);
        btnStartEnroll.addActionListener(e -> startEnrollment());
        
        btnCancelEnroll = new JButton("CANCELAR");
        btnCancelEnroll.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnCancelEnroll.setBackground(new Color(231, 76, 60));
        btnCancelEnroll.setForeground(Color.WHITE);
        btnCancelEnroll.setFocusPainted(false);
        btnCancelEnroll.setPreferredSize(new Dimension(150, 35));
        btnCancelEnroll.setMaximumSize(new Dimension(300, 35));
        btnCancelEnroll.setAlignmentX(LEFT_ALIGNMENT);
        btnCancelEnroll.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancelEnroll.setVisible(false);
        btnCancelEnroll.addActionListener(e -> cancelEnrollment());
        
        enrollPanel.add(lblUser);
        enrollPanel.add(Box.createVerticalStrut(5));
        enrollPanel.add(cmbUsers);
        enrollPanel.add(Box.createVerticalStrut(12));
        enrollPanel.add(lblFingerprintId);
        enrollPanel.add(Box.createVerticalStrut(5));
        enrollPanel.add(idPanel);
        enrollPanel.add(Box.createVerticalStrut(15));
        enrollPanel.add(btnStartEnroll);
        enrollPanel.add(Box.createVerticalStrut(8));
        enrollPanel.add(btnCancelEnroll);
        
        panel.add(connectionPanel, BorderLayout.NORTH);
        panel.add(enrollPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        JLabel lblProgressTitle = new JLabel("Progreso del Enrolamiento");
        lblProgressTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblProgressTitle.setForeground(new Color(44, 62, 80));
        
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
        statusPanel.setOpaque(false);
        statusPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        lblCurrentStep = new JLabel("Esperando inicio...");
        lblCurrentStep.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblCurrentStep.setForeground(new Color(52, 152, 219));
        lblCurrentStep.setAlignmentX(LEFT_ALIGNMENT);
        
        lblStepCount = new JLabel(" ");
        lblStepCount.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStepCount.setForeground(new Color(127, 140, 141));
        lblStepCount.setAlignmentX(LEFT_ALIGNMENT);
        
        statusPanel.add(lblCurrentStep);
        statusPanel.add(Box.createVerticalStrut(5));
        statusPanel.add(lblStepCount);
        
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        progressBar.setForeground(new Color(52, 152, 219));
        progressBar.setPreferredSize(new Dimension(0, 30));
        progressBar.setValue(0);
        
        JLabel lblLogTitle = new JLabel("Registro de Actividad:");
        lblLogTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblLogTitle.setForeground(new Color(44, 62, 80));
        
        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtLog.setBackground(new Color(250, 250, 250));
        txtLog.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane scrollPane = new JScrollPane(txtLog);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setOpaque(false);
        contentPanel.add(statusPanel, BorderLayout.NORTH);
        contentPanel.add(progressBar, BorderLayout.CENTER);
        
        panel.add(lblProgressTitle, BorderLayout.NORTH);
        panel.add(contentPanel, BorderLayout.CENTER);
        
        JPanel logPanel = new JPanel(new BorderLayout(5, 5));
        logPanel.setOpaque(false);
        logPanel.add(lblLogTitle, BorderLayout.NORTH);
        logPanel.add(scrollPane, BorderLayout.CENTER);
        
        panel.add(logPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void refreshPorts() {
        cmbPorts.removeAllItems();
        List<String> ports = SerialPortManager.getAvailablePorts();
        
        if (ports.isEmpty()) {
            cmbPorts.addItem("Sin puertos");
            btnConnect.setEnabled(false);
            addLog("⚠ No se encontraron puertos COM disponibles");
        } else {
            for (String port : ports) {
                cmbPorts.addItem(port);
            }
            btnConnect.setEnabled(true);
            addLog("✓ Puertos detectados: " + String.join(", ", ports));
        }
    }
    
    private void toggleConnection() {
        if (arduinoService.isConnected()) {
            arduinoService.disconnect();
            updateConnectionStatus(false);
            addLog("✓ Desconectado del Arduino");
            
        } else {
            String selectedPort = (String) cmbPorts.getSelectedItem();
            if (selectedPort == null || selectedPort.equals("Sin puertos")) {
                JOptionPane.showMessageDialog(this,
                    "Por favor seleccione un puerto COM valido",
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
                            addLog("✓ Conexion establecida con " + selectedPort);
                            addLog("✓ Sensor biometrico responde correctamente");
                            
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
                                "• Arduino esta conectado al puerto\n" +
                                "• El firmware esta cargado correctamente\n" +
                                "• No hay otra aplicacion usando el puerto\n" +
                                "• Baudrate configurado en 115200",
                                "Error de Conexion",
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
            lblConnectionStatus.setText("CONECTADO");
            lblConnectionStatus.setForeground(new Color(46, 204, 113));
            btnConnect.setText("DESCONECTAR");
            btnConnect.setBackground(new Color(231, 76, 60));
            btnStartEnroll.setEnabled(true);
            cmbPorts.setEnabled(false);
            btnRefreshPorts.setEnabled(false);
        } else {
            lblConnectionStatus.setText("DESCONECTADO");
            lblConnectionStatus.setForeground(new Color(231, 76, 60));
            btnConnect.setText("CONECTAR");
            btnConnect.setBackground(new Color(46, 204, 113));
            btnStartEnroll.setEnabled(false);
            cmbPorts.setEnabled(true);
            btnRefreshPorts.setEnabled(true);
            lblSensorInfo.setText("Sensor: Esperando conexion...");
        }
    }
    
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
                logger.info("Proximo ID disponible: {}", nextAvailableId);
            }
            
        } catch (Exception e) {
            logger.error("Error al calcular proximo ID", e);
            nextAvailableId = 1;
        }
    }
    
    private void autoAssignId() {
        txtFingerprintId.setText(String.valueOf(nextAvailableId));
        addLog("✓ ID auto-asignado: " + nextAvailableId);
    }
    
    private void startEnrollment() {
        UserItem selectedUser = (UserItem) cmbUsers.getSelectedItem();
        if (selectedUser == null) {
            JOptionPane.showMessageDialog(this,
                "Por favor seleccione un usuario",
                "Usuario Requerido",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String fingerprintIdStr = txtFingerprintId.getText().trim();
        if (fingerprintIdStr.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Por favor ingrese el ID de huella",
                "ID Requerido",
                JOptionPane.WARNING_MESSAGE);
            txtFingerprintId.requestFocus();
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
                "El ID debe ser un numero entre 1 y 255",
                "ID Invalido",
                JOptionPane.ERROR_MESSAGE);
            txtFingerprintId.requestFocus();
            return;
        }
        
        if (!arduinoService.isConnected()) {
            JOptionPane.showMessageDialog(this,
                "Debe conectar el Arduino primero",
                "No Conectado",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        enrolling = true;
        btnStartEnroll.setEnabled(false);
        btnCancelEnroll.setVisible(true);
        cmbUsers.setEnabled(false);
        txtFingerprintId.setEnabled(false);
        btnAutoAssignId.setEnabled(false);
        
        progressBar.setValue(0);
        lblCurrentStep.setText("Iniciando enrolamiento...");
        lblStepCount.setText("Preparando sensor");
        
        addLog("═══════════════════════════════════════");
        addLog("INICIANDO ENROLAMIENTO");
        addLog("Usuario: " + selectedUser.toString());
        addLog("ID Huella: " + fingerprintId);
        addLog("═══════════════════════════════════════");
        
        final int finalFingerprintId = fingerprintId;
        final int finalUserId = selectedUser.getId();
        
        arduinoService.startEnroll(fingerprintId, new ArduinoCommService.EnrollCallback() {
            @Override
            public void onProgress(String message) {
                addLog("• " + message);
                lblCurrentStep.setText(message);
                
                if (message.contains("Coloque el dedo")) {
                    progressBar.setValue(20);
                    lblStepCount.setText("Paso 1/5: Primera captura");
                } else if (message.contains("capturada") && progressBar.getValue() < 40) {
                    progressBar.setValue(40);
                    lblStepCount.setText("Paso 2/5: Captura exitosa");
                } else if (message.contains("Retire")) {
                    progressBar.setValue(50);
                    lblStepCount.setText("Paso 3/5: Retire el dedo");
                } else if (message.contains("nuevamente")) {
                    progressBar.setValue(60);
                    lblStepCount.setText("Paso 4/5: Segunda captura");
                } else if (message.contains("modelo")) {
                    progressBar.setValue(80);
                    lblStepCount.setText("Paso 5/5: Creando modelo");
                } else if (message.contains("Guardando")) {
                    progressBar.setValue(90);
                    lblStepCount.setText("Finalizando...");
                }
            }
            
            @Override
            public void onSuccess(int id) {
                progressBar.setValue(100);
                lblCurrentStep.setText("¡Enrolamiento exitoso!");
                lblStepCount.setText("Completado");
                addLog("✓ ENROLAMIENTO COMPLETADO EXITOSAMENTE");
                addLog("✓ ID de huella: " + id);
                
                updateUserFingerprint(finalUserId, id);
                
                JOptionPane.showMessageDialog(EnrollPanel.this,
                    "Huella enrolada exitosamente!\n\n" +
                    "Usuario: " + selectedUser.toString() + "\n" +
                    "ID Huella: " + id,
                    "Exito",
                    JOptionPane.INFORMATION_MESSAGE);
                
                resetEnrollmentUI();
                loadUsers();
                calculateNextAvailableId();
                
                int templateCount = arduinoService.getTemplateCount();
                if (templateCount >= 0) {
                    lblSensorInfo.setText("Sensor: " + templateCount + " huellas registradas");
                }
            }
            
            @Override
            public void onError(String error) {
                progressBar.setValue(0);
                lblCurrentStep.setText("Error en enrolamiento");
                lblStepCount.setText("Proceso fallido");
                addLog("✗ ERROR: " + error);
                
                JOptionPane.showMessageDialog(EnrollPanel.this,
                    "Error en el enrolamiento:\n" + error + "\n\n" +
                    "Intente nuevamente.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                
                resetEnrollmentUI();
            }
        });
    }
    
    private void cancelEnrollment() {
        enrolling = false;
        resetEnrollmentUI();
        addLog("✗ Enrolamiento cancelado por el usuario");
        lblCurrentStep.setText("Enrolamiento cancelado");
        lblStepCount.setText(" ");
        progressBar.setValue(0);
    }
    
    private void resetEnrollmentUI() {
        enrolling = false;
        btnStartEnroll.setEnabled(true);
        btnCancelEnroll.setVisible(false);
        cmbUsers.setEnabled(true);
        txtFingerprintId.setEnabled(true);
        btnAutoAssignId.setEnabled(true);
        lblCurrentStep.setText("Esperando inicio...");
        lblStepCount.setText(" ");
    }
    
    private void updateUserFingerprint(int userId, int fingerprintId) {
        String sql = "UPDATE usuarios SET fingerprint_id = ?, fecha_modificacion = CURRENT_TIMESTAMP " +
                     "WHERE id_usuario = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, fingerprintId);
            pstmt.setInt(2, userId);
            
            int updated = pstmt.executeUpdate();
            
            if (updated > 0) {
                addLog("✓ Base de datos actualizada correctamente");
                logger.info("Usuario {} actualizado con fingerprint ID {}", userId, fingerprintId);
            } else {
                addLog("⚠ No se pudo actualizar la base de datos");
            }
            
        } catch (Exception e) {
            logger.error("Error al actualizar usuario", e);
            addLog("✗ Error al actualizar BD: " + e.getMessage());
        }
    }
    
    private void addLog(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = java.time.LocalTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            txtLog.append("[" + timestamp + "] " + message + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }
    
    public void cleanup() {
        if (arduinoService != null && arduinoService.isConnected()) {
            arduinoService.disconnect();
        }
    }
    
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
            String status = fingerprintId != null ? " [Huella: " + fingerprintId + "]" : "";
            return nombres + " " + apellidos + " (" + dni + ")" + status;
        }
    }
}
