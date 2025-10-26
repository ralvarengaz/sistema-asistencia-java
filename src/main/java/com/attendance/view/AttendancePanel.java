package com.attendance.view;

import com.attendance.config.DatabaseConfig;
import com.attendance.service.ArduinoCommService;
import com.attendance.util.SerialPortManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Panel de registro de asistencias
 * VERSIÓN MEJORADA: Sin funciones almacenadas + Logging detallado
 * 
 * @author Sistema Biométrico
 * @version 2.1 - Mejorado
 */
public class AttendancePanel extends JPanel {
    
    private static final Logger logger = LoggerFactory.getLogger(AttendancePanel.class);
    
    private ArduinoCommService arduinoService;
    
    private JComboBox<String> cmbPorts;
    private JButton btnConnect;
    private JButton btnRefreshPorts;
    private JLabel lblConnectionStatus;
    
    private JLabel lblCurrentTime;
    private JLabel lblInstructions;
    private JButton btnMarkEntry;
    private JButton btnMarkExit;
    private JLabel lblStatusMessage;
    
    private JPanel userInfoPanel;
    private JLabel lblUserPhoto;
    private JLabel lblUserName;
    private JLabel lblUserDNI;
    private JLabel lblUserDepartment;
    private JLabel lblConfidence;
    
    private JTable tableAttendances;
    private DefaultTableModel tableModel;
    
    private boolean waiting = false;
    private Timer clockTimer;
    private String currentMarkType = "ENTRADA";
    
    public AttendancePanel() {
        logger.info("Inicializando AttendancePanel...");
        this.arduinoService = new ArduinoCommService();
        initComponents();
        refreshPorts();
        startClock();
        loadTodayAttendances();
        logger.info("AttendancePanel inicializado correctamente");
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(250, 250, 250));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JPanel headerPanel = createHeaderPanel();
        JPanel mainPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        mainPanel.setOpaque(false);
        
        JPanel leftPanel = createLeftPanel();
        JPanel centerPanel = createCenterPanel();
        JPanel rightPanel = createRightPanel();
        
        mainPanel.add(leftPanel);
        mainPanel.add(centerPanel);
        mainPanel.add(rightPanel);
        
        JPanel bottomPanel = createBottomPanel();
        
        add(headerPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        
        JLabel lblTitle = new JLabel("Marcar Asistencia");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(new Color(44, 62, 80));
        
        JLabel lblSubtitle = new JLabel("Sistema de registro biométrico de entrada y salida");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSubtitle.setForeground(new Color(127, 140, 141));
        
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        titlePanel.add(lblTitle);
        titlePanel.add(Box.createVerticalStrut(5));
        titlePanel.add(lblSubtitle);
        
        panel.add(titlePanel, BorderLayout.WEST);
        
        return panel;
    }
    
    private JPanel createLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        JPanel connectionPanel = new JPanel();
        connectionPanel.setLayout(new BoxLayout(connectionPanel, BoxLayout.Y_AXIS));
        connectionPanel.setOpaque(false);
        connectionPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            "Conexión Arduino",
            0, 0,
            new Font("Segoe UI", Font.BOLD, 13)
        ));
        
        JLabel lblPort = new JLabel("Puerto COM:");
        lblPort.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblPort.setAlignmentX(LEFT_ALIGNMENT);
        
        JPanel portPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        portPanel.setOpaque(false);
        portPanel.setAlignmentX(LEFT_ALIGNMENT);
        
        cmbPorts = new JComboBox<>();
        cmbPorts.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cmbPorts.setPreferredSize(new Dimension(120, 28));
        
        btnRefreshPorts = new JButton("↻");
        btnRefreshPorts.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnRefreshPorts.setPreferredSize(new Dimension(35, 28));
        btnRefreshPorts.setToolTipText("Actualizar puertos");
        btnRefreshPorts.addActionListener(e -> refreshPorts());
        
        portPanel.add(cmbPorts);
        portPanel.add(btnRefreshPorts);
        
        btnConnect = new JButton("CONECTAR");
        btnConnect.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnConnect.setBackground(new Color(46, 204, 113));
        btnConnect.setForeground(Color.WHITE);
        btnConnect.setFocusPainted(false);
        btnConnect.setPreferredSize(new Dimension(200, 35));
        btnConnect.setMaximumSize(new Dimension(250, 35));
        btnConnect.setAlignmentX(LEFT_ALIGNMENT);
        btnConnect.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnConnect.addActionListener(e -> toggleConnection());
        
        lblConnectionStatus = new JLabel("DESCONECTADO");
        lblConnectionStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblConnectionStatus.setForeground(new Color(231, 76, 60));
        lblConnectionStatus.setAlignmentX(LEFT_ALIGNMENT);
        
        connectionPanel.add(lblPort);
        connectionPanel.add(Box.createVerticalStrut(5));
        connectionPanel.add(portPanel);
        connectionPanel.add(Box.createVerticalStrut(10));
        connectionPanel.add(btnConnect);
        connectionPanel.add(Box.createVerticalStrut(8));
        connectionPanel.add(lblConnectionStatus);
        
        JPanel markPanel = new JPanel();
        markPanel.setLayout(new BoxLayout(markPanel, BoxLayout.Y_AXIS));
        markPanel.setOpaque(false);
        markPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            "Tipo de Marcación",
            0, 0,
            new Font("Segoe UI", Font.BOLD, 13)
        ));
        
        btnMarkEntry = new JButton("ENTRADA");
        btnMarkEntry.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnMarkEntry.setBackground(new Color(52, 152, 219));
        btnMarkEntry.setForeground(Color.WHITE);
        btnMarkEntry.setFocusPainted(false);
        btnMarkEntry.setBorderPainted(false);
        btnMarkEntry.setPreferredSize(new Dimension(200, 55));
        btnMarkEntry.setMaximumSize(new Dimension(250, 55));
        btnMarkEntry.setAlignmentX(LEFT_ALIGNMENT);
        btnMarkEntry.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnMarkEntry.setEnabled(false);
        btnMarkEntry.addActionListener(e -> startMarking("ENTRADA"));
        
        btnMarkExit = new JButton("SALIDA");
        btnMarkExit.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnMarkExit.setBackground(new Color(230, 126, 34));
        btnMarkExit.setForeground(Color.WHITE);
        btnMarkExit.setFocusPainted(false);
        btnMarkExit.setBorderPainted(false);
        btnMarkExit.setPreferredSize(new Dimension(200, 55));
        btnMarkExit.setMaximumSize(new Dimension(250, 55));
        btnMarkExit.setAlignmentX(LEFT_ALIGNMENT);
        btnMarkExit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnMarkExit.setEnabled(false);
        btnMarkExit.addActionListener(e -> startMarking("SALIDA"));
        
        markPanel.add(btnMarkEntry);
        markPanel.add(Box.createVerticalStrut(10));
        markPanel.add(btnMarkExit);
        
        panel.add(connectionPanel, BorderLayout.NORTH);
        panel.add(markPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        timePanel.setOpaque(false);
        
        lblCurrentTime = new JLabel("--:--:--");
        lblCurrentTime.setFont(new Font("Segoe UI", Font.BOLD, 48));
        lblCurrentTime.setForeground(new Color(52, 73, 94));
        
        timePanel.add(lblCurrentTime);
        
        lblStatusMessage = new JLabel("Coloque su dedo en el sensor");
        lblStatusMessage.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblStatusMessage.setForeground(new Color(52, 152, 219));
        lblStatusMessage.setHorizontalAlignment(SwingConstants.CENTER);
        
        lblInstructions = new JLabel("<html><center>Presione un botón de marcación y<br>coloque su dedo en el sensor</center></html>");
        lblInstructions.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblInstructions.setForeground(new Color(127, 140, 141));
        lblInstructions.setHorizontalAlignment(SwingConstants.CENTER);
        
        userInfoPanel = createUserInfoPanel();
        
        JPanel centerContent = new JPanel();
        centerContent.setLayout(new BoxLayout(centerContent, BoxLayout.Y_AXIS));
        centerContent.setOpaque(false);
        
        centerContent.add(timePanel);
        centerContent.add(Box.createVerticalStrut(20));
        centerContent.add(lblStatusMessage);
        centerContent.add(Box.createVerticalStrut(10));
        centerContent.add(lblInstructions);
        centerContent.add(Box.createVerticalStrut(20));
        centerContent.add(userInfoPanel);
        
        panel.add(centerContent, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createUserInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 10));
        panel.setBackground(new Color(245, 245, 245));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        panel.setVisible(false);
        
        lblUserPhoto = new JLabel("?", SwingConstants.CENTER);
        lblUserPhoto.setFont(new Font("Segoe UI", Font.BOLD, 40));
        lblUserPhoto.setForeground(Color.WHITE);
        lblUserPhoto.setOpaque(true);
        lblUserPhoto.setBackground(new Color(149, 165, 166));
        lblUserPhoto.setPreferredSize(new Dimension(90, 90));
        lblUserPhoto.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        
        lblUserName = new JLabel("Nombre del Usuario");
        lblUserName.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblUserName.setForeground(new Color(44, 62, 80));
        lblUserName.setAlignmentX(LEFT_ALIGNMENT);
        
        lblUserDNI = new JLabel("DNI: 00000000");
        lblUserDNI.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblUserDNI.setForeground(new Color(127, 140, 141));
        lblUserDNI.setAlignmentX(LEFT_ALIGNMENT);
        
        lblUserDepartment = new JLabel("Departamento");
        lblUserDepartment.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        lblUserDepartment.setForeground(new Color(149, 165, 166));
        lblUserDepartment.setAlignmentX(LEFT_ALIGNMENT);
        
        lblConfidence = new JLabel("Confianza: 0/255");
        lblConfidence.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblConfidence.setForeground(new Color(46, 204, 113));
        lblConfidence.setAlignmentX(LEFT_ALIGNMENT);
        
        infoPanel.add(lblUserName);
        infoPanel.add(Box.createVerticalStrut(3));
        infoPanel.add(lblUserDNI);
        infoPanel.add(Box.createVerticalStrut(3));
        infoPanel.add(lblUserDepartment);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(lblConfidence);
        
        panel.add(lblUserPhoto, BorderLayout.WEST);
        panel.add(infoPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        JLabel lblTableTitle = new JLabel("Asistencias de Hoy");
        lblTableTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTableTitle.setForeground(new Color(44, 62, 80));
        
        String[] columns = {"#", "Hora", "Nombre", "DNI", "Tipo", "Conf.", "Estado"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        tableAttendances = new JTable(tableModel);
        tableAttendances.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tableAttendances.setRowHeight(28);
        tableAttendances.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tableAttendances.getTableHeader().setBackground(new Color(52, 73, 94));
        tableAttendances.getTableHeader().setForeground(Color.WHITE);
        
        tableAttendances.getColumnModel().getColumn(0).setPreferredWidth(40);
        tableAttendances.getColumnModel().getColumn(1).setPreferredWidth(80);
        tableAttendances.getColumnModel().getColumn(2).setPreferredWidth(150);
        tableAttendances.getColumnModel().getColumn(3).setPreferredWidth(90);
        tableAttendances.getColumnModel().getColumn(4).setPreferredWidth(80);
        tableAttendances.getColumnModel().getColumn(5).setPreferredWidth(60);
        tableAttendances.getColumnModel().getColumn(6).setPreferredWidth(80);
        
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        tableAttendances.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        tableAttendances.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        tableAttendances.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);
        
        DefaultTableCellRenderer tipoRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                
                if (!isSelected) {
                    if ("ENTRADA".equals(value)) {
                        setBackground(new Color(46, 204, 113));
                        setForeground(Color.WHITE);
                    } else if ("SALIDA".equals(value)) {
                        setBackground(new Color(230, 126, 34));
                        setForeground(Color.WHITE);
                    } else {
                        setBackground(Color.WHITE);
                        setForeground(Color.BLACK);
                    }
                }
                return c;
            }
        };
        tableAttendances.getColumnModel().getColumn(4).setCellRenderer(tipoRenderer);
        
        DefaultTableCellRenderer estadoRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                
                if (!isSelected) {
                    if ("TARDE".equals(value)) {
                        setBackground(new Color(231, 76, 60));
                        setForeground(Color.WHITE);
                    } else {
                        setBackground(new Color(46, 204, 113));
                        setForeground(Color.WHITE);
                    }
                }
                return c;
            }
        };
        tableAttendances.getColumnModel().getColumn(6).setCellRenderer(estadoRenderer);
        
        JScrollPane scrollPane = new JScrollPane(tableAttendances);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        
        panel.add(lblTableTitle, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        panel.setOpaque(false);
        
        JButton btnRefresh = new JButton("Actualizar Lista");
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRefresh.setBackground(new Color(52, 152, 219));
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.setFocusPainted(false);
        btnRefresh.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefresh.setPreferredSize(new Dimension(140, 32));
        btnRefresh.addActionListener(e -> loadTodayAttendances());
        
        panel.add(btnRefresh);
        
        return panel;
    }
    
    private void refreshPorts() {
        logger.debug("Actualizando lista de puertos COM...");
        cmbPorts.removeAllItems();
        java.util.List<String> ports = SerialPortManager.getAvailablePorts();
        
        if (ports.isEmpty()) {
            cmbPorts.addItem("Sin puertos");
            btnConnect.setEnabled(false);
            logger.warn("⚠️  No se encontraron puertos COM disponibles");
        } else {
            for (String port : ports) {
                cmbPorts.addItem(port);
            }
            btnConnect.setEnabled(true);
            logger.info("✅ Puertos COM detectados: {}", ports);
        }
    }
    
    private void toggleConnection() {
        if (arduinoService.isConnected()) {
            logger.info("Desconectando Arduino...");
            arduinoService.disconnect();
            
            btnConnect.setText("CONECTAR");
            btnConnect.setBackground(new Color(46, 204, 113));
            lblConnectionStatus.setText("DESCONECTADO");
            lblConnectionStatus.setForeground(new Color(231, 76, 60));
            
            btnMarkEntry.setEnabled(false);
            btnMarkExit.setEnabled(false);
            cmbPorts.setEnabled(true);
            btnRefreshPorts.setEnabled(true);
            
            logger.info("✅ Desconectado del Arduino");
            
        } else {
            String selectedPort = (String) cmbPorts.getSelectedItem();
            if (selectedPort == null || selectedPort.equals("Sin puertos")) {
                logger.warn("⚠️  Intento de conexión sin puerto válido");
                JOptionPane.showMessageDialog(this,
                    "No hay puertos COM disponibles.\nVerifique que el Arduino esté conectado.",
                    "Puerto No Disponible",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            logger.info("Intentando conectar al puerto: {}", selectedPort);
            btnConnect.setEnabled(false);
            btnConnect.setText("CONECTANDO...");
            
            SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    return arduinoService.connect(selectedPort);
                }
                
                @Override
                protected void done() {
                    try {
                        boolean success = get();
                        
                        if (success) {
                            btnConnect.setText("DESCONECTAR");
                            btnConnect.setBackground(new Color(231, 76, 60));
                            lblConnectionStatus.setText("CONECTADO");
                            lblConnectionStatus.setForeground(new Color(46, 204, 113));
                            
                            btnMarkEntry.setEnabled(true);
                            btnMarkExit.setEnabled(true);
                            cmbPorts.setEnabled(false);
                            btnRefreshPorts.setEnabled(false);
                            
                            logger.info("✅ Conexión exitosa con Arduino en {}", selectedPort);
                            
                            JOptionPane.showMessageDialog(AttendancePanel.this,
                                "Conexión establecida correctamente con " + selectedPort,
                                "Conectado",
                                JOptionPane.INFORMATION_MESSAGE);
                            
                        } else {
                            btnConnect.setText("CONECTAR");
                            logger.error("❌ Fallo al conectar con Arduino en {}", selectedPort);
                            
                            JOptionPane.showMessageDialog(AttendancePanel.this,
                                "No se pudo conectar con el Arduino.\n\n" +
                                "Verifique:\n" +
                                "1. El Arduino está conectado al puerto " + selectedPort + "\n" +
                                "2. El firmware está cargado correctamente\n" +
                                "3. El sensor DY50 está conectado (TX→D10, RX→D11)\n" +
                                "4. Baudrate configurado en 115200",
                                "Error de Conexión",
                                JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception e) {
                        btnConnect.setText("CONECTAR");
                        logger.error("❌ Error en conexión", e);
                        JOptionPane.showMessageDialog(AttendancePanel.this,
                            "Error al conectar: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    } finally {
                        btnConnect.setEnabled(true);
                    }
                }
            };
            
            worker.execute();
        }
    }
    
    private void startMarking(String tipo) {
        if (!arduinoService.isConnected()) {
            logger.warn("⚠️  Intento de marcación sin Arduino conectado");
            JOptionPane.showMessageDialog(this,
                "Debe conectar el Arduino primero",
                "No Conectado",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (waiting) {
            logger.debug("Ya hay una marcación en progreso");
            return;
        }
        
        waiting = true;
        currentMarkType = tipo;
        userInfoPanel.setVisible(false);
        
        btnMarkEntry.setEnabled(false);
        btnMarkExit.setEnabled(false);
        
        lblStatusMessage.setText("Esperando huella...");
        lblInstructions.setText("<html><center>Coloque su dedo en el sensor<br>para marcar " + tipo + "</center></html>");
        
        logger.info("🔄 Iniciando marcación tipo: {}", tipo);
        
        arduinoService.startVerify(new ArduinoCommService.VerifyCallback() {
            @Override
            public void onWaiting(String message) {
                logger.debug("Esperando: {}", message);
                lblInstructions.setText("<html><center>" + message + "</center></html>");
            }
            
            @Override
            public void onSuccess(int fingerprintId, int confidence) {
                logger.info("✅ Huella reconocida - ID: {}, Confianza: {}", fingerprintId, confidence);
                processAttendance(fingerprintId, confidence, currentMarkType);
            }
            
            @Override
            public void onNotFound() {
                waiting = false;
                btnMarkEntry.setEnabled(true);
                btnMarkExit.setEnabled(true);
                
                logger.warn("⚠️  Huella no registrada en el sistema");
                
                lblStatusMessage.setText("Huella no registrada");
                lblInstructions.setText("<html><center>La huella no está registrada en el sistema<br>Contacte al administrador</center></html>");
                
                JOptionPane.showMessageDialog(AttendancePanel.this,
                    "Huella no registrada en el sistema.\n" +
                    "Por favor contacte al administrador para registrar su huella.",
                    "Huella No Registrada",
                    JOptionPane.WARNING_MESSAGE);
                
                javax.swing.Timer timer = new javax.swing.Timer(3000, e -> resetUI());
                timer.setRepeats(false);
                timer.start();
            }
            
            @Override
            public void onError(String error) {
                waiting = false;
                btnMarkEntry.setEnabled(true);
                btnMarkExit.setEnabled(true);
                
                logger.error("❌ Error en verificación: {}", error);
                
                lblStatusMessage.setText("Error");
                lblInstructions.setText("<html><center>Error: " + error + "</center></html>");
                
                javax.swing.Timer timer = new javax.swing.Timer(3000, e -> resetUI());
                timer.setRepeats(false);
                timer.start();
            }
        });
    }
    
    /**
     * MÉTODO MEJORADO: Procesa asistencia sin funciones almacenadas
     * Usa SQL directo con transacciones y logging detallado
     */
    private void processAttendance(int fingerprintId, int confidence, String tipo) {
        logger.info("╔════════════════════════════════════════════════════════╗");
        logger.info("║  PROCESANDO ASISTENCIA (Versión mejorada sin función) ║");
        logger.info("╚════════════════════════════════════════════════════════╝");
        logger.info("📊 Fingerprint ID: {}", fingerprintId);
        logger.info("📊 Confidence: {}", confidence);
        logger.info("📊 Tipo: {}", tipo);
        logger.info("───────────────────────────────────────────────────────────");
        
        Connection conn = null;
        
        try {
            // ============================================
            // PASO 1: Obtener conexión e iniciar transacción
            // ============================================
            conn = DatabaseConfig.getConnection();
            
            if (conn == null || conn.isClosed()) {
                logger.error("❌ ERROR CRÍTICO: Conexión a BD cerrada o nula");
                showError("Error de conexión a la base de datos");
                return;
            }
            
            conn.setAutoCommit(false);
            logger.info("✅ Conexión establecida - Transacción iniciada");
            
            // ============================================
            // PASO 2: Buscar el usuario por fingerprint_id
            // ============================================
            Integer idUsuario = null;
            String nombreUsuario = null;
            
            String sqlBuscarUsuario = 
                "SELECT id_usuario, nombres || ' ' || apellidos as nombre_completo " +
                "FROM usuarios " +
                "WHERE fingerprint_id = ? AND activo = TRUE";
            
            logger.debug("🔍 Buscando usuario con fingerprint_id: {}", fingerprintId);
            
            try (PreparedStatement pstmt = conn.prepareStatement(sqlBuscarUsuario)) {
                pstmt.setInt(1, fingerprintId);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        idUsuario = rs.getInt("id_usuario");
                        nombreUsuario = rs.getString("nombre_completo");
                        logger.info("✅ Usuario encontrado: {} (ID: {})", nombreUsuario, idUsuario);
                    } else {
                        logger.warn("⚠️  Usuario no encontrado con fingerprint_id: {}", fingerprintId);
                    }
                }
            }
            
            // Verificar si se encontró el usuario
            if (idUsuario == null) {
                conn.rollback();
                logger.warn("⚠️  Transacción revertida - Usuario no encontrado");
                
                lblStatusMessage.setText("Usuario no encontrado");
                lblInstructions.setText(
                    "<html><center>Huella no registrada<br>" +
                    "Fingerprint ID: " + fingerprintId + "</center></html>");
                
                JOptionPane.showMessageDialog(this,
                    "No se encontró ningún usuario con la huella proporcionada.\n" +
                    "Fingerprint ID: " + fingerprintId + "\n\n" +
                    "Contacte al administrador para registrar su huella.",
                    "Usuario no encontrado",
                    JOptionPane.WARNING_MESSAGE);
                
                return;
            }
            
            // ============================================
            // PASO 3: Registrar la asistencia
            // ============================================
            Integer idAsistencia = null;
            
            String sqlInsertarAsistencia = 
                "INSERT INTO asistencias " +
                "(id_usuario, tipo_marcacion, confidence_score, metodo, fecha_hora) " +
                "VALUES (?, ?, ?, 'FINGERPRINT', CURRENT_TIMESTAMP) " +
                "RETURNING id_asistencia";
            
            logger.debug("💾 Insertando asistencia...");
            
            try (PreparedStatement pstmt = conn.prepareStatement(sqlInsertarAsistencia)) {
                pstmt.setInt(1, idUsuario);
                pstmt.setString(2, tipo);
                pstmt.setInt(3, confidence);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        idAsistencia = rs.getInt("id_asistencia");
                        logger.info("✅ Asistencia registrada con ID: {}", idAsistencia);
                    }
                }
            }
            
            // ============================================
            // PASO 4: Registrar en el log del sistema
            // ============================================
            String sqlInsertarLog = 
                "INSERT INTO logs_sistema (nivel, modulo, mensaje, usuario, fecha_hora) " +
                "VALUES ('INFO', 'ASISTENCIA', ?, ?, CURRENT_TIMESTAMP)";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sqlInsertarLog)) {
                String mensaje = String.format(
                    "%s registrada - Usuario: %s (ID: %d) - FP_ID: %d - Confidence: %d",
                    tipo, nombreUsuario, idUsuario, fingerprintId, confidence
                );
                pstmt.setString(1, mensaje);
                pstmt.setString(2, nombreUsuario);
                pstmt.executeUpdate();
                
                logger.debug("✅ Log del sistema registrado");
            }
            
            // ============================================
            // PASO 5: Commit de la transacción
            // ============================================
            conn.commit();
            logger.info("✅ Transacción completada exitosamente");
            logger.info("───────────────────────────────────────────────────────────");
            logger.info("🎉 ASISTENCIA PROCESADA CORRECTAMENTE");
            logger.info("╚════════════════════════════════════════════════════════╝");
            
            // ============================================
            // PASO 6: Actualizar la interfaz
            // ============================================
            displayUserInfo(idUsuario, nombreUsuario, confidence, tipo);
            loadTodayAttendances();
            
            lblStatusMessage.setText(tipo + " registrada!");
            lblInstructions.setText(
                "<html><center>" + nombreUsuario + 
                "<br>Asistencia registrada correctamente</center></html>");
            
            // Sonido de confirmación
            Toolkit.getDefaultToolkit().beep();
            
        } catch (SQLException e) {
            // Rollback en caso de error
            if (conn != null) {
                try {
                    conn.rollback();
                    logger.error("⚠️  Transacción revertida debido a error SQL");
                } catch (SQLException ex) {
                    logger.error("❌ Error al hacer rollback", ex);
                }
            }
            
            logger.error("❌ ERROR SQL al procesar asistencia", e);
            logger.error("   Mensaje: {}", e.getMessage());
            logger.error("   Estado SQL: {}", e.getSQLState());
            logger.error("   Código: {}", e.getErrorCode());
            logger.error("╚════════════════════════════════════════════════════════╝");
            
            lblStatusMessage.setText("Error al registrar");
            lblInstructions.setText(
                "<html><center>Error en la base de datos<br>" +
                "Revise los logs para más detalles</center></html>");
            
            JOptionPane.showMessageDialog(this,
                "Error al registrar asistencia:\n" + e.getMessage() +
                "\n\nEstado SQL: " + e.getSQLState(),
                "Error de Base de Datos",
                JOptionPane.ERROR_MESSAGE);
            
        } catch (Exception e) {
            // Rollback en caso de error general
            if (conn != null) {
                try {
                    conn.rollback();
                    logger.error("⚠️  Transacción revertida debido a error general");
                } catch (SQLException ex) {
                    logger.error("❌ Error al hacer rollback", ex);
                }
            }
            
            logger.error("❌ ERROR GENERAL al procesar asistencia", e);
            logger.error("╚════════════════════════════════════════════════════════╝");
            
            lblStatusMessage.setText("Error inesperado");
            lblInstructions.setText("<html><center>Error inesperado</center></html>");
            
            JOptionPane.showMessageDialog(this,
                "Error inesperado: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
            
        } finally {
            // Restaurar auto-commit y cerrar conexión
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                    logger.debug("🔒 Conexión cerrada - Auto-commit restaurado");
                } catch (SQLException e) {
                    logger.error("❌ Error al cerrar conexión", e);
                }
            }
            
            // Reset UI después de 4 segundos
            javax.swing.Timer timer = new javax.swing.Timer(4000, e -> resetUI());
            timer.setRepeats(false);
            timer.start();
        }
    }
    
    /**
     * Muestra mensaje de error en la interfaz
     */
    private void showError(String message) {
        lblStatusMessage.setText("Error al registrar");
        lblInstructions.setText("<html><center>" + message + "</center></html>");
        
        JOptionPane.showMessageDialog(this,
            message,
            "Error",
            JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Muestra información del usuario en la interfaz
     */
    private void displayUserInfo(int idUsuario, String nombreCompleto, int confidence, String tipo) {
        logger.debug("📋 Cargando información detallada del usuario ID: {}", idUsuario);
        
        String sql = "SELECT u.dni, u.nombres, u.apellidos, d.nombre AS departamento " +
                     "FROM usuarios u " +
                     "LEFT JOIN departamentos d ON u.id_departamento = d.id_departamento " +
                     "WHERE u.id_usuario = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, idUsuario);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String dni = rs.getString("dni");
                    String nombres = rs.getString("nombres");
                    String apellidos = rs.getString("apellidos");
                    String departamento = rs.getString("departamento");
                    
                    logger.debug("✅ Información cargada: {} {} - DNI: {}", nombres, apellidos, dni);
                    
                    // Generar iniciales
                    String iniciales = "";
                    if (nombres != null && nombres.length() > 0) {
                        iniciales += nombres.charAt(0);
                    }
                    if (apellidos != null && apellidos.length() > 0) {
                        iniciales += apellidos.charAt(0);
                    }
                    
                    if (iniciales.isEmpty()) {
                        lblUserPhoto.setText("?");
                    } else {
                        lblUserPhoto.setText(iniciales.toUpperCase());
                    }
                    
                    // Color según tipo de marcación
                    if ("ENTRADA".equals(tipo)) {
                        lblUserPhoto.setBackground(new Color(52, 152, 219)); // Azul
                    } else {
                        lblUserPhoto.setBackground(new Color(230, 126, 34)); // Naranja
                    }
                    
                    lblUserName.setText(nombres + " " + apellidos);
                    lblUserDNI.setText("DNI: " + dni);
                    lblUserDepartment.setText(departamento != null ? departamento : "Sin departamento");
                    lblConfidence.setText("Confianza: " + confidence);
                    
                    // Ajustar colores según nivel de confianza
                    // Sensor DY50: valores típicos 0-1000+
                    if (confidence > 500) {
                        lblConfidence.setForeground(new Color(46, 204, 113)); // Verde - Alta
                    } else if (confidence > 200) {
                        lblConfidence.setForeground(new Color(243, 156, 18)); // Amarillo - Media
                    } else {
                        lblConfidence.setForeground(new Color(231, 76, 60)); // Rojo - Baja
                    }
                    
                    userInfoPanel.setVisible(true);
                }
            }
            
        } catch (Exception e) {
            logger.error("❌ Error al cargar información del usuario", e);
        }
    }
    
    /**
     * Resetea la interfaz al estado inicial
     */
    private void resetUI() {
        logger.debug("🔄 Reseteando interfaz de usuario");
        waiting = false;
        btnMarkEntry.setEnabled(true);
        btnMarkExit.setEnabled(true);
        userInfoPanel.setVisible(false);
        
        lblStatusMessage.setText("Coloque su dedo en el sensor");
        lblInstructions.setText(
            "<html><center>Presione un botón de marcación y<br>coloque su dedo en el sensor</center></html>");
    }
    
    /**
     * Carga las asistencias del día actual
     */
    private void loadTodayAttendances() {
        logger.debug("📊 Cargando asistencias del día...");
        tableModel.setRowCount(0);
        
        String sql = "SELECT a.id_asistencia, " +
                     "       TO_CHAR(a.fecha_hora, 'HH24:MI:SS') AS hora, " +
                     "       u.nombres || ' ' || u.apellidos AS nombre_completo, " +
                     "       u.dni, " +
                     "       a.tipo_marcacion, " +
                     "       a.confidence_score, " +
                     "       CASE " +
                     "           WHEN EXTRACT(HOUR FROM a.fecha_hora) >= 9 AND a.tipo_marcacion = 'ENTRADA' " +
                     "           THEN 'TARDE' " +
                     "           ELSE 'A TIEMPO' " +
                     "       END AS estado " +
                     "FROM asistencias a " +
                     "INNER JOIN usuarios u ON a.id_usuario = u.id_usuario " +
                     "WHERE DATE(a.fecha_hora) = CURRENT_DATE " +
                     "ORDER BY a.fecha_hora DESC " +
                     "LIMIT 50";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            int row = 1;
            while (rs.next()) {
                Object[] rowData = {
                    row++,
                    rs.getString("hora"),
                    rs.getString("nombre_completo"),
                    rs.getString("dni"),
                    rs.getString("tipo_marcacion"),
                    rs.getInt("confidence_score"),
                    rs.getString("estado")
                };
                tableModel.addRow(rowData);
            }
            
            logger.info("✅ Cargadas {} asistencias de hoy", tableModel.getRowCount());
            
        } catch (Exception e) {
            logger.error("❌ Error al cargar asistencias de hoy", e);
        }
    }
    
    /**
     * Inicia el reloj en tiempo real
     */
    private void startClock() {
        logger.debug("⏰ Iniciando reloj en tiempo real");
        clockTimer = new Timer();
        clockTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    LocalDateTime now = LocalDateTime.now();
                    lblCurrentTime.setText(now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                });
            }
        }, 0, 1000);
    }
    
    /**
     * Limpieza de recursos al cerrar el panel
     */
    public void cleanup() {
        logger.info("🧹 Limpiando recursos del AttendancePanel...");
        
        if (clockTimer != null) {
            clockTimer.cancel();
            logger.debug("✅ Timer del reloj cancelado");
        }
        
        if (arduinoService != null && arduinoService.isConnected()) {
            arduinoService.disconnect();
            logger.debug("✅ Arduino desconectado");
        }
        
        logger.info("✅ Limpieza de recursos completada");
    }
}