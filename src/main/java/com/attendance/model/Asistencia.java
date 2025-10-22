package com.attendance.model;

import java.time.LocalDateTime;

/**
 * Entidad Asistencia - Representa un registro de entrada/salida
 * 
 * @author Sistema Biométrico
 * @version 1.0
 */
public class Asistencia {
    
    private Integer idAsistencia;
    private Integer idUsuario;
    private LocalDateTime fechaHora;
    private String tipoMarcacion;  // ENTRADA, SALIDA, ENTRADA_BREAK, SALIDA_BREAK
    private Integer confidenceScore;
    private String metodo;  // FINGERPRINT, MANUAL, RFID, FACIAL
    private Double latitud;
    private Double longitud;
    private String ipAddress;
    private String dispositivo;
    private String observaciones;
    private String registradoPor;
    private LocalDateTime fechaRegistro;
    
    // Campos relacionados (joins)
    private String nombreUsuario;
    private String dniUsuario;
    private String departamentoUsuario;
    
    // Constructores
    public Asistencia() {
        this.fechaHora = LocalDateTime.now();
        this.fechaRegistro = LocalDateTime.now();
        this.metodo = "FINGERPRINT";
    }
    
    public Asistencia(Integer idUsuario, String tipoMarcacion) {
        this();
        this.idUsuario = idUsuario;
        this.tipoMarcacion = tipoMarcacion;
    }
    
    public Asistencia(Integer idUsuario, String tipoMarcacion, Integer confidenceScore) {
        this(idUsuario, tipoMarcacion);
        this.confidenceScore = confidenceScore;
    }
    
    // Getters y Setters
    public Integer getIdAsistencia() {
        return idAsistencia;
    }
    
    public void setIdAsistencia(Integer idAsistencia) {
        this.idAsistencia = idAsistencia;
    }
    
    public Integer getIdUsuario() {
        return idUsuario;
    }
    
    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }
    
    public LocalDateTime getFechaHora() {
        return fechaHora;
    }
    
    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }
    
    public String getTipoMarcacion() {
        return tipoMarcacion;
    }
    
    public void setTipoMarcacion(String tipoMarcacion) {
        this.tipoMarcacion = tipoMarcacion;
    }
    
    public boolean isEntrada() {
        return "ENTRADA".equals(tipoMarcacion) || "ENTRADA_BREAK".equals(tipoMarcacion);
    }
    
    public boolean isSalida() {
        return "SALIDA".equals(tipoMarcacion) || "SALIDA_BREAK".equals(tipoMarcacion);
    }
    
    public Integer getConfidenceScore() {
        return confidenceScore;
    }
    
    public void setConfidenceScore(Integer confidenceScore) {
        this.confidenceScore = confidenceScore;
    }
    
    public String getConfidenceLevel() {
        if (confidenceScore == null) return "Desconocido";
        if (confidenceScore > 150) return "Alta";
        if (confidenceScore > 100) return "Media";
        return "Baja";
    }
    
    public String getMetodo() {
        return metodo;
    }
    
    public void setMetodo(String metodo) {
        this.metodo = metodo;
    }
    
    public Double getLatitud() {
        return latitud;
    }
    
    public void setLatitud(Double latitud) {
        this.latitud = latitud;
    }
    
    public Double getLongitud() {
        return longitud;
    }
    
    public void setLongitud(Double longitud) {
        this.longitud = longitud;
    }
    
    public boolean tieneUbicacion() {
        return latitud != null && longitud != null;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getDispositivo() {
        return dispositivo;
    }
    
    public void setDispositivo(String dispositivo) {
        this.dispositivo = dispositivo;
    }
    
    public String getObservaciones() {
        return observaciones;
    }
    
    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
    
    public String getRegistradoPor() {
        return registradoPor;
    }
    
    public void setRegistradoPor(String registradoPor) {
        this.registradoPor = registradoPor;
    }
    
    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }
    
    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
    
    // Campos relacionados
    public String getNombreUsuario() {
        return nombreUsuario;
    }
    
    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }
    
    public String getDniUsuario() {
        return dniUsuario;
    }
    
    public void setDniUsuario(String dniUsuario) {
        this.dniUsuario = dniUsuario;
    }
    
    public String getDepartamentoUsuario() {
        return departamentoUsuario;
    }
    
    public void setDepartamentoUsuario(String departamentoUsuario) {
        this.departamentoUsuario = departamentoUsuario;
    }
    
    @Override
    public String toString() {
        return String.format("Asistencia[id=%d, usuario=%s, tipo=%s, fecha=%s]",
            idAsistencia,
            nombreUsuario != null ? nombreUsuario : "ID:" + idUsuario,
            tipoMarcacion,
            fechaHora);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Asistencia that = (Asistencia) o;
        return idAsistencia != null && idAsistencia.equals(that.idAsistencia);
    }
    
    @Override
    public int hashCode() {
        return idAsistencia != null ? idAsistencia.hashCode() : 0;
    }
}