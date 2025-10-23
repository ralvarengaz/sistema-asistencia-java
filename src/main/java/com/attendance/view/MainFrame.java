package com.attendance.view;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Ventana principal del sistema - SIN ICONOS EMOJI
 * 
 * @author Sistema Biométrico  
 * @version 2.1 - Corregido
 */
public class MainFrame extends JFrame {
    
    private static final Logger logger = LoggerFactory.getLogger(MainFrame.class);
    
    private JPanel sidebarPanel;
    private JPanel contentPanel;
    private JPanel topBarPanel;
    private JLabel lblDateTime;
    private JLabel lblStatus;
    private JPanel currentPanel;
    
    private static final Color SIDEBAR_COLOR = new Color(44, 62, 80);
    private static final Color SIDEBAR_HOVER = new Color(52, 73, 94);
    private static final Color SIDEBAR_SELECTED = new Color(41, 128, 185);
    private static final Color TOPBAR_COLOR = new Color(236, 240, 241);
    private static final Color CONTENT_BG = new Color(250, 250, 250);
    
    public MainFrame() {
        initComponents();
        setupFrame();
        startClock();
        showDashboard();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        createTopBar();
        createSidebar();
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(CONTENT_BG);
        add(topBarPanel, BorderLayout.NORTH);
        add(sidebarPanel, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);
    }
    
    private void createTopBar() {
        topBarPanel = new JPanel(new BorderLayout());
        topBarPanel.setBackground(TOPBAR_COLOR);
        topBarPanel.setPreferredSize(new Dimension(0, 65));
        topBarPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 200, 200)));
        
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 25, 18));
        leftPanel.setOpaque(false);
        JLabel lblTitle = new JLabel("Sistema de Asistencia Biometrico");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(new Color(44, 62, 80));
        leftPanel.add(lblTitle);
        
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 25, 12));
        rightPanel.setOpaque(false);
        
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        statusPanel.setOpaque(false);
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(46, 204, 113), 1, true),
            BorderFactory.createEmptyBorder(3, 10, 3, 10)));
        
        JLabel lblStatusIcon = new JLabel("\u25CF");
        lblStatusIcon.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblStatusIcon.setForeground(new Color(46, 204, 113));
        lblStatus = new JLabel("Sistema Activo");
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblStatus.setForeground(new Color(46, 204, 113));
        statusPanel.add(lblStatusIcon);
        statusPanel.add(lblStatus);
        
        lblDateTime = new JLabel();
        lblDateTime.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblDateTime.setForeground(new Color(100, 100, 100));
        
        JButton btnExit = new JButton("Salir");
        btnExit.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnExit.setForeground(Color.WHITE);
        btnExit.setBackground(new Color(231, 76, 60));
        btnExit.setBorderPainted(false);
        btnExit.setFocusPainted(false);
        btnExit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnExit.setPreferredSize(new Dimension(80, 32));
        btnExit.addActionListener(e -> exitApplication());
        
        rightPanel.add(statusPanel);
        rightPanel.add(lblDateTime);
        rightPanel.add(btnExit);
        
        topBarPanel.add(leftPanel, BorderLayout.WEST);
        topBarPanel.add(rightPanel, BorderLayout.EAST);
    }
    
    private void createSidebar() {
        sidebarPanel = new JPanel();
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setBackground(SIDEBAR_COLOR);
        sidebarPanel.setPreferredSize(new Dimension(260, 0));
        
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(SIDEBAR_COLOR);
        headerPanel.setMaximumSize(new Dimension(260, 130));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(25, 20, 25, 20));
        
        JLabel lblLogo = new JLabel("SISTEMA", SwingConstants.CENTER);
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblLogo.setForeground(Color.WHITE);
        lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel lblAppName = new JLabel("Control Biometrico", SwingConstants.CENTER);
        lblAppName.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblAppName.setForeground(Color.WHITE);
        lblAppName.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel lblVersion = new JLabel("v2.1.0", SwingConstants.CENTER);
        lblVersion.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblVersion.setForeground(new Color(189, 195, 199));
        lblVersion.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        headerPanel.add(lblLogo);
        headerPanel.add(Box.createVerticalStrut(10));
        headerPanel.add(lblAppName);
        headerPanel.add(Box.createVerticalStrut(3));
        headerPanel.add(lblVersion);
        
        sidebarPanel.add(headerPanel);
        sidebarPanel.add(Box.createVerticalStrut(15));
        
        JSeparator sep1 = new JSeparator();
        sep1.setMaximumSize(new Dimension(260, 1));
        sep1.setForeground(new Color(70, 90, 110));
        sidebarPanel.add(sep1);
        sidebarPanel.add(Box.createVerticalStrut(15));
        
        addMenuButton("Dashboard", true, this::showDashboard);
        addMenuButton("Marcar Asistencia", false, this::showAttendance);
        addMenuButton("Enrolar Huella", false, this::showEnroll);
        sidebarPanel.add(Box.createVerticalStrut(10));
        
        JSeparator sep2 = new JSeparator();
        sep2.setMaximumSize(new Dimension(260, 1));
        sep2.setForeground(new Color(70, 90, 110));
        sidebarPanel.add(sep2);
        sidebarPanel.add(Box.createVerticalStrut(10));
        
        addMenuButton("Usuarios", false, this::showUsers);
        addMenuButton("Reportes", false, this::showReports);
        addMenuButton("Configuracion", false, this::showSettings);
        
        sidebarPanel.add(Box.createVerticalGlue());
        
        JPanel footerPanel = new JPanel();
        footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.Y_AXIS));
        footerPanel.setBackground(SIDEBAR_COLOR);
        footerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));
        footerPanel.setMaximumSize(new Dimension(260, 80));
        
        JLabel lblFooter1 = new JLabel("Sistema sin autenticacion");
        lblFooter1.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblFooter1.setForeground(new Color(150, 150, 150));
        lblFooter1.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel lblFooter2 = new JLabel("Acceso directo habilitado");
        lblFooter2.setFont(new Font("Segoe UI", Font.ITALIC, 9));
        lblFooter2.setForeground(new Color(120, 120, 120));
        lblFooter2.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        footerPanel.add(lblFooter1);
        footerPanel.add(Box.createVerticalStrut(3));
        footerPanel.add(lblFooter2);
        
        sidebarPanel.add(footerPanel);
    }
    
    private void addMenuButton(String text, boolean selected, Runnable action) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(260, 50));
        button.setPreferredSize(new Dimension(260, 50));
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(selected ? SIDEBAR_SELECTED : SIDEBAR_COLOR);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(BorderFactory.createEmptyBorder(12, 25, 12, 12));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
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
            for (Component comp : sidebarPanel.getComponents()) {
                if (comp instanceof JButton) {
                    comp.setBackground(SIDEBAR_COLOR);
                }
            }
            button.setBackground(SIDEBAR_SELECTED);
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
        switchPanel(new AttendancePanel());
    }
    
    private void showEnroll() {
        logger.info("Mostrando Panel de Enrolamiento");
        switchPanel(new EnrollPanel());
    }
    
    private void showUsers() {
        logger.info("Mostrando Panel de Usuarios");
        try {
            switchPanel(new UsersPanel());
        } catch (Exception e) {
            logger.error("Error al cargar UsersPanel", e);
            showErrorPanel("Usuarios", "Error al cargar el modulo.");
        }
    }
    
    private void showReports() {
        logger.info("Mostrando Panel de Reportes");
        try {
            switchPanel(new ReportsPanel());
        } catch (Exception e) {
            logger.error("Error al cargar ReportsPanel", e);
            showErrorPanel("Reportes", "Error al cargar el modulo.");
        }
    }
    
    private void showSettings() {
        logger.info("Mostrando Panel de Configuracion");
        try {
            switchPanel(new ConfigurationPanel());
        } catch (Exception e) {
            logger.error("Error al cargar ConfigurationPanel", e);
            showErrorPanel("Configuracion", "Error al cargar el modulo.");
        }
    }
    
    private void showErrorPanel(String title, String message) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        JLabel label = new JLabel("<html><div style='text-align: center;'><h1>" + title + 
            "</h1><p style='color: gray;'>" + message + "</p></div></html>", SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        panel.add(label, BorderLayout.CENTER);
        switchPanel(panel);
    }
    
    private void switchPanel(JPanel newPanel) {
        if (currentPanel != null) {
            try {
                java.lang.reflect.Method cleanupMethod = currentPanel.getClass().getMethod("cleanup");
                cleanupMethod.invoke(currentPanel);
                logger.debug("Recursos liberados");
            } catch (NoSuchMethodException e) {
                // Sin cleanup
            } catch (Exception e) {
                logger.error("Error limpiando recursos", e);
            }
        }
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
    
    private void exitApplication() {
        int option = JOptionPane.showConfirmDialog(this,
            "Esta seguro que desea salir del sistema?",
            "Confirmar Salida",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (option == JOptionPane.YES_OPTION) {
            logger.info("Cerrando aplicacion");
            if (currentPanel != null) {
                try {
                    java.lang.reflect.Method cleanupMethod = currentPanel.getClass().getMethod("cleanup");
                    cleanupMethod.invoke(currentPanel);
                } catch (Exception e) {
                    // Ignorar
                }
            }
            dispose();
            System.exit(0);
        }
    }
    
    private void setupFrame() {
        setTitle("Sistema de Asistencia Biometrico - v2.1");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1450, 850);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(1200, 700));
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                exitApplication();
            }
        });
    }
}
