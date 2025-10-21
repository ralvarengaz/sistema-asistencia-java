package com.attendance.view;

import com.attendance.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Ventana principal del sistema con menú lateral
 * 
 * @author Sistema Biométrico
 * @version 1.0
 */
public class MainFrame extends JFrame {
    
    private static final Logger logger = LoggerFactory.getLogger(MainFrame.class);
    
    // Componentes
    private JPanel sidebarPanel;
    private JPanel contentPanel;
    private JPanel topBarPanel;
    private JLabel lblUsername;
    private JLabel lblDateTime;
    
    // Paneles de contenido
    private JPanel currentPanel;
    
    // Usuario actual
    private String currentUser;
    
    // Colores del tema
    private static final Color SIDEBAR_COLOR = new Color(44, 62, 80);
    private static final Color SIDEBAR_HOVER = new Color(52, 73, 94);
    private static final Color SIDEBAR_SELECTED = new Color(41, 128, 185);
    private static final Color TOPBAR_COLOR = new Color(236, 240, 241);
    private static final Color CONTENT_BG = new Color(250, 250, 250);
    
    public MainFrame(String username) {
        this.currentUser = username;
        initComponents();
        setupFrame();
        startClock();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        
        // Panel superior (TopBar)
        createTopBar();
        
        // Panel lateral (Sidebar)
        createSidebar();
        
        // Panel de contenido
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(CONTENT_BG);
        
        // Mostrar Dashboard por defecto
        showDashboard();
        
        // Agregar componentes al frame
        add(topBarPanel, BorderLayout.NORTH);
        add(sidebarPanel, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);
    }
    
    private void createTopBar() {
        topBarPanel = new JPanel(new BorderLayout());
        topBarPanel.setBackground(TOPBAR_COLOR);
        topBarPanel.setPreferredSize(new Dimension(0, 60));
        topBarPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 200, 200)));
        
        // Panel izquierdo - Título
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15));
        leftPanel.setOpaque(false);
        
        JLabel lblTitle = new JLabel("Sistema de Asistencia Biométrico");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(new Color(44, 62, 80));
        leftPanel.add(lblTitle);
        
        // Panel derecho - Usuario y hora
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 10));
        rightPanel.setOpaque(false);
        
        // Fecha y hora
        lblDateTime = new JLabel();
        lblDateTime.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblDateTime.setForeground(new Color(100, 100, 100));
        
        // Usuario
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        userPanel.setOpaque(false);
        userPanel.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true));
        
        JLabel lblUserIcon = new JLabel("👤");
        lblUserIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        
        lblUsername = new JLabel(currentUser);
        lblUsername.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblUsername.setForeground(new Color(44, 62, 80));
        
        JButton btnLogout = new JButton("Salir");
        btnLogout.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setBackground(new Color(231, 76, 60));
        btnLogout.setBorderPainted(false);
        btnLogout.setFocusPainted(false);
        btnLogout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogout.addActionListener(e -> logout());
        
        userPanel.add(lblUserIcon);
        userPanel.add(lblUsername);
        
        rightPanel.add(lblDateTime);
        rightPanel.add(userPanel);
        rightPanel.add(btnLogout);
        
        topBarPanel.add(leftPanel, BorderLayout.WEST);
        topBarPanel.add(rightPanel, BorderLayout.EAST);
    }
    
    private void createSidebar() {
        sidebarPanel = new JPanel();
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setBackground(SIDEBAR_COLOR);
        sidebarPanel.setPreferredSize(new Dimension(250, 0));
        
        // Logo/Header
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(SIDEBAR_COLOR);
        headerPanel.setMaximumSize(new Dimension(250, 120));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel lblLogo = new JLabel("🔐", SwingConstants.CENTER);
        lblLogo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel lblAppName = new JLabel("Control Biométrico", SwingConstants.CENTER);
        lblAppName.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblAppName.setForeground(Color.WHITE);
        lblAppName.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        headerPanel.add(lblLogo);
        headerPanel.add(Box.createVerticalStrut(10));
        headerPanel.add(lblAppName);
        
        sidebarPanel.add(headerPanel);
        sidebarPanel.add(Box.createVerticalStrut(20));
        
        // Botones del menú
        addMenuButton("🏠  Dashboard", true, this::showDashboard);
        addMenuButton("✅  Marcar Asistencia", false, this::showAttendance);
        addMenuButton("👆  Enrolar Huella", false, this::showEnroll);
        addMenuButton("👥  Usuarios", false, this::showUsers);
        addMenuButton("📊  Reportes", false, this::showReports);
        addMenuButton("⚙️  Configuración", false, this::showSettings);
        
        // Espaciador para empujar contenido hacia arriba
        sidebarPanel.add(Box.createVerticalGlue());
        
        // Info en el footer
        JPanel footerPanel = new JPanel();
        footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.Y_AXIS));
        footerPanel.setBackground(SIDEBAR_COLOR);
        footerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        footerPanel.setMaximumSize(new Dimension(250, 60));
        
        JLabel lblVersion = new JLabel("Versión 1.0.0");
        lblVersion.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblVersion.setForeground(new Color(150, 150, 150));
        lblVersion.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel lblCopyright = new JLabel("© 2025 Sistema Biométrico");
        lblCopyright.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        lblCopyright.setForeground(new Color(120, 120, 120));
        lblCopyright.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        footerPanel.add(lblVersion);
        footerPanel.add(lblCopyright);
        
        sidebarPanel.add(footerPanel);
    }
    
    private void addMenuButton(String text, boolean selected, Runnable action) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(250, 50));
        button.setPreferredSize(new Dimension(250, 50));
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(selected ? SIDEBAR_SELECTED : SIDEBAR_COLOR);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 10));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Efectos hover
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (!button.getBackground().equals(SIDEBAR_SELECTED)) {
                    button.setBackground(SIDEBAR_HOVER);
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (!button.getBackground().equals(SIDEBAR_SELECTED)) {
                    button.setBackground(SIDEBAR_COLOR);
                }
            }
        });
        
        button.addActionListener(e -> {
            // Resetear todos los botones
            for (Component comp : sidebarPanel.getComponents()) {
                if (comp instanceof JButton) {
                    comp.setBackground(SIDEBAR_COLOR);
                }
            }
            // Marcar botón actual como seleccionado
            button.setBackground(SIDEBAR_SELECTED);
            // Ejecutar acción
            action.run();
        });
        
        sidebarPanel.add(button);
    }
    
    private void showDashboard() {
        logger.info("Mostrando Dashboard");
        switchPanel(new DashboardPanel());
    }
    
    private void showAttendance() {
        logger.info("Mostrando Panel de Asistencia");
        JPanel panel = createPlaceholderPanel("Marcar Asistencia", 
            "Panel de marcación de asistencia en desarrollo...");
        switchPanel(panel);
    }
    
    private void showEnroll() {
        logger.info("Mostrando Panel de Enrolamiento");
        switchPanel(new EnrollPanel());
    }
    
    private void showUsers() {
        logger.info("Mostrando Panel de Usuarios");
        JPanel panel = createPlaceholderPanel("Gestión de Usuarios", 
            "Panel de gestión de usuarios en desarrollo...");
        switchPanel(panel);
    }
    
    private void showReports() {
        logger.info("Mostrando Panel de Reportes");
        JPanel panel = createPlaceholderPanel("Reportes", 
            "Panel de reportes en desarrollo...");
        switchPanel(panel);
    }
    
    private void showSettings() {
        logger.info("Mostrando Panel de Configuración");
        JPanel panel = createPlaceholderPanel("Configuración", 
            "Panel de configuración en desarrollo...");
        switchPanel(panel);
    }
    
    private JPanel createPlaceholderPanel(String title, String message) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        JLabel label = new JLabel(
            "<html><div style='text-align: center;'>" +
            "<h1>" + title + "</h1>" +
            "<p style='color: gray;'>" + message + "</p>" +
            "</div></html>",
            SwingConstants.CENTER
        );
        label.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }
    
    private void switchPanel(JPanel newPanel) {
        contentPanel.removeAll();
        contentPanel.add(newPanel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
        currentPanel = newPanel;
    }
    
    private void startClock() {
        Timer timer = new Timer(1000, e -> updateDateTime());
        timer.start();
        updateDateTime();
    }
    
    private void updateDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy - HH:mm:ss");
        lblDateTime.setText(now.format(formatter));
    }
    
    private void logout() {
        int option = JOptionPane.showConfirmDialog(
            this,
            "¿Está seguro que desea cerrar sesión?",
            "Confirmar Salida",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (option == JOptionPane.YES_OPTION) {
            logger.info("Usuario {} cerró sesión", currentUser);
            this.dispose();
            
            // Volver a mostrar login
            SwingUtilities.invokeLater(() -> {
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
            });
        }
    }
    
    private void setupFrame() {
        setTitle("Sistema de Asistencia Biométrico");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 800);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(1200, 700));
    }
}