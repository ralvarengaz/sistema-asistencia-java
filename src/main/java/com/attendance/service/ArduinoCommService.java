package com.attendance.service;

import com.attendance.config.DatabaseConfig;
import com.attendance.util.SerialPortManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servicio de comunicación con Arduino
 * Baudrate: 57600 (configurado para sensor DY50)
 * 
 * @author Sistema Biométrico
 * @version 2.0 - Optimizado
 */
public class ArduinoCommService {
    
    private static final Logger logger = LoggerFactory.getLogger(ArduinoCommService.class);
    
    // BAUDRATE CORRECTO PARA SENSOR DY50
    private static final int DEFAULT_BAUDRATE = 57600;
    
    private SerialPortManager serialManager;
    private String currentPort;
    private int baudRate;
    
    public ArduinoCommService() {
        this.serialManager = new SerialPortManager();
        
        // Obtener baudrate desde configuración
        String baudRateStr = DatabaseConfig.getProperty("arduino.baudRate", String.valueOf(DEFAULT_BAUDRATE));
        try {
            this.baudRate = Integer.parseInt(baudRateStr);
            logger.info("Baudrate configurado: {} baudios", this.baudRate);
        } catch (NumberFormatException e) {
            logger.warn("Baudrate inválido en configuración, usando default: {}", DEFAULT_BAUDRATE);
            this.baudRate = DEFAULT_BAUDRATE;
        }
    }
    
    /**
     * Conecta con Arduino en el puerto especificado
     */
    public boolean connect(String portName) {
        logger.info("═══════════════════════════════════════");
        logger.info("  Intentando conectar con Arduino");
        logger.info("  Puerto: {}", portName);
        logger.info("  Baudrate: {} baudios", baudRate);
        logger.info("═══════════════════════════════════════");
        
        boolean connected = serialManager.connect(portName, baudRate);
        
        if (connected) {
            currentPort = portName;
            
            // Verificar comunicación con PING
            logger.info("Verificando comunicación con sensor...");
            if (ping()) {
                logger.info("✓ Comunicación establecida correctamente");
                logger.info("✓ Sensor responde a comandos");
                
                // Obtener info del sensor
                getSensorInfo();
                
                return true;
            } else {
                logger.warn("⚠ Arduino conectado pero no responde a PING");
                logger.warn("⚠ Verifique que el firmware esté cargado correctamente");
                serialManager.disconnect();
                return false;
            }
        } else {
            logger.error("✗ No se pudo conectar con Arduino en {}", portName);
        }
        
        return false;
    }
    
    /**
     * Desconecta del Arduino
     */
    public void disconnect() {
        serialManager.disconnect();
        currentPort = null;
        logger.info("⊗ Desconectado de Arduino");
    }
    
    /**
     * Verifica si está conectado
     */
    public boolean isConnected() {
        return serialManager.isConnected();
    }
    
    /**
     * Obtiene el puerto actual
     */
    public String getCurrentPort() {
        return currentPort;
    }
    
    /**
     * Ping para verificar comunicación
     */
    public boolean ping() {
        serialManager.clearBuffer();
        serialManager.sendCommand("PING");
        
        String response = serialManager.readLine(2000);
        
        if (response != null && response.contains("PONG")) {
            logger.debug("✓ PING exitoso");
            return true;
        }
        
        logger.warn("⚠ No se recibió respuesta PONG");
        return false;
    }
    
    /**
     * Obtiene información del sensor
     */
    private void getSensorInfo() {
        serialManager.clearBuffer();
        serialManager.sendCommand("INFO");
        
        // Esperar respuestas múltiples
        long startTime = System.currentTimeMillis();
        StringBuilder info = new StringBuilder();
        
        while (System.currentTimeMillis() - startTime < 1000) {
            String line = serialManager.readLine(100);
            if (line != null && line.startsWith("INFO:")) {
                info.append(line).append("\n");
                logger.debug(line);
            }
        }
        
        if (info.length() > 0) {
            logger.info("Información del sensor recibida");
        }
    }
    
    /**
     * Inicia proceso de enrolamiento de huella
     * 
     * @param fingerprintId ID de la huella (1-255)
     * @param callback Callback para recibir actualizaciones
     * @return true si el proceso inició correctamente
     */
    public boolean startEnroll(int fingerprintId, EnrollCallback callback) {
        if (!isConnected()) {
            callback.onError("No hay conexión con Arduino");
            return false;
        }
        
        if (fingerprintId < 1 || fingerprintId > 255) {
            callback.onError("ID de huella debe estar entre 1 y 255");
            return false;
        }
        
        logger.info("═══════════════════════════════════════");
        logger.info("  Iniciando enrolamiento");
        logger.info("  ID: {}", fingerprintId);
        logger.info("═══════════════════════════════════════");
        
        // Limpiar buffer
        serialManager.clearBuffer();
        
        // Enviar comando de enrolamiento
        serialManager.sendCommand("ENROLL:" + fingerprintId);
        
        // Escuchar respuestas en hilo separado
        new Thread(() -> listenEnrollProcess(callback)).start();
        
        return true;
    }
    
