package com.attendance.view;

import com.attendance.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Panel de Dashboard - Pantalla principal con estadísticas
 * 
 * @author Sistema Biométrico
 * @version 1.0
 */
public class DashboardPanel extends JPanel {
    
    private static final Logger logger = LoggerFactory.getLogger(DashboardPanel.class);
    
    private JLabel lblTotalUsuarios;
    private JLabel lblAsistenciasHoy;
    private JLabel lblUsuariosConHuella;
    private JLabel lblUltimaAsistencia;
    
    public DashboardPanel() {
        initComponents();
        loadStatistics();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(20, 20));
        setBackground(new Color(250, 250, 250));
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        
        // Panel superior con título
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        JLabel lblTitle = new JLabel("Dashboard - Resumen del Sistema");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(new Color(44, 62, 80));
        
        JLabel lblSubtitle = new JLabel("Vista general de asistencias y usuarios");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblSubtitle.setForeground(new Color(127, 140, 141));
        
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        titlePanel.add(lblTitle);
        titlePanel.add(Box.createVerticalStrut(5));
        titlePanel.add(lblSubtitle);
        
        headerPanel.add(titlePanel, BorderLayout.WEST);
        
        // Panel de estadísticas (cards)
        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 20, 20));
        statsPanel.setOpaque(false);
        
        // Card 1: Total Usuarios
        JPanel card1 = createStatCard("👥", "Total Usuarios", "0", new Color(52, 152, 219));
        lblTotalUsuarios = findValueLabel(card1);
        
        // Card 2: Asistencias Hoy
        JPanel card2 = createStatCard("✅", "Asistencias Hoy", "0", new Color(46, 204, 113));
        lblAsistenciasHoy = findValueLabel(card2);
        
        // Card 3: Usuarios con Huella
        JPanel card3 = createStatCard("👆", "Huellas Registradas", "0", new Color(155, 89, 182));
        lblUsuariosConHuella = findValueLabel(card3);
        
        // Card 4: Última Asistencia
        JPanel card4 = createStatCard("🕐", "Última Asistencia", "Sin registros", new Color(230, 126, 34));
        lblUltimaAsistencia = findValueLabel(card4);
        lblUltimaAsistencia.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        statsPanel.add(card1);
        statsPanel.add(card2);
        statsPanel.add(card3);
        statsPanel.add(card4);
        
        // Panel de información adicional
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setOpaque(false);
        infoPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
            "Información del Sistema",
            0, 0,
            new Font("Segoe UI", Font.BOLD, 14),
            new Color(44, 62, 80)
        ));
        
        JTextArea txtInfo = new JTextArea();
        txtInfo.setEditable(false);
        txtInfo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtInfo.setBackground(Color.WHITE);
        txtInfo.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        txtInfo.setText(
            "📌 Bienvenido al Sistema de Control de Asistencia Biométrico\n\n" +
            "Este sistema permite:\n" +
            "  • Registrar asistencias mediante sensor de huella digital\n" +
            "  • Gestionar usuarios y sus huellas dactilares\n" +
            "  • Generar reportes de asistencias\n" +
            "  • Visualizar estadísticas en tiempo real\n\n" +
            "🔹 Estado de Conexión:\n" +
            "  Base de Datos: ✅ Conectado\n" +
            "  Arduino: ⚠️ No conectado (disponible en panel de Enrolamiento)\n\n" +
            "Para comenzar:\n" +
            "1. Registre huellas en el panel 'Enrolar Huella'\n" +
            "2. Los usuarios podrán marcar asistencia en 'Marcar Asistencia'\n" +
            "3. Consulte reportes en el panel 'Reportes'"
        );
        
        JScrollPane scrollPane = new JScrollPane(txtInfo);
        scrollPane.setBorder(null);
        infoPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Botón de refrescar
        JButton btnRefresh = new JButton("🔄 Actualizar Estadísticas");
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnRefresh.setBackground(new Color(52, 152, 219));
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.setFocusPainted(false);
        btnRefresh.setBorderPainted(false);
        btnRefresh.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefresh.setPreferredSize(new Dimension(200, 40));
        btnRefresh.addActionListener(e -> loadStatistics());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.add(btnRefresh);
        
        // Layout principal
        JPanel contentPanel = new JPanel(new BorderLayout(0, 20));
        contentPanel.setOpaque(false);
        contentPanel.add(statsPanel, BorderLayout.NORTH);
        contentPanel.add(infoPanel, BorderLayout.CENTER);
        
        add(headerPanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private JLabel findValueLabel(JPanel card) {
        // Buscar el JLabel con el valor en el panel de texto
        Component[] components = card.getComponents();
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                JPanel textPanel = (JPanel) comp;
                Component[] textComponents = textPanel.getComponents();
                for (Component textComp : textComponents) {
                    if (textComp instanceof JLabel) {
                        JLabel label = (JLabel) textComp;
                        // El label del valor tiene fuente grande (32)
                        if (label.getFont().getSize() == 32) {
                            return label;
                        }
                    }
                }
            }
        }
        return new JLabel("0"); // Fallback
    }
    
    private JPanel createStatCard(String icon, String title, String value, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(15, 15));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        // Icono
        JLabel lblIcon = new JLabel(icon);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        lblIcon.setOpaque(true);
        lblIcon.setBackground(accentColor);
        lblIcon.setForeground(Color.WHITE);
        lblIcon.setHorizontalAlignment(SwingConstants.CENTER);
        lblIcon.setPreferredSize(new Dimension(80, 80));
        lblIcon.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Panel de texto
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblTitle.setForeground(new Color(127, 140, 141));
        
        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 32));
        lblValue.setForeground(new Color(44, 62, 80));
        
        textPanel.add(lblTitle);
        textPanel.add(Box.createVerticalStrut(5));
        textPanel.add(lblValue);
        
        card.add(lblIcon, BorderLayout.WEST);
        card.add(textPanel, BorderLayout.CENTER);
        
        return card;
    }
    
    private void loadStatistics() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            int totalUsuarios = 0;
            int asistenciasHoy = 0;
            int usuariosConHuella = 0;
            String ultimaAsistencia = "Sin registros";
            
            @Override
            protected Void doInBackground() throws Exception {
                try (Connection conn = DatabaseConfig.getConnection()) {
                    
                    // Total de usuarios activos
                    String sql1 = "SELECT COUNT(*) FROM usuarios WHERE activo = TRUE";
                    try (PreparedStatement pstmt = conn.prepareStatement(sql1);
                         ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            totalUsuarios = rs.getInt(1);
                        }
                    }
                    
                    // Asistencias de hoy
                    String sql2 = "SELECT COUNT(*) FROM asistencias WHERE DATE(fecha_hora) = CURRENT_DATE";
                    try (PreparedStatement pstmt = conn.prepareStatement(sql2);
                         ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            asistenciasHoy = rs.getInt(1);
                        }
                    }
                    
                    // Usuarios con huella registrada
                    String sql3 = "SELECT COUNT(*) FROM usuarios WHERE fingerprint_id IS NOT NULL AND activo = TRUE";
                    try (PreparedStatement pstmt = conn.prepareStatement(sql3);
                         ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            usuariosConHuella = rs.getInt(1);
                        }
                    }
                    
                    // Última asistencia
                    String sql4 = "SELECT u.nombres || ' ' || u.apellidos, TO_CHAR(a.fecha_hora, 'HH24:MI') " +
                                  "FROM asistencias a " +
                                  "INNER JOIN usuarios u ON a.id_usuario = u.id_usuario " +
                                  "WHERE DATE(a.fecha_hora) = CURRENT_DATE " +
                                  "ORDER BY a.fecha_hora DESC LIMIT 1";
                    try (PreparedStatement pstmt = conn.prepareStatement(sql4);
                         ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            ultimaAsistencia = rs.getString(1) + " - " + rs.getString(2);
                        }
                    }
                    
                } catch (Exception e) {
                    logger.error("Error al cargar estadísticas", e);
                }
                return null;
            }
            
            @Override
            protected void done() {
                lblTotalUsuarios.setText(String.valueOf(totalUsuarios));
                lblAsistenciasHoy.setText(String.valueOf(asistenciasHoy));
                lblUsuariosConHuella.setText(String.valueOf(usuariosConHuella));
                lblUltimaAsistencia.setText(ultimaAsistencia);
                logger.info("Estadísticas actualizadas");
            }
        };
        
        worker.execute();
    }
}