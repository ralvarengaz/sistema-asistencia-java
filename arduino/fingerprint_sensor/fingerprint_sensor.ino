/*
 * ============================================
 * Sistema de Control de Asistencia Biométrico
 * Firmware para Arduino UNO + Sensor DY50
 * ============================================
 * Versión: 2.0
 * Fecha: 2025-10-20
 * Autor: Sistema Biométrico Java
 * 
 * Hardware:
 * - Arduino UNO
 * - Sensor de huella DY50 (compatible R30x)
 * - Buzzer en pin 6 (opcional)
 * - LED Rojo en pin 7
 * - LED Verde en pin 8
 * - LED Azul en pin 9
 * 
 * Protocolo de comunicación:
 * - Baudrate: 115200
 * - Formato: COMANDO:PARAMETROS
 * - Respuestas: TIPO:CODIGO:MENSAJE
 */

#include <Adafruit_Fingerprint.h>
#include <SoftwareSerial.h>

// ============================================
// CONFIGURACIÓN DE PINES
// ============================================
#define SENSOR_RX 10  // Pin RX del sensor (conectado a TX del sensor)
#define SENSOR_TX 11  // Pin TX del sensor (conectado a RX del sensor)
#define BUZZER_PIN 6  // Pin del buzzer (opcional)
#define LED_RED 7     // LED Rojo para errores
#define LED_GREEN 8   // LED Verde para éxito
#define LED_BLUE 9    // LED Azul para procesando

// ============================================
// CONSTANTES
// ============================================
#define BAUDRATE 115200        // Velocidad de comunicación con PC
#define TIMEOUT 20000          // 20 segundos de timeout
#define MAX_BUFFER 128         // Tamaño del buffer de comandos
#define MAX_FINGERPRINTS 255   // Capacidad máxima del sensor

// ============================================
// OBJETOS GLOBALES
// ============================================
SoftwareSerial mySerial(SENSOR_RX, SENSOR_TX);
Adafruit_Fingerprint finger = Adafruit_Fingerprint(&mySerial);

// Variables globales
char commandBuffer[MAX_BUFFER];
uint8_t bufferIndex = 0;
bool buzzerEnabled = true;

// ============================================
// SETUP - INICIALIZACIÓN
// ============================================
void setup() {
  // Inicializar comunicación serial con PC
  Serial.begin(BAUDRATE);
  while (!Serial && millis() < 3000);  // Esperar hasta 3 segundos
  
  // Configurar pines de salida
  pinMode(BUZZER_PIN, OUTPUT);
  pinMode(LED_RED, OUTPUT);
  pinMode(LED_GREEN, OUTPUT);
  pinMode(LED_BLUE, OUTPUT);
  
  // Apagar todos los LEDs
  allLedsOff();
  
  // Mensaje de inicio
  sendStatus("INIT", "Iniciando sistema");
  
  // Inicializar sensor de huella
  finger.begin(57600);
  delay(100);
  
  // Verificar conexión con el sensor
  if (finger.verifyPassword()) {
    sendStatus("SENSOR_OK", "Sensor conectado correctamente");
    blinkLed(LED_GREEN, 2, 200);
    beep(1, 100);
  } else {
    sendError("SENSOR_ERROR", "No se puede comunicar con el sensor");
    blinkLed(LED_RED, 5, 100);
    beep(3, 50);
    while (1) { delay(1); }  // Detener ejecución si no hay sensor
  }
  
  // Obtener información del sensor
  finger.getParameters();
  sendStatus("CAPACITY", String(finger.capacity));
  sendStatus("TEMPLATES", String(finger.templateCount));
  sendStatus("PACKET_SIZE", String(finger.packetLen));
  
  // Sistema listo
  sendStatus("READY", "Sistema listo para recibir comandos");
  beep(1, 100);
}

// ============================================
// LOOP PRINCIPAL
// ============================================
void loop() {
  // Leer comandos desde el puerto serial
  while (Serial.available() > 0) {
    char c = Serial.read();
    
    // Fin de comando
    if (c == '\n' || c == '\r') {
      if (bufferIndex > 0) {
        commandBuffer[bufferIndex] = '\0';
        processCommand(commandBuffer);
        bufferIndex = 0;
        memset(commandBuffer, 0, MAX_BUFFER);  // Limpiar buffer
      }
    } 
    // Agregar carácter al buffer
    else if (bufferIndex < MAX_BUFFER - 1) {
      commandBuffer[bufferIndex++] = c;
    }
  }
  
  delay(10);  // Pequeña pausa para estabilidad
}

