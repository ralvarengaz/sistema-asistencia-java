package com.attendance.util;

import com.fazecast.jSerialComm.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestor de comunicación con puerto serial (Arduino)
 * 
 * @author Sistema Biométrico
 * @version 1.0
 */
public class SerialPortManager {
    
    private static final Logger logger = LoggerFactory.getLogger(SerialPortManager.class);
    
    private SerialPort serialPort;
    private BufferedReader reader;
    private OutputStream outputStream;
    private boolean connected = false;
    
    /**
     * Obtiene la lista de puertos COM disponibles
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
     * Conecta al puerto serial especificado
     */
    public boolean connect(String portName, int baudRate) {
        try {
            // Cerrar conexión anterior si existe
            if (connected) {
                disconnect();
            }
            
            serialPort = SerialPort.getCommPort(portName);
            serialPort.setBaudRate(baudRate);
            serialPort.setNumDataBits(8);
            serialPort.setNumStopBits(1);
            serialPort.setParity(SerialPort.NO_PARITY);
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
            
            if (serialPort.openPort()) {
                reader = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
                outputStream = serialPort.getOutputStream();
                connected = true;
                
                // Esperar a que Arduino se inicialice
                Thread.sleep(2000);
                
                logger.info("Conectado exitosamente al puerto: {} @ {} baud", portName, baudRate);
                return true;
            } else {
                logger.error("No se pudo abrir el puerto: {}", portName);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error al conectar con el puerto serial", e);
            return false;
        }
    }
    
    /**
     * Desconecta del puerto serial
     */
    public void disconnect() {
        try {
            if (reader != null) {
                reader.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            if (serialPort != null && serialPort.isOpen()) {
                serialPort.closePort();
            }
            connected = false;
            logger.info("Desconectado del puerto serial");
        } catch (Exception e) {
            logger.error("Error al desconectar del puerto serial", e);
        }
    }
    
    /**
     * Envía un comando al Arduino
     */
    public boolean sendCommand(String command) {
        if (!connected || outputStream == null) {
            logger.error("No hay conexión activa con Arduino");
            return false;
        }
        
        try {
            String commandWithNewline = command + "\n";
            outputStream.write(commandWithNewline.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            logger.debug("Comando enviado: {}", command);
            return true;
        } catch (Exception e) {
            logger.error("Error al enviar comando: {}", command, e);
            return false;
        }
    }
    
    /**
     * Lee una línea desde Arduino
     */
    public String readLine() {
        if (!connected || reader == null) {
            return null;
        }
        
        try {
            if (reader.ready()) {
                String line = reader.readLine();
                if (line != null && !line.isEmpty()) {
                    logger.debug("Respuesta Arduino: {}", line);
                    return line.trim();
                }
            }
        } catch (Exception e) {
            logger.error("Error al leer del puerto serial", e);
        }
        
        return null;
    }
    
    /**
     * Lee línea con timeout
     */
    public String readLine(long timeoutMillis) {
        long startTime = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            String line = readLine();
            if (line != null) {
                return line;
            }
            
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        logger.warn("Timeout esperando respuesta de Arduino");
        return null;
    }
    
    /**
     * Verifica si está conectado
     */
    public boolean isConnected() {
        return connected && serialPort != null && serialPort.isOpen();
    }
    
    /**
     * Obtiene el nombre del puerto actual
     */
    public String getCurrentPort() {
        if (serialPort != null) {
            return serialPort.getSystemPortName();
        }
        return null;
    }
    
    /**
     * Limpia el buffer de entrada
     */
    public void clearBuffer() {
        try {
            while (reader != null && reader.ready()) {
                reader.readLine();
            }
        } catch (Exception e) {
            logger.error("Error al limpiar buffer", e);
        }
    }
}