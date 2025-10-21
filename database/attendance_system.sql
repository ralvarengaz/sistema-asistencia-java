-- ============================================
-- SCHEMA: Sistema de Asistencia Biométrico
-- Base de Datos: PostgreSQL 12+
-- Versión: 1.0 - Para pgAdmin
-- Fecha: 2025-10-20
-- ============================================

-- IMPORTANTE: Este script debe ejecutarse EN DOS PASOS
-- PASO 1: Crear la base de datos (ejecutar solo estas líneas primero)
-- PASO 2: Conectarse a attendance_system y ejecutar el resto

-- ============================================
-- PASO 1: CREAR BASE DE DATOS
-- Ejecuta SOLO estas líneas primero, luego conéctate a la BD
-- ============================================

/*
CREATE DATABASE attendance_system
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;
*/

-- ============================================
-- PASO 2: EJECUTAR DESDE AQUÍ
-- Después de crear la BD, conéctate a ella y ejecuta desde aquí
-- ============================================

-- Habilitar extensiones
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Configurar esquema
SET search_path TO public;

-- ============================================
-- TABLA: roles
-- ============================================
CREATE TABLE IF NOT EXISTS roles (
    id_rol SERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion TEXT,
    permisos TEXT[],
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE roles IS 'Roles y permisos del sistema';
COMMENT ON COLUMN roles.permisos IS 'Array de permisos: ["users.create", "reports.view"]';

-- ============================================
-- TABLA: departamentos
-- ============================================
CREATE TABLE IF NOT EXISTS departamentos (
    id_departamento SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT,
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE departamentos IS 'Departamentos de la organización';

-- ============================================
-- TABLA: usuarios
-- ============================================
CREATE TABLE IF NOT EXISTS usuarios (
    id_usuario SERIAL PRIMARY KEY,
    dni VARCHAR(20) NOT NULL UNIQUE,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE,
    telefono VARCHAR(20),
    id_rol INTEGER REFERENCES roles(id_rol) ON DELETE SET NULL,
    id_departamento INTEGER REFERENCES departamentos(id_departamento) ON DELETE SET NULL,
    fingerprint_id INTEGER UNIQUE CHECK (fingerprint_id >= 1 AND fingerprint_id <= 255),
    foto_url VARCHAR(255),
    direccion TEXT,
    fecha_nacimiento DATE,
    genero VARCHAR(10) CHECK (genero IN ('M', 'F', 'Otro')),
    activo BOOLEAN DEFAULT TRUE,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(100),
    observaciones TEXT
);

COMMENT ON TABLE usuarios IS 'Empleados y personal del sistema';
COMMENT ON COLUMN usuarios.fingerprint_id IS 'ID único en el sensor (1-255)';

-- ============================================
-- TABLA: asistencias
-- ============================================
CREATE TABLE IF NOT EXISTS asistencias (
    id_asistencia SERIAL PRIMARY KEY,
    id_usuario INTEGER NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tipo_marcacion VARCHAR(20) NOT NULL DEFAULT 'ENTRADA' 
        CHECK (tipo_marcacion IN ('ENTRADA', 'SALIDA', 'ENTRADA_BREAK', 'SALIDA_BREAK')),
    confidence_score INTEGER CHECK (confidence_score >= 0 AND confidence_score <= 255),
    metodo VARCHAR(20) DEFAULT 'FINGERPRINT' 
        CHECK (metodo IN ('FINGERPRINT', 'MANUAL', 'RFID', 'FACIAL')),
    latitud DECIMAL(10, 8),
    longitud DECIMAL(11, 8),
    ip_address VARCHAR(45),
    dispositivo VARCHAR(100),
    observaciones TEXT,
    registrado_por VARCHAR(100),
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE asistencias IS 'Registro de entradas/salidas del personal';
COMMENT ON COLUMN asistencias.confidence_score IS 'Nivel de confianza del sensor (0-255)';

-- ============================================
-- TABLA: usuarios_sistema
-- ============================================
CREATE TABLE IF NOT EXISTS usuarios_sistema (
    id_usuario_sistema SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    id_usuario INTEGER REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    ultimo_acceso TIMESTAMP,
    intentos_fallidos INTEGER DEFAULT 0,
    bloqueado BOOLEAN DEFAULT FALSE,
    fecha_bloqueo TIMESTAMP,
    token_sesion VARCHAR(255),
    expiracion_token TIMESTAMP,
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE usuarios_sistema IS 'Credenciales de acceso al sistema';
COMMENT ON COLUMN usuarios_sistema.password_hash IS 'Hash BCrypt de la contraseña';

-- ============================================
-- TABLA: configuracion
-- ============================================
CREATE TABLE IF NOT EXISTS configuracion (
    id_config SERIAL PRIMARY KEY,
    clave VARCHAR(100) NOT NULL UNIQUE,
    valor TEXT NOT NULL,
    descripcion TEXT,
    tipo_dato VARCHAR(20) DEFAULT 'STRING' 
        CHECK (tipo_dato IN ('STRING', 'INTEGER', 'BOOLEAN', 'DECIMAL', 'JSON')),
    categoria VARCHAR(50),
    modificable BOOLEAN DEFAULT TRUE,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modificado_por VARCHAR(100)
);

COMMENT ON TABLE configuracion IS 'Configuración general del sistema';

-- ============================================
-- TABLA: logs_sistema
-- ============================================
CREATE TABLE IF NOT EXISTS logs_sistema (
    id_log SERIAL PRIMARY KEY,
    nivel VARCHAR(20) NOT NULL DEFAULT 'INFO' 
        CHECK (nivel IN ('DEBUG', 'INFO', 'WARNING', 'ERROR', 'CRITICAL')),
    modulo VARCHAR(100),
    mensaje TEXT NOT NULL,
    stack_trace TEXT,
    usuario VARCHAR(100),
    ip_address VARCHAR(45),
    dispositivo VARCHAR(100),
    fecha_hora TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE logs_sistema IS 'Log de eventos y errores del sistema';

-- ============================================
-- TABLA: horarios_trabajo
-- ============================================
CREATE TABLE IF NOT EXISTS horarios_trabajo (
    id_horario SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    hora_entrada TIME NOT NULL,
    hora_salida TIME NOT NULL,
    hora_entrada_break TIME,
    hora_salida_break TIME,
    tolerancia_minutos INTEGER DEFAULT 15,
    dias_semana VARCHAR(3)[] NOT NULL,
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE horarios_trabajo IS 'Definición de horarios de trabajo';
COMMENT ON COLUMN horarios_trabajo.dias_semana IS 'Array de días: LUN, MAR, MIE, JUE, VIE, SAB, DOM';

-- ============================================
-- TABLA: usuarios_horarios
-- ============================================
CREATE TABLE IF NOT EXISTS usuarios_horarios (
    id_usuario_horario SERIAL PRIMARY KEY,
    id_usuario INTEGER NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    id_horario INTEGER NOT NULL REFERENCES horarios_trabajo(id_horario) ON DELETE CASCADE,
    fecha_inicio DATE NOT NULL DEFAULT CURRENT_DATE,
    fecha_fin DATE,
    activo BOOLEAN DEFAULT TRUE,
    UNIQUE(id_usuario, id_horario, fecha_inicio)
);

COMMENT ON TABLE usuarios_horarios IS 'Relación entre usuarios y sus horarios';

-- ============================================
-- TABLA: justificaciones
-- ============================================
CREATE TABLE IF NOT EXISTS justificaciones (
    id_justificacion SERIAL PRIMARY KEY,
    id_usuario INTEGER NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    tipo VARCHAR(50) NOT NULL CHECK (tipo IN ('PERMISO', 'VACACIONES', 'ENFERMEDAD', 'OTRO')),
    motivo TEXT NOT NULL,
    documento_url VARCHAR(255),
    estado VARCHAR(20) DEFAULT 'PENDIENTE' 
        CHECK (estado IN ('PENDIENTE', 'APROBADO', 'RECHAZADO')),
    aprobado_por INTEGER REFERENCES usuarios_sistema(id_usuario_sistema),
    fecha_aprobacion TIMESTAMP,
    observaciones TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE justificaciones IS 'Justificaciones de ausencias';

-- ============================================
-- TABLA: reportes_generados
-- ============================================
CREATE TABLE IF NOT EXISTS reportes_generados (
    id_reporte SERIAL PRIMARY KEY,
    nombre_reporte VARCHAR(200) NOT NULL,
    tipo_reporte VARCHAR(50) NOT NULL,
    formato VARCHAR(10) NOT NULL CHECK (formato IN ('PDF', 'XLSX', 'CSV')),
    parametros JSONB,
    ruta_archivo VARCHAR(500),
    generado_por INTEGER REFERENCES usuarios_sistema(id_usuario_sistema),
    fecha_generacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tamanio_kb INTEGER
);

COMMENT ON TABLE reportes_generados IS 'Historial de reportes generados';

-- ============================================
-- ÍNDICES PARA OPTIMIZACIÓN
-- ============================================

CREATE INDEX IF NOT EXISTS idx_asistencias_usuario ON asistencias(id_usuario);
CREATE INDEX IF NOT EXISTS idx_asistencias_fecha ON asistencias(fecha_hora DESC);
CREATE INDEX IF NOT EXISTS idx_asistencias_tipo ON asistencias(tipo_marcacion);
CREATE INDEX IF NOT EXISTS idx_asistencias_usuario_fecha ON asistencias(id_usuario, fecha_hora DESC);

CREATE INDEX IF NOT EXISTS idx_usuarios_dni ON usuarios(dni);
CREATE INDEX IF NOT EXISTS idx_usuarios_fingerprint ON usuarios(fingerprint_id) WHERE fingerprint_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_usuarios_activo ON usuarios(activo) WHERE activo = TRUE;
CREATE INDEX IF NOT EXISTS idx_usuarios_email ON usuarios(email);

CREATE INDEX IF NOT EXISTS idx_logs_fecha ON logs_sistema(fecha_hora DESC);
CREATE INDEX IF NOT EXISTS idx_logs_nivel ON logs_sistema(nivel);
CREATE INDEX IF NOT EXISTS idx_logs_modulo ON logs_sistema(modulo);

CREATE INDEX IF NOT EXISTS idx_usuarios_sistema_username ON usuarios_sistema(username);
CREATE INDEX IF NOT EXISTS idx_usuarios_sistema_token ON usuarios_sistema(token_sesion);

CREATE INDEX IF NOT EXISTS idx_config_clave ON configuracion(clave);
CREATE INDEX IF NOT EXISTS idx_config_categoria ON configuracion(categoria);

-- ============================================
-- FUNCIONES AUXILIARES
-- ============================================

CREATE OR REPLACE FUNCTION update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.fecha_modificacion = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION log_cambios()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO logs_sistema (nivel, modulo, mensaje, usuario)
    VALUES ('INFO', TG_TABLE_NAME, 
            TG_OP || ' en ' || TG_TABLE_NAME || ' ID: ' || COALESCE(NEW.id_usuario::TEXT, OLD.id_usuario::TEXT),
            CURRENT_USER);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- TRIGGERS
-- ============================================

DROP TRIGGER IF EXISTS update_usuarios_modtime ON usuarios;
CREATE TRIGGER update_usuarios_modtime
    BEFORE UPDATE ON usuarios
    FOR EACH ROW
    EXECUTE FUNCTION update_modified_column();

DROP TRIGGER IF EXISTS update_usuarios_sistema_modtime ON usuarios_sistema;
CREATE TRIGGER update_usuarios_sistema_modtime
    BEFORE UPDATE ON usuarios_sistema
    FOR EACH ROW
    EXECUTE FUNCTION update_modified_column();

DROP TRIGGER IF EXISTS update_roles_modtime ON roles;
CREATE TRIGGER update_roles_modtime
    BEFORE UPDATE ON roles
    FOR EACH ROW
    EXECUTE FUNCTION update_modified_column();

DROP TRIGGER IF EXISTS update_horarios_modtime ON horarios_trabajo;
CREATE TRIGGER update_horarios_modtime
    BEFORE UPDATE ON horarios_trabajo
    FOR EACH ROW
    EXECUTE FUNCTION update_modified_column();

-- ============================================
-- VISTAS ÚTILES
-- ============================================

CREATE OR REPLACE VIEW v_asistencias_hoy AS
SELECT 
    a.id_asistencia,
    u.id_usuario,
    u.dni,
    u.nombres || ' ' || u.apellidos AS nombre_completo,
    u.email,
    d.nombre AS departamento,
    a.fecha_hora,
    a.tipo_marcacion,
    a.confidence_score,
    a.metodo,
    a.observaciones
FROM asistencias a
INNER JOIN usuarios u ON a.id_usuario = u.id_usuario
LEFT JOIN departamentos d ON u.id_departamento = d.id_departamento
WHERE DATE(a.fecha_hora) = CURRENT_DATE
ORDER BY a.fecha_hora DESC;

CREATE OR REPLACE VIEW v_usuarios_activos AS
SELECT 
    u.id_usuario,
    u.dni,
    u.nombres,
    u.apellidos,
    u.nombres || ' ' || u.apellidos AS nombre_completo,
    u.email,
    u.telefono,
    u.fingerprint_id,
    r.nombre AS rol,
    d.nombre AS departamento,
    u.fecha_registro,
    CASE 
        WHEN u.fingerprint_id IS NOT NULL THEN 'Sí'
        ELSE 'No'
    END AS huella_registrada
FROM usuarios u
LEFT JOIN roles r ON u.id_rol = r.id_rol
LEFT JOIN departamentos d ON u.id_departamento = d.id_departamento
WHERE u.activo = TRUE
ORDER BY u.apellidos, u.nombres;

CREATE OR REPLACE VIEW v_resumen_asistencias AS
SELECT 
    u.id_usuario,
    u.dni,
    u.nombres || ' ' || u.apellidos AS nombre_completo,
    COUNT(a.id_asistencia) AS total_marcaciones,
    COUNT(DISTINCT DATE(a.fecha_hora)) AS dias_asistidos,
    MAX(a.fecha_hora) AS ultima_marcacion,
    COUNT(CASE WHEN a.tipo_marcacion = 'ENTRADA' THEN 1 END) AS total_entradas,
    COUNT(CASE WHEN a.tipo_marcacion = 'SALIDA' THEN 1 END) AS total_salidas
FROM usuarios u
LEFT JOIN asistencias a ON u.id_usuario = a.id_usuario
WHERE u.activo = TRUE
GROUP BY u.id_usuario, u.dni, u.nombres, u.apellidos
ORDER BY total_marcaciones DESC;

CREATE OR REPLACE VIEW v_errores_criticos AS
SELECT 
    id_log,
    nivel,
    modulo,
    mensaje,
    usuario,
    fecha_hora
FROM logs_sistema
WHERE nivel IN ('ERROR', 'CRITICAL')
ORDER BY fecha_hora DESC
LIMIT 100;

-- ============================================
-- PROCEDIMIENTOS ALMACENADOS
-- ============================================

CREATE OR REPLACE FUNCTION sp_registrar_asistencia(
    p_fingerprint_id INTEGER,
    p_confidence INTEGER,
    p_tipo_marcacion VARCHAR(20) DEFAULT 'ENTRADA'
)
RETURNS TABLE(
    success BOOLEAN,
    message TEXT,
    id_usuario INTEGER,
    usuario_nombre TEXT,
    id_asistencia INTEGER
) AS $$
DECLARE
    v_id_usuario INTEGER;
    v_nombre_completo TEXT;
    v_id_asistencia INTEGER;
BEGIN
    SELECT u.id_usuario, u.nombres || ' ' || u.apellidos
    INTO v_id_usuario, v_nombre_completo
    FROM usuarios u
    WHERE u.fingerprint_id = p_fingerprint_id AND u.activo = TRUE;
    
    IF v_id_usuario IS NULL THEN
        RETURN QUERY SELECT 
            FALSE, 
            'Usuario no encontrado o inactivo'::TEXT, 
            NULL::INTEGER,
            NULL::TEXT,
            NULL::INTEGER;
        RETURN;
    END IF;
    
    INSERT INTO asistencias (id_usuario, tipo_marcacion, confidence_score, metodo)
    VALUES (v_id_usuario, p_tipo_marcacion, p_confidence, 'FINGERPRINT')
    RETURNING asistencias.id_asistencia INTO v_id_asistencia;
    
    INSERT INTO logs_sistema (nivel, modulo, mensaje, usuario)
    VALUES ('INFO', 'ASISTENCIA', 
            'Asistencia registrada para usuario ID: ' || v_id_usuario, 
            v_nombre_completo);
    
    RETURN QUERY SELECT 
        TRUE, 
        'Asistencia registrada correctamente'::TEXT, 
        v_id_usuario,
        v_nombre_completo,
        v_id_asistencia;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION sp_reporte_mensual(
    p_mes INTEGER DEFAULT EXTRACT(MONTH FROM CURRENT_DATE)::INTEGER,
    p_anio INTEGER DEFAULT EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER
)
RETURNS TABLE(
    dni VARCHAR,
    nombre_completo TEXT,
    departamento TEXT,
    total_asistencias BIGINT,
    dias_trabajados BIGINT,
    entradas_tarde BIGINT,
    salidas_temprano BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        u.dni,
        u.nombres || ' ' || u.apellidos AS nombre_completo,
        COALESCE(d.nombre, 'Sin Departamento') AS departamento,
        COUNT(a.id_asistencia) AS total_asistencias,
        COUNT(DISTINCT DATE(a.fecha_hora)) AS dias_trabajados,
        COUNT(CASE 
            WHEN EXTRACT(HOUR FROM a.fecha_hora) >= 9 
            AND a.tipo_marcacion = 'ENTRADA' THEN 1 
        END) AS entradas_tarde,
        COUNT(CASE 
            WHEN EXTRACT(HOUR FROM a.fecha_hora) < 17 
            AND a.tipo_marcacion = 'SALIDA' THEN 1 
        END) AS salidas_temprano
    FROM usuarios u
    LEFT JOIN departamentos d ON u.id_departamento = d.id_departamento
    LEFT JOIN asistencias a ON u.id_usuario = a.id_usuario
        AND EXTRACT(MONTH FROM a.fecha_hora) = p_mes
        AND EXTRACT(YEAR FROM a.fecha_hora) = p_anio
    WHERE u.activo = TRUE
    GROUP BY u.dni, u.nombres, u.apellidos, d.nombre
    ORDER BY u.apellidos, u.nombres;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION sp_limpiar_logs_antiguos(
    p_dias_antiguedad INTEGER DEFAULT 90
)
RETURNS INTEGER AS $$
DECLARE
    v_registros_eliminados INTEGER;
BEGIN
    DELETE FROM logs_sistema
    WHERE fecha_hora < CURRENT_TIMESTAMP - (p_dias_antiguedad || ' days')::INTERVAL
    AND nivel IN ('DEBUG', 'INFO');
    
    GET DIAGNOSTICS v_registros_eliminados = ROW_COUNT;
    
    INSERT INTO logs_sistema (nivel, modulo, mensaje)
    VALUES ('INFO', 'MANTENIMIENTO', 
            'Logs eliminados: ' || v_registros_eliminados || ' registros');
    
    RETURN v_registros_eliminados;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- DATOS INICIALES
-- ============================================

INSERT INTO roles (nombre, descripcion, permisos) VALUES
('ADMINISTRADOR', 'Acceso total al sistema', 
    ARRAY['users.create', 'users.edit', 'users.delete', 'reports.all', 'config.edit']),
('SUPERVISOR', 'Gestión de personal y reportes', 
    ARRAY['users.view', 'users.edit', 'reports.view', 'reports.create']),
('RECURSOS_HUMANOS', 'Gestión de RRHH', 
    ARRAY['users.view', 'users.edit', 'reports.all', 'justifications.manage']),
('EMPLEADO', 'Solo registro de asistencia', 
    ARRAY['attendance.mark', 'attendance.view_own'])
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO departamentos (nombre, descripcion) VALUES
('Administración', 'Departamento administrativo'),
('Tecnología', 'Departamento de TI'),
('Recursos Humanos', 'Gestión de personal'),
('Operaciones', 'Departamento operativo'),
('Ventas', 'Departamento comercial')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO configuracion (clave, valor, descripcion, tipo_dato, categoria) VALUES
('PUERTO_ARDUINO', 'COM3', 'Puerto serial del Arduino', 'STRING', 'HARDWARE'),
('BAUDRATE', '115200', 'Velocidad de comunicación serial', 'INTEGER', 'HARDWARE'),
('TIMEOUT_SENSOR', '10', 'Timeout en segundos para el sensor', 'INTEGER', 'HARDWARE'),
('MAX_INTENTOS_LOGIN', '3', 'Máximo de intentos de login fallidos', 'INTEGER', 'SEGURIDAD'),
('TIEMPO_BLOQUEO_MINUTOS', '30', 'Tiempo de bloqueo tras intentos fallidos', 'INTEGER', 'SEGURIDAD'),
('HABILITAR_BUZZER', 'true', 'Habilitar feedback sonoro', 'BOOLEAN', 'HARDWARE'),
('UMBRAL_CONFIDENCE', '50', 'Umbral mínimo de confianza (0-255)', 'INTEGER', 'SENSOR'),
('BACKUP_AUTOMATICO', 'true', 'Realizar backup diario automático', 'BOOLEAN', 'SISTEMA'),
('HORA_ENTRADA_ESTANDAR', '08:00', 'Hora de entrada estándar', 'STRING', 'HORARIOS'),
('HORA_SALIDA_ESTANDAR', '17:00', 'Hora de salida estándar', 'STRING', 'HORARIOS'),
('TOLERANCIA_MINUTOS', '15', 'Minutos de tolerancia para entrada', 'INTEGER', 'HORARIOS'),
('EMPRESA_NOMBRE', 'Mi Empresa S.A.', 'Nombre de la empresa', 'STRING', 'GENERAL'),
('EMPRESA_RUC', '00000000000', 'RUC de la empresa', 'STRING', 'GENERAL')
ON CONFLICT (clave) DO NOTHING;

INSERT INTO horarios_trabajo (nombre, descripcion, hora_entrada, hora_salida, tolerancia_minutos, dias_semana) VALUES
('Turno Normal', 'Horario estándar de oficina', '08:00:00', '17:00:00', 15, 
    ARRAY['LUN','MAR','MIE','JUE','VIE']),
('Turno Mañana', 'Turno matutino', '06:00:00', '14:00:00', 10, 
    ARRAY['LUN','MAR','MIE','JUE','VIE','SAB']),
('Turno Tarde', 'Turno vespertino', '14:00:00', '22:00:00', 10, 
    ARRAY['LUN','MAR','MIE','JUE','VIE','SAB'])
ON CONFLICT DO NOTHING;

INSERT INTO usuarios (dni, nombres, apellidos, email, id_rol, id_departamento, activo) 
VALUES ('00000000', 'Administrador', 'Sistema', 'admin@sistema.com', 
    (SELECT id_rol FROM roles WHERE nombre = 'ADMINISTRADOR'), 
    (SELECT id_departamento FROM departamentos WHERE nombre = 'Administración'),
    TRUE)
ON CONFLICT (dni) DO NOTHING;

INSERT INTO usuarios_sistema (username, password_hash, id_usuario, activo) 
VALUES ('admin', 
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 
    (SELECT id_usuario FROM usuarios WHERE dni = '00000000'),
    TRUE)
ON CONFLICT (username) DO NOTHING;

-- ============================================
-- PERMISOS
-- ============================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_user WHERE usename = 'app_attendance') THEN
        CREATE USER app_attendance WITH PASSWORD 'SecurePass2025!';
    END IF;
END
$$;

GRANT CONNECT ON DATABASE attendance_system TO app_attendance;
GRANT USAGE ON SCHEMA public TO app_attendance;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO app_attendance;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_attendance;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO app_attendance;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_attendance;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT USAGE, SELECT ON SEQUENCES TO app_attendance;

-- ============================================
-- LOG FINAL
-- ============================================

INSERT INTO logs_sistema (nivel, modulo, mensaje, usuario)
VALUES ('INFO', 'INSTALACION', 'Base de datos inicializada correctamente', 'postgres');

SELECT 'Base de datos creada exitosamente' AS status;