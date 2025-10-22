package com.attendance.view;

import com.attendance.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

/**
 * Panel de Configuración del Sistema
 * CRUD completo para gestionar parámetros de configuración
 * 
 * @author Sistema Biométrico
 * @version 1.0
 */
public class ConfigurationPanel extends JPanel {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationPanel.class);
    
    // Componentes de filtro
    private JComboBox<String> cmbCategory;
    private JTextField txtSearch;
    private JButton btnSearch;
    private JButton btnClearSearch;
    
    // Tabla de configuración
    private JTable tableConfig;
    private DefaultTableModel tableModel;
    
    // Botones de acción
    private JButton btnAdd;
    private JButton btnEdit;
    private JButton btnDelete;
    private JButton btnRefresh;
    private JButton btnRestoreDefaults;
    
    // Panel de información
    private JTextArea txtInfo;
    
    public ConfigurationPanel() {
        initComponents();
        loadCategories();
        loadConfigurations();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(20, 20));
        setBackground(new Color(250, 250, 250));
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        
        // Panel superior - Título
        JPanel headerPanel = createHeaderPanel();
        
        // Panel de búsqueda y filtros
        JPanel searchPanel = createSearchPanel();
        
        // Panel central dividido
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        centerPanel.setOpaque(false);
        
        // Panel izquierdo - Tabla
        JPanel tablePanel = createTablePanel();
        
        // Panel derecho - Información
        JPanel infoPanel = createInfoPanel();
        
        centerPanel.add(tablePanel);
        centerPanel.add(infoPanel);
        
        // Panel de botones
        JPanel actionPanel = createActionPanel();
        
        // Layout principal
        JPanel topSection = new JPanel(new BorderLayout(10, 10));
        topSection.setOpaque(false);
        topSection.add(headerPanel, BorderLayout.NORTH);
        topSection.add(searchPanel, BorderLayout.CENTER);
        
        add(topSection, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(actionPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        
        JLabel lblTitle = new JLabel("Configuración del Sistema");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(new Color(44, 62, 80));
        lblTitle.setAlignmentX(LEFT_ALIGNMENT);
        
        JLabel lblSubtitle = new JLabel("Administrar parámetros y ajustes del sistema");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblSubtitle.setForeground(new Color(127, 140, 141));
        lblSubtitle.setAlignmentX(LEFT_ALIGNMENT);
        
        titlePanel.add(lblTitle);
        titlePanel.add(Box.createVerticalStrut(5));
        titlePanel.add(lblSubtitle);
        
        panel.add(titlePanel, BorderLayout.WEST);
        
        return panel;
    }
    
    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        // Panel de búsqueda
        JPanel searchFieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchFieldPanel.setOpaque(false);
        
        JLabel lblSearch = new JLabel("Buscar:");
        lblSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        txtSearch = new JTextField(25);
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtSearch.setPreferredSize(new Dimension(250, 32));
        
        btnSearch = new JButton("Buscar");
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSearch.setBackground(new Color(52, 152, 219));
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setFocusPainted(false);
        btnSearch.setBorderPainted(false);
        btnSearch.setPreferredSize(new Dimension(80, 32));
        btnSearch.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSearch.addActionListener(e -> searchConfigurations());
        
        btnClearSearch = new JButton("Limpiar");
        btnClearSearch.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClearSearch.setBackground(new Color(149, 165, 166));
        btnClearSearch.setForeground(Color.WHITE);
        btnClearSearch.setFocusPainted(false);
        btnClearSearch.setBorderPainted(false);
        btnClearSearch.setPreferredSize(new Dimension(80, 32));
        btnClearSearch.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClearSearch.addActionListener(e -> clearSearch());
        
        searchFieldPanel.add(lblSearch);
        searchFieldPanel.add(txtSearch);
        searchFieldPanel.add(btnSearch);
        searchFieldPanel.add(btnClearSearch);
        
        // Panel de filtros
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterPanel.setOpaque(false);
        
        JLabel lblCategory = new JLabel("Categoría:");
        lblCategory.setFont(new Font("Segoe UI", Font.BOLD, 13));
        
        cmbCategory = new JComboBox<>();
        cmbCategory.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cmbCategory.setPreferredSize(new Dimension(200, 30));
        cmbCategory.addActionListener(e -> loadConfigurations());
        
        filterPanel.add(lblCategory);
        filterPanel.add(cmbCategory);
        
        panel.add(searchFieldPanel, BorderLayout.NORTH);
        panel.add(filterPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        // Título
        JLabel lblTitle = new JLabel("Parámetros de Configuración");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(new Color(44, 62, 80));
        
        // Tabla
        String[] columns = {"ID", "Clave", "Valor", "Categoría", "Modificable"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        tableConfig = new JTable(tableModel);
        tableConfig.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tableConfig.setRowHeight(28);
        tableConfig.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableConfig.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tableConfig.getTableHeader().setBackground(new Color(52, 152, 219));
        tableConfig.getTableHeader().setForeground(Color.WHITE);
        
        // Centrar columnas
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        tableConfig.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        tableConfig.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        
        // Ajustar anchos
        tableConfig.getColumnModel().getColumn(0).setPreferredWidth(50);
        tableConfig.getColumnModel().getColumn(1).setPreferredWidth(200);
        tableConfig.getColumnModel().getColumn(2).setPreferredWidth(150);
        tableConfig.getColumnModel().getColumn(3).setPreferredWidth(120);
        tableConfig.getColumnModel().getColumn(4).setPreferredWidth(80);
        
        // Listener para mostrar info al seleccionar
        tableConfig.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedConfigInfo();
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(tableConfig);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        
        panel.add(lblTitle, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        // Título
        JLabel lblTitle = new JLabel("Información del Parámetro");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(new Color(44, 62, 80));
        
        // Área de texto
        txtInfo = new JTextArea();
        txtInfo.setEditable(false);
        txtInfo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtInfo.setLineWrap(true);
        txtInfo.setWrapStyleWord(true);
        txtInfo.setBackground(new Color(250, 250, 250));
        txtInfo.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        txtInfo.setText("Seleccione un parámetro para ver su información detallada.");
        
        JScrollPane scrollPane = new JScrollPane(txtInfo);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        
        panel.add(lblTitle, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createActionPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        
        // Panel izquierdo - Botones CRUD
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);
        
        btnAdd = createButton("Nuevo Parámetro", new Color(46, 204, 113));
        btnAdd.addActionListener(e -> addConfiguration());
        
        btnEdit = createButton("Editar", new Color(52, 152, 219));
        btnEdit.addActionListener(e -> editConfiguration());
        btnEdit.setEnabled(false);
        
        btnDelete = createButton("Eliminar", new Color(231, 76, 60));
        btnDelete.addActionListener(e -> deleteConfiguration());
        btnDelete.setEnabled(false);
        
        btnRestoreDefaults = createButton("Restaurar Valores", new Color(243, 156, 18));
        btnRestoreDefaults.addActionListener(e -> restoreDefaults());
        
        leftPanel.add(btnAdd);
        leftPanel.add(btnEdit);
        leftPanel.add(btnDelete);
        leftPanel.add(btnRestoreDefaults);
        
        // Panel derecho - Botón refrescar
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);
        
        btnRefresh = createButton("Actualizar", new Color(52, 73, 94));
        btnRefresh.addActionListener(e -> loadConfigurations());
        
        rightPanel.add(btnRefresh);
        
        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.EAST);
        
        // Listener para habilitar/deshabilitar botones
        tableConfig.getSelectionModel().addListSelectionListener(e -> {
            boolean hasSelection = tableConfig.getSelectedRow() != -1;
            btnEdit.setEnabled(hasSelection);
            
            // Solo permitir eliminar si es modificable
            if (hasSelection) {
                int row = tableConfig.getSelectedRow();
                String modificable = (String) tableModel.getValueAt(row, 4);
                btnDelete.setEnabled("Sí".equals(modificable));
            } else {
                btnDelete.setEnabled(false);
            }
        });
        
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
    
    private void loadCategories() {
        cmbCategory.removeAllItems();
        cmbCategory.addItem("Todas");
        
        String sql = "SELECT DISTINCT categoria FROM configuracion ORDER BY categoria";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                String categoria = rs.getString("categoria");
                if (categoria != null && !categoria.isEmpty()) {
                    cmbCategory.addItem(categoria);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error al cargar categorías", e);
        }
    }
    
    private void loadConfigurations() {
        tableModel.setRowCount(0);
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id_config, clave, valor, categoria, modificable ");
        sql.append("FROM configuracion WHERE 1=1 ");
        
        String category = (String) cmbCategory.getSelectedItem();
        if (category != null && !"Todas".equals(category)) {
            sql.append("AND categoria = ? ");
        }
        
        sql.append("ORDER BY categoria, clave");
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            
            if (category != null && !"Todas".equals(category)) {
                pstmt.setString(1, category);
            }
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Object[] row = {
                        rs.getInt("id_config"),
                        rs.getString("clave"),
                        rs.getString("valor"),
                        rs.getString("categoria"),
                        rs.getBoolean("modificable") ? "Sí" : "No"
                    };
                    tableModel.addRow(row);
                }
            }
            
            logger.info("Cargados {} parámetros de configuración", tableModel.getRowCount());
            
        } catch (Exception e) {
            logger.error("Error al cargar configuraciones", e);
            JOptionPane.showMessageDialog(this,
                "Error al cargar configuraciones: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void searchConfigurations() {
        String search = txtSearch.getText().trim().toLowerCase();
        
        if (search.isEmpty()) {
            loadConfigurations();
            return;
        }
        
        tableModel.setRowCount(0);
        
        String sql = "SELECT id_config, clave, valor, categoria, modificable " +
                     "FROM configuracion " +
                     "WHERE LOWER(clave) LIKE ? OR LOWER(valor) LIKE ? OR LOWER(descripcion) LIKE ? " +
                     "ORDER BY categoria, clave";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String searchPattern = "%" + search + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Object[] row = {
                        rs.getInt("id_config"),
                        rs.getString("clave"),
                        rs.getString("valor"),
                        rs.getString("categoria"),
                        rs.getBoolean("modificable") ? "Sí" : "No"
                    };
                    tableModel.addRow(row);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error en búsqueda", e);
        }
    }
    
    private void clearSearch() {
        txtSearch.setText("");
        cmbCategory.setSelectedIndex(0);
        loadConfigurations();
    }
    
    private void showSelectedConfigInfo() {
        int selectedRow = tableConfig.getSelectedRow();
        
        if (selectedRow == -1) {
            txtInfo.setText("Seleccione un parámetro para ver su información detallada.");
            return;
        }
        
        int configId = (int) tableModel.getValueAt(selectedRow, 0);
        
        String sql = "SELECT clave, valor, descripcion, tipo_dato, categoria, " +
                     "modificable, fecha_modificacion, modificado_por " +
                     "FROM configuracion WHERE id_config = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, configId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    StringBuilder info = new StringBuilder();
                    info.append("══════════════════════════════════════\n");
                    info.append("  INFORMACIÓN DEL PARÁMETRO\n");
                    info.append("══════════════════════════════════════\n\n");
                    
                    info.append("Clave: ").append(rs.getString("clave")).append("\n\n");
                    info.append("Valor Actual: ").append(rs.getString("valor")).append("\n\n");
                    info.append("Descripción:\n");
                    info.append(rs.getString("descripcion") != null ? 
                        rs.getString("descripcion") : "Sin descripción").append("\n\n");
                    info.append("Tipo de Dato: ").append(rs.getString("tipo_dato")).append("\n");
                    info.append("Categoría: ").append(rs.getString("categoria")).append("\n");
                    info.append("Modificable: ").append(rs.getBoolean("modificable") ? "Sí" : "No").append("\n\n");
                    
                    Timestamp fechaMod = rs.getTimestamp("fecha_modificacion");
                    if (fechaMod != null) {
                        info.append("Última Modificación: ").append(fechaMod).append("\n");
                    }
                    
                    String modificadoPor = rs.getString("modificado_por");
                    if (modificadoPor != null) {
                        info.append("Modificado Por: ").append(modificadoPor).append("\n");
                    }
                    
                    txtInfo.setText(info.toString());
                    txtInfo.setCaretPosition(0);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error al cargar información", e);
        }
    }
    
    private void addConfiguration() {
        ConfigFormDialog dialog = new ConfigFormDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            "Nuevo Parámetro",
            null
        );
        dialog.setVisible(true);
        
        if (dialog.isSaved()) {
            loadConfigurations();
        }
    }
    
    private void editConfiguration() {
        int selectedRow = tableConfig.getSelectedRow();
        
        if (selectedRow == -1) {
            return;
        }
        
        int configId = (int) tableModel.getValueAt(selectedRow, 0);
        String modificable = (String) tableModel.getValueAt(selectedRow, 4);
        
        if ("No".equals(modificable)) {
            JOptionPane.showMessageDialog(this,
                "Este parámetro no es modificable por razones de seguridad del sistema",
                "Advertencia",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        ConfigFormDialog dialog = new ConfigFormDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            "Editar Parámetro",
            configId
        );
        dialog.setVisible(true);
        
        if (dialog.isSaved()) {
            loadConfigurations();
        }
    }
    
    private void deleteConfiguration() {
        int selectedRow = tableConfig.getSelectedRow();
        
        if (selectedRow == -1) {
            return;
        }
        
        int configId = (int) tableModel.getValueAt(selectedRow, 0);
        String clave = (String) tableModel.getValueAt(selectedRow, 1);
        
        int option = JOptionPane.showConfirmDialog(this,
            "¿Está seguro de eliminar este parámetro?\n\n" +
            "Clave: " + clave + "\n\n" +
            "Esta acción no se puede deshacer.",
            "Confirmar Eliminación",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (option == JOptionPane.YES_OPTION) {
            String sql = "DELETE FROM configuracion WHERE id_config = ?";
            
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setInt(1, configId);
                int deleted = pstmt.executeUpdate();
                
                if (deleted > 0) {
                    JOptionPane.showMessageDialog(this,
                        "Parámetro eliminado correctamente",
                        "Éxito",
                        JOptionPane.INFORMATION_MESSAGE);
                    
                    logger.info("Parámetro eliminado: {}", clave);
                    loadConfigurations();
                }
                
            } catch (SQLException e) {
                logger.error("Error al eliminar parámetro", e);
                JOptionPane.showMessageDialog(this,
                    "Error al eliminar parámetro: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void restoreDefaults() {
        int option = JOptionPane.showConfirmDialog(this,
            "¿Está seguro de restaurar los valores por defecto?\n\n" +
            "Esta acción sobrescribirá las configuraciones actuales.",
            "Confirmar Restauración",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (option == JOptionPane.YES_OPTION) {
            // Aquí iría la lógica para restaurar valores por defecto
            JOptionPane.showMessageDialog(this,
                "Funcionalidad de restauración en desarrollo.\n" +
                "Contacte al administrador del sistema.",
                "Información",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
