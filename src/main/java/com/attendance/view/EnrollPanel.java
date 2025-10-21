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
 * Panel para enrolar huellas dactilares
 * 
 * @author Sistema Biométrico
 * @version 1.0
 */
public class EnrollPanel extends JPanel {
    
    private static final Logger logger = LoggerFactory.getLogger(EnrollPanel.class);
    
    private ArduinoCommService arduinoService;
    
    // Componentes de conexión
    private JComboBox<String> cmbPorts;
    private JButton btnConnect;
    private JButton btnRefreshPorts;
    private JLabel lblConnectionStatus;
    
    // Componentes de selección de usuario
    private JComboBox<UserItem> cmbUsers;
    private JTextField txtFingerprintId;
    private JButton btnStartEnroll;
    
    // Componentes de progreso
    private JTextArea txtLog;
    private JProgressBar progressBar;
    private JLabel lblCurrentStep;
    
    // Estado
    private boolean enrolling = false;
    
    public EnrollPanel() {
        this.arduinoService = new ArduinoCommService();
        initComponents();
        loadUsers();
        refreshPorts();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(20, 20));
        setBackground(new Color(250, 250, 250));
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        
        // Panel superior - Título
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        JLabel lblTitle = new JLabel("Enrolar Huella Dactilar");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(new Color(44, 62, 80));
        
        JLabel lblSubtitle = new JLabel("Registre huellas dactilares para los usuarios");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblSubtitle.setForeground(new Color(127, 140, 141));
        
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        titlePanel.add(lblTitle);
        titlePanel.add(Box.createVerticalStrut(5));
        titlePanel.add(lblSubtitle);
        
        headerPanel.add(titlePanel, BorderLayout.WEST);
        
        // Panel de contenido principal
        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        mainPanel.setOpaque(false);
        
        // Panel izquierdo - Configuración
        JPanel leftPanel = createLeftPanel();
        
        // Panel derecho - Log y progreso
        JPanel rightPanel = createRightPanel();
        
        mainPanel.add(leftPanel);
        mainPanel.add(rightPanel);
        
        add(headerPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
    }
    
    private JPanel createLeftPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        // Sección de conexión
        JPanel connectionSection = createSection("1. Conexión con Arduino");
        
        JPanel portPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        portPanel.setOpaque(false);
        
        JLabel lblPort = new JLabel("Puerto COM:");
        lblPort.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        cmbPorts = new JComboBox<>();
        cmbPorts.setPreferredSize(new Dimension(120, 30));
        cmbPorts.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        btnRefreshPorts = new JButton("🔄");
        btnRefreshPorts.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnRefreshPorts.setPreferredSize(new Dimension(40, 30));
        btnRefreshPorts.setToolTipText("Actualizar puertos");
        btnRefreshPorts.addActionListener(e -> refreshPorts());
        
        btnConnect = new JButton("Conectar");
        btnConnect.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnConnect.setBackground(new Color(46, 204, 113));
        btnConnect.setForeground(Color.WHITE);
        btnConnect.setFocusPainted(false);
        btnConnect.setBorderPainted(false);
        btnConnect.setPreferredSize(new Dimension(100, 30));
        btnConnect.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnConnect.addActionListener(e -> toggleConnection());
        
        portPanel.add(lblPort);
        portPanel.add(cmbPorts);
        portPanel.add(btnRefreshPorts);
        portPanel.add(btnConnect);
        
        lblConnectionStatus = new JLabel("⚫ Desconectado");
        lblConnectionStatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblConnectionStatus.setForeground(new Color(231, 76, 60));
        
        connectionSection.add(portPanel);
        connectionSection.add(Box.createVerticalStrut(10));
        connectionSection.add(lblConnectionStatus);
        
        // Sección de selección de usuario
        JPanel userSection = createSection("2. Seleccionar Usuario");
        
        JLabel lblUser = new JLabel("Usuario:");
        lblUser.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblUser.setAlignmentX(LEFT_ALIGNMENT);
        
        cmbUsers = new JComboBox<>();
        cmbUsers.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbUsers.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        cmbUsers.setAlignmentX(LEFT_ALIGNMENT);
        
        JButton btnReloadUsers = new JButton("🔄 Recargar Usuarios");
        btnReloadUsers.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnReloadUsers.setAlignmentX(LEFT_ALIGNMENT);
        btnReloadUsers.addActionListener(e -> loadUsers());
        
        userSection.add(lblUser);
        userSection.add(Box.createVerticalStrut(5));
        userSection.add(cmbUsers);
        userSection.add(Box.createVerticalStrut(10));
        userSection.add(btnReloadUsers);
        
        // Sección de ID de huella
        JPanel idSection = createSection("3. ID de Huella");
        
        JLabel lblId = new JLabel("ID en el sensor (1-255):");
        lblId.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblId.setAlignmentX(LEFT_ALIGNMENT);
        
        txtFingerprintId = new JTextField();
        txtFingerprintId.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtFingerprintId.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        txtFingerprintId.setAlignmentX(LEFT_ALIGNMENT);
        
