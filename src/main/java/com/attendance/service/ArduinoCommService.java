package com.attendance.service;

import com.attendance.util.SerialPortManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.concurrent.CompletableFuture;

public class ArduinoCommService {
    private static final Logger logger = LoggerFactory.getLogger(ArduinoCommService.class);
    private static final int BAUD_RATE = 115200;
    private static final int CONNECTION_TIMEOUT = 5000;
    private static final int COMMAND_TIMEOUT = 3000;
    
    private SerialPortManager serialPortManager;
    private boolean isConnected = false;
    
    public ArduinoCommService() {
        this.serialPortManager = new SerialPortManager();
        logger.info("Baudrate configurado: {} baudios", BAUD_RATE);
    }
    
    public boolean connect(String portName) {
        try {
            logger.info("═══════════════════════════════════════");
            logger.info("  Conectando con Arduino");
            logger.info("  Puerto: {}", portName);
            logger.info("  Baudrate: {} baudios", BAUD_RATE);
            logger.info("═══════════════════════════════════════");
            
            if (!serialPortManager.connect(portName, BAUD_RATE)) {
                logger.error("✗ No se pudo abrir el puerto {}", portName);
                return false;
            }
            
            logger.info("Puerto abierto correctamente");
            Thread.sleep(2500);
            
            serialPortManager.clearBuffer();
            Thread.sleep(300);
            
            logger.info("Verificando comunicación...");
            
            if (verifyConnection()) {
                isConnected = true;
                logger.info("✓ Arduino conectado y verificado");
                return true;
            } else {
                logger.warn("⚠ Arduino no responde correctamente");
                serialPortManager.disconnect();
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error al conectar: {}", e.getMessage(), e);
            if (serialPortManager != null) {
                serialPortManager.disconnect();
            }
            return false;
        }
    }
    
    public CompletableFuture<Boolean> connectAsync(String portName) {
        return CompletableFuture.supplyAsync(() -> connect(portName));
    }
    
    private boolean verifyConnection() {
        try {
            serialPortManager.clearBuffer();
            Thread.sleep(100);
            
            String response = serialPortManager.sendCommand("PING", COMMAND_TIMEOUT);
            
            if (response != null && response.contains("READY")) {
                logger.debug("✓ Respuesta READY recibida");
                return true;
            } else {
                logger.warn("✗ No se recibió READY");
                if (response != null) {
                    logger.debug("Respuesta: {}", response);
                }
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error verificando conexión: {}", e.getMessage());
            return false;
        }
    }
    
    public int getTemplateCount() {
        if (!isConnected()) {
            logger.error("No hay conexión con Arduino");
            return 0;
        }
        
        try {
            serialPortManager.clearBuffer();
            Thread.sleep(50);
            
            String response = serialPortManager.sendCommand("COUNT", COMMAND_TIMEOUT);
            
            if (response != null && response.contains("COUNT:")) {
                String[] lines = response.split("\n");
                for (String line : lines) {
                    if (line.contains("COUNT:")) {
                        String countStr = line.substring(line.indexOf("COUNT:") + 6).trim();
                        return Integer.parseInt(countStr.split("\\s+")[0]);
                    }
                }
            }
            return 0;
            
        } catch (Exception e) {
            logger.error("Error obteniendo template count: {}", e.getMessage());
            return 0;
        }
    }
    
    public void disconnect() {
        if (serialPortManager != null) {
            serialPortManager.disconnect();
        }
        isConnected = false;
        logger.info("Desconectado del Arduino");
    }
    
    public boolean isConnected() {
        return isConnected && serialPortManager != null && serialPortManager.isConnected();
    }
    
    public void startEnroll(int id, EnrollCallback callback) {
        enrollFingerprint(id, callback).thenAccept(result -> {
            if (!result.success) {
                SwingUtilities.invokeLater(() -> callback.onError(result.message));
            }
        });
    }
    
    public CompletableFuture<EnrollResult> enrollFingerprint(int id, EnrollCallback callback) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isConnected()) {
                logger.error("No hay conexión con Arduino");
                return new EnrollResult(false, "No hay conexión con Arduino");
            }
            
            try {
                logger.info("Iniciando enrollamiento para ID: {}", id);
                
                serialPortManager.clearBuffer();
                Thread.sleep(100);
                
                boolean sent = serialPortManager.sendCommandNoResponse("ENROLL:" + id);
                
                if (!sent) {
                    return new EnrollResult(false, "Error al enviar comando");
                }
                
                Thread.sleep(200);
                
                return listenEnrollProcess(callback);
                
            } catch (Exception e) {
                logger.error("Error en enrollamiento: {}", e.getMessage(), e);
                return new EnrollResult(false, "Error: " + e.getMessage());
            }
        });
    }
    
    private EnrollResult listenEnrollProcess(EnrollCallback callback) {
        try {
            long startTime = System.currentTimeMillis();
            long timeout = 60000;
            
            while (System.currentTimeMillis() - startTime < timeout) {
                String message = serialPortManager.readLine(500);
                
                if (message == null || message.isEmpty()) {
                    continue;
                }
                
                logger.debug("Enroll msg: {}", message);
                
                if (message.contains("ENROLL:START:")) {
                    SwingUtilities.invokeLater(() -> callback.onProgress("Proceso iniciado"));
                    
                } else if (message.contains("PLACE_FINGER")) {
                    SwingUtilities.invokeLater(() -> callback.onProgress("Coloque el dedo en el sensor"));
                    
                } else if (message.contains("CAPTURED")) {
                    SwingUtilities.invokeLater(() -> callback.onProgress("Imagen capturada"));
                    
                } else if (message.contains("REMOVE_FINGER")) {
                    SwingUtilities.invokeLater(() -> callback.onProgress("Retire el dedo"));
                    
                } else if (message.contains("PLACE_AGAIN")) {
                    SwingUtilities.invokeLater(() -> callback.onProgress("Coloque el mismo dedo nuevamente"));
                    
                } else if (message.contains("CREATING_MODEL")) {
                    SwingUtilities.invokeLater(() -> callback.onProgress("Creando modelo de huella"));
                    
                } else if (message.contains("SAVING")) {
                    SwingUtilities.invokeLater(() -> callback.onProgress("Guardando huella"));
                    
                } else if (message.contains("SUCCESS")) {
                    SwingUtilities.invokeLater(() -> callback.onProgress("Huella guardada exitosamente"));
                    
                } else if (message.contains("ENROLL:OK:")) {
                    String[] parts = message.split(":");
                    if (parts.length >= 3) {
                        int enrolledId = Integer.parseInt(parts[2].trim());
                        SwingUtilities.invokeLater(() -> callback.onSuccess(enrolledId));
                        logger.info("✓ Enrollamiento exitoso - ID: {}", enrolledId);
                        return new EnrollResult(true, "Enrollamiento exitoso", enrolledId);
                    }
                    
                } else if (message.contains("ENROLL:FAIL:")) {
                    String[] parts = message.split(":", 3);
                    String error = parts.length >= 3 ? parts[2] : "Error desconocido";
                    SwingUtilities.invokeLater(() -> callback.onError(error));
                    logger.error("✗ Error en enrollamiento: {}", error);
                    return new EnrollResult(false, error);
                    
                } else if (message.contains("TIMEOUT")) {
                    SwingUtilities.invokeLater(() -> callback.onError("Tiempo de espera agotado"));
                    return new EnrollResult(false, "Timeout");
                    
                } else if (message.contains("NOT_MATCH")) {
                    SwingUtilities.invokeLater(() -> callback.onError("Las huellas no coinciden"));
                    return new EnrollResult(false, "No coinciden");
                }
            }
            
            SwingUtilities.invokeLater(() -> callback.onError("Timeout general del proceso"));
            return new EnrollResult(false, "Timeout");
            
        } catch (Exception e) {
            logger.error("Error escuchando enrollamiento: {}", e.getMessage());
            SwingUtilities.invokeLater(() -> callback.onError("Error: " + e.getMessage()));
            return new EnrollResult(false, "Error: " + e.getMessage());
        }
    }
    
    public void startVerify(VerifyCallback callback) {
        CompletableFuture.runAsync(() -> {
            if (!isConnected()) {
                SwingUtilities.invokeLater(() -> callback.onError("No hay conexión con Arduino"));
                return;
            }
            
            try {
                logger.info("Iniciando verificación de huella");
                
                serialPortManager.clearBuffer();
                Thread.sleep(100);
                
                boolean sent = serialPortManager.sendCommandNoResponse("VERIFY");
                
                if (!sent) {
                    SwingUtilities.invokeLater(() -> callback.onError("Error al enviar comando"));
                    return;
                }
                
                Thread.sleep(200);
                
                SwingUtilities.invokeLater(() -> callback.onWaiting("Esperando huella..."));
                
                listenVerifyProcess(callback);
                
            } catch (Exception e) {
                logger.error("Error en verificación: {}", e.getMessage(), e);
                SwingUtilities.invokeLater(() -> callback.onError("Error: " + e.getMessage()));
            }
        });
    }
    
    private void listenVerifyProcess(VerifyCallback callback) {
        try {
            long startTime = System.currentTimeMillis();
            long timeout = 30000;
            
            while (System.currentTimeMillis() - startTime < timeout) {
                String message = serialPortManager.readLine(500);
                
                if (message == null || message.isEmpty()) {
                    continue;
                }
                
                logger.debug("Verify msg: {}", message);
                
                if (message.contains("PLACE_FINGER")) {
                    SwingUtilities.invokeLater(() -> callback.onWaiting("Coloque el dedo en el sensor"));
                    
                } else if (message.contains("CAPTURED")) {
                    SwingUtilities.invokeLater(() -> callback.onWaiting("Imagen capturada, verificando..."));
                    
                } else if (message.contains("FOUND:")) {
                    SwingUtilities.invokeLater(() -> callback.onWaiting("Huella encontrada, validando..."));
                    
                } else if (message.contains("VERIFY:OK:")) {
                    String[] parts = message.split(":");
                    if (parts.length >= 4) {
                        int id = Integer.parseInt(parts[2].trim());
                        int confidence = Integer.parseInt(parts[3].trim());
                        SwingUtilities.invokeLater(() -> callback.onSuccess(id, confidence));
                        logger.info("✓ Verificación exitosa - ID: {}, Confianza: {}", id, confidence);
                        return;
                    }
                    
                } else if (message.contains("VERIFY:FAIL:NOT_FOUND")) {
                    SwingUtilities.invokeLater(() -> callback.onNotFound());
                    logger.warn("✗ Huella no registrada");
                    return;
                    
                } else if (message.contains("VERIFY:FAIL:") || message.contains("ERROR")) {
                    String[] parts = message.split(":", 3);
                    String error = parts.length >= 3 ? parts[2] : "Error desconocido";
                    SwingUtilities.invokeLater(() -> callback.onError(error));
                    return;
                    
                } else if (message.contains("TIMEOUT")) {
                    SwingUtilities.invokeLater(() -> callback.onError("Tiempo de espera agotado"));
                    return;
                }
            }
            
            SwingUtilities.invokeLater(() -> callback.onError("Timeout: No se colocó el dedo"));
            
        } catch (Exception e) {
            logger.error("Error escuchando verificación: {}", e.getMessage());
            SwingUtilities.invokeLater(() -> callback.onError("Error: " + e.getMessage()));
        }
    }
    
    public CompletableFuture<VerifyResult> verifyFingerprint() {
        return CompletableFuture.supplyAsync(() -> {
            if (!isConnected()) {
                logger.error("No hay conexión con Arduino");
                return new VerifyResult(false, -1, 0, "No hay conexión");
            }
            
            try {
                logger.info("Iniciando verificación de huella");
                
                serialPortManager.clearBuffer();
                Thread.sleep(100);
                
                boolean sent = serialPortManager.sendCommandNoResponse("VERIFY");
                
                if (!sent) {
                    return new VerifyResult(false, -1, 0, "Error al enviar comando");
                }
                
                Thread.sleep(200);
                
                return listenVerifyProcessSync();
                
            } catch (Exception e) {
                logger.error("Error en verificación: {}", e.getMessage(), e);
                return new VerifyResult(false, -1, 0, "Error: " + e.getMessage());
            }
        });
    }
    
    private VerifyResult listenVerifyProcessSync() {
        try {
            long startTime = System.currentTimeMillis();
            long timeout = 30000;
            
            while (System.currentTimeMillis() - startTime < timeout) {
                String message = serialPortManager.readLine(500);
                
                if (message == null || message.isEmpty()) {
                    continue;
                }
                
                logger.debug("Verify msg: {}", message);
                
                if (message.contains("VERIFY:OK:")) {
                    String[] parts = message.split(":");
                    if (parts.length >= 4) {
                        int id = Integer.parseInt(parts[2].trim());
                        int confidence = Integer.parseInt(parts[3].trim());
                        logger.info("✓ Verificación exitosa - ID: {}, Confianza: {}", id, confidence);
                        return new VerifyResult(true, id, confidence, "Huella reconocida");
                    }
                } else if (message.contains("VERIFY:FAIL:NOT_FOUND")) {
                    logger.warn("✗ Huella no registrada");
                    return new VerifyResult(false, -1, 0, "Huella no registrada");
                } else if (message.contains("VERIFY:FAIL:") || message.contains("ERROR")) {
                    String[] parts = message.split(":", 3);
                    String error = parts.length >= 3 ? parts[2] : "Error desconocido";
                    return new VerifyResult(false, -1, 0, error);
                } else if (message.contains("TIMEOUT")) {
                    return new VerifyResult(false, -1, 0, "Tiempo de espera agotado");
                }
            }
            
            return new VerifyResult(false, -1, 0, "Timeout: No se colocó el dedo");
            
        } catch (Exception e) {
            logger.error("Error escuchando verificación: {}", e.getMessage());
            return new VerifyResult(false, -1, 0, "Error: " + e.getMessage());
        }
    }
    
    public CompletableFuture<Boolean> deleteFingerprint(int id) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isConnected()) {
                logger.error("No hay conexión con Arduino");
                return false;
            }
            
            try {
                logger.info("Eliminando huella ID: {}", id);
                
                serialPortManager.clearBuffer();
                Thread.sleep(50);
                
                String response = serialPortManager.sendCommand("DELETE:" + id, COMMAND_TIMEOUT);
                
                if (response != null && response.contains("DELETE:OK:")) {
                    logger.info("✓ Huella {} eliminada correctamente", id);
                    return true;
                } else {
                    logger.error("✗ Error al eliminar huella {}", id);
                    return false;
                }
                
            } catch (Exception e) {
                logger.error("Error eliminando huella: {}", e.getMessage(), e);
                return false;
            }
        });
    }
    
    public CompletableFuture<Boolean> clearDatabase() {
        return CompletableFuture.supplyAsync(() -> {
            if (!isConnected()) {
                logger.error("No hay conexión con Arduino");
                return false;
            }
            
            try {
                logger.info("Borrando base de datos del sensor");
                
                serialPortManager.clearBuffer();
                Thread.sleep(50);
                
                String response = serialPortManager.sendCommand("CLEAR", 5000);
                
                if (response != null && response.contains("CLEAR:OK")) {
                    logger.info("✓ Base de datos borrada correctamente");
                    return true;
                } else {
                    logger.error("✗ Error al borrar base de datos");
                    return false;
                }
                
            } catch (Exception e) {
                logger.error("Error borrando base de datos: {}", e.getMessage(), e);
                return false;
            }
        });
    }
    
    public CompletableFuture<Boolean> testSensor() {
        return CompletableFuture.supplyAsync(() -> {
            if (!isConnected()) {
                logger.error("No hay conexión con Arduino");
                return false;
            }
            
            try {
                logger.info("Probando sensor");
                
                serialPortManager.clearBuffer();
                Thread.sleep(50);
                
                String response = serialPortManager.sendCommand("TEST", 20000);
                
                if (response != null && response.contains("TEST:OK")) {
                    logger.info("✓ Sensor funcionando correctamente");
                    return true;
                } else {
                    logger.error("✗ Sensor no responde");
                    return false;
                }
                
            } catch (Exception e) {
                logger.error("Error probando sensor: {}", e.getMessage(), e);
                return false;
            }
        });
    }
    
    public static class EnrollResult {
        public final boolean success;
        public final String message;
        public final int id;
        
        public EnrollResult(boolean success, String message) {
            this(success, message, -1);
        }
        
        public EnrollResult(boolean success, String message, int id) {
            this.success = success;
            this.message = message;
            this.id = id;
        }
    }
    
    public static class VerifyResult {
        public final boolean success;
        public final int id;
        public final int confidence;
        public final String message;
        
        public VerifyResult(boolean success, int id, int confidence, String message) {
            this.success = success;
            this.id = id;
            this.confidence = confidence;
            this.message = message;
        }
    }
    
    public static class SensorInfo {
        public int capacity;
        public int templateCount;
        public int sensorBaudrate;
    }
    
    public interface EnrollCallback {
        void onProgress(String message);
        void onSuccess(int id);
        void onError(String error);
    }
    
    public interface VerifyCallback {
        void onWaiting(String message);
        void onSuccess(int fingerprintId, int confidence);
        void onNotFound();
        void onError(String error);
    }
}