    /**
     * Escucha el proceso de enrolamiento
     */
    private void listenEnrollProcess(EnrollCallback callback) {
        boolean finished = false;
        long timeout = 30000; // 30 segundos
        long startTime = System.currentTimeMillis();
        
        while (!finished && (System.currentTimeMillis() - startTime < timeout)) {
            String response = serialManager.readLine(100);
            
            if (response != null) {
                logger.debug("← {}", response);
                
                if (response.startsWith("STATUS:ENROLL_START:")) {
                    String id = response.split(":")[2];
                    callback.onProgress("Iniciando enrolamiento para ID " + id);
                    
                } else if (response.startsWith("STATUS:ENROLL_STEP:")) {
                    String message = response.substring("STATUS:ENROLL_STEP:".length());
                    callback.onProgress(message);
                    
                } else if (response.startsWith("STATUS:ENROLL_SUCCESS:")) {
                    String id = response.split(":")[2];
                    logger.info("✓ Enrolamiento exitoso - ID: {}", id);
                    callback.onSuccess(Integer.parseInt(id));
                    finished = true;
                    
                } else if (response.startsWith("ERROR:")) {
                    String[] parts = response.split(":", 3);
                    String errorCode = parts.length > 1 ? parts[1] : "UNKNOWN";
                    String errorMessage = parts.length > 2 ? parts[2] : "Error desconocido";
                    logger.error("✗ Error en enrolamiento: {} - {}", errorCode, errorMessage);
                    callback.onError(errorMessage);
                    finished = true;
                }
            }
            
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        if (!finished) {
            logger.error("✗ Timeout: El proceso tardó demasiado");
            callback.onError("Timeout: El proceso tardó demasiado");
        }
    }
    
    /**
     * Verifica una huella
     */
    public void startVerify(VerifyCallback callback) {
        if (!isConnected()) {
            callback.onError("No hay conexión con Arduino");
            return;
        }
        
        logger.info("Iniciando verificación de huella...");
        
        serialManager.clearBuffer();
        serialManager.sendCommand("VERIFY");
        
        new Thread(() -> listenVerifyProcess(callback)).start();
    }
    
    /**
     * Escucha el proceso de verificación
     */
    private void listenVerifyProcess(VerifyCallback callback) {
        boolean finished = false;
        long timeout = 15000; // 15 segundos
        long startTime = System.currentTimeMillis();
        
        while (!finished && (System.currentTimeMillis() - startTime < timeout)) {
            String response = serialManager.readLine(100);
            
            if (response != null) {
                logger.debug("← {}", response);
                
                if (response.startsWith("STATUS:VERIFY_START")) {
                    callback.onWaiting("Esperando huella...");
                    
                } else if (response.startsWith("VERIFY_SUCCESS:")) {
                    String[] parts = response.split(":");
                    if (parts.length >= 3) {
                        int fingerprintId = Integer.parseInt(parts[1]);
                        int confidence = Integer.parseInt(parts[2]);
                        logger.info("✓ Huella verificada - ID: {}, Confianza: {}", fingerprintId, confidence);
                        callback.onSuccess(fingerprintId, confidence);
                        finished = true;
                    }
                    
                } else if (response.startsWith("ERROR:NOT_FOUND")) {
                    logger.warn("⚠ Huella no encontrada en el sistema");
                    callback.onNotFound();
                    finished = true;
                    
                } else if (response.startsWith("ERROR:")) {
                    String[] parts = response.split(":", 3);
                    String errorMessage = parts.length > 2 ? parts[2] : "Error desconocido";
                    logger.error("✗ Error en verificación: {}", errorMessage);
                    callback.onError(errorMessage);
                    finished = true;
                }
            }
            
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        if (!finished) {
            logger.error("✗ Timeout en verificación");
            callback.onError("Timeout: El proceso tardó demasiado");
        }
    }
    
    /**
     * Elimina una huella del sensor
     */
    public boolean deleteFingerprint(int fingerprintId) {
        if (!isConnected()) {
            logger.error("✗ No hay conexión con Arduino");
            return false;
        }
        
        logger.info("Eliminando huella ID: {}", fingerprintId);
        
        serialManager.clearBuffer();
        serialManager.sendCommand("DELETE:" + fingerprintId);
        
        String response = serialManager.readLine(3000);
        
        boolean success = response != null && response.contains("DELETE_SUCCESS");
        
        if (success) {
            logger.info("✓ Huella {} eliminada del sensor", fingerprintId);
        } else {
            logger.warn("⚠ No se pudo eliminar huella {}", fingerprintId);
        }
        
        return success;
    }
    
    /**
     * Obtiene la cantidad de huellas almacenadas
     */
    public int getTemplateCount() {
        if (!isConnected()) {
            logger.warn("⚠ No hay conexión con Arduino");
            return -1;
        }
        
        serialManager.clearBuffer();
        serialManager.sendCommand("COUNT");
        
        String response = serialManager.readLine(2000);
        
        if (response != null && response.startsWith("STATUS:TEMPLATES:")) {
            try {
                int count = Integer.parseInt(response.split(":")[2]);
                logger.info("ℹ Huellas en sensor: {}", count);
                return count;
            } catch (Exception e) {
                logger.error("✗ Error parseando cantidad de templates", e);
            }
        }
        
        return -1;
    }
    
    /**
     * Ejecuta un test del sensor
     */
    public void runTest() {
        if (!isConnected()) {
            logger.warn("⚠ No hay conexión con Arduino");
            return;
        }
        
        logger.info("Ejecutando test del sensor...");
        serialManager.clearBuffer();
        serialManager.sendCommand("TEST");
        
        // Esperar respuestas del test
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 5000) {
            String line = serialManager.readLine(100);
            if (line != null) {
                logger.info("TEST: {}", line);
            }
        }
    }
    
    // ============================================
    // INTERFACES DE CALLBACKS
    // ============================================
    
    public interface EnrollCallback {
        void onProgress(String message);
        void onSuccess(int fingerprintId);
        void onError(String error);
    }
    
    public interface VerifyCallback {
        void onWaiting(String message);
        void onSuccess(int fingerprintId, int confidence);
        void onNotFound();
        void onError(String error);
    }
}
