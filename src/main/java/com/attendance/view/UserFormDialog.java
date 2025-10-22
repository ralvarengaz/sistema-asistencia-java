package com.attendance.view;

import com.attendance.config.DatabaseConfig;
import com.attendance.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Diálogo de formulario para crear/editar usuarios
 * 
 * @author Sistema Biométrico
 * @version 1.0
 */
public class UserFormDialog extends JDialog {
    
    private static final Logger logger = LoggerFactory.getLogger(UserFormDialog.class);
    
    // Componentes del formulario
    private JTextField txtDni;
    private JTextField txtNombres;
    private JTextField txtApellidos;
    private JTextField txtEmail;
    private JTextField txtTelefono;
    private JTextField txtDireccion;
    private JTextField txtFechaNacimiento;
    private JTextField txtFingerprintId;
    private JComboBox<String> cmbGenero;
    private JComboBox<RolItem> cmbRol;
    private JComboBox<DepartmentItem> cmbDepartamento;
    private JCheckBox chkActivo;
    private JTextArea txtObservaciones;
    
    private JButton btnSave;
    private JButton btnCancel;
    
    // Estado
    private Usuario usuario;
    private boolean saved = false;
    private boolean isEditing = false;
    
    public UserFormDialog(Frame parent, String title, Usuario usuario) {
        super(parent, title, true);
        this.usuario = usuario;
        this.isEditing = (usuario != null);
        
        initComponents();
        loadComboData();
        
        if (isEditing) {
            fillFormData();
        }
        
        pack();
        setLocationRelativeTo(parent);
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(15, 15));
        setSize(700, 750);
        setResizable(false);
        
        // Panel principal con formulario
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(Color.WHITE);
        
        // Título
        JLabel lblTitle = new JLabel(isEditing ? "Editar Usuario" : "Nuevo Usuario");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(new Color(44, 62, 80));
        
        // Panel de formulario con GridBagLayout
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        
        int row = 0;
        
        // DNI
        addFormField(formPanel, gbc, row++, "DNI:", 
            txtDni = createTextField(20, !isEditing));
        
        // Nombres
        addFormField(formPanel, gbc, row++, "Nombres:", 
            txtNombres = createTextField(30, true));
        
        // Apellidos
        addFormField(formPanel, gbc, row++, "Apellidos:", 
            txtApellidos = createTextField(30, true));
        
        // Email
        addFormField(formPanel, gbc, row++, "Email:", 
            txtEmail = createTextField(30, false));
        
        // Teléfono
        addFormField(formPanel, gbc, row++, "Teléfono:", 
            txtTelefono = createTextField(20, false));
        
        // Fecha de Nacimiento
        addFormField(formPanel, gbc, row++, "Fecha Nacimiento (YYYY-MM-DD):", 
            txtFechaNacimiento = createTextField(20, false));
        txtFechaNacimiento.setToolTipText("Formato: YYYY-MM-DD (ejemplo: 1990-05-15)");
        
        // Género
        cmbGenero = new JComboBox<>(new String[]{"", "M", "F", "Otro"});
        cmbGenero.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        addFormField(formPanel, gbc, row++, "Género:", cmbGenero);
        
        // Rol
        cmbRol = new JComboBox<>();
        cmbRol.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        addFormField(formPanel, gbc, row++, "Rol:", cmbRol);
        
        // Departamento
        cmbDepartamento = new JComboBox<>();
        cmbDepartamento.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        addFormField(formPanel, gbc, row++, "Departamento:", cmbDepartamento);
        
        // Dirección
        addFormField(formPanel, gbc, row++, "Dirección:", 
            txtDireccion = createTextField(50, false));
        
        // ID Huella (solo lectura si está editando)
        txtFingerprintId = createTextField(10, false);
        if (isEditing) {
            txtFingerprintId.setEditable(false);
            txtFingerprintId.setBackground(new Color(240, 240, 240));
        }
        addFormField(formPanel, gbc, row++, "ID Huella:", txtFingerprintId);
        
