package com.attendance.util;

import com.fazecast.jSerialComm.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SerialPortManager {
    private static final Logger logger = LoggerFactory.getLogger(SerialPortManager.class);
    
    private SerialPort serialPort;
    private boolean isConnected = false;
    
    /**
     * Lista todos los puertos COM disponibles
     */
    public static List<String> getAvailablePorts() {
        List<String> ports = new ArrayList<>();
        SerialPort[] serialPorts = SerialPort.getCommPorts();
        
        for (SerialPort port : serialPorts) {
            ports.add(port.getSystemPortName());
        }
        
        logger.info("Puertos COM disponibles: {}", ports);
        return ports;
    }
    
    /**
     * Conecta a un puerto serial específico
     */
    public boolean connect(String portName, int baudRate) {
        try {
            // Buscar el puerto
            SerialPort[] ports = SerialPort.getCommPorts();
            serialPort = null;
            
            for (SerialPort port : ports) {
                if (port.getSystemPortName().equals(portName)) {
                    serialPort = port;
                    break;
                }
            }
            
            if (serialPort == null) {
                logger.error("Puerto {} no encontrado", portName);
                return false;
            }
            
            // Configurar puerto
            serialPort.setBaudRate(baudRate);
            serialPort.setNumDataBits(8);
            serialPort.setNumStopBits(1);
            serialPort.setParity(SerialPort.NO_PARITY);
            serialPort.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_SEMI_BLOCKING,
                100,  // Read timeout
                0     // Write timeout
            );
            
            // Abrir puerto
            if (!serialPort.openPort()) {
                logger.error("No se pudo abrir el puerto {}", portName);
                return false;
            }
            
            // Dar tiempo para que el puerto se estabilice
            Thread.sleep(100);
            
            isConnected = true;
            logger.info("Conectado exitosamente al puerto: {} @ {} baud", portName, baudRate);
            return true;
            
        } catch (Exception e) {
            logger.error("Error al conectar al puerto: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Desconecta del puerto serial
     */
    public void disconnect() {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
            logger.info("Desconectado del puerto serial");
        }
        isConnected = false;
        serialPort = null;
    }
    
    /**
     * Verifica si está conectado
     */
    public boolean isConnected() {
        return isConnected && serialPort != null && serialPort.isOpen();
    }
    
    /**
     * Limpia el buffer de entrada
     */
    public void clearBuffer() {
        if (serialPort == null || !serialPort.isOpen()) {
            return;
        }
        
        try {
            int available = serialPort.bytesAvailable();
            if (available > 0) {
                byte[] buffer = new byte[available];
                serialPort.readBytes(buffer, available);
                logger.debug("Buffer limpiado: {} bytes descartados", available);
            }
        } catch (Exception e) {
            logger.warn("Error limpiando buffer: {}", e.getMessage());
        }
    }
    
    /**
     * Envía un comando y espera respuesta
     */
    public String sendCommand(String command, int timeoutMs) {
        if (!isConnected()) {
            logger.error("No hay conexión serial");
            return null;
        }
        
        try {
            // Limpiar buffer antes de enviar
            clearBuffer();
            Thread.sleep(50);
            
            // Enviar comando
            String commandWithNewline = command + "\n";
            byte[] commandBytes = commandWithNewline.getBytes(StandardCharsets.UTF_8);
            
            int bytesWritten = serialPort.writeBytes(commandBytes, commandBytes.length);
            
            if (bytesWritten != commandBytes.length) {
                logger.error("Error: no se enviaron todos los bytes del comando");
                return null;
            }
            
            serialPort.flushIOBuffers();
            logger.debug("Comando enviado: {}", command);
            
            // Esperar y leer respuesta
            Thread.sleep(100);
            return readResponse(timeoutMs);
            
        } catch (Exception e) {
            logger.error("Error enviando comando: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Lee una respuesta del Arduino con timeout
     */
    private String readResponse(int timeoutMs) {
        StringBuilder response = new StringBuilder();
        long startTime = System.currentTimeMillis();
        
        try {
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                if (serialPort.bytesAvailable() > 0) {
                    byte[] buffer = new byte[1024];
                    int bytesRead = serialPort.readBytes(buffer, buffer.length);
                    
                    if (bytesRead > 0) {
                        String chunk = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                        response.append(chunk);
                        
                        // Si encontramos PONG o ERROR, retornar inmediatamente
                        if (response.toString().contains("PONG") || 
                            response.toString().contains("ERROR") ||
                            response.toString().contains("SUCCESS")) {
                            break;
                        }
                    }
                }
                
                Thread.sleep(50);
            }
            
            String result = response.toString().trim();
            if (!result.isEmpty()) {
                logger.debug("Respuesta Arduino: {}", result.length() > 100 ? 
                    result.substring(0, 100) + "..." : result);
            }
            
            return result.isEmpty() ? null : result;
            
        } catch (Exception e) {
            logger.error("Error leyendo respuesta: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Lee una línea del serial con timeout
     */
    public String readLine(int timeoutMs) {
        if (!isConnected()) {
            return null;
        }
        
        try {
            StringBuilder line = new StringBuilder();
            long startTime = System.currentTimeMillis();
            
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                if (serialPort.bytesAvailable() > 0) {
                    byte[] buffer = new byte[1];
                    int bytesRead = serialPort.readBytes(buffer, 1);
                    
                    if (bytesRead > 0) {
                        char c = (char) buffer[0];
                        
                        if (c == '\n') {
                            String result = line.toString().trim();
                            if (!result.isEmpty()) {
                                return result;
                            }
                            line.setLength(0);
                        } else if (c != '\r') {
                            line.append(c);
                        }
                    }
                }
                
                Thread.sleep(10);
            }
            
            // Si hay algo en el buffer al final del timeout, devolverlo
            String result = line.toString().trim();
            return result.isEmpty() ? null : result;
            
        } catch (Exception e) {
            logger.error("Error leyendo línea: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Envía un comando sin esperar respuesta
     */
    public boolean sendCommandNoResponse(String command) {
        if (!isConnected()) {
            logger.error("No hay conexión serial");
            return false;
        }
        
        try {
            String commandWithNewline = command + "\n";
            byte[] commandBytes = commandWithNewline.getBytes(StandardCharsets.UTF_8);
            
            int bytesWritten = serialPort.writeBytes(commandBytes, commandBytes.length);
            serialPort.flushIOBuffers();
            
            logger.debug("Comando enviado (sin esperar respuesta): {}", command);
            return bytesWritten == commandBytes.length;
            
        } catch (Exception e) {
            logger.error("Error enviando comando: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Lee todos los datos disponibles
     */
    public String readAvailable() {
        if (!isConnected()) {
            return null;
        }
        
        try {
            int available = serialPort.bytesAvailable();
            if (available > 0) {
                byte[] buffer = new byte[available];
                int bytesRead = serialPort.readBytes(buffer, available);
                
                if (bytesRead > 0) {
                    return new String(buffer, 0, bytesRead, StandardCharsets.UTF_8).trim();
                }
            }
            return null;
            
        } catch (Exception e) {
            logger.error("Error leyendo datos disponibles: {}", e.getMessage());
            return null;
        }
    }
}