        JLabel lblIdHelp = new JLabel("<html><small>Sugerencia: Use el DNI del usuario</small></html>");
        lblIdHelp.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblIdHelp.setForeground(Color.GRAY);
        lblIdHelp.setAlignmentX(LEFT_ALIGNMENT);
        
        idSection.add(lblId);
        idSection.add(Box.createVerticalStrut(5));
        idSection.add(txtFingerprintId);
        idSection.add(Box.createVerticalStrut(5));
        idSection.add(lblIdHelp);
        
        // Botón de iniciar enrolamiento
        btnStartEnroll = new JButton("▶ Iniciar Enrolamiento");
        btnStartEnroll.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnStartEnroll.setBackground(new Color(41, 128, 185));
        btnStartEnroll.setForeground(Color.WHITE);
        btnStartEnroll.setFocusPainted(false);
        btnStartEnroll.setBorderPainted(false);
        btnStartEnroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        btnStartEnroll.setAlignmentX(LEFT_ALIGNMENT);
        btnStartEnroll.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnStartEnroll.setEnabled(false);
        btnStartEnroll.addActionListener(e -> startEnrollment());
        
        // Agregar todo al panel
        panel.add(connectionSection);
        panel.add(Box.createVerticalStrut(20));
        panel.add(userSection);
        panel.add(Box.createVerticalStrut(20));
        panel.add(idSection);
        panel.add(Box.createVerticalStrut(30));
        panel.add(btnStartEnroll);
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
        
        // Título
        JLabel lblLogTitle = new JLabel("Proceso de Enrolamiento");
        lblLogTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblLogTitle.setForeground(new Color(44, 62, 80));
        
        // Paso actual
        lblCurrentStep = new JLabel("Esperando inicio...");
        lblCurrentStep.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblCurrentStep.setForeground(new Color(52, 152, 219));
        
        // Área de log
        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtLog.setBackground(new Color(250, 250, 250));
        txtLog.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        txtLog.setText("Sistema listo.\nConecte con Arduino para comenzar.");
        
        JScrollPane scrollPane = new JScrollPane(txtLog);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        
        // Barra de progreso
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setForeground(new Color(52, 152, 219));
        progressBar.setValue(0);
        