        // Estado activo
        chkActivo = new JCheckBox("Usuario Activo");
        chkActivo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chkActivo.setBackground(Color.WHITE);
        chkActivo.setSelected(true);
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        formPanel.add(chkActivo, gbc);
        gbc.gridwidth = 1;
        
        // Observaciones
        JLabel lblObs = new JLabel("Observaciones:");
        lblObs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        formPanel.add(lblObs, gbc);
        
        txtObservaciones = new JTextArea(4, 40);
        txtObservaciones.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtObservaciones.setLineWrap(true);
        txtObservaciones.setWrapStyleWord(true);
        txtObservaciones.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        
        JScrollPane scrollObs = new JScrollPane(txtObservaciones);
        gbc.gridy = row++;
        formPanel.add(scrollObs, gbc);
        
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
        btnSave.addActionListener(e -> saveUser());
        
        buttonPanel.add(btnCancel);
        buttonPanel.add(btnSave);
        
        // Agregar todo al panel principal
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
    
    private void loadComboData() {
        // Cargar roles
        cmbRol.addItem(new RolItem(null, "-- Seleccione Rol --"));
        String sqlRoles = "SELECT id_rol, nombre FROM roles WHERE activo = TRUE ORDER BY nombre";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlRoles);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                cmbRol.addItem(new RolItem(rs.getInt("id_rol"), rs.getString("nombre")));
            }
            
        } catch (Exception e) {
            logger.error("Error al cargar roles", e);
        }
        
        // Cargar departamentos
        cmbDepartamento.addItem(new DepartmentItem(null, "-- Seleccione Departamento --"));
        String sqlDepts = "SELECT id_departamento, nombre FROM departamentos WHERE activo = TRUE ORDER BY nombre";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlDepts);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                cmbDepartamento.addItem(new DepartmentItem(
                    rs.getInt("id_departamento"), 
                    rs.getString("nombre")
                ));
            }
            
        } catch (Exception e) {
            logger.error("Error al cargar departamentos", e);
        }
    }
    
    private void fillFormData() {
        if (usuario == null) return;
        
        txtDni.setText(usuario.getDni());
        txtNombres.setText(usuario.getNombres());
        txtApellidos.setText(usuario.getApellidos());
        txtEmail.setText(usuario.getEmail());
        txtTelefono.setText(usuario.getTelefono());
        txtDireccion.setText(usuario.getDireccion());
        
        if (usuario.getFechaNacimiento() != null) {
            txtFechaNacimiento.setText(usuario.getFechaNacimiento().format(
                DateTimeFormatter.ISO_LOCAL_DATE));
        }
        
        if (usuario.getGenero() != null) {
            cmbGenero.setSelectedItem(usuario.getGenero());
        }
        
        // Seleccionar rol
        if (usuario.getIdRol() != null) {
            for (int i = 0; i < cmbRol.getItemCount(); i++) {
                RolItem item = cmbRol.getItemAt(i);
                if (item.getId() != null && item.getId().equals(usuario.getIdRol())) {
                    cmbRol.setSelectedIndex(i);
                    break;
                }
            }
        }
        
        // Seleccionar departamento
        if (usuario.getIdDepartamento() != null) {
            for (int i = 0; i < cmbDepartamento.getItemCount(); i++) {
                DepartmentItem item = cmbDepartamento.getItemAt(i);
                if (item.getId() != null && item.getId().equals(usuario.getIdDepartamento())) {
                    cmbDepartamento.setSelectedIndex(i);
                    break;
                }
            }
        }
        
        if (usuario.getFingerprintId() != null) {
            txtFingerprintId.setText(String.valueOf(usuario.getFingerprintId()));
        }
        
        chkActivo.setSelected(usuario.isActivo());
        txtObservaciones.setText(usuario.getObservaciones());
    }
    
    private void saveUser() {
        // Validaciones
        if (txtDni.getText().trim().isEmpty()) {
            showError("El DNI es obligatorio");
            txtDni.requestFocus();
            return;
        }
        
        if (txtNombres.getText().trim().isEmpty()) {
            showError("Los nombres son obligatorios");
            txtNombres.requestFocus();
            return;
        }
        
        if (txtApellidos.getText().trim().isEmpty()) {
            showError("Los apellidos son obligatorios");
            txtApellidos.requestFocus();
            return;
        }
        
        // Validar email si se proporciona
        String email = txtEmail.getText().trim();
        if (!email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showError("El formato del email no es válido");
            txtEmail.requestFocus();
            return;
        }
        
        // Validar fecha de nacimiento si se proporciona
        LocalDate fechaNacimiento = null;
        String fechaNacStr = txtFechaNacimiento.getText().trim();
        if (!fechaNacStr.isEmpty()) {
            try {
                fechaNacimiento = LocalDate.parse(fechaNacStr);
                
                if (fechaNacimiento.isAfter(LocalDate.now())) {
                    showError("La fecha de nacimiento no puede ser futura");
                    txtFechaNacimiento.requestFocus();
                    return;
                }
            } catch (Exception e) {
                showError("Formato de fecha inválido. Use: YYYY-MM-DD");
                txtFechaNacimiento.requestFocus();
                return;
            }
        }
        
        // Validar fingerprint ID si se proporciona
        Integer fingerprintId = null;
        String fingerprintStr = txtFingerprintId.getText().trim();
        if (!fingerprintStr.isEmpty()) {
            try {
                fingerprintId = Integer.parseInt(fingerprintStr);
                if (fingerprintId < 1 || fingerprintId > 255) {
                    showError("El ID de huella debe estar entre 1 y 255");
                    txtFingerprintId.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                showError("El ID de huella debe ser un número");
                txtFingerprintId.requestFocus();
                return;
            }
        }
        
        // Obtener valores del formulario
        String dni = txtDni.getText().trim();
        String nombres = txtNombres.getText().trim();
        String apellidos = txtApellidos.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String direccion = txtDireccion.getText().trim();
        String genero = (String) cmbGenero.getSelectedItem();
        if ("".equals(genero)) genero = null;
        
        RolItem rolItem = (RolItem) cmbRol.getSelectedItem();
        Integer idRol = rolItem != null ? rolItem.getId() : null;
        
        DepartmentItem deptItem = (DepartmentItem) cmbDepartamento.getSelectedItem();
        Integer idDepartamento = deptItem != null ? deptItem.getId() : null;
        
        boolean activo = chkActivo.isSelected();
        String observaciones = txtObservaciones.getText().trim();
        
        // Guardar en base de datos
        try {
            if (isEditing) {
                updateUser(usuario.getIdUsuario(), dni, nombres, apellidos, email, telefono,
                          idRol, idDepartamento, fingerprintId, direccion, fechaNacimiento,
                          genero, activo, observaciones);
            } else {
                insertUser(dni, nombres, apellidos, email, telefono, idRol, idDepartamento,
                          fingerprintId, direccion, fechaNacimiento, genero, activo, observaciones);
            }
            
            saved = true;
            dispose();
            
        } catch (SQLException e) {
            logger.error("Error al guardar usuario", e);
            
            String message = "Error al guardar usuario: " + e.getMessage();
            if (e.getMessage().contains("unique") || e.getMessage().contains("duplicate")) {
                if (e.getMessage().toLowerCase().contains("dni")) {
                    message = "Ya existe un usuario con ese DNI";
                } else if (e.getMessage().toLowerCase().contains("email")) {
                    message = "Ya existe un usuario con ese email";
                } else if (e.getMessage().toLowerCase().contains("fingerprint")) {
                    message = "Ya existe un usuario con ese ID de huella";
                }
            }
            
            showError(message);
        }
    }
    
    private void insertUser(String dni, String nombres, String apellidos, String email,
                           String telefono, Integer idRol, Integer idDepartamento,
                           Integer fingerprintId, String direccion, LocalDate fechaNacimiento,
                           String genero, boolean activo, String observaciones) throws SQLException {
        
        String sql = "INSERT INTO usuarios (dni, nombres, apellidos, email, telefono, " +
                     "id_rol, id_departamento, fingerprint_id, direccion, fecha_nacimiento, " +
                     "genero, activo, observaciones) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, dni);
            pstmt.setString(2, nombres);
            pstmt.setString(3, apellidos);
            pstmt.setString(4, email.isEmpty() ? null : email);
            pstmt.setString(5, telefono.isEmpty() ? null : telefono);
            pstmt.setObject(6, idRol);
            pstmt.setObject(7, idDepartamento);
            pstmt.setObject(8, fingerprintId);
            pstmt.setString(9, direccion.isEmpty() ? null : direccion);
            pstmt.setObject(10, fechaNacimiento != null ? Date.valueOf(fechaNacimiento) : null);
            pstmt.setString(11, genero);
            pstmt.setBoolean(12, activo);
            pstmt.setString(13, observaciones.isEmpty() ? null : observaciones);
            
            int inserted = pstmt.executeUpdate();
            
            if (inserted > 0) {
                logger.info("Usuario creado: {} {}", nombres, apellidos);
                JOptionPane.showMessageDialog(this,
                    "Usuario creado correctamente",
                    "Éxito",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
    
    private void updateUser(int idUsuario, String dni, String nombres, String apellidos,
                           String email, String telefono, Integer idRol, Integer idDepartamento,
                           Integer fingerprintId, String direccion, LocalDate fechaNacimiento,
                           String genero, boolean activo, String observaciones) throws SQLException {
        
        String sql = "UPDATE usuarios SET nombres = ?, apellidos = ?, email = ?, " +
                     "telefono = ?, id_rol = ?, id_departamento = ?, fingerprint_id = ?, " +
                     "direccion = ?, fecha_nacimiento = ?, genero = ?, activo = ?, " +
                     "observaciones = ?, fecha_modificacion = CURRENT_TIMESTAMP " +
                     "WHERE id_usuario = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, nombres);
            pstmt.setString(2, apellidos);
            pstmt.setString(3, email.isEmpty() ? null : email);
            pstmt.setString(4, telefono.isEmpty() ? null : telefono);
            pstmt.setObject(5, idRol);
            pstmt.setObject(6, idDepartamento);
            pstmt.setObject(7, fingerprintId);
            pstmt.setString(8, direccion.isEmpty() ? null : direccion);
            pstmt.setObject(9, fechaNacimiento != null ? Date.valueOf(fechaNacimiento) : null);
            pstmt.setString(10, genero);
            pstmt.setBoolean(11, activo);
            pstmt.setString(12, observaciones.isEmpty() ? null : observaciones);
            pstmt.setInt(13, idUsuario);
            
            int updated = pstmt.executeUpdate();
            
            if (updated > 0) {
                logger.info("Usuario actualizado: {} {}", nombres, apellidos);
                JOptionPane.showMessageDialog(this,
                    "Usuario actualizado correctamente",
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
    
    // Clases auxiliares para ComboBox
    private static class RolItem {
        private Integer id;
        private String nombre;
        
        public RolItem(Integer id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }
        
        public Integer getId() {
            return id;
        }
        
        @Override
        public String toString() {
            return nombre;
        }
    }
    
    private static class DepartmentItem {
        private Integer id;
        private String nombre;
        
        public DepartmentItem(Integer id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }
        
        public Integer getId() {
            return id;
        }
        
        @Override
        public String toString() {
            return nombre;
        }
    }
}