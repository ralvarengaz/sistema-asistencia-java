package com.attendance;

import com.attendance.config.DatabaseConfig;
import com.attendance.view.LoginFrame;
import com.formdev.flatlaf.FlatLightLaf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class Main {
    
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String APP_NAME = "Sistema de Asistencia Biometrico";
    private static final String VERSION = "1.0.0";
    
    public static void main(String[] args) {
        configureLookAndFeel();
        
        SwingUtilities.invokeLater(() -> {
            try {
                logger.info("Iniciando aplicacion...");
                
                JWindow splash = showSplashScreen();
                Thread.sleep(2000);
                
                DatabaseConfig.initialize();
                
                if (!DatabaseConfig.testConnection()) {
                    splash.dispose();
                    showErrorDialog("Error de Conexion", 
                        "No se pudo conectar a la base de datos.");
                    System.exit(1);
                }
                
                splash.dispose();
                
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
                
            } catch (Exception e) {
                logger.error("Error al iniciar", e);
                System.exit(1);
            }
        });
    }
    
    private static void configureLookAndFeel() {
        try {
            FlatLightLaf.setup();
            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arc", 10);
        } catch (Exception e) {
            logger.warn("Error configurando Look and Feel", e);
        }
    }
    
    private static JWindow showSplashScreen() {
        JWindow splash = new JWindow();
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBackground(new Color(41, 128, 185));
        content.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        
        JLabel iconLabel = new JLabel("SISTEMA", SwingConstants.CENTER);
        iconLabel.setFont(new Font("Segoe UI", Font.BOLD, 48));
        iconLabel.setForeground(Color.WHITE);
        
        JLabel titleLabel = new JLabel(APP_NAME, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        centerPanel.add(iconLabel);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(titleLabel);
        
        content.add(centerPanel, BorderLayout.CENTER);
        content.add(progressBar, BorderLayout.SOUTH);
        
        splash.setContentPane(content);
        splash.setSize(500, 300);
        splash.setLocationRelativeTo(null);
        splash.setVisible(true);
        
        return splash;
    }
    
    private static void showErrorDialog(String title, String message) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }
    
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Cerrando aplicacion...");
            DatabaseConfig.close();
        }));
    }
}