package com.attendance;

import com.attendance.config.DatabaseConfig;
import com.attendance.view.LoginFrame;
import com.formdev.flatlaf.FlatLightLaf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Clase principal del sistema de asistencia biométrico
 * 
 * @author Sistema Biométrico
 * @version 1.0.0
 */
public class Main {
    
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String APP_NAME = "Sistema de Asistencia Biométrico";
    private static final String VERSION = "1.0.0";
    
    public static void main(String[] args) {
        
        // Configurar Look and Feel moderno
        configureLookAndFeel();
        
        // Iniciar aplicación en el Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                logger.info("========================================");
                logger.info("Iniciando {} v{}", APP_NAME, VERSION);
                logger.info("========================================");
                
                // Mostrar splash screen
                JWindow splash = showSplashScreen();
                
                // Simular carga (puedes quitarlo después)
                Thread.sleep(2000);
                
                // Inicializar configuración de base de datos
                logger.info("Inicializando conexión a base de datos...");
                DatabaseConfig.initialize();
                
                // Verificar conexión a base de datos
                if (!DatabaseConfig.testConnection()) {
                    splash.dispose();
                    showErrorDialog(
                        "Error de Conexión",
                        "No se pudo conectar a la base de datos.\n\n" +
                        "Verifique:\n" +
                        "1. PostgreSQL está ejecutándose\n" +
                        "2. La base de datos 'attendance_system' existe\n" +
                        "3. Las credenciales en application.properties son correctas\n\n" +
                        "Usuario: app_attendance\n" +
                        "Base de datos: attendance_system"
                    );
                    System.exit(1);
                }
                
                logger.info("Conexión a base de datos exitosa");
                logger.info(DatabaseConfig.getPoolStats());
                
                // Cerrar splash
                splash.dispose();
                
                // Mostrar ventana de login
                logger.info("Mostrando ventana de login");
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
                
                logger.info("Aplicación iniciada correctamente");
                
            } catch (Exception e) {
                logger.error("Error fatal al iniciar aplicación", e);
                showErrorDialog(
                    "Error Fatal",
                    "Error al iniciar la aplicación:\n" + e.getMessage() + 
                    "\n\nRevise los logs para más detalles."
                );
                System.exit(1);
            }
        });
    }
    
    /**
     * Configura el Look and Feel de la aplicación
     */
    private static void configureLookAndFeel() {
        try {
            // Usar FlatLaf Look and Feel moderno
            FlatLightLaf.setup();
            
            // Configuraciones adicionales de UI
            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arc", 10);
            UIManager.put("TextComponent.arc", 10);
            UIManager.put("ProgressBar.arc", 10);
            UIManager.put("ScrollBar.width", 12);
            
            // Colores personalizados
            UIManager.put("Button.background", new Color(41, 128, 185));
            UIManager.put("Button.foreground", Color.WHITE);
            
            logger.info("Look and Feel configurado: FlatLaf Light");
            
        } catch (Exception e) {
            logger.warn("No se pudo configurar FlatLaf, usando Look and Feel por defecto", e);
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                logger.error("Error configurando Look and Feel", ex);
            }
        }
    }
    
    /**
     * Muestra pantalla de splash durante la carga
     */
    private static JWindow showSplashScreen() {
        JWindow splash = new JWindow();
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBackground(new Color(41, 128, 185));
        content.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        
        // Icono/Logo (puedes agregar una imagen aquí)
        JLabel iconLabel = new JLabel("🔐", SwingConstants.CENTER);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 64));
        iconLabel.setForeground(Color.WHITE);
        
        // Título
        JLabel titleLabel = new JLabel(APP_NAME, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        
        // Versión
        JLabel versionLabel = new JLabel("Versión " + VERSION, SwingConstants.CENTER);
        versionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        versionLabel.setForeground(new Color(230, 240, 255));
        
        // Mensaje de carga
        JLabel loadingLabel = new JLabel("Iniciando sistema...", SwingConstants.CENTER);
        loadingLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        loadingLabel.setForeground(new Color(200, 220, 255));
        
        // Barra de progreso
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setPreferredSize(new Dimension(400, 8));
        
        // Panel central
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        centerPanel.add(iconLabel);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(titleLabel);
        centerPanel.add(Box.createVerticalStrut(5));
        centerPanel.add(versionLabel);
        centerPanel.add(Box.createVerticalStrut(20));
        centerPanel.add(loadingLabel);
        
        content.add(centerPanel, BorderLayout.CENTER);
        content.add(progressBar, BorderLayout.SOUTH);
        
        splash.setContentPane(content);
        splash.setSize(500, 300);
        splash.setLocationRelativeTo(null);
        splash.setVisible(true);
        
        return splash;
    }
    
    /**
     * Muestra un diálogo de error
     */
    private static void showErrorDialog(String title, String message) {
        JOptionPane.showMessageDialog(
            null,
            message,
            title,
            JOptionPane.ERROR_MESSAGE
        );
    }
    
    /**
     * Hook para cerrar recursos al salir
     */
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Cerrando aplicación...");
            DatabaseConfig.close();
            logger.info("Aplicación cerrada correctamente");
        }));
    }
}