// ============================================
// PROCESAMIENTO DE COMANDOS
// ============================================
void processCommand(char* cmd) {
  String command = String(cmd);
  command.trim();
  
  // ENROLL:ID - Enrolar una nueva huella
  if (command.startsWith("ENROLL:")) {
    int id = command.substring(7).toInt();
    if (id >= 1 && id <= MAX_FINGERPRINTS) {
      enrollFingerprint(id);
    } else {
      sendError("INVALID_ID", "ID debe estar entre 1 y 255");
    }
  }
  
  // VERIFY - Verificar huella
  else if (command == "VERIFY") {
    verifyFingerprint();
  }
  
  // DELETE:ID - Eliminar huella por ID
  else if (command.startsWith("DELETE:")) {
    int id = command.substring(7).toInt();
    deleteFingerprint(id);
  }
  
  // EMPTY - Vaciar toda la base de datos del sensor
  else if (command == "EMPTY") {
    emptyDatabase();
  }
  
  // COUNT - Obtener cantidad de huellas registradas
  else if (command == "COUNT") {
    finger.getTemplateCount();
    sendStatus("TEMPLATES", String(finger.templateCount));
  }
  
  // TEST - Ejecutar prueba del sensor y componentes
  else if (command == "TEST") {
    testSensor();
  }
  
  // PING - Verificar comunicación
  else if (command == "PING") {
    sendStatus("PONG", "OK");
  }
  
  // BUZZER:ON/OFF - Habilitar/deshabilitar buzzer
  else if (command.startsWith("BUZZER:")) {
    String state = command.substring(7);
    state.toUpperCase();
    if (state == "ON") {
      buzzerEnabled = true;
      sendStatus("BUZZER", "Habilitado");
      beep(1, 50);
    } else if (state == "OFF") {
      buzzerEnabled = false;
      sendStatus("BUZZER", "Deshabilitado");
    }
  }
  
  // INFO - Información del sensor
  else if (command == "INFO") {
    sendSensorInfo();
  }
  
  // Comando desconocido
  else {
    sendError("UNKNOWN_CMD", "Comando no reconocido: " + command);
  }
}

// ============================================
// ENROLAR HUELLA
// ============================================
void enrollFingerprint(uint8_t id) {
  sendStatus("ENROLL_START", String(id));
  setLed(LED_BLUE);
  
  // ===== PRIMER ESCANEO =====
  sendStatus("ENROLL_STEP", "Coloque el dedo en el sensor");
  beep(1, 100);
  
  int p = -1;
  unsigned long startTime = millis();
  
  // Esperar a que se coloque el dedo
  while (p != FINGERPRINT_OK) {
    p = finger.getImage();
    
    // Verificar timeout
    if (millis() - startTime > TIMEOUT) {
      sendError("TIMEOUT", "Tiempo de espera agotado");
      allLedsOff();
      return;
    }
    
    if (p == FINGERPRINT_OK) {
      sendStatus("ENROLL_STEP", "Imagen capturada");
      break;
    } else if (p == FINGERPRINT_NOFINGER) {
      // Esperando dedo...
      continue;
    } else if (p == FINGERPRINT_IMAGEFAIL) {
      sendError("IMAGE_ERROR", "Error al capturar imagen");
      blinkLed(LED_RED, 3, 200);
      allLedsOff();
      return;
    }
  }
  
  // Convertir imagen a características
  p = finger.image2Tz(1);
  if (p != FINGERPRINT_OK) {
    sendError("CONVERT_ERROR", "Error al convertir imagen");
    blinkLed(LED_RED, 3, 200);
    allLedsOff();
    return;
  }
  
  sendStatus("ENROLL_STEP", "Primera imagen procesada");
  beep(2, 100);
  
  // Esperar a que se retire el dedo
  sendStatus("ENROLL_STEP", "Retire el dedo");
  delay(2000);
  
  p = 0;
  while (p != FINGERPRINT_NOFINGER) {
    p = finger.getImage();
    delay(100);
  }
  
  // ===== SEGUNDO ESCANEO =====
  sendStatus("ENROLL_STEP", "Coloque el mismo dedo nuevamente");
  beep(1, 100);
  
  p = -1;
  startTime = millis();
  
  // Esperar segundo escaneo
  while (p != FINGERPRINT_OK) {
    p = finger.getImage();
    
    if (millis() - startTime > TIMEOUT) {
      sendError("TIMEOUT", "Tiempo de espera agotado");
      allLedsOff();
      return;
    }
    
    if (p == FINGERPRINT_OK) {
      sendStatus("ENROLL_STEP", "Segunda imagen capturada");
      break;
    } else if (p == FINGERPRINT_NOFINGER) {
      continue;
    }
  }
  
  // Convertir segunda imagen
  p = finger.image2Tz(2);
  if (p != FINGERPRINT_OK) {
    sendError("CONVERT_ERROR", "Error al convertir segunda imagen");
    blinkLed(LED_RED, 3, 200);
    allLedsOff();
    return;
  }
  
  // Crear modelo unificado
  sendStatus("ENROLL_STEP", "Creando modelo");
  p = finger.createModel();
  
  if (p == FINGERPRINT_OK) {
    sendStatus("ENROLL_STEP", "Huellas coinciden - Guardando");
  } else if (p == FINGERPRINT_ENROLLMISMATCH) {
    sendError("MISMATCH", "Las huellas no coinciden - Intente nuevamente");
    blinkLed(LED_RED, 3, 200);
    allLedsOff();
    return;
  } else {
    sendError("MODEL_ERROR", "Error al crear modelo");
    blinkLed(LED_RED, 3, 200);
    allLedsOff();
    return;
  }
  
  // Guardar modelo en la posición especificada
  p = finger.storeModel(id);
  
  if (p == FINGERPRINT_OK) {
    sendStatus("ENROLL_SUCCESS", String(id));
    blinkLed(LED_GREEN, 3, 200);
    beep(3, 100);
    
    // Actualizar contador
    finger.getTemplateCount();
    sendStatus("TEMPLATES", String(finger.templateCount));
  } else if (p == FINGERPRINT_BADLOCATION) {
    sendError("STORE_ERROR", "Ubicación de almacenamiento inválida");
    blinkLed(LED_RED, 3, 200);
  } else if (p == FINGERPRINT_FLASHERR) {
    sendError("STORE_ERROR", "Error al escribir en memoria");
    blinkLed(LED_RED, 3, 200);
  } else {
    sendError("STORE_ERROR", "Error desconocido al guardar");
    blinkLed(LED_RED, 3, 200);
  }
  
  allLedsOff();
}

// ============================================
// VERIFICAR HUELLA
// ============================================
void verifyFingerprint() {
  sendStatus("VERIFY_START", "Esperando huella");
  setLed(LED_BLUE);
  
  int p = -1;
  unsigned long startTime = millis();
  
  // Esperar a que se coloque el dedo
  while (p != FINGERPRINT_OK) {
    p = finger.getImage();
    
    if (millis() - startTime > TIMEOUT) {
      sendError("TIMEOUT", "Tiempo de espera agotado");
      allLedsOff();
      return;
    }
    
    if (p == FINGERPRINT_OK) {
      break;
    } else if (p == FINGERPRINT_NOFINGER) {
      continue;
    } else {
      sendError("IMAGE_ERROR", "Error al capturar imagen");
      blinkLed(LED_RED, 2, 200);
      allLedsOff();
      return;
    }
  }
  
  // Convertir imagen
  p = finger.image2Tz();
  if (p != FINGERPRINT_OK) {
    sendError("CONVERT_ERROR", "Error al procesar huella");
    blinkLed(LED_RED, 2, 200);
    allLedsOff();
    return;
  }
  
  // Buscar coincidencia en la base de datos
  p = finger.fingerSearch();
  
  if (p == FINGERPRINT_OK) {
    // ¡Huella encontrada!
    String result = "VERIFY_SUCCESS:" + 
                    String(finger.fingerID) + ":" + 
                    String(finger.confidence);
    Serial.println(result);
    
    blinkLed(LED_GREEN, 2, 200);
    beep(1, 200);
    
  } else if (p == FINGERPRINT_NOTFOUND) {
    sendError("NOT_FOUND", "Huella no registrada en el sistema");
    blinkLed(LED_RED, 3, 100);
    beep(2, 50);
    
  } else {
    sendError("SEARCH_ERROR", "Error durante la búsqueda");
    blinkLed(LED_RED, 2, 200);
  }
  
  allLedsOff();
}

// ============================================
// ELIMINAR HUELLA
// ============================================
void deleteFingerprint(uint8_t id) {
  setLed(LED_BLUE);
  
  uint8_t p = finger.deleteModel(id);
  
  if (p == FINGERPRINT_OK) {
    sendStatus("DELETE_SUCCESS", String(id));
    blinkLed(LED_GREEN, 2, 200);
    beep(1, 100);
    
    // Actualizar contador
    finger.getTemplateCount();
    sendStatus("TEMPLATES", String(finger.templateCount));
    
  } else if (p == FINGERPRINT_BADLOCATION) {
    sendError("DELETE_ERROR", "ID inválido: " + String(id));
    blinkLed(LED_RED, 2, 200);
  } else {
    sendError("DELETE_ERROR", "No se pudo eliminar ID " + String(id));
    blinkLed(LED_RED, 2, 200);
  }
  
  allLedsOff();
}

// ============================================
// VACIAR BASE DE DATOS DEL SENSOR
// ============================================
void emptyDatabase() {
  setLed(LED_BLUE);
  
  sendStatus("EMPTY_START", "Eliminando todas las huellas");
  
  uint8_t p = finger.emptyDatabase();
  
  if (p == FINGERPRINT_OK) {
    sendStatus("EMPTY_SUCCESS", "Base de datos limpiada");
    blinkLed(LED_GREEN, 3, 200);
    beep(2, 150);
    
    finger.getTemplateCount();
    sendStatus("TEMPLATES", "0");
  } else {
    sendError("EMPTY_ERROR", "Error al limpiar base de datos");
    blinkLed(LED_RED, 3, 200);
  }
  
  allLedsOff();
}

// ============================================
// PROBAR SENSOR Y COMPONENTES
// ============================================
void testSensor() {
  sendStatus("TEST_START", "Iniciando prueba de sistema");
  
  // Probar LEDs
  sendStatus("TEST_STEP", "Probando LED Rojo");
  setLed(LED_RED);
  delay(500);
  
  sendStatus("TEST_STEP", "Probando LED Verde");
  setLed(LED_GREEN);
  delay(500);
  
  sendStatus("TEST_STEP", "Probando LED Azul");
  setLed(LED_BLUE);
  delay(500);
  
  allLedsOff();
  
  // Probar Buzzer
  sendStatus("TEST_STEP", "Probando Buzzer");
  beep(3, 100);
  
  // Probar sensor
  sendStatus("TEST_STEP", "Probando sensor");
  int p = finger.getImage();
  
  if (p == FINGERPRINT_NOFINGER) {
    sendStatus("TEST_RESULT", "Sensor OK - Sin dedo detectado");
  } else if (p == FINGERPRINT_OK) {
    sendStatus("TEST_RESULT", "Sensor OK - Dedo detectado");
  } else {
    sendError("TEST_ERROR", "Sensor con problemas - Código: " + String(p));
  }
  
  sendStatus("TEST_COMPLETE", "Prueba finalizada");
  blinkLed(LED_GREEN, 2, 200);
}

// ============================================
// INFORMACIÓN DEL SENSOR
// ============================================
void sendSensorInfo() {
  finger.getParameters();
  
  Serial.println("INFO:STATUS_REG:" + String(finger.status_reg, HEX));
  Serial.println("INFO:SYSTEM_ID:" + String(finger.system_id, HEX));
  Serial.println("INFO:CAPACITY:" + String(finger.capacity));
  Serial.println("INFO:SECURITY:" + String(finger.security_level));
  Serial.println("INFO:ADDRESS:" + String(finger.device_addr, HEX));
  Serial.println("INFO:PACKET_LEN:" + String(finger.packetLen));
  Serial.println("INFO:BAUD:" + String(finger.baud_rate));
  Serial.println("INFO:TEMPLATES:" + String(finger.templateCount));
}

// ============================================
// FUNCIONES DE UTILIDAD
// ============================================

// Enviar mensaje de estado
void sendStatus(String code, String message) {
  Serial.println("STATUS:" + code + ":" + message);
}

// Enviar mensaje de error
void sendError(String code, String message) {
  Serial.println("ERROR:" + code + ":" + message);
}

// Hacer sonar el buzzer
void beep(int times, int duration) {
  if (!buzzerEnabled) return;
  
  for (int i = 0; i < times; i++) {
    digitalWrite(BUZZER_PIN, HIGH);
    delay(duration);
    digitalWrite(BUZZER_PIN, LOW);
    if (i < times - 1) delay(duration);
  }
}

// Encender un LED específico
void setLed(int pin) {
  allLedsOff();
  digitalWrite(pin, HIGH);
}

// Apagar todos los LEDs
void allLedsOff() {
  digitalWrite(LED_RED, LOW);
  digitalWrite(LED_GREEN, LOW);
  digitalWrite(LED_BLUE, LOW);
}

// Parpadear un LED
void blinkLed(int pin, int times, int duration) {
  for (int i = 0; i < times; i++) {
    digitalWrite(pin, HIGH);
    delay(duration);
    digitalWrite(pin, LOW);
    delay(duration);
  }
}

// ============================================
// FIN DEL CÓDIGO
// ============================================