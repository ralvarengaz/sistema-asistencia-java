import java.sql.*;

/**
 * Test independiente de la función PostgreSQL
 * 
 * CÓMO USAR:
 * 1. Guarda este archivo como TestFuncionPostgres.java
 * 2. Compila: javac TestFuncionPostgres.java
 * 3. Ejecuta: java -cp ".;postgresql-42.x.x.jar" TestFuncionPostgres
 *    (En Linux/Mac usa : en lugar de ;)
 */
public class TestFuncionPostgres {
    
    // CONFIGURACIÓN - Ajusta estos valores
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/attendance_system";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "MiNuevaPassword123!"; // CAMBIA ESTO
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════╗");
        System.out.println("║   TEST DE FUNCIÓN POSTGRESQL                       ║");
        System.out.println("╚════════════════════════════════════════════════════╝");
        System.out.println();
        
        Connection conn = null;
        
        try {
            // 1. Cargar driver
            System.out.println("📦 Cargando driver JDBC...");
            Class.forName("org.postgresql.Driver");
            System.out.println("✅ Driver cargado correctamente");
            System.out.println();
            
            // 2. Conectar
            System.out.println("🔌 Conectando a la base de datos...");
            System.out.println("   URL: " + DB_URL);
            System.out.println("   Usuario: " + DB_USER);
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("✅ Conexión establecida");
            System.out.println();
            
            // 3. Información de versiones
            DatabaseMetaData metadata = conn.getMetaData();
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("INFORMACIÓN DEL SISTEMA");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("JDBC Driver: " + metadata.getDriverName());
            System.out.println("Driver Version: " + metadata.getDriverVersion());
            System.out.println("Database: " + metadata.getDatabaseProductName());
            System.out.println("DB Version: " + metadata.getDatabaseProductVersion());
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println();
            
            // 4. Verificar que la función existe
            System.out.println("🔍 Verificando que la función existe...");
            String checkSql = "SELECT COUNT(*) FROM pg_proc WHERE proname = 'registrar_asistencia_v2'";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(checkSql)) {
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("✅ La función registrar_asistencia_v2 existe");
                } else {
                    System.out.println("❌ La función registrar_asistencia_v2 NO existe");
                    System.out.println("   Ejecuta el script SOLUCION_RAPIDA.sql primero");
                    return;
                }
            }
            System.out.println();
            
            // 5. Probar la función
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("TEST 1: Fingerprint ID inexistente (999)");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            testFunction(conn, 999, 100, "ENTRADA");
            System.out.println();
            
            // 6. Preguntar si probar con usuario real
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("TEST 2: Listar usuarios con fingerprint_id");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            String userSql = "SELECT id_usuario, dni, nombres, apellidos, fingerprint_id " +
                           "FROM usuarios WHERE fingerprint_id IS NOT NULL AND activo = TRUE";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(userSql)) {
                boolean hasUsers = false;
                while (rs.next()) {
                    hasUsers = true;
                    System.out.println(String.format("  Usuario: %s %s (DNI: %s, FP_ID: %d)",
                        rs.getString("nombres"),
                        rs.getString("apellidos"),
                        rs.getString("dni"),
                        rs.getInt("fingerprint_id")
                    ));
                }
                
                if (!hasUsers) {
                    System.out.println("⚠️  No hay usuarios con fingerprint_id registrado");
                    System.out.println("   Registra usuarios primero para probar con datos reales");
                }
            }
            System.out.println();
            
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("✅ TODAS LAS PRUEBAS COMPLETADAS");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            
        } catch (ClassNotFoundException e) {
            System.err.println("❌ ERROR: Driver JDBC no encontrado");
            System.err.println("   Descarga postgresql-42.x.x.jar y agrégalo al classpath");
            e.printStackTrace();
            
        } catch (SQLException e) {
            System.err.println("❌ ERROR SQL");
            System.err.println("   Mensaje: " + e.getMessage());
            System.err.println("   Estado SQL: " + e.getSQLState());
            System.err.println("   Código: " + e.getErrorCode());
            System.err.println();
            System.err.println("Stack trace:");
            e.printStackTrace();
            
        } catch (Exception e) {
            System.err.println("❌ ERROR INESPERADO");
            e.printStackTrace();
            
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                    System.out.println("\n🔌 Conexión cerrada");
                } catch (SQLException e) {
                    System.err.println("Error al cerrar conexión: " + e.getMessage());
                }
            }
        }
    }
    
    private static void testFunction(Connection conn, int fingerprintId, int confidence, String tipo) {
        String sql = "SELECT * FROM registrar_asistencia_v2(?, ?, ?)";
        
        System.out.println("Ejecutando función con parámetros:");
        System.out.println("  fingerprint_id: " + fingerprintId);
        System.out.println("  confidence: " + confidence);
        System.out.println("  tipo: " + tipo);
        System.out.println();
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, fingerprintId);
            pstmt.setInt(2, confidence);
            pstmt.setString(3, tipo);
            
            System.out.println("⏳ Ejecutando consulta...");
            
            try (ResultSet rs = pstmt.executeQuery()) {
                
                // Obtener metadata
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();
                
                System.out.println("✅ Consulta ejecutada");
                System.out.println();
                System.out.println("Columnas devueltas: " + columnCount);
                for (int i = 1; i <= columnCount; i++) {
                    System.out.println(String.format("  %d. %s (%s)",
                        i,
                        meta.getColumnName(i),
                        meta.getColumnTypeName(i)
                    ));
                }
                System.out.println();
                
                // Verificar si hay datos
                if (!rs.next()) {
                    System.out.println("❌ PROBLEMA: ResultSet vacío (no hay filas)");
                    System.out.println("   La función no devolvió ningún resultado");
                    System.out.println("   Esto indica un problema con la función en PostgreSQL");
                    return;
                }
                
                System.out.println("✅ ResultSet contiene datos");
                System.out.println();
                
                // Intentar leer las columnas
                System.out.println("Leyendo columnas:");
                try {
                    boolean success = rs.getBoolean("success");
                    System.out.println("  ✅ success: " + success);
                    
                    String message = rs.getString("message");
                    System.out.println("  ✅ message: " + message);
                    
                    Object idUsuarioObj = rs.getObject("id_usuario");
                    System.out.println("  ✅ id_usuario: " + idUsuarioObj);
                    
                    String usuarioNombre = rs.getString("usuario_nombre");
                    System.out.println("  ✅ usuario_nombre: " + usuarioNombre);
                    
                    Object idAsistenciaObj = rs.getObject("id_asistencia");
                    System.out.println("  ✅ id_asistencia: " + idAsistenciaObj);
                    
                    System.out.println();
                    System.out.println("✅ TODAS LAS COLUMNAS LEÍDAS CORRECTAMENTE");
                    System.out.println("   Si esto funciona aquí pero falla en tu aplicación,");
                    System.out.println("   el problema está en el código de tu aplicación.");
                    
                } catch (SQLException e) {
                    System.err.println("❌ ERROR AL LEER COLUMNAS");
                    System.err.println("   Mensaje: " + e.getMessage());
                    System.err.println("   Columna problemática: " + e.getMessage());
                    System.err.println();
                    System.err.println("   Columnas disponibles:");
                    for (int i = 1; i <= columnCount; i++) {
                        System.err.println("     - " + meta.getColumnName(i));
                    }
                    throw e;
                }
            }
            
        } catch (SQLException e) {
            System.err.println("❌ ERROR EN LA PRUEBA");
            System.err.println("   Mensaje: " + e.getMessage());
            System.err.println("   Estado SQL: " + e.getSQLState());
            System.err.println("   Código: " + e.getErrorCode());
            e.printStackTrace();
        }
    }
}