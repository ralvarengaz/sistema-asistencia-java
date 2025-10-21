package com.attendance.service;

import com.attendance.config.DatabaseConfig;
import com.attendance.util.SerialPortManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servicio de comunicación con Arduino
 * 
 * @author Sistema Biométrico
 * @version 1.0
 */
public class ArduinoCommService {
    
    private static final Logger logger = LoggerFactory.getLogger(ArduinoCommService.class);
    
    private SerialPortManager serialManager;
    private String currentPort;
    private int baudRate;
    
    public ArduinoCommService() {
        this.serialManager = new SerialPortManager();
        this.baudRate = Integer.parseInt(DatabaseConfig.getProperty("arduino.baudRate", "115200"));
    }
    
    /**
     * Conecta con Arduino en el puerto especificado
     */
    public boolean connect(String portName) {
        logger.info("Intentando conectar con Arduino en {}", portName);
        
        boolean connected = serialManager.connect(portName, baudRate);
        
        if (connected) {
            currentPort = portName;
            
            // Verificar comunicación con PING
            if (ping()) {
                logger.info("Comunicación con Arduino establecida correctamente");
                return true;
            } else {
                logger.warn("Arduino conectado pero no responde a PING");
                serialManager.disconnect();
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * Desconecta del Arduino
     */
    public void disconnect() {
        serialManager.disconnect();
        currentPort = null;
        logger.info("Desconectado de Arduino");
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
            logger.debug("PING exitoso");
            return true;
        }
        
        logger.warn("No se recibió respuesta PONG");
        return false;
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
        
        logger.info("Iniciando enrolamiento para ID: {}", fingerprintId);
        
        // Limpiar buffer
        serialManager.clearBuffer();
        
        // Enviar comando de enrolamiento
        serialManager.sendCommand("ENROLL:" + fingerprintId);
        
        // Escuchar respuestas en hilo separado
        new Thread(() -> listenEnrollProcess(callback)).start();
        
        return true;
    }
    
    private void listenEnrollProcess(EnrollCallback callback) {
        boolean finished = false;
        long timeout = 30000; // 30 segundos
        long startTime = System.currentTimeMillis();
        
        while (!finished && (System.currentTimeMillis() - startTime < timeout)) {
            String response = serialManager.readLine(100);
            
            if (response != null) {
                logger.debug("Respuesta enroll: {}", response);
                
                if (response.startsWith("STATUS:ENROLL_START:")) {
                    String id = response.split(":")[2];
                    callback.onProgress("Iniciando enrolamiento para ID " + id);
                    
                } else if (response.startsWith("STATUS:ENROLL_STEP:")) {
                    String message = response.substring("STATUS:ENROLL_STEP:".length());
                    callback.onProgress(message);
                    
                } else if (response.startsWith("STATUS:ENROLL_SUCCESS:")) {
                    String id = response.split(":")[2];
                    callback.onSuccess(Integer.parseInt(id));
                    finished = true;
                    
                } else if (response.startsWith("ERROR:")) {
                    String[] parts = response.split(":", 3);
                    String errorCode = parts.length > 1 ? parts[1] : "UNKNOWN";
                    String errorMessage = parts.length > 2 ? parts[2] : "Error desconocido";
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
        
        logger.info("Iniciando verificación de huella");
        
        serialManager.clearBuffer();
        serialManager.sendCommand("VERIFY");
        
        new Thread(() -> listenVerifyProcess(callback)).start();
    }
    
    private void listenVerifyProcess(VerifyCallback callback) {
        boolean finished = false;
        long timeout = 15000; // 15 segundos
        long startTime = System.currentTimeMillis();
        
        while (!finished && (System.currentTimeMillis() - startTime < timeout)) {
            String response = serialManager.readLine(100);
            
            if (response != null) {
                logger.debug("Respuesta verify: {}", response);
                
                if (response.startsWith("STATUS:VERIFY_START")) {
                    callback.onWaiting("Esperando huella...");
                    
                } else if (response.startsWith("VERIFY_SUCCESS:")) {
                    String[] parts = response.split(":");
                    if (parts.length >= 3) {
                        int fingerprintId = Integer.parseInt(parts[1]);
                        int confidence = Integer.parseInt(parts[2]);
                        callback.onSuccess(fingerprintId, confidence);
                        finished = true;
                    }
                    
                } else if (response.startsWith("ERROR:NOT_FOUND")) {
                    callback.onNotFound();
                    finished = true;
                    
                } else if (response.startsWith("ERROR:")) {
                    String[] parts = response.split(":", 3);
                    String errorMessage = parts.length > 2 ? parts[2] : "Error desconocido";
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
            callback.onError("Timeout: El proceso tardó demasiado");
        }
    }
    
    /**
     * Elimina una huella del sensor
     */
    public boolean deleteFingerprint(int fingerprintId) {
        if (!isConnected()) {
            return false;
        }
        
        serialManager.clearBuffer();
        serialManager.sendCommand("DELETE:" + fingerprintId);
        
        String response = serialManager.readLine(3000);
        
        return response != null && response.contains("DELETE_SUCCESS");
    }
    
    /**
     * Obtiene la cantidad de huellas almacenadas
     */
    public int getTemplateCount() {
        if (!isConnected()) {
            return -1;
        }
        
        serialManager.clearBuffer();
        serialManager.sendCommand("COUNT");
        
        String response = serialManager.readLine(2000);
        
        if (response != null && response.startsWith("STATUS:TEMPLATES:")) {
            try {
                return Integer.parseInt(response.split(":")[2]);
            } catch (Exception e) {
                logger.error("Error parseando cantidad de templates", e);
            }
        }
        
        return -1;
    }
    
    // Interfaces de callbacks
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