        // Panel superior
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);
        topPanel.add(lblLogTitle);
        topPanel.add(Box.createVerticalStrut(10));
        topPanel.add(lblCurrentStep);
        
        panel.add(topPanel, BorderLayout.NORTH);
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
        return section;
    }
    
    private void refreshPorts() {
        cmbPorts.removeAllItems();
        List<String> ports = SerialPortManager.getAvailablePorts();
        
        if (ports.isEmpty()) {
            cmbPorts.addItem("Sin puertos disponibles");
            btnConnect.setEnabled(false);
            addLog("⚠️ No se encontraron puertos COM disponibles");
        } else {
            for (String port : ports) {
                cmbPorts.addItem(port);
            }
            btnConnect.setEnabled(true);
            addLog("✓ Puertos COM actualizados: " + ports.size() + " encontrados");
        }
    }
    
    private void toggleConnection() {
        if (arduinoService.isConnected()) {
            // Desconectar
            arduinoService.disconnect();
            updateConnectionStatus(false);
            addLog("✓ Desconectado de Arduino");
        } else {
            // Conectar
            String selectedPort = (String) cmbPorts.getSelectedItem();
            if (selectedPort == null || selectedPort.equals("Sin puertos disponibles")) {
                JOptionPane.showMessageDialog(this,
                    "Por favor seleccione un puerto COM válido",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            addLog("Conectando con " + selectedPort + "...");
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
                            addLog("✓ Conectado exitosamente a " + selectedPort);
                            addLog("✓ Arduino responde correctamente");
                        } else {
                            addLog("✗ Error al conectar con Arduino");
                            JOptionPane.showMessageDialog(EnrollPanel.this,
                                "No se pudo conectar con Arduino.\nVerifique:\n" +
                                "1. Arduino está conectado al puerto correcto\n" +
                                "2. El firmware está cargado\n" +
                                "3. No hay otra aplicación usando el puerto",
                                "Error de Conexión",
                                JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception e) {
                        logger.error("Error al conectar", e);
                        updateConnectionStatus(false);
                        addLog("✗ Error: " + e.getMessage());
                    }
                    
                    btnConnect.setEnabled(true);
                    btnConnect.setText(arduinoService.isConnected() ? "Desconectar" : "Conectar");
                }
            };
            
            worker.execute();
        }
    }
    
    private void updateConnectionStatus(boolean connected) {
        if (connected) {
            lblConnectionStatus.setText("🟢 Conectado - " + arduinoService.getCurrentPort());
            lblConnectionStatus.setForeground(new Color(46, 204, 113));
            btnConnect.setText("Desconectar");
            btnConnect.setBackground(new Color(231, 76, 60));
            btnStartEnroll.setEnabled(true);
            cmbPorts.setEnabled(false);
            btnRefreshPorts.setEnabled(false);
        } else {
            lblConnectionStatus.setText("⚫ Desconectado");
            lblConnectionStatus.setForeground(new Color(231, 76, 60));
            btnConnect.setText("Conectar");
            btnConnect.setBackground(new Color(46, 204, 113));
            btnStartEnroll.setEnabled(false);
            cmbPorts.setEnabled(true);
            btnRefreshPorts.setEnabled(true);
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
            
            addLog("✓ Cargados " + count + " usuarios");
            
        } catch (Exception e) {
            logger.error("Error al cargar usuarios", e);
            addLog("✗ Error al cargar usuarios: " + e.getMessage());
        }
    }
    
    private void startEnrollment() {
        if (enrolling) {
            JOptionPane.showMessageDialog(this,
                "Ya hay un proceso de enrolamiento en curso",
                "Advertencia",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Validar selección de usuario
        UserItem selectedUser = (UserItem) cmbUsers.getSelectedItem();
        if (selectedUser == null) {
            JOptionPane.showMessageDialog(this,
                "Por favor seleccione un usuario",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Validar ID de huella
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
                "El ID de huella debe ser un número entre 1 y 255",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Confirmar
        int confirm = JOptionPane.showConfirmDialog(this,
            "¿Iniciar enrolamiento para?\n\n" +
            "Usuario: " + selectedUser.toString() + "\n" +
            "ID Huella: " + fingerprintId,
            "Confirmar Enrolamiento",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        
        // Iniciar proceso
        enrolling = true;
        btnStartEnroll.setEnabled(false);
        btnConnect.setEnabled(false);
        cmbUsers.setEnabled(false);
        txtFingerprintId.setEnabled(false);
        progressBar.setValue(0);
        progressBar.setIndeterminate(true);
        
        txtLog.setText("");
        addLog("=== INICIANDO ENROLAMIENTO ===");
        addLog("Usuario: " + selectedUser.toString());
        addLog("ID Huella: " + fingerprintId);
        addLog("==============================\n");
        
        arduinoService.startEnroll(fingerprintId, new ArduinoCommService.EnrollCallback() {
            @Override
            public void onProgress(String message) {
                SwingUtilities.invokeLater(() -> {
                    addLog("▶ " + message);
                    lblCurrentStep.setText(message);
                    
                    // Actualizar progreso basado en el mensaje
                    if (message.contains("Coloque el dedo")) {
                        progressBar.setValue(25);
                    } else if (message.contains("Retire el dedo")) {
                        progressBar.setValue(50);
                    } else if (message.contains("mismo dedo nuevamente")) {
                        progressBar.setValue(75);
                    }
                });
            }
            
            @Override
            public void onSuccess(int enrolledId) {
                SwingUtilities.invokeLater(() -> {
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(100);
                    lblCurrentStep.setText("✓ Enrolamiento completado");
                    addLog("\n✓✓✓ ENROLAMIENTO EXITOSO ✓✓✓");
                    
                    // Actualizar en base de datos
                    updateUserFingerprint(selectedUser.getId(), fingerprintId);
                    
                    enrolling = false;
                    btnStartEnroll.setEnabled(true);
                    btnConnect.setEnabled(true);
                    cmbUsers.setEnabled(true);
                    txtFingerprintId.setEnabled(true);
                    
                    JOptionPane.showMessageDialog(EnrollPanel.this,
                        "Huella registrada exitosamente para:\n" + selectedUser.toString(),
                        "Éxito",
                        JOptionPane.INFORMATION_MESSAGE);
                    
                    // Recargar usuarios
                    loadUsers();
                });
            }
            
            @Override
            public void onError(String error) {
                SwingUtilities.invokeLater(() -> {
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(0);
                    lblCurrentStep.setText("✗ Error en el proceso");
                    addLog("\n✗✗✗ ERROR: " + error + " ✗✗✗");
                    
                    enrolling = false;
                    btnStartEnroll.setEnabled(true);
                    btnConnect.setEnabled(true);
                    cmbUsers.setEnabled(true);
                    txtFingerprintId.setEnabled(true);
                    
                    JOptionPane.showMessageDialog(EnrollPanel.this,
                        "Error durante el enrolamiento:\n" + error,
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                });
            }
        });
    }
    
    private void updateUserFingerprint(int userId, int fingerprintId) {
        String sql = "UPDATE usuarios SET fingerprint_id = ? WHERE id_usuario = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, fingerprintId);
            pstmt.setInt(2, userId);
            int updated = pstmt.executeUpdate();
            
            if (updated > 0) {
                addLog("✓ Usuario actualizado en base de datos");
                logger.info("Fingerprint ID {} asignado al usuario {}", fingerprintId, userId);
            }
            
        } catch (Exception e) {
            logger.error("Error al actualizar usuario", e);
            addLog("✗ Error al actualizar base de datos: " + e.getMessage());
        }
    }
    
    private void addLog(String message) {
        SwingUtilities.invokeLater(() -> {
            txtLog.append(message + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }
    
    // Clase interna para items del combo de usuarios
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
        
        @Override
        public String toString() {
            String name = apellidos + ", " + nombres + " (" + dni + ")";
            if (fingerprintId != null) {
                return name + " [Huella: " + fingerprintId + "]";
            }
            return name;
        }
    }
}