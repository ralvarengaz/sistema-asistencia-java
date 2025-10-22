package com.attendance.view;

import com.attendance.config.DatabaseConfig;
import com.attendance.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Panel de Gestión de Usuarios
 * CRUD completo para administrar empleados del sistema
 * 
 * @author Sistema Biométrico
 * @version 1.0
 */
public class UsersPanel extends JPanel {
    
    private static final Logger logger = LoggerFactory.getLogger(UsersPanel.class);
    
    // Componentes de búsqueda
    private JTextField txtSearch;
    private JButton btnSearch;
    private JButton btnClearSearch;
    private JComboBox<String> cmbFilterActive;
    private JComboBox<String> cmbFilterDepartment;
    
    // Tabla de usuarios
    private JTable tableUsers;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    
    // Botones de acción
    private JButton btnAdd;
    private JButton btnEdit;
    private JButton btnDelete;
    private JButton btnRefresh;
    private JButton btnViewDetails;
    
    // Panel de estadísticas
    private JLabel lblTotalUsers;
    private JLabel lblActiveUsers;
    private JLabel lblUsersWithFingerprint;
    
    public UsersPanel() {
        initComponents();
        loadDepartments();
        loadUsers();
        loadStatistics();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(20, 20));
        setBackground(new Color(250, 250, 250));
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        
        // Panel superior - Título y estadísticas
        JPanel headerPanel = createHeaderPanel();
        
        // Panel de búsqueda y filtros
        JPanel searchPanel = createSearchPanel();
        
        // Panel de tabla
        JPanel tablePanel = createTablePanel();
        
        // Panel de botones de acción
        JPanel actionPanel = createActionPanel();
        
        // Layout principal
        JPanel topSection = new JPanel(new BorderLayout(10, 10));
        topSection.setOpaque(false);
        topSection.add(headerPanel, BorderLayout.NORTH);
        topSection.add(searchPanel, BorderLayout.CENTER);
        
        add(topSection, BorderLayout.NORTH);
        add(tablePanel, BorderLayout.CENTER);
        add(actionPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        
        // Título
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        
        JLabel lblTitle = new JLabel("Gestion de Usuarios");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(new Color(44, 62, 80));
        lblTitle.setAlignmentX(LEFT_ALIGNMENT);
        
        JLabel lblSubtitle = new JLabel("Administrar empleados y su informacion");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblSubtitle.setForeground(new Color(127, 140, 141));
        lblSubtitle.setAlignmentX(LEFT_ALIGNMENT);
        
        titlePanel.add(lblTitle);
        titlePanel.add(Box.createVerticalStrut(5));
        titlePanel.add(lblSubtitle);
        
        // Panel de estadísticas
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        statsPanel.setOpaque(false);
        
        JPanel stat1 = createStatCard("Total Usuarios", "0", new Color(52, 152, 219));
        JPanel stat2 = createStatCard("Usuarios Activos", "0", new Color(46, 204, 113));
        JPanel stat3 = createStatCard("Con Huella", "0", new Color(155, 89, 182));
        
        lblTotalUsers = findStatLabel(stat1);
        lblActiveUsers = findStatLabel(stat2);
        lblUsersWithFingerprint = findStatLabel(stat3);
        
        statsPanel.add(stat1);
        statsPanel.add(stat2);
        statsPanel.add(stat3);
        
        panel.add(titlePanel, BorderLayout.WEST);
        panel.add(statsPanel, BorderLayout.EAST);
        
        return panel;
    }
    
    private JPanel createStatCard(String label, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout(10, 5));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        card.setPreferredSize(new Dimension(180, 80));
        
        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 32));
        lblValue.setForeground(color);
        lblValue.setHorizontalAlignment(SwingConstants.CENTER);
        
        JLabel lblLabel = new JLabel(label);
        lblLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
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
                if (label.getFont().getSize() == 32) {
                    return label;
                }
            }
        }
        return new JLabel("0");
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
        txtSearch.addActionListener(e -> searchUsers());
        
        btnSearch = new JButton("Buscar");
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSearch.setBackground(new Color(52, 152, 219));
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setFocusPainted(false);
        btnSearch.setBorderPainted(false);
        btnSearch.setPreferredSize(new Dimension(80, 32));
        btnSearch.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSearch.addActionListener(e -> searchUsers());
        
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
        
        JLabel lblFilters = new JLabel("Filtros:");
        lblFilters.setFont(new Font("Segoe UI", Font.BOLD, 13));
        
        JLabel lblActive = new JLabel("Estado:");
        lblActive.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        cmbFilterActive = new JComboBox<>(new String[]{"Todos", "Activos", "Inactivos"});
        cmbFilterActive.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cmbFilterActive.setPreferredSize(new Dimension(120, 30));
        cmbFilterActive.addActionListener(e -> loadUsers());
        
        JLabel lblDept = new JLabel("Departamento:");
        lblDept.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        cmbFilterDepartment = new JComboBox<>();
        cmbFilterDepartment.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cmbFilterDepartment.setPreferredSize(new Dimension(150, 30));
        cmbFilterDepartment.addActionListener(e -> loadUsers());
        
        filterPanel.add(lblFilters);
        filterPanel.add(lblActive);
        filterPanel.add(cmbFilterActive);
        filterPanel.add(Box.createHorizontalStrut(10));
        filterPanel.add(lblDept);
        filterPanel.add(cmbFilterDepartment);
        
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
        
        // Título de tabla
        JLabel lblTableTitle = new JLabel("Lista de Usuarios");
        lblTableTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTableTitle.setForeground(new Color(44, 62, 80));
        
        // Tabla
        String[] columns = {"ID", "DNI", "Apellidos", "Nombres", "Email", "Departamento", 
                           "Telefono", "ID Huella", "Estado"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        tableUsers = new JTable(tableModel);
        tableUsers.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tableUsers.setRowHeight(28);
        tableUsers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableUsers.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tableUsers.getTableHeader().setBackground(new Color(52, 152, 219));
        tableUsers.getTableHeader().setForeground(Color.WHITE);
        tableUsers.getTableHeader().setReorderingAllowed(false);
        
        // Sorter para ordenar columnas
        sorter = new TableRowSorter<>(tableModel);
        tableUsers.setRowSorter(sorter);
        
        // Centrar contenido de celdas
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        tableUsers.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);  // ID
        tableUsers.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);  // DNI
        tableUsers.getColumnModel().getColumn(7).setCellRenderer(centerRenderer);  // ID Huella
        tableUsers.getColumnModel().getColumn(8).setCellRenderer(centerRenderer);  // Estado
        
        // Renderer personalizado para el estado
        tableUsers.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                
                if (!isSelected) {
                    if ("Activo".equals(value)) {
                        c.setBackground(new Color(212, 239, 223));
                        setForeground(new Color(39, 174, 96));
                    } else {
                        c.setBackground(new Color(248, 215, 218));
                        setForeground(new Color(220, 53, 69));
                    }
                }
                return c;
            }
        });
        
        // Ajustar anchos de columnas
        tableUsers.getColumnModel().getColumn(0).setPreferredWidth(50);   // ID
        tableUsers.getColumnModel().getColumn(1).setPreferredWidth(100);  // DNI
        tableUsers.getColumnModel().getColumn(2).setPreferredWidth(150);  // Apellidos
        tableUsers.getColumnModel().getColumn(3).setPreferredWidth(150);  // Nombres
        tableUsers.getColumnModel().getColumn(4).setPreferredWidth(200);  // Email
        tableUsers.getColumnModel().getColumn(5).setPreferredWidth(130);  // Departamento
        tableUsers.getColumnModel().getColumn(6).setPreferredWidth(100);  // Teléfono
        tableUsers.getColumnModel().getColumn(7).setPreferredWidth(80);   // ID Huella
        tableUsers.getColumnModel().getColumn(8).setPreferredWidth(80);   // Estado
        
        // Doble clic para editar
        tableUsers.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    editUser();
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(tableUsers);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        
        panel.add(lblTableTitle, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createActionPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        
        // Panel izquierdo - Botones de acción
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);
        
        btnAdd = createButton("Nuevo Usuario", new Color(46, 204, 113));
        btnAdd.addActionListener(e -> addUser());
        
        btnEdit = createButton("Editar", new Color(52, 152, 219));
        btnEdit.addActionListener(e -> editUser());
        btnEdit.setEnabled(false);
        
        btnDelete = createButton("Eliminar", new Color(231, 76, 60));
        btnDelete.addActionListener(e -> deleteUser());
        btnDelete.setEnabled(false);
        
        btnViewDetails = createButton("Ver Detalles", new Color(149, 165, 166));
        btnViewDetails.addActionListener(e -> viewUserDetails());
        btnViewDetails.setEnabled(false);
        
        leftPanel.add(btnAdd);
        leftPanel.add(btnEdit);
        leftPanel.add(btnDelete);
        leftPanel.add(btnViewDetails);
        
        // Panel derecho - Botón refrescar
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);
        
        btnRefresh = createButton("Actualizar", new Color(52, 73, 94));
        btnRefresh.addActionListener(e -> {
            loadUsers();
            loadStatistics();
        });
        
        rightPanel.add(btnRefresh);
        
        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.EAST);
        
        // Listener para habilitar/deshabilitar botones según selección
        tableUsers.getSelectionModel().addListSelectionListener(e -> {
            boolean hasSelection = tableUsers.getSelectedRow() != -1;
            btnEdit.setEnabled(hasSelection);
            btnDelete.setEnabled(hasSelection);
            btnViewDetails.setEnabled(hasSelection);
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
    
    private void loadDepartments() {
        cmbFilterDepartment.removeAllItems();
        cmbFilterDepartment.addItem("Todos");
        
        String sql = "SELECT nombre FROM departamentos WHERE activo = TRUE ORDER BY nombre";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                cmbFilterDepartment.addItem(rs.getString("nombre"));
            }
            
        } catch (Exception e) {
            logger.error("Error al cargar departamentos", e);
        }
    }
    
    private void loadUsers() {
        tableModel.setRowCount(0);
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT u.id_usuario, u.dni, u.apellidos, u.nombres, u.email, ");
        sql.append("       d.nombre AS departamento, u.telefono, u.fingerprint_id, u.activo ");
        sql.append("FROM usuarios u ");
        sql.append("LEFT JOIN departamentos d ON u.id_departamento = d.id_departamento ");
        sql.append("WHERE 1=1 ");
        
        // Filtro de estado
        String filterActive = (String) cmbFilterActive.getSelectedItem();
        if ("Activos".equals(filterActive)) {
            sql.append("AND u.activo = TRUE ");
        } else if ("Inactivos".equals(filterActive)) {
            sql.append("AND u.activo = FALSE ");
        }
        
        // Filtro de departamento
        String filterDept = (String) cmbFilterDepartment.getSelectedItem();
        if (filterDept != null && !"Todos".equals(filterDept)) {
            sql.append("AND d.nombre = ? ");
        }
        
        sql.append("ORDER BY u.apellidos, u.nombres");
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            
            int paramIndex = 1;
            if (filterDept != null && !"Todos".equals(filterDept)) {
                pstmt.setString(paramIndex++, filterDept);
            }
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Object[] row = {
                        rs.getInt("id_usuario"),
                        rs.getString("dni"),
                        rs.getString("apellidos"),
                        rs.getString("nombres"),
                        rs.getString("email"),
                        rs.getString("departamento"),
                        rs.getString("telefono"),
                        rs.getObject("fingerprint_id") != null ? rs.getInt("fingerprint_id") : "-",
                        rs.getBoolean("activo") ? "Activo" : "Inactivo"
                    };
                    tableModel.addRow(row);
                }
            }
            
            logger.info("Cargados {} usuarios", tableModel.getRowCount());
            
        } catch (Exception e) {
            logger.error("Error al cargar usuarios", e);
            JOptionPane.showMessageDialog(this,
                "Error al cargar usuarios: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void searchUsers() {
        String searchText = txtSearch.getText().trim();
        
        if (searchText.isEmpty()) {
            sorter.setRowFilter(null);
            return;
        }
        
        RowFilter<DefaultTableModel, Object> filter = RowFilter.regexFilter("(?i)" + searchText);
        sorter.setRowFilter(filter);
        
        logger.info("Busqueda aplicada: {}", searchText);
    }
    
    private void clearSearch() {
        txtSearch.setText("");
        sorter.setRowFilter(null);
        cmbFilterActive.setSelectedIndex(0);
        cmbFilterDepartment.setSelectedIndex(0);
        loadUsers();
    }
    
    private void loadStatistics() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            int total = 0;
            int active = 0;
            int withFingerprint = 0;
            
            @Override
            protected Void doInBackground() throws Exception {
                try (Connection conn = DatabaseConfig.getConnection()) {
                    
                    // Total usuarios
                    String sql1 = "SELECT COUNT(*) FROM usuarios";
                    try (PreparedStatement pstmt = conn.prepareStatement(sql1);
                         ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) total = rs.getInt(1);
                    }
                    
                    // Usuarios activos
                    String sql2 = "SELECT COUNT(*) FROM usuarios WHERE activo = TRUE";
                    try (PreparedStatement pstmt = conn.prepareStatement(sql2);
                         ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) active = rs.getInt(1);
                    }
                    
                    // Con huella
                    String sql3 = "SELECT COUNT(*) FROM usuarios WHERE fingerprint_id IS NOT NULL AND activo = TRUE";
                    try (PreparedStatement pstmt = conn.prepareStatement(sql3);
                         ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) withFingerprint = rs.getInt(1);
                    }
                    
                } catch (Exception e) {
                    logger.error("Error al cargar estadisticas", e);
                }
                return null;
            }
            
            @Override
            protected void done() {
                lblTotalUsers.setText(String.valueOf(total));
                lblActiveUsers.setText(String.valueOf(active));
                lblUsersWithFingerprint.setText(String.valueOf(withFingerprint));
            }
        };
        
        worker.execute();
    }
    
    private void addUser() {
        UserFormDialog dialog = new UserFormDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), 
            "Nuevo Usuario", null);
        dialog.setVisible(true);
        
        if (dialog.isSaved()) {
            loadUsers();
            loadStatistics();
        }
    }
    
    private void editUser() {
        int selectedRow = tableUsers.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Por favor seleccione un usuario",
                "Advertencia",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int modelRow = tableUsers.convertRowIndexToModel(selectedRow);
        int userId = (int) tableModel.getValueAt(modelRow, 0);
        
        // Cargar usuario completo
        Usuario usuario = loadUserById(userId);
        if (usuario == null) {
            JOptionPane.showMessageDialog(this,
                "Error al cargar los datos del usuario",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        UserFormDialog dialog = new UserFormDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), 
            "Editar Usuario", usuario);
        dialog.setVisible(true);
        
        if (dialog.isSaved()) {
            loadUsers();
            loadStatistics();
        }
    }
    
    private void deleteUser() {
        int selectedRow = tableUsers.getSelectedRow();
        if (selectedRow == -1) {
            return;
        }
        
        int modelRow = tableUsers.convertRowIndexToModel(selectedRow);
        int userId = (int) tableModel.getValueAt(modelRow, 0);
        String userName = tableModel.getValueAt(modelRow, 3) + " " + tableModel.getValueAt(modelRow, 2);
        
        int option = JOptionPane.showConfirmDialog(this,
            "Esta seguro de eliminar al usuario?\n\n" + userName + "\n\n" +
            "Esta accion no se puede deshacer.",
            "Confirmar Eliminacion",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (option == JOptionPane.YES_OPTION) {
            String sql = "DELETE FROM usuarios WHERE id_usuario = ?";
            
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setInt(1, userId);
                int deleted = pstmt.executeUpdate();
                
                if (deleted > 0) {
                    JOptionPane.showMessageDialog(this,
                        "Usuario eliminado correctamente",
                        "Exito",
                        JOptionPane.INFORMATION_MESSAGE);
                    
                    logger.info("Usuario eliminado: ID {}", userId);
                    loadUsers();
                    loadStatistics();
                }
                
            } catch (SQLException e) {
                logger.error("Error al eliminar usuario", e);
                
                String message = "Error al eliminar usuario";
                if (e.getMessage().contains("foreign key")) {
                    message = "No se puede eliminar el usuario porque tiene registros asociados.\n" +
                             "Considere desactivarlo en lugar de eliminarlo.";
                }
                
                JOptionPane.showMessageDialog(this,
                    message,
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void viewUserDetails() {
        int selectedRow = tableUsers.getSelectedRow();
        if (selectedRow == -1) {
            return;
        }
        
        int modelRow = tableUsers.convertRowIndexToModel(selectedRow);
        int userId = (int) tableModel.getValueAt(modelRow, 0);
        
        Usuario usuario = loadUserById(userId);
        if (usuario == null) {
            JOptionPane.showMessageDialog(this,
                "Error al cargar los datos del usuario",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        UserDetailsDialog dialog = new UserDetailsDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), 
            usuario);
        dialog.setVisible(true);
    }
    
    private Usuario loadUserById(int userId) {
        String sql = "SELECT u.*, r.nombre AS rol_nombre, d.nombre AS departamento_nombre " +
                     "FROM usuarios u " +
                     "LEFT JOIN roles r ON u.id_rol = r.id_rol " +
                     "LEFT JOIN departamentos d ON u.id_departamento = d.id_departamento " +
                     "WHERE u.id_usuario = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Usuario usuario = new Usuario();
                    usuario.setIdUsuario(rs.getInt("id_usuario"));
                    usuario.setDni(rs.getString("dni"));
                    usuario.setNombres(rs.getString("nombres"));
                    usuario.setApellidos(rs.getString("apellidos"));
                    usuario.setEmail(rs.getString("email"));
                    usuario.setTelefono(rs.getString("telefono"));
                    usuario.setIdRol((Integer) rs.getObject("id_rol"));
                    usuario.setNombreRol(rs.getString("rol_nombre"));
                    usuario.setIdDepartamento((Integer) rs.getObject("id_departamento"));
                    usuario.setNombreDepartamento(rs.getString("departamento_nombre"));
                    usuario.setFingerprintId((Integer) rs.getObject("fingerprint_id"));
                    usuario.setFotoUrl(rs.getString("foto_url"));
                    usuario.setDireccion(rs.getString("direccion"));
                    
                    Date fechaNac = rs.getDate("fecha_nacimiento");
                    if (fechaNac != null) {
                        usuario.setFechaNacimiento(fechaNac.toLocalDate());
                    }
                    
                    usuario.setGenero(rs.getString("genero"));
                    usuario.setActivo(rs.getBoolean("activo"));
                    
                    Timestamp fechaReg = rs.getTimestamp("fecha_registro");
                    if (fechaReg != null) {
                        usuario.setFechaRegistro(fechaReg.toLocalDateTime());
                    }
                    
                    usuario.setObservaciones(rs.getString("observaciones"));
                    
                    return usuario;
                }
            }
            
        } catch (Exception e) {
            logger.error("Error al cargar usuario por ID", e);
        }
        
        return null;
    }
}