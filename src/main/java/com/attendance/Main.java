package com.attendance;

import com.attendance.config.DatabaseConfig;
import com.attendance.view.MainFrame;
import com.formdev.flatlaf.FlatLightLaf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Clase principal del sistema - Arranque directo al Dashboard
 * Sin sistema de autenticación
 * 
 * @author Sistema Biométrico
 * @version 2.0 - Optimizado
 */
public class Main {
    
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String APP_NAME = "Sistema de Asistencia Biométrico";
    private static final String VERSION = "2.0.0";
    
    public static void main(String[] args) {
        configureLookAndFeel();
        
        SwingUtilities.invokeLater(() -> {
            try {
                logger.info("===========================================");
                logger.info("  INICIANDO SISTEMA DE ASISTENCIA v{}", VERSION);
                logger.info("===========================================");
                
                // Mostrar splash screen
                JWindow splash = showSplashScreen();
                
                // Inicializar base de datos
                logger.info("Inicializando conexión a base de datos...");
                DatabaseConfig.initialize();
                
                // Verificar conexión
                if (!DatabaseConfig.testConnection()) {
                    splash.dispose();
                    showErrorDialog("Error de Conexión", 
                        "No se pudo conectar a la base de datos.\n\n" +
                        "Verifique:\n" +
                        "1. PostgreSQL está ejecutándose\n" +
                        "2. La base de datos 'attendance_system' existe\n" +
                        "3. Las credenciales en application.properties son correctas");
                    System.exit(1);
                }
                
                logger.info("Conexión a base de datos: OK");
                
                // Pequeña pausa para mostrar el splash
                Thread.sleep(1500);
                splash.dispose();
                
                // Abrir ventana principal directamente
                logger.info("Abriendo interfaz principal...");
                MainFrame mainFrame = new MainFrame();
                mainFrame.setVisible(true);
                
                logger.info("Sistema iniciado correctamente");
                logger.info("===========================================");
                
            } catch (Exception e) {
                logger.error("Error crítico al iniciar aplicación", e);
                showErrorDialog("Error Fatal", 
                    "Error al iniciar el sistema:\n" + e.getMessage());
                System.exit(1);
            }
        });
    }
    
    /**
     * Configura el Look and Feel moderno
     */
    private static void configureLookAndFeel() {
        try {
            FlatLightLaf.setup();
            
            // Personalizaciones UI
            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arc", 10);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("ScrollBar.width", 12);
            
            logger.debug("Look and Feel configurado: FlatLightLaf");
            
        } catch (Exception e) {
            logger.warn("No se pudo configurar FlatLightLaf, usando Look and Feel por defecto", e);
        }
    }
    
    /**
     * Muestra splash screen durante la carga
     */
    private static JWindow showSplashScreen() {
        JWindow splash = new JWindow();
        JPanel content = new JPanel(new BorderLayout(15, 15));
        content.setBackground(new Color(41, 128, 185));
        content.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        
        // Panel central
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        
        // Icono
        JLabel iconLabel = new JLabel("🔐", SwingConstants.CENTER);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 64));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Título
        JLabel titleLabel = new JLabel(APP_NAME, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Versión
        JLabel versionLabel = new JLabel("Versión " + VERSION, SwingConstants.CENTER);
        versionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        versionLabel.setForeground(new Color(236, 240, 241));
        versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Mensaje de carga
        JLabel loadingLabel = new JLabel("Iniciando sistema...", SwingConstants.CENTER);
        loadingLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        loadingLabel.setForeground(new Color(189, 195, 199));
        loadingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        centerPanel.add(iconLabel);
        centerPanel.add(Box.createVerticalStrut(15));
        centerPanel.add(titleLabel);
        centerPanel.add(Box.createVerticalStrut(8));
        centerPanel.add(versionLabel);
        centerPanel.add(Box.createVerticalStrut(25));
        centerPanel.add(loadingLabel);
        
        // Progress bar
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setForeground(Color.WHITE);
        progressBar.setBackground(new Color(52, 152, 219));
        progressBar.setBorderPainted(false);
        
        content.add(centerPanel, BorderLayout.CENTER);
        content.add(progressBar, BorderLayout.SOUTH);
        
        splash.setContentPane(content);
        splash.setSize(550, 350);
        splash.setLocationRelativeTo(null);
        splash.setVisible(true);
        
        return splash;
    }
    
    /**
     * Muestra diálogo de error
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
     * Hook de shutdown para cerrar recursos
     */
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Cerrando sistema...");
            DatabaseConfig.close();
            logger.info("Sistema cerrado correctamente");
        }));
    }
}
