package com.attendance.view;

import com.attendance.config.DatabaseConfig;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Ventana de Login del sistema
 * 
 * @author Sistema Biométrico
 * @version 1.0
 */
public class LoginFrame extends JFrame {
    
    private static final Logger logger = LoggerFactory.getLogger(LoginFrame.class);
    
    // Componentes
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JButton btnCancel;
    private JLabel lblMessage;
    
    public LoginFrame() {
        initComponents();
        setupFrame();
    }
    
    private void initComponents() {
        // Panel principal
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        mainPanel.setBackground(Color.WHITE);
        
        // Panel superior - Logo y título
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(Color.WHITE);
        
        JLabel lblIcon = new JLabel("🔐", SwingConstants.CENTER);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 64));
        lblIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel lblTitle = new JLabel("Sistema de Asistencia", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(new Color(41, 128, 185));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel lblSubtitle = new JLabel("Control Biométrico", SwingConstants.CENTER);
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblSubtitle.setForeground(new Color(100, 100, 100));
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        headerPanel.add(lblIcon);
        headerPanel.add(Box.createVerticalStrut(10));
        headerPanel.add(lblTitle);
        headerPanel.add(Box.createVerticalStrut(5));
        headerPanel.add(lblSubtitle);
        
        // Panel central - Formulario
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Usuario
        JLabel lblUsername = new JLabel("Usuario:");
        lblUsername.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        formPanel.add(lblUsername, gbc);
        
        txtUsername = new JTextField(20);
        txtUsername.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtUsername.setPreferredSize(new Dimension(300, 35));
        gbc.gridy = 1;
        formPanel.add(txtUsername, gbc);
        
        // Contraseña
        JLabel lblPassword = new JLabel("Contraseña:");
        lblPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridy = 2;
        formPanel.add(lblPassword, gbc);
        
        txtPassword = new JPasswordField(20);
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtPassword.setPreferredSize(new Dimension(300, 35));
        gbc.gridy = 3;
        formPanel.add(txtPassword, gbc);
        
        // Mensaje de error/info
        lblMessage = new JLabel(" ");
        lblMessage.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblMessage.setForeground(Color.RED);
        lblMessage.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 4;
        gbc.insets = new Insets(10, 5, 5, 5);
        formPanel.add(lblMessage, gbc);
        
        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setBackground(Color.WHITE);
        
        btnLogin = new JButton("Iniciar Sesión");
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLogin.setPreferredSize(new Dimension(140, 40));
        btnLogin.setBackground(new Color(41, 128, 185));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btnCancel = new JButton("Cancelar");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnCancel.setPreferredSize(new Dimension(140, 40));
        btnCancel.setBackground(new Color(200, 200, 200));
        btnCancel.setForeground(Color.BLACK);
        btnCancel.setFocusPainted(false);
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        buttonPanel.add(btnLogin);
        buttonPanel.add(btnCancel);
        
        gbc.gridy = 5;
        gbc.insets = new Insets(15, 5, 5, 5);
        formPanel.add(buttonPanel, gbc);
        
        // Agregar todo al panel principal
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(formPanel, BorderLayout.CENTER);
        
        add(mainPanel);
        
        // Event Listeners
        btnLogin.addActionListener(e -> login());
        btnCancel.addActionListener(e -> System.exit(0));
        
        // Enter para login
        txtPassword.addActionListener(e -> login());
        
        // Focus inicial
        SwingUtilities.invokeLater(() -> txtUsername.requestFocus());
    }
    
    private void setupFrame() {
        setTitle("Login - Sistema de Asistencia Biométrico");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 480);
        setLocationRelativeTo(null);
        setResizable(false);
    }
    
    private void login() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());
        
        // Validaciones básicas
        if (username.isEmpty()) {
            showMessage("Por favor ingrese su usuario", Color.RED);
            txtUsername.requestFocus();
            return;
        }
        
        if (password.isEmpty()) {
            showMessage("Por favor ingrese su contraseña", Color.RED);
            txtPassword.requestFocus();
            return;
        }
        
        // Deshabilitar botón mientras se procesa
        btnLogin.setEnabled(false);
        btnLogin.setText("Verificando...");
        lblMessage.setText("Autenticando...");
        lblMessage.setForeground(new Color(41, 128, 185));
        
        // Ejecutar login en hilo separado para no bloquear UI
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            String errorMessage = "";
            String nombreUsuario = "";
            
            @Override
            protected Boolean doInBackground() throws Exception {
                return authenticateUser(username, password);
            }
            
            @Override
            protected void done() {
                try {
                    boolean success = get();
                    
                    if (success) {
                        showMessage("✓ Acceso concedido", new Color(39, 174, 96));
                        logger.info("Login exitoso para usuario: {}", username);
                        
                        // Pequeña pausa para mostrar mensaje de éxito
                        Timer timer = new Timer(500, e -> {
                            openMainWindow(username);
                        });
                        timer.setRepeats(false);
                        timer.start();
                        
                    } else {
                        showMessage("✗ Usuario o contraseña incorrectos", Color.RED);
                        logger.warn("Intento de login fallido para usuario: {}", username);
                        txtPassword.setText("");
                        txtPassword.requestFocus();
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Iniciar Sesión");
                    }
                    
                } catch (Exception e) {
                    logger.error("Error durante el login", e);
                    showMessage("Error al conectar con el sistema", Color.RED);
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Iniciar Sesión");
                }
            }
        };
        
        worker.execute();
    }
    
    private boolean authenticateUser(String username, String password) {
        String sql = "SELECT us.password_hash, u.nombres, u.apellidos " +
                     "FROM usuarios_sistema us " +
                     "INNER JOIN usuarios u ON us.id_usuario = u.id_usuario " +
                     "WHERE us.username = ? AND us.activo = TRUE AND u.activo = TRUE";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String passwordHash = rs.getString("password_hash");
                    
                    // Primero intentar comparación directa (sin hash)
                    if (password.equals(passwordHash)) {
                        logger.info("Autenticación directa exitosa");
                        updateLastAccess(username);
                        return true;
                    }
                    
                    // Solo intentar BCrypt si el hash comienza con $2a$, $2y$ o $2b$
                    if (passwordHash.startsWith("$2a$") || passwordHash.startsWith("$2y$") || passwordHash.startsWith("$2b$")) {
                        try {
                            if (BCrypt.checkpw(password, passwordHash)) {
                                logger.info("Autenticación BCrypt exitosa");
                                updateLastAccess(username);
                                return true;
                            }
                        } catch (Exception e) {
                            logger.error("Error en BCrypt.checkpw", e);
                        }
                    }
                } else {
                    logger.warn("No se encontró usuario: {}", username);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error al autenticar usuario", e);
        }
        
        return false;
    }
    
    private void updateLastAccess(String username) {
        String sql = "UPDATE usuarios_sistema SET ultimo_acceso = CURRENT_TIMESTAMP " +
                     "WHERE username = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.executeUpdate();
            
        } catch (Exception e) {
            logger.error("Error al actualizar último acceso", e);
        }
    }
    
    private void openMainWindow(String username) {
        // Cerrar ventana de login
        this.dispose();
        
        // Abrir ventana principal
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame(username);
            mainFrame.setVisible(true);
            logger.info("Ventana principal abierta para usuario: {}", username);
        });
    }
    
    private void showMessage(String message, Color color) {
        lblMessage.setText(message);
        lblMessage.setForeground(color);
    }
}