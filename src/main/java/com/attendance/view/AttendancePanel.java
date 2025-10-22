package com.attendance.view;

import com.attendance.config.DatabaseConfig;
import com.attendance.model.Usuario;
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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Panel para marcar asistencia mediante huella dactilar
 * 
 * @author Sistema Biométrico
 * @version 1.0
 */
public class AttendancePanel extends JPanel {
    
    private static final Logger logger = LoggerFactory.getLogger(AttendancePanel.class);
    
    private ArduinoCommService arduinoService;
    
    // Componentes de conexión
    private JComboBox<String> cmbPorts;
    private JButton btnConnect;
    private JButton btnRefreshPorts;
    private JLabel lblConnectionStatus;
    
    // Componentes de marcación
    private JLabel lblCurrentTime;
    private JLabel lblInstructions;
    private JButton btnMarkEntry;
    private JButton btnMarkExit;
    private JLabel lblStatusIcon;
    private JLabel lblStatusMessage;
    
    // Componentes de información del usuario
    private JPanel userInfoPanel;
    private JLabel lblUserPhoto;
    private JLabel lblUserName;
    private JLabel lblUserDNI;
    private JLabel lblUserDepartment;
    private JLabel lblConfidence;
    
    // Tabla de asistencias del día
    private JTable tableAttendances;
    private DefaultTableModel tableModel;
    
    // Estado
    private boolean waiting = false;
    private Timer clockTimer;
    private Usuario currentUser;
    
    public AttendancePanel() {
        this.arduinoService = new ArduinoCommService();
        initComponents();
        refreshPorts();
        startClock();
        loadTodayAttendances();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(20, 20));
        setBackground(new Color(250, 250, 250));
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        
        // Panel superior - Título
        JPanel headerPanel = createHeaderPanel();
        
        // Panel central - División en tres secciones
        JPanel mainPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        mainPanel.setOpaque(false);
        
        // Panel izquierdo - Conexión y controles
        JPanel leftPanel = createLeftPanel();
        
        // Panel central - Área de marcación
        JPanel centerPanel = createCenterPanel();
        
        // Panel derecho - Información del usuario
        JPanel rightPanel = createRightPanel();
        
        mainPanel.add(leftPanel);
        mainPanel.add(centerPanel);
        mainPanel.add(rightPanel);
        
        // Panel inferior - Tabla de asistencias de hoy
        JPanel bottomPanel = createBottomPanel();
        
