package com.attendance.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Configuración de conexión a base de datos PostgreSQL con pooling HikariCP
 * 
 * @author Sistema Biométrico
 * @version 1.0
 */
public class DatabaseConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static HikariDataSource dataSource;
    private static Properties properties;
    
    /**
     * Inicializa la configuración de base de datos
     */
    public static void initialize() {
        try {
            // Cargar propiedades
            properties = loadProperties();
            
            // Configurar HikariCP
            HikariConfig config = new HikariConfig();
            
            String host = properties.getProperty("db.host", "localhost");
            String port = properties.getProperty("db.port", "5432");
            String dbName = properties.getProperty("db.name", "attendance_system");
            String username = properties.getProperty("db.username", "app_attendance");
            String password = properties.getProperty("db.password");
            
            String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", host, port, dbName);
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);
            
            // Configuraciones del pool
            config.setMaximumPoolSize(Integer.parseInt(properties.getProperty("db.pool.maxPoolSize", "10")));
            config.setMinimumIdle(Integer.parseInt(properties.getProperty("db.pool.minIdle", "2")));
            config.setConnectionTimeout(Long.parseLong(properties.getProperty("db.pool.connectionTimeout", "30000")));
            config.setIdleTimeout(Long.parseLong(properties.getProperty("db.pool.idleTimeout", "600000")));
            config.setMaxLifetime(Long.parseLong(properties.getProperty("db.pool.maxLifetime", "1800000")));
            
            // Configuraciones adicionales
            config.setConnectionTestQuery("SELECT 1");
            config.setPoolName("AttendancePool");
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            
            dataSource = new HikariDataSource(config);
            
            logger.info("Pool de conexiones inicializado correctamente");
            logger.info("Conectado a: {}", jdbcUrl);
            
        } catch (Exception e) {
            logger.error("Error al inicializar configuración de base de datos", e);
            throw new RuntimeException("No se pudo inicializar la base de datos", e);
        }
    }
    
    /**
     * Obtiene una conexión del pool
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource no inicializado. Llamar a initialize() primero.");
        }
        return dataSource.getConnection();
    }
    
    /**
     * Cierra el pool de conexiones
     */
    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Pool de conexiones cerrado");
        }
    }
    
    /**
     * Prueba la conexión a la base de datos
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            boolean isValid = conn != null && conn.isValid(5);
            if (isValid) {
                logger.info("Conexión a base de datos exitosa");
            }
            return isValid;
        } catch (SQLException e) {
            logger.error("Error al probar conexión a base de datos", e);
            return false;
        }
    }
    
    /**
     * Carga las propiedades desde application.properties
     */
    private static Properties loadProperties() throws IOException {
        Properties props = new Properties();
        
        // Intentar cargar desde resources
        try (InputStream input = DatabaseConfig.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            
            if (input == null) {
                logger.warn("No se encontró application.properties, usando valores por defecto");
                // Valores por defecto
                props.setProperty("db.host", "localhost");
                props.setProperty("db.port", "5432");
                props.setProperty("db.name", "attendance_system");
                props.setProperty("db.username", "app_attendance");
                props.setProperty("db.password", "SecurePass2025!");
                props.setProperty("db.pool.maxPoolSize", "10");
                props.setProperty("db.pool.minIdle", "2");
                props.setProperty("db.pool.connectionTimeout", "30000");
                props.setProperty("db.pool.idleTimeout", "600000");
                props.setProperty("db.pool.maxLifetime", "1800000");
            } else {
                props.load(input);
                logger.info("Propiedades cargadas desde application.properties");
            }
        }
        
        return props;
    }
    
    /**
     * Obtiene una propiedad de configuración
     */
    public static String getProperty(String key) {
        if (properties == null) {
            try {
                properties = loadProperties();
            } catch (IOException e) {
                logger.error("Error al cargar propiedades", e);
                return null;
            }
        }
        return properties.getProperty(key);
    }
    
    /**
     * Obtiene una propiedad con valor por defecto
     */
    public static String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Obtiene estadísticas del pool de conexiones
     */
    public static String getPoolStats() {
        if (dataSource == null) {
            return "Pool no inicializado";
        }
        
        return String.format(
            "Pool: %s | Activas: %d | Inactivas: %d | Total: %d | Esperando: %d",
            dataSource.getPoolName(),
            dataSource.getHikariPoolMXBean().getActiveConnections(),
            dataSource.getHikariPoolMXBean().getIdleConnections(),
            dataSource.getHikariPoolMXBean().getTotalConnections(),
            dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
        );
    }
}