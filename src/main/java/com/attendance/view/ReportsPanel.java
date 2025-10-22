package com.attendance.view;

import com.attendance.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Panel de Reportes de Asistencias
 * Generacion de reportes con filtros y exportacion
 * 
 * @author Sistema Biometrico
 * @version 1.0
 */
public class ReportsPanel extends JPanel {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportsPanel.class);
    
    // Componentes de filtros
    private JComboBox<String> cmbReportType;
    private JTextField txtFechaInicio;
    private JTextField txtFechaFin;
    private JComboBox<String> cmbDepartamento;
    private JComboBox<String> cmbTipoMarcacion;
    private JTextField txtUsuario;
    
    // Tabla de resultados
    private JTable tableResults;
    private DefaultTableModel tableModel;
    
    // Botones
    private JButton btnGenerar;
    private JButton btnExportPDF;
    private JButton btnExportExcel;
    private JButton btnLimpiar;
    
    // Estadisticas
    private JLabel lblTotalRegistros;
    private JLabel lblTotalEntradas;
    private JLabel lblTotalSalidas;
    private JLabel lblUsuariosUnicos;
    
    public ReportsPanel() {
        initComponents();
        loadDepartments();
        setDefaultDates();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(20, 20));
        setBackground(new Color(250, 250, 250));
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        
        // Panel superior - Titulo
        JPanel headerPanel = createHeaderPanel();
        
        // Panel de filtros
        JPanel filterPanel = createFilterPanel();
        
        // Panel de resultados
        JPanel resultsPanel = createResultsPanel();
        
        // Panel de botones
        JPanel buttonPanel = createButtonPanel();
        
        // Layout principal
        JPanel topSection = new JPanel(new BorderLayout(10, 10));
        topSection.setOpaque(false);
        topSection.add(headerPanel, BorderLayout.NORTH);
        topSection.add(filterPanel, BorderLayout.CENTER);
        
        add(topSection, BorderLayout.NORTH);
        add(resultsPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        
        // Titulo
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        
        JLabel lblTitle = new JLabel("Reportes de Asistencias");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(new Color(44, 62, 80));
        lblTitle.setAlignmentX(LEFT_ALIGNMENT);
        
        JLabel lblSubtitle = new JLabel("Genere reportes personalizados y exportelos");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblSubtitle.setForeground(new Color(127, 140, 141));
        lblSubtitle.setAlignmentX(LEFT_ALIGNMENT);
        
        titlePanel.add(lblTitle);
        titlePanel.add(Box.createVerticalStrut(5));
        titlePanel.add(lblSubtitle);
        
        // Panel de estadisticas
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        statsPanel.setOpaque(false);
        
        JPanel stat1 = createStatCard("Total Registros", "0", new Color(52, 152, 219));
        JPanel stat2 = createStatCard("Entradas", "0", new Color(46, 204, 113));
        JPanel stat3 = createStatCard("Salidas", "0", new Color(230, 126, 34));
        JPanel stat4 = createStatCard("Usuarios", "0", new Color(155, 89, 182));
        
        lblTotalRegistros = findStatLabel(stat1);
        lblTotalEntradas = findStatLabel(stat2);
        lblTotalSalidas = findStatLabel(stat3);
        lblUsuariosUnicos = findStatLabel(stat4);
        
        statsPanel.add(stat1);
        statsPanel.add(stat2);
        statsPanel.add(stat3);
        statsPanel.add(stat4);
        
        panel.add(titlePanel, BorderLayout.WEST);
        panel.add(statsPanel, BorderLayout.EAST);
        
        return panel;
    }
    
    private JPanel createStatCard(String label, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        card.setPreferredSize(new Dimension(140, 70));
        
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
    
    private JLabel findStatLabel(JPanel card) {
        Component[] components = card.getComponents();
        for (Component comp : components) {
            if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                if (label.getFont().getSize() == 28) {
                    return label;
                }
            }
        }
        return new JLabel("0");
    }
    
    private JPanel createFilterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "Filtros de Busqueda",
                0, 0,
                new Font("Segoe UI", Font.BOLD, 14),
                new Color(44, 62, 80)
            ),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Fila 1: Tipo de reporte y rango de fechas
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Tipo de Reporte:"), gbc);
        
        gbc.gridx = 1;
        cmbReportType = new JComboBox<>(new String[]{
            "Asistencias por Fecha",
            "Asistencias por Usuario",
            "Asistencias por Departamento",
            "Tardanzas",
            "Ausencias",
            "Reporte Mensual"
        });
        cmbReportType.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cmbReportType.setPreferredSize(new Dimension(200, 30));
        panel.add(cmbReportType, gbc);
        
        gbc.gridx = 2;
        panel.add(new JLabel("Fecha Inicio:"), gbc);
        
        gbc.gridx = 3;
        txtFechaInicio = new JTextField(10);
        txtFechaInicio.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtFechaInicio.setToolTipText("Formato: YYYY-MM-DD");
        panel.add(txtFechaInicio, gbc);
        
        gbc.gridx = 4;
        panel.add(new JLabel("Fecha Fin:"), gbc);
        
        gbc.gridx = 5;
        txtFechaFin = new JTextField(10);
        txtFechaFin.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtFechaFin.setToolTipText("Formato: YYYY-MM-DD");
        panel.add(txtFechaFin, gbc);
        
        // Fila 2: Departamento, Tipo de Marcacion y Usuario
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Departamento:"), gbc);
        
        gbc.gridx = 1;
        cmbDepartamento = new JComboBox<>();
        cmbDepartamento.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cmbDepartamento.setPreferredSize(new Dimension(200, 30));
        panel.add(cmbDepartamento, gbc);
        
        gbc.gridx = 2;
        panel.add(new JLabel("Tipo Marcacion:"), gbc);
        
        gbc.gridx = 3;
        cmbTipoMarcacion = new JComboBox<>(new String[]{
            "Todos",
            "ENTRADA",
            "SALIDA"
        });
        cmbTipoMarcacion.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(cmbTipoMarcacion, gbc);
        
        gbc.gridx = 4;
        panel.add(new JLabel("Usuario (DNI/Nombre):"), gbc);
        
        gbc.gridx = 5;
        txtUsuario = new JTextField(15);
        txtUsuario.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtUsuario.setToolTipText("Buscar por DNI o nombre");
        panel.add(txtUsuario, gbc);
        
        return panel;
    }
    
    private JPanel createResultsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        // Titulo
        JLabel lblTitle = new JLabel("Resultados del Reporte");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(new Color(44, 62, 80));
        
        // Tabla
        String[] columns = {"#", "Fecha", "Hora", "Usuario", "DNI", "Departamento", 
                           "Tipo", "Confianza", "Metodo"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        tableResults = new JTable(tableModel);
        tableResults.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tableResults.setRowHeight(25);
        tableResults.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tableResults.getTableHeader().setBackground(new Color(52, 152, 219));
        tableResults.getTableHeader().setForeground(Color.WHITE);
        
        // Centrar contenido
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < tableResults.getColumnCount(); i++) {
            tableResults.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        
        // Ajustar anchos
        tableResults.getColumnModel().getColumn(0).setPreferredWidth(40);   // #
        tableResults.getColumnModel().getColumn(1).setPreferredWidth(100);  // Fecha
        tableResults.getColumnModel().getColumn(2).setPreferredWidth(80);   // Hora
        tableResults.getColumnModel().getColumn(3).setPreferredWidth(180);  // Usuario
        tableResults.getColumnModel().getColumn(4).setPreferredWidth(100);  // DNI
        tableResults.getColumnModel().getColumn(5).setPreferredWidth(120);  // Departamento
        tableResults.getColumnModel().getColumn(6).setPreferredWidth(80);   // Tipo
        tableResults.getColumnModel().getColumn(7).setPreferredWidth(80);   // Confianza
        tableResults.getColumnModel().getColumn(8).setPreferredWidth(80);   // Metodo
        
        JScrollPane scrollPane = new JScrollPane(tableResults);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        
        panel.add(lblTitle, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        
        // Panel izquierdo - Botones de accion
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);
        
        btnGenerar = createButton("Generar Reporte", new Color(52, 152, 219));
        btnGenerar.addActionListener(e -> generateReport());
        
        btnExportPDF = createButton("Exportar PDF", new Color(231, 76, 60));
        btnExportPDF.addActionListener(e -> exportToPDF());
        btnExportPDF.setEnabled(false);
        
        btnExportExcel = createButton("Exportar Excel", new Color(46, 204, 113));
        btnExportExcel.addActionListener(e -> exportToExcel());
        btnExportExcel.setEnabled(false);
        
        btnLimpiar = createButton("Limpiar", new Color(149, 165, 166));
        btnLimpiar.addActionListener(e -> clearReport());
        
        leftPanel.add(btnGenerar);
        leftPanel.add(btnExportPDF);
        leftPanel.add(btnExportExcel);
        leftPanel.add(btnLimpiar);
        
        panel.add(leftPanel, BorderLayout.WEST);
        
        return panel;
    }
    
    private JButton createButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(150, 40));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }
    
    private void loadDepartments() {
        cmbDepartamento.removeAllItems();
        cmbDepartamento.addItem("Todos");
        
        String sql = "SELECT nombre FROM departamentos WHERE activo = TRUE ORDER BY nombre";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                cmbDepartamento.addItem(rs.getString("nombre"));
            }
            
        } catch (Exception e) {
            logger.error("Error al cargar departamentos", e);
        }
    }
    
    private void setDefaultDates() {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        
        txtFechaInicio.setText(firstDayOfMonth.format(DateTimeFormatter.ISO_LOCAL_DATE));
        txtFechaFin.setText(today.format(DateTimeFormatter.ISO_LOCAL_DATE));
    }
    
    private void generateReport() {
        // Validar fechas
        String fechaInicio = txtFechaInicio.getText().trim();
        String fechaFin = txtFechaFin.getText().trim();
        
        if (fechaInicio.isEmpty() || fechaFin.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Por favor ingrese las fechas de inicio y fin",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Limpiar tabla
        tableModel.setRowCount(0);
        
        // Construir consulta SQL
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT a.id_asistencia, ");
        sql.append("       TO_CHAR(a.fecha_hora, 'YYYY-MM-DD') AS fecha, ");
        sql.append("       TO_CHAR(a.fecha_hora, 'HH24:MI:SS') AS hora, ");
        sql.append("       u.nombres || ' ' || u.apellidos AS usuario, ");
        sql.append("       u.dni, ");
        sql.append("       d.nombre AS departamento, ");
        sql.append("       a.tipo_marcacion, ");
        sql.append("       a.confidence_score, ");
        sql.append("       a.metodo ");
        sql.append("FROM asistencias a ");
        sql.append("INNER JOIN usuarios u ON a.id_usuario = u.id_usuario ");
        sql.append("LEFT JOIN departamentos d ON u.id_departamento = d.id_departamento ");
        sql.append("WHERE DATE(a.fecha_hora) BETWEEN ?::date AND ?::date ");
        
        // Agregar filtros
        String departamento = (String) cmbDepartamento.getSelectedItem();
        if (departamento != null && !"Todos".equals(departamento)) {
            sql.append("AND d.nombre = ? ");
        }
        
        String tipoMarcacion = (String) cmbTipoMarcacion.getSelectedItem();
        if (tipoMarcacion != null && !"Todos".equals(tipoMarcacion)) {
            sql.append("AND a.tipo_marcacion = ? ");
        }
        
        String usuario = txtUsuario.getText().trim();
        if (!usuario.isEmpty()) {
            sql.append("AND (u.dni LIKE ? OR LOWER(u.nombres || ' ' || u.apellidos) LIKE LOWER(?)) ");
        }
        
        sql.append("ORDER BY a.fecha_hora DESC");
        
        // Ejecutar consulta
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            
            int paramIndex = 1;
            pstmt.setString(paramIndex++, fechaInicio);
            pstmt.setString(paramIndex++, fechaFin);
            
            if (departamento != null && !"Todos".equals(departamento)) {
                pstmt.setString(paramIndex++, departamento);
            }
            
            if (tipoMarcacion != null && !"Todos".equals(tipoMarcacion)) {
                pstmt.setString(paramIndex++, tipoMarcacion);
            }
            
            if (!usuario.isEmpty()) {
                pstmt.setString(paramIndex++, "%" + usuario + "%");
                pstmt.setString(paramIndex++, "%" + usuario + "%");
            }
            
            try (ResultSet rs = pstmt.executeQuery()) {
                int rowNum = 1;
                int totalEntradas = 0;
                int totalSalidas = 0;
                java.util.Set<String> usuariosUnicos = new java.util.HashSet<>();
                
                while (rs.next()) {
                    Object[] row = {
                        rowNum++,
                        rs.getString("fecha"),
                        rs.getString("hora"),
                        rs.getString("usuario"),
                        rs.getString("dni"),
                        rs.getString("departamento"),
                        rs.getString("tipo_marcacion"),
                        rs.getInt("confidence_score"),
                        rs.getString("metodo")
                    };
                    tableModel.addRow(row);
                    
                    // Contadores
                    String tipo = rs.getString("tipo_marcacion");
                    if ("ENTRADA".equals(tipo)) totalEntradas++;
                    if ("SALIDA".equals(tipo)) totalSalidas++;
                    usuariosUnicos.add(rs.getString("dni"));
                }
                
                // Actualizar estadisticas
                lblTotalRegistros.setText(String.valueOf(tableModel.getRowCount()));
                lblTotalEntradas.setText(String.valueOf(totalEntradas));
                lblTotalSalidas.setText(String.valueOf(totalSalidas));
                lblUsuariosUnicos.setText(String.valueOf(usuariosUnicos.size()));
                
                // Habilitar botones de exportacion
                btnExportPDF.setEnabled(tableModel.getRowCount() > 0);
                btnExportExcel.setEnabled(tableModel.getRowCount() > 0);
                
                logger.info("Reporte generado: {} registros", tableModel.getRowCount());
                
                if (tableModel.getRowCount() == 0) {
                    JOptionPane.showMessageDialog(this,
                        "No se encontraron registros con los filtros especificados",
                        "Sin resultados",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error al generar reporte", e);
            JOptionPane.showMessageDialog(this,
                "Error al generar reporte: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void exportToPDF() {
        JOptionPane.showMessageDialog(this,
            "Funcionalidad de exportacion a PDF en desarrollo.\n" +
            "Datos: " + tableModel.getRowCount() + " registros listos para exportar.",
            "Exportar PDF",
            JOptionPane.INFORMATION_MESSAGE);
        
        // TODO: Implementar exportacion a PDF usando iText
        logger.info("Exportacion a PDF solicitada");
    }
    
    private void exportToExcel() {
        JOptionPane.showMessageDialog(this,
            "Funcionalidad de exportacion a Excel en desarrollo.\n" +
            "Datos: " + tableModel.getRowCount() + " registros listos para exportar.",
            "Exportar Excel",
            JOptionPane.INFORMATION_MESSAGE);
        
        // TODO: Implementar exportacion a Excel usando Apache POI
        logger.info("Exportacion a Excel solicitada");
    }
    
    private void clearReport() {
        tableModel.setRowCount(0);
        setDefaultDates();
        cmbReportType.setSelectedIndex(0);
        cmbDepartamento.setSelectedIndex(0);
        cmbTipoMarcacion.setSelectedIndex(0);
        txtUsuario.setText("");
        
        lblTotalRegistros.setText("0");
        lblTotalEntradas.setText("0");
        lblTotalSalidas.setText("0");
        lblUsuariosUnicos.setText("0");
        
        btnExportPDF.setEnabled(false);
        btnExportExcel.setEnabled(false);
        
        logger.info("Reporte limpiado");
    }
}