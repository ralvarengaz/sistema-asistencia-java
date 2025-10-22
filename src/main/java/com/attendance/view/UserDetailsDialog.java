package com.attendance.view;

import com.attendance.config.DatabaseConfig;
import com.attendance.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.format.DateTimeFormatter;

/**
 * Dialogo para ver detalles completos del usuario
 * 
 * @author Sistema Biometrico
 * @version 1.0
 */
public class UserDetailsDialog extends JDialog {
    
    private static final Logger logger = LoggerFactory.getLogger(UserDetailsDialog.class);
    
    private Usuario usuario;
    
    public UserDetailsDialog(Frame parent, Usuario usuario) {
        super(parent, "Detalles del Usuario", true);
        this.usuario = usuario;
        
        initComponents();
        pack();
        setLocationRelativeTo(parent);
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(15, 15));
        setSize(650, 700);
        setResizable(false);
        
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(Color.WHITE);
        
        // Panel superior - Foto y nombre
        JPanel headerPanel = createHeaderPanel();
        
        // Panel central - Informacion detallada
        JPanel detailsPanel = createDetailsPanel();
        
        // Panel inferior - Estadisticas de asistencia
        JPanel statsPanel = createStatsPanel();
        
        // Boton cerrar
        JButton btnClose = new JButton("Cerrar");
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnClose.setPreferredSize(new Dimension(120, 35));
        btnClose.setBackground(new Color(52, 152, 219));
        btnClose.setForeground(Color.WHITE);
        btnClose.setFocusPainted(false);
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> dispose());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(btnClose);
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(detailsPanel, BorderLayout.CENTER);
        mainPanel.add(statsPanel, BorderLayout.SOUTH);
        
        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 10));
        panel.setBackground(new Color(52, 152, 219));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Foto del usuario
        JLabel lblPhoto = new JLabel("Usuario", SwingConstants.CENTER);
        lblPhoto.setFont(new Font("Segoe UI", Font.BOLD, 40));
        lblPhoto.setForeground(Color.WHITE);
        lblPhoto.setPreferredSize(new Dimension(100, 100));
        lblPhoto.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        lblPhoto.setOpaque(true);
        lblPhoto.setBackground(new Color(41, 128, 185));
        
        // Informacion basica
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        
        JLabel lblName = new JLabel(usuario.getNombreCompleto());
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblName.setForeground(Color.WHITE);
        lblName.setAlignmentX(LEFT_ALIGNMENT);
        
        JLabel lblDni = new JLabel("DNI: " + usuario.getDni());
        lblDni.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblDni.setForeground(new Color(236, 240, 241));
        lblDni.setAlignmentX(LEFT_ALIGNMENT);
        
        JLabel lblRole = new JLabel(usuario.getNombreRol() != null ? 
            "Rol: " + usuario.getNombreRol() : "Sin rol asignado");
        lblRole.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblRole.setForeground(new Color(189, 195, 199));
        lblRole.setAlignmentX(LEFT_ALIGNMENT);
        
        JLabel lblStatus = new JLabel(usuario.isActivo() ? "ACTIVO" : "INACTIVO");
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblStatus.setForeground(usuario.isActivo() ? 
            new Color(46, 204, 113) : new Color(231, 76, 60));
        lblStatus.setAlignmentX(LEFT_ALIGNMENT);
        
        infoPanel.add(lblName);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(lblDni);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(lblRole);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(lblStatus);
        
        panel.add(lblPhoto, BorderLayout.WEST);
        panel.add(infoPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createDetailsPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 15, 12));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "Informacion Personal",
                0, 0,
                new Font("Segoe UI", Font.BOLD, 14),
                new Color(44, 62, 80)
            ),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        // Email
        addDetailField(panel, "Email:", 
            usuario.getEmail() != null ? usuario.getEmail() : "No registrado");
        
        // Telefono
        addDetailField(panel, "Telefono:", 
            usuario.getTelefono() != null ? usuario.getTelefono() : "No registrado");
        
        // Departamento
        addDetailField(panel, "Departamento:", 
            usuario.getNombreDepartamento() != null ? usuario.getNombreDepartamento() : "Sin asignar");
        
        // Fecha de Nacimiento
        String fechaNac = "No registrada";
        if (usuario.getFechaNacimiento() != null) {
            fechaNac = usuario.getFechaNacimiento().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
        addDetailField(panel, "Fecha de Nacimiento:", fechaNac);
        
        // Genero
        addDetailField(panel, "Genero:", 
            usuario.getGenero() != null ? usuario.getGenero() : "No especificado");
        
        // ID Huella
        addDetailField(panel, "ID de Huella:", 
            usuario.getFingerprintId() != null ? 
            String.valueOf(usuario.getFingerprintId()) : "Sin registrar");
        
        // Direccion
        String direccion = usuario.getDireccion() != null ? usuario.getDireccion() : "No registrada";
        JLabel lblDireccionTitle = new JLabel("Direccion:");
        lblDireccionTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblDireccionTitle.setForeground(new Color(52, 73, 94));
        
        JTextArea txtDireccion = new JTextArea(direccion);
        txtDireccion.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtDireccion.setLineWrap(true);
        txtDireccion.setWrapStyleWord(true);
        txtDireccion.setEditable(false);
        txtDireccion.setBackground(Color.WHITE);
        txtDireccion.setRows(2);
        
        panel.add(lblDireccionTitle);
        panel.add(txtDireccion);
        
        // Fecha de Registro
        String fechaReg = "No disponible";
        if (usuario.getFechaRegistro() != null) {
            fechaReg = usuario.getFechaRegistro().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }
        addDetailField(panel, "Fecha de Registro:", fechaReg);
        
        // Observaciones
        if (usuario.getObservaciones() != null && !usuario.getObservaciones().isEmpty()) {
            JLabel lblObsTitle = new JLabel("Observaciones:");
            lblObsTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblObsTitle.setForeground(new Color(52, 73, 94));
            
            JTextArea txtObs = new JTextArea(usuario.getObservaciones());
            txtObs.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            txtObs.setLineWrap(true);
            txtObs.setWrapStyleWord(true);
            txtObs.setEditable(false);
            txtObs.setBackground(Color.WHITE);
            txtObs.setRows(3);
            
            JScrollPane scrollObs = new JScrollPane(txtObs);
            scrollObs.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
            
            panel.add(lblObsTitle);
            panel.add(scrollObs);
        }
        
        return panel;
    }
    
    private void addDetailField(JPanel panel, String label, String value) {
        JLabel lblTitle = new JLabel(label);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTitle.setForeground(new Color(52, 73, 94));
        
        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblValue.setForeground(new Color(44, 62, 80));
        
        panel.add(lblTitle);
        panel.add(lblValue);
    }
    
    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "Estadisticas de Asistencia",
                0, 0,
                new Font("Segoe UI", Font.BOLD, 14),
                new Color(44, 62, 80)
            ),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        // Cargar estadisticas
        int totalAsistencias = 0;
        int asistenciasMes = 0;
        String ultimaAsistencia = "Sin registros";
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            
            // Total de asistencias
            String sql1 = "SELECT COUNT(*) FROM asistencias WHERE id_usuario = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql1)) {
                pstmt.setInt(1, usuario.getIdUsuario());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        totalAsistencias = rs.getInt(1);
                    }
                }
            }
            
            // Asistencias del mes actual
            String sql2 = "SELECT COUNT(*) FROM asistencias " +
                         "WHERE id_usuario = ? " +
                         "AND EXTRACT(MONTH FROM fecha_hora) = EXTRACT(MONTH FROM CURRENT_DATE) " +
                         "AND EXTRACT(YEAR FROM fecha_hora) = EXTRACT(YEAR FROM CURRENT_DATE)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql2)) {
                pstmt.setInt(1, usuario.getIdUsuario());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        asistenciasMes = rs.getInt(1);
                    }
                }
            }
            
            // Ultima asistencia
            String sql3 = "SELECT TO_CHAR(fecha_hora, 'DD/MM/YYYY HH24:MI'), tipo_marcacion " +
                         "FROM asistencias WHERE id_usuario = ? " +
                         "ORDER BY fecha_hora DESC LIMIT 1";
            try (PreparedStatement pstmt = conn.prepareStatement(sql3)) {
                pstmt.setInt(1, usuario.getIdUsuario());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        ultimaAsistencia = rs.getString(1) + " - " + rs.getString(2);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Error al cargar estadisticas de asistencia", e);
        }
        
        // Panel con las estadisticas
        JPanel statsGrid = new JPanel(new GridLayout(1, 3, 15, 0));
        statsGrid.setOpaque(false);
        
        // Card 1: Total
        JPanel card1 = createStatCard("Total Asistencias", String.valueOf(totalAsistencias), 
            new Color(52, 152, 219));
        
        // Card 2: Este mes
        JPanel card2 = createStatCard("Este Mes", String.valueOf(asistenciasMes), 
            new Color(46, 204, 113));
        
        // Card 3: Ultima asistencia
        JPanel card3 = new JPanel(new BorderLayout(5, 5));
        card3.setBackground(new Color(250, 250, 250));
        card3.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true));
        card3.setPreferredSize(new Dimension(180, 70));
        
        JLabel lblLastTitle = new JLabel("Ultima Asistencia");
        lblLastTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblLastTitle.setForeground(new Color(127, 140, 141));
        lblLastTitle.setHorizontalAlignment(SwingConstants.CENTER);
        
        JLabel lblLastValue = new JLabel("<html><center>" + ultimaAsistencia + "</center></html>");
        lblLastValue.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblLastValue.setForeground(new Color(44, 62, 80));
        lblLastValue.setHorizontalAlignment(SwingConstants.CENTER);
        
        card3.add(lblLastTitle, BorderLayout.NORTH);
        card3.add(lblLastValue, BorderLayout.CENTER);
        
        statsGrid.add(card1);
        statsGrid.add(card2);
        statsGrid.add(card3);
        
        panel.add(statsGrid, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createStatCard(String label, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBackground(new Color(250, 250, 250));
        card.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true));
        card.setPreferredSize(new Dimension(180, 70));
        
        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblValue.setForeground(color);
        lblValue.setHorizontalAlignment(SwingConstants.CENTER);
        
        JLabel lblLabel = new JLabel(label);
        lblLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblLabel.setForeground(new Color(127, 140, 141));
        lblLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        card.add(lblValue, BorderLayout.CENTER);
        card.add(lblLabel, BorderLayout.SOUTH);
        
        return card;
    }
}