        add(headerPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        
        JLabel lblTitle = new JLabel("Marcar Asistencia");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(new Color(44, 62, 80));
        
        JLabel lblSubtitle = new JLabel("Sistema de registro biométrico de entrada y salida");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
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
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        // Título de sección
        JLabel lblSectionTitle = new JLabel("Conexión Arduino");
        lblSectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblSectionTitle.setForeground(new Color(44, 62, 80));
        lblSectionTitle.setAlignmentX(LEFT_ALIGNMENT);
        
        panel.add(lblSectionTitle);
        panel.add(Box.createVerticalStrut(15));
        
        // Puerto COM
        JLabel lblPort = new JLabel("Puerto COM:");
        lblPort.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblPort.setAlignmentX(LEFT_ALIGNMENT);
        
        JPanel portPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        portPanel.setOpaque(false);
        portPanel.setAlignmentX(LEFT_ALIGNMENT);
        portPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        
        cmbPorts = new JComboBox<>();
        cmbPorts.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbPorts.setPreferredSize(new Dimension(120, 30));
        
        btnRefreshPorts = new JButton("🔄");
        btnRefreshPorts.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnRefreshPorts.setPreferredSize(new Dimension(35, 30));
        btnRefreshPorts.setToolTipText("Actualizar puertos");
        btnRefreshPorts.addActionListener(e -> refreshPorts());
        
        portPanel.add(cmbPorts);
        portPanel.add(btnRefreshPorts);
        
        panel.add(lblPort);
        panel.add(Box.createVerticalStrut(5));
        panel.add(portPanel);
        panel.add(Box.createVerticalStrut(15));
        
        // Botón conectar
        btnConnect = new JButton("Conectar");
        btnConnect.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnConnect.setBackground(new Color(46, 204, 113));
        btnConnect.setForeground(Color.WHITE);
        btnConnect.setFocusPainted(false);
        btnConnect.setBorderPainted(false);
        btnConnect.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnConnect.setAlignmentX(LEFT_ALIGNMENT);
        btnConnect.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnConnect.addActionListener(e -> toggleConnection());
        
        panel.add(btnConnect);
        panel.add(Box.createVerticalStrut(15));
        
        // Estado de conexión
        lblConnectionStatus = new JLabel("⚫ Desconectado");
        lblConnectionStatus.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblConnectionStatus.setForeground(new Color(231, 76, 60));
        lblConnectionStatus.setAlignmentX(LEFT_ALIGNMENT);
        
        panel.add(lblConnectionStatus);
        panel.add(Box.createVerticalStrut(30));
        
        // Separador
        JSeparator separator = new JSeparator();
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        panel.add(separator);
        panel.add(Box.createVerticalStrut(20));
        
        // Tipo de marcación
        JLabel lblMarkType = new JLabel("Tipo de Marcación");
        lblMarkType.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblMarkType.setForeground(new Color(44, 62, 80));
        lblMarkType.setAlignmentX(LEFT_ALIGNMENT);
        
        panel.add(lblMarkType);
        panel.add(Box.createVerticalStrut(15));
        
        // Botón marcar entrada
        btnMarkEntry = new JButton("▶ Marcar ENTRADA");
        btnMarkEntry.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnMarkEntry.setBackground(new Color(52, 152, 219));
        btnMarkEntry.setForeground(Color.WHITE);
        btnMarkEntry.setFocusPainted(false);
        btnMarkEntry.setBorderPainted(false);
        btnMarkEntry.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        btnMarkEntry.setAlignmentX(LEFT_ALIGNMENT);
        btnMarkEntry.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnMarkEntry.setEnabled(false);
        btnMarkEntry.addActionListener(e -> startAttendanceMark("ENTRADA"));
        
        panel.add(btnMarkEntry);
        panel.add(Box.createVerticalStrut(10));
        
        // Botón marcar salida
        btnMarkExit = new JButton("◀ Marcar SALIDA");
        btnMarkExit.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnMarkExit.setBackground(new Color(230, 126, 34));
        btnMarkExit.setForeground(Color.WHITE);
        btnMarkExit.setFocusPainted(false);
        btnMarkExit.setBorderPainted(false);
        btnMarkExit.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        btnMarkExit.setAlignmentX(LEFT_ALIGNMENT);
        btnMarkExit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnMarkExit.setEnabled(false);
        btnMarkExit.addActionListener(e -> startAttendanceMark("SALIDA"));
        
        panel.add(btnMarkExit);
        panel.add(Box.createVerticalGlue());
        
        return panel;
    }
    
    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(new Color(41, 128, 185));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
            BorderFactory.createEmptyBorder(30, 30, 30, 30)
        ));
        
        // Panel superior con reloj
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);
        
        lblCurrentTime = new JLabel("--:--:--");
        lblCurrentTime.setFont(new Font("Segoe UI", Font.BOLD, 48));
        lblCurrentTime.setForeground(Color.WHITE);
        lblCurrentTime.setAlignmentX(CENTER_ALIGNMENT);
        
        JLabel lblDate = new JLabel(LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")));
        lblDate.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblDate.setForeground(new Color(236, 240, 241));
        lblDate.setAlignmentX(CENTER_ALIGNMENT);
        
        topPanel.add(lblCurrentTime);
        topPanel.add(Box.createVerticalStrut(10));
        topPanel.add(lblDate);
        
        // Panel central con icono y estado
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        
        lblStatusIcon = new JLabel("👆", SwingConstants.CENTER);
        lblStatusIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 120));
        lblStatusIcon.setAlignmentX(CENTER_ALIGNMENT);
        
        lblStatusMessage = new JLabel("Coloque su dedo en el sensor");
        lblStatusMessage.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblStatusMessage.setForeground(Color.WHITE);
        lblStatusMessage.setAlignmentX(CENTER_ALIGNMENT);
        lblStatusMessage.setHorizontalAlignment(SwingConstants.CENTER);
        
        lblInstructions = new JLabel(
            "<html><center>Presione un botón de marcación y<br>coloque su dedo en el sensor</center></html>");
        lblInstructions.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblInstructions.setForeground(new Color(189, 195, 199));
        lblInstructions.setAlignmentX(CENTER_ALIGNMENT);
        lblInstructions.setHorizontalAlignment(SwingConstants.CENTER);
        
        centerPanel.add(Box.createVerticalGlue());
        centerPanel.add(lblStatusIcon);
        centerPanel.add(Box.createVerticalStrut(20));
        centerPanel.add(lblStatusMessage);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(lblInstructions);
        centerPanel.add(Box.createVerticalGlue());
        
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        // Título
        JLabel lblTitle = new JLabel("Último Usuario");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(new Color(44, 62, 80));
        
        // Panel de información del usuario
        userInfoPanel = new JPanel();
        userInfoPanel.setLayout(new BoxLayout(userInfoPanel, BoxLayout.Y_AXIS));
        userInfoPanel.setOpaque(false);
        
        // Foto del usuario
        lblUserPhoto = new JLabel("👤", SwingConstants.CENTER);
        lblUserPhoto.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 80));
        lblUserPhoto.setAlignmentX(CENTER_ALIGNMENT);
        lblUserPhoto.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 2, true));
        lblUserPhoto.setOpaque(true);
        lblUserPhoto.setBackground(new Color(245, 245, 245));
        lblUserPhoto.setPreferredSize(new Dimension(120, 120));
        
        // Nombre
        lblUserName = new JLabel("Sin registro", SwingConstants.CENTER);
        lblUserName.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblUserName.setForeground(new Color(44, 62, 80));
        lblUserName.setAlignmentX(CENTER_ALIGNMENT);
        
        // DNI
        lblUserDNI = new JLabel("---", SwingConstants.CENTER);
        lblUserDNI.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblUserDNI.setForeground(new Color(127, 140, 141));
        lblUserDNI.setAlignmentX(CENTER_ALIGNMENT);
        
        // Departamento
        lblUserDepartment = new JLabel("---", SwingConstants.CENTER);
        lblUserDepartment.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblUserDepartment.setForeground(new Color(149, 165, 166));
        lblUserDepartment.setAlignmentX(CENTER_ALIGNMENT);
        
        // Confianza
        lblConfidence = new JLabel("Confianza: ---", SwingConstants.CENTER);
        lblConfidence.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblConfidence.setForeground(new Color(52, 152, 219));
        lblConfidence.setAlignmentX(CENTER_ALIGNMENT);
        
        userInfoPanel.add(Box.createVerticalStrut(20));
        userInfoPanel.add(lblUserPhoto);
        userInfoPanel.add(Box.createVerticalStrut(15));
        userInfoPanel.add(lblUserName);
        userInfoPanel.add(Box.createVerticalStrut(5));
        userInfoPanel.add(lblUserDNI);
        userInfoPanel.add(Box.createVerticalStrut(5));
        userInfoPanel.add(lblUserDepartment);
        userInfoPanel.add(Box.createVerticalStrut(10));
        userInfoPanel.add(lblConfidence);
        
        panel.add(lblTitle, BorderLayout.NORTH);
        panel.add(userInfoPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        panel.setPreferredSize(new Dimension(0, 250));
        
        // Título
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        
        JLabel lblTableTitle = new JLabel("Asistencias de Hoy");
        lblTableTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTableTitle.setForeground(new Color(44, 62, 80));
        
        JButton btnRefresh = new JButton("🔄 Actualizar");
        btnRefresh.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnRefresh.setFocusPainted(false);
        btnRefresh.addActionListener(e -> loadTodayAttendances());
        
        titlePanel.add(lblTableTitle, BorderLayout.WEST);
        titlePanel.add(btnRefresh, BorderLayout.EAST);
        
        // Tabla
        String[] columns = {"#", "Hora", "Usuario", "DNI", "Tipo", "Confianza", "Estado"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        tableAttendances = new JTable(tableModel);
        tableAttendances.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tableAttendances.setRowHeight(25);
        tableAttendances.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tableAttendances.getTableHeader().setBackground(new Color(52, 152, 219));
        tableAttendances.getTableHeader().setForeground(Color.WHITE);
        
        // Centrar contenido de celdas
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < tableAttendances.getColumnCount(); i++) {
            tableAttendances.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        
        // Ajustar anchos de columnas
        tableAttendances.getColumnModel().getColumn(0).setPreferredWidth(40);   // #
        tableAttendances.getColumnModel().getColumn(1).setPreferredWidth(80);   // Hora
        tableAttendances.getColumnModel().getColumn(2).setPreferredWidth(200);  // Usuario
        tableAttendances.getColumnModel().getColumn(3).setPreferredWidth(100);  // DNI
        tableAttendances.getColumnModel().getColumn(4).setPreferredWidth(80);   // Tipo
        tableAttendances.getColumnModel().getColumn(5).setPreferredWidth(80);   // Confianza
        tableAttendances.getColumnModel().getColumn(6).setPreferredWidth(80);   // Estado
        
        JScrollPane scrollPane = new JScrollPane(tableAttendances);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        
        panel.add(titlePanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void refreshPorts() {
        cmbPorts.removeAllItems();
        java.util.List<String> ports = SerialPortManager.getAvailablePorts();
        
        if (ports.isEmpty()) {
            cmbPorts.addItem("Sin puertos disponibles");
            btnConnect.setEnabled(false);
        } else {
            for (String port : ports) {
                cmbPorts.addItem(port);
            }
            btnConnect.setEnabled(true);
        }
    }
    
    private void toggleConnection() {
        if (arduinoService.isConnected()) {
            arduinoService.disconnect();
            updateConnectionStatus(false);
            logger.info("Desconectado de Arduino");
        } else {
            String selectedPort = (String) cmbPorts.getSelectedItem();
            if (selectedPort == null || selectedPort.equals("Sin puertos disponibles")) {
                JOptionPane.showMessageDialog(this,
                    "Por favor seleccione un puerto COM válido",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
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
                        
                        if (!connected) {
                            JOptionPane.showMessageDialog(AttendancePanel.this,
                                "No se pudo conectar con Arduino.\nVerifique la conexión.",
                                "Error de Conexión",
                                JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception e) {
                        logger.error("Error al conectar", e);
                        updateConnectionStatus(false);
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
            lblConnectionStatus.setText("🟢 Conectado");
            lblConnectionStatus.setForeground(new Color(46, 204, 113));
            btnConnect.setText("Desconectar");
            btnConnect.setBackground(new Color(231, 76, 60));
            btnMarkEntry.setEnabled(true);
            btnMarkExit.setEnabled(true);
            cmbPorts.setEnabled(false);
            btnRefreshPorts.setEnabled(false);
        } else {
            lblConnectionStatus.setText("⚫ Desconectado");
            lblConnectionStatus.setForeground(new Color(231, 76, 60));
            btnConnect.setText("Conectar");
            btnConnect.setBackground(new Color(46, 204, 113));
            btnMarkEntry.setEnabled(false);
            btnMarkExit.setEnabled(false);
            cmbPorts.setEnabled(true);
            btnRefreshPorts.setEnabled(true);
        }
    }
    
    private void startAttendanceMark(String tipo) {
        if (waiting) {
            JOptionPane.showMessageDialog(this,
                "Ya hay un proceso de marcación en curso",
                "Advertencia",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        waiting = true;
        btnMarkEntry.setEnabled(false);
        btnMarkExit.setEnabled(false);
        
        lblStatusIcon.setText("⏳");
        lblStatusMessage.setText("Coloque su dedo en el sensor");
        lblInstructions.setText("<html><center>Esperando lectura de huella...<br>Tiempo máximo: 15 segundos</center></html>");
        
        logger.info("Iniciando marcación de tipo: {}", tipo);
        
        arduinoService.startVerify(new ArduinoCommService.VerifyCallback() {
            @Override
            public void onWaiting(String message) {
                SwingUtilities.invokeLater(() -> {
                    lblInstructions.setText("<html><center>" + message + "</center></html>");
                });
            }
            
            @Override
            public void onSuccess(int fingerprintId, int confidence) {
                SwingUtilities.invokeLater(() -> {
                    lblStatusIcon.setText("✅");
                    lblStatusMessage.setText("¡Huella reconocida!");
                    
                    // Buscar usuario y registrar asistencia
                    processAttendance(fingerprintId, confidence, tipo);
                });
            }
            
            @Override
            public void onNotFound() {
                SwingUtilities.invokeLater(() -> {
                    lblStatusIcon.setText("❌");
                    lblStatusMessage.setText("Huella no registrada");
                    lblInstructions.setText("<html><center>No se encontró la huella en el sistema<br>Contacte al administrador</center></html>");
                    
                    JOptionPane.showMessageDialog(AttendancePanel.this,
                        "Huella no registrada en el sistema",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                    
                    resetUI();
                });
            }
            
            @Override
            public void onError(String error) {
                SwingUtilities.invokeLater(() -> {
                    lblStatusIcon.setText("⚠️");
                    lblStatusMessage.setText("Error en el sensor");
                    lblInstructions.setText("<html><center>" + error + "</center></html>");
                    
                    JOptionPane.showMessageDialog(AttendancePanel.this,
                        "Error: " + error,
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                    
                    resetUI();
                });
            }
        });
    }
    
    private void processAttendance(int fingerprintId, int confidence, String tipo) {
        String sql = "SELECT sp_registrar_asistencia(?, ?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, fingerprintId);
            pstmt.setInt(2, confidence);
            pstmt.setString(3, tipo);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    boolean success = rs.getBoolean("success");
                    String message = rs.getString("message");
                    Integer idUsuario = (Integer) rs.getObject("id_usuario");
                    String usuarioNombre = rs.getString("usuario_nombre");
                    
                    if (success && idUsuario != null) {
                        // Éxito - Mostrar información del usuario
                        displayUserInfo(idUsuario, usuarioNombre, confidence, tipo);
                        loadTodayAttendances();
                        
                        // Mensaje de éxito
                        lblStatusIcon.setText("✅");
                        lblStatusMessage.setText("¡" + tipo + " registrada!");
                        lblInstructions.setText(
                            "<html><center>" + usuarioNombre + "<br>" + message + "</center></html>");
                        
                        // Sonido de éxito (opcional)
                        Toolkit.getDefaultToolkit().beep();
                        
                        logger.info("{} registrada para usuario: {} (ID Huella: {})", 
                            tipo, usuarioNombre, fingerprintId);
                        
                    } else {
                        lblStatusIcon.setText("⚠️");
                        lblStatusMessage.setText("Advertencia");
                        lblInstructions.setText("<html><center>" + message + "</center></html>");
                        
                        JOptionPane.showMessageDialog(this,
                            message,
                            "Advertencia",
                            JOptionPane.WARNING_MESSAGE);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Error al procesar asistencia", e);
            lblStatusIcon.setText("❌");
            lblStatusMessage.setText("Error al registrar");
            lblInstructions.setText("<html><center>Error en la base de datos</center></html>");
            
            JOptionPane.showMessageDialog(this,
                "Error al registrar asistencia: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
        
        // Resetear después de 3 segundos
        javax.swing.Timer timer = new javax.swing.Timer(3000, e -> resetUI());
        timer.setRepeats(false);
        timer.start();
    }
    
    private void displayUserInfo(int idUsuario, String nombreCompleto, int confidence, String tipo) {
        // Consultar información completa del usuario
        String sql = "SELECT u.dni, u.nombres, u.apellidos, u.foto_url, d.nombre AS departamento " +
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
                    
                    // Actualizar interfaz
                    lblUserName.setText(nombres + " " + apellidos);
                    lblUserDNI.setText("DNI: " + dni);
                    lblUserDepartment.setText(departamento != null ? departamento : "Sin departamento");
                    lblConfidence.setText("Confianza: " + confidence + "/255");
                    
                    // Color según confianza
                    if (confidence > 150) {
                        lblConfidence.setForeground(new Color(46, 204, 113));
                    } else if (confidence > 100) {
                        lblConfidence.setForeground(new Color(243, 156, 18));
                    } else {
                        lblConfidence.setForeground(new Color(231, 76, 60));
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Error al cargar información del usuario", e);
        }
    }
    
    private void resetUI() {
        waiting = false;
        btnMarkEntry.setEnabled(true);
        btnMarkExit.setEnabled(true);
        
        lblStatusIcon.setText("👆");
        lblStatusMessage.setText("Coloque su dedo en el sensor");
        lblInstructions.setText(
            "<html><center>Presione un botón de marcación y<br>coloque su dedo en el sensor</center></html>");
    }
    
    private void loadTodayAttendances() {
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
            
            logger.info("Cargadas {} asistencias de hoy", tableModel.getRowCount());
            
        } catch (Exception e) {
            logger.error("Error al cargar asistencias de hoy", e);
            JOptionPane.showMessageDialog(this,
                "Error al cargar asistencias: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void startClock() {
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
     * Método para limpiar recursos al cerrar el panel
     */
    public void cleanup() {
        if (clockTimer != null) {
            clockTimer.cancel();
        }
        if (arduinoService != null && arduinoService.isConnected()) {
            arduinoService.disconnect();
        }
    }
}