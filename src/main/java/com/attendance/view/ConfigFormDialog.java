package com.attendance.view;

import com.attendance.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

/**
 * Diálogo para crear/editar parámetros de configuración
 * 
 * @author Sistema Biométrico
 * @version 1.0
 */
public class ConfigFormDialog extends JDialog {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigFormDialog.class);
    
    // Componentes
    private JTextField txtClave;
    private JTextField txtValor;
    private JTextArea txtDescripcion;
    private JComboBox<String> cmbTipoDato;
    private JTextField txtCategoria;
    private JCheckBox chkModificable;
    
    private JButton btnSave;
    private JButton btnCancel;
    
    // Estado
    private Integer configId;
    private boolean saved = false;
    private boolean isEditing = false;
    
    public ConfigFormDialog(Frame parent, String title, Integer configId) {
        super(parent, title, true);
        this.configId = configId;
        this.isEditing = (configId != null);
        
        initComponents();
        
        if (isEditing) {
            loadData();
        }
        
        pack();
        setLocationRelativeTo(parent);
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(15, 15));
        setSize(600, 550);
        setResizable(false);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(Color.WHITE);
        
        // Título
        JLabel lblTitle = new JLabel(isEditing ? "Editar Parámetro" : "Nuevo Parámetro");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(new Color(44, 62, 80));
        
        // Formulario
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        
        int row = 0;
        
        // Clave
        addFormField(formPanel, gbc, row++, "Clave:",
            txtClave = createTextField(30, !isEditing));
        txtClave.setToolTipText("Identificador único del parámetro");
        
        // Valor
        addFormField(formPanel, gbc, row++, "Valor:",
            txtValor = createTextField(30, true));
        txtValor.setToolTipText("Valor actual del parámetro");
        
        // Tipo de Dato
        cmbTipoDato = new JComboBox<>(new String[]{"STRING", "INTEGER", "BOOLEAN", "DECIMAL", "JSON"});
        cmbTipoDato.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        addFormField(formPanel, gbc, row++, "Tipo de Dato:", cmbTipoDato);
        
        // Categoría
        addFormField(formPanel, gbc, row++, "Categoría:",
            txtCategoria = createTextField(30, false));
        txtCategoria.setToolTipText("Categoría para agrupar parámetros");
        
        // Modificable
        chkModificable = new JCheckBox("Parámetro Modificable");
        chkModificable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chkModificable.setBackground(Color.WHITE);
        chkModificable.setSelected(true);
        chkModificable.setToolTipText("Permitir modificación desde la interfaz");
        
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        formPanel.add(chkModificable, gbc);
        gbc.gridwidth = 1;
        
        // Descripción
        JLabel lblDesc = new JLabel("Descripción:");
        lblDesc.setFont(new Font("Segoe UI", Font.BOLD, 13));
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        formPanel.add(lblDesc, gbc);
        
        txtDescripcion = new JTextArea(4, 40);
        txtDescripcion.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtDescripcion.setLineWrap(true);
        txtDescripcion.setWrapStyleWord(true);
        txtDescripcion.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        
        JScrollPane scrollDesc = new JScrollPane(txtDescripcion);
        gbc.gridy = row++;
        formPanel.add(scrollDesc, gbc);
        
        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(Color.WHITE);
        
        btnCancel = new JButton("Cancelar");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnCancel.setPreferredSize(new Dimension(120, 35));
        btnCancel.setBackground(new Color(149, 165, 166));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setFocusPainted(false);
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancel.addActionListener(e -> dispose());
        
        btnSave = new JButton("Guardar");
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSave.setPreferredSize(new Dimension(120, 35));
        btnSave.setBackground(new Color(46, 204, 113));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSave.addActionListener(e -> save());
        
        buttonPanel.add(btnCancel);
        buttonPanel.add(btnSave);
        
        // Agregar al panel principal
        mainPanel.add(lblTitle, BorderLayout.NORTH);
        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    private JTextField createTextField(int columns, boolean required) {
        JTextField field = new JTextField(columns);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, 32));
        
        if (required) {
            field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(231, 76, 60), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
            ));
        } else {
            field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
            ));
        }
        
        return field;
    }
    
    private void addFormField(JPanel panel, GridBagConstraints gbc, int row,
                              String labelText, JComponent component) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.3;
        panel.add(label, gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        panel.add(component, gbc);
    }
    
    private void loadData() {
        if (configId == null) return;
        
        String sql = "SELECT clave, valor, descripcion, tipo_dato, categoria, modificable " +
                     "FROM configuracion WHERE id_config = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, configId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    txtClave.setText(rs.getString("clave"));
                    txtValor.setText(rs.getString("valor"));
                    txtDescripcion.setText(rs.getString("descripcion"));
                    
                    String tipoDato = rs.getString("tipo_dato");
                    if (tipoDato != null) {
                        cmbTipoDato.setSelectedItem(tipoDato);
                    }
                    
                    txtCategoria.setText(rs.getString("categoria"));
                    chkModificable.setSelected(rs.getBoolean("modificable"));
                }
            }
            
        } catch (Exception e) {
            logger.error("Error al cargar datos", e);
            JOptionPane.showMessageDialog(this,
                "Error al cargar datos: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void save() {
        // Validaciones
        String clave = txtClave.getText().trim();
        if (clave.isEmpty()) {
            showError("La clave es obligatoria");
            txtClave.requestFocus();
            return;
        }
        
        String valor = txtValor.getText().trim();
        if (valor.isEmpty()) {
            showError("El valor es obligatorio");
            txtValor.requestFocus();
            return;
        }
        
        String tipoDato = (String) cmbTipoDato.getSelectedItem();
        String categoria = txtCategoria.getText().trim();
        String descripcion = txtDescripcion.getText().trim();
        boolean modificable = chkModificable.isSelected();
        
        try {
            if (isEditing) {
                updateConfig(clave, valor, descripcion, tipoDato, categoria, modificable);
            } else {
                insertConfig(clave, valor, descripcion, tipoDato, categoria, modificable);
            }
            
            saved = true;
            dispose();
            
        } catch (SQLException e) {
            logger.error("Error al guardar configuración", e);
            
            String message = "Error al guardar: " + e.getMessage();
            if (e.getMessage().contains("unique") || e.getMessage().contains("duplicate")) {
                message = "Ya existe un parámetro con esa clave";
            }
            
            showError(message);
        }
    }
    
    private void insertConfig(String clave, String valor, String descripcion,
                             String tipoDato, String categoria, boolean modificable) throws SQLException {
        
        String sql = "INSERT INTO configuracion (clave, valor, descripcion, tipo_dato, " +
                     "categoria, modificable) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, clave);
            pstmt.setString(2, valor);
            pstmt.setString(3, descripcion.isEmpty() ? null : descripcion);
            pstmt.setString(4, tipoDato);
            pstmt.setString(5, categoria.isEmpty() ? null : categoria);
            pstmt.setBoolean(6, modificable);
            
            int inserted = pstmt.executeUpdate();
            
            if (inserted > 0) {
                logger.info("Parámetro creado: {}", clave);
                JOptionPane.showMessageDialog(this,
                    "Parámetro creado correctamente",
                    "Éxito",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
    
    private void updateConfig(String clave, String valor, String descripcion,
                             String tipoDato, String categoria, boolean modificable) throws SQLException {
        
        String sql = "UPDATE configuracion SET valor = ?, descripcion = ?, " +
                     "tipo_dato = ?, categoria = ?, modificable = ?, " +
                     "fecha_modificacion = CURRENT_TIMESTAMP " +
                     "WHERE id_config = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, valor);
            pstmt.setString(2, descripcion.isEmpty() ? null : descripcion);
            pstmt.setString(3, tipoDato);
            pstmt.setString(4, categoria.isEmpty() ? null : categoria);
            pstmt.setBoolean(5, modificable);
            pstmt.setInt(6, configId);
            
            int updated = pstmt.executeUpdate();
            
            if (updated > 0) {
                logger.info("Parámetro actualizado: {}", clave);
                JOptionPane.showMessageDialog(this,
                    "Parámetro actualizado correctamente",
                    "Éxito",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this,
            message,
            "Error de Validación",
            JOptionPane.ERROR_MESSAGE);
    }
    
    public boolean isSaved() {
        return saved;
    }
}
