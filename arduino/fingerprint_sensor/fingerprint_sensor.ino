/*
 * ============================================
 * FIRMWARE OPTIMIZADO - SENSOR DY50
 * ============================================
 * Versión: 4.1 - OPTIMIZADO PARA MEMORIA
 * Fecha: 2025-10-22
 * 
 * MEJORAS v4.1:
 * - Corrección de error de compilación StringSumHelper
 * - Optimización de memoria RAM y Flash
 * - Uso intensivo de F() macro para strings
 * - Simplificación de mensajes
 * - Buffer optimizado
 */

#include <Adafruit_Fingerprint.h>
#include <SoftwareSerial.h>

// ============================================
// CONFIGURACIÓN DE PINES
// ============================================
#define SENSOR_RX 10
#define SENSOR_TX 11
#define BUZZER_PIN 6
#define LED_RED 7
#define LED_GREEN 8
#define LED_BLUE 9

// ============================================
// CONSTANTES
// ============================================
#define BAUDRATE_PC 115200
#define TIMEOUT 20000
#define MAX_BUFFER 32

// Baudrates más comunes primero
const long SENSOR_BAUDRATES[] PROGMEM = {
  9600, 57600, 19200, 38400, 115200
};
#define NUM_BAUDRATES 5

// ============================================
// OBJETOS GLOBALES
// ============================================
SoftwareSerial mySerial(SENSOR_RX, SENSOR_TX);
Adafruit_Fingerprint finger = Adafruit_Fingerprint(&mySerial);

// Variables globales
char cmdBuf[MAX_BUFFER];
uint8_t bufIdx = 0;
bool buzzerOn = true;
long sensorBaud = 0;

// ============================================
// FUNCIONES DE COMUNICACIÓN
// ============================================
void sendStatus(const char* code, const char* msg) {
  Serial.print(F("STATUS:"));
  Serial.print(code);
  Serial.print(F(":"));
  Serial.println(msg);
}

void sendError(const char* code, const char* msg) {
  Serial.print(F("ERROR:"));
  Serial.print(code);
  Serial.print(F(":"));
  Serial.println(msg);
}

void sendNum(const char* code, long num) {
  Serial.print(F("STATUS:"));
  Serial.print(code);
  Serial.print(F(":"));
  Serial.println(num);
}

// ============================================
// SETUP
// ============================================
void setup() {
  Serial.begin(BAUDRATE_PC);
  while (!Serial && millis() < 3000);
  
  // Configurar pines
  pinMode(BUZZER_PIN, OUTPUT);
  pinMode(LED_RED, OUTPUT);
  pinMode(LED_GREEN, OUTPUT);
  pinMode(LED_BLUE, OUTPUT);
  
  allLedsOff();
  
  Serial.println(F("\n========================================"));
  Serial.println(F("  SISTEMA BIOMETRICO v4.1 OPTIMIZADO"));
  Serial.println(F("========================================\n"));
  
  sendStatus("INIT", "Sistema iniciado");
  
  // Test rápido hardware
  digitalWrite(LED_RED, HIGH);
  delay(80);
  digitalWrite(LED_RED, LOW);
  digitalWrite(LED_GREEN, HIGH);
  delay(80);
  digitalWrite(LED_GREEN, LOW);
  digitalWrite(LED_BLUE, HIGH);
  delay(80);
  digitalWrite(LED_BLUE, LOW);
  
  beep(1, 50);
  sendStatus("HARDWARE", "OK");
  
  // Detectar sensor
  if (detectSensor()) {
    Serial.println(F("*** SENSOR CONECTADO ***"));
    sendStatus("SENSOR_OK", "DY50 conectado");
    sendNum("BAUDRATE", sensorBaud);
    
    blinkLed(LED_GREEN, 3, 200);
    beep(2, 100);
    
    delay(100);
    finger.getParameters();
    
    sendNum("CAPACITY", finger.capacity);
    sendNum("TEMPLATES", finger.templateCount);
    
    sendStatus("READY", "Listo");
    
  } else {
    Serial.println(F("*** SENSOR NO DETECTADO ***"));
    sendError("SENSOR_ERROR", "No detectado");
    printHelp();
    blinkLed(LED_RED, 5, 200);
    beep(3, 100);
    sendStatus("READY", "Modo diagnostico");
  }
  
  Serial.println(F("\nComandos: DETECT, HARDWARE, HELP, PING\n"));
}

// ============================================
// DETECCIÓN DE SENSOR (CORREGIDA)
// ============================================
bool detectSensor() {
  setLed(LED_BLUE);
  
  // Probar baudrates más comunes
  for (int i = 0; i < NUM_BAUDRATES; i++) {
    long testBaud = pgm_read_dword(&SENSOR_BAUDRATES[i]);
    
    Serial.print(F("Probando "));
    Serial.print(testBaud);
    Serial.print(F("..."));
    
    // 2 intentos por baudrate
    for (int attempt = 0; attempt < 2; attempt++) {
      mySerial.end();
      delay(50);
      mySerial.begin(testBaud);
      finger.begin(testBaud);
      delay(200);
      
      if (finger.verifyPassword()) {
        sensorBaud = testBaud;
        Serial.println(F(" OK"));
        allLedsOff();
        return true;
      }
      delay(80);
    }
    Serial.println(F(" X"));
  }
  
  allLedsOff();
  return false;
}

// ============================================
// AYUDA SIMPLIFICADA
// ============================================
void printHelp() {
  Serial.println(F("\n--- VERIFICAR ---"));
  Serial.println(F("1. VCC (Rojo) --> 5V"));
  Serial.println(F("2. GND (Negro) --> GND"));
  Serial.println(F("3. TX (Blanco) --> Pin 10"));
  Serial.println(F("4. RX (Verde) --> Pin 11"));
  Serial.println(F("\nSi no funciona, invertir TX/RX"));
}

// ============================================
// TEST DE HARDWARE
// ============================================
void testHardware() {
  Serial.println(F("\n--- TEST HARDWARE ---"));
  
  Serial.print(F("LED Rojo...  "));
  digitalWrite(LED_RED, HIGH);
  delay(400);
  digitalWrite(LED_RED, LOW);
  Serial.println(F("OK"));
  
  Serial.print(F("LED Verde... "));
  digitalWrite(LED_GREEN, HIGH);
  delay(400);
  digitalWrite(LED_GREEN, LOW);
  Serial.println(F("OK"));
  
  Serial.print(F("LED Azul...  "));
  digitalWrite(LED_BLUE, HIGH);
  delay(400);
  digitalWrite(LED_BLUE, LOW);
  Serial.println(F("OK"));
  
  Serial.print(F("Buzzer...    "));
  beep(2, 100);
  Serial.println(F("OK"));
  
  Serial.print(F("Serial...    "));
  Serial.println(F("OK (leyendo esto)"));
  
  Serial.println(F("\nTest completado"));
  sendStatus("HARDWARE_TEST", "Completado");
}

// ============================================
// LOOP PRINCIPAL
// ============================================
void loop() {
  // Leer comandos
  while (Serial.available() > 0) {
    char c = Serial.read();
    
    if (c == '\n' || c == '\r') {
      if (bufIdx > 0) {
        cmdBuf[bufIdx] = '\0';
        processCommand(cmdBuf);
        bufIdx = 0;
      }
    } else if (bufIdx < MAX_BUFFER - 1) {
      cmdBuf[bufIdx++] = c;
    }
  }
}

// ============================================
// PROCESAMIENTO DE COMANDOS
// ============================================
void processCommand(char* cmd) {
  // Convertir a mayúsculas
  for (int i = 0; cmd[i]; i++) {
    if (cmd[i] >= 'a' && cmd[i] <= 'z') {
      cmd[i] -= 32;
    }
  }
  
  // PING
  if (strcmp(cmd, "PING") == 0) {
    sendStatus("PONG", "Sistema activo");
    if (sensorBaud > 0) {
      sendNum("SENSOR_BAUD", sensorBaud);
    }
  }
  
  // DETECT
  else if (strcmp(cmd, "DETECT") == 0) {
    sendStatus("DETECTING", "Buscando sensor...");
    if (detectSensor()) {
      sendStatus("SENSOR_OK", "Conectado");
      sendNum("BAUDRATE", sensorBaud);
      finger.getParameters();
      sendNum("TEMPLATES", finger.templateCount);
    } else {
      sendError("SENSOR_ERROR", "No encontrado");
    }
  }
  
  // HARDWARE
  else if (strcmp(cmd, "HARDWARE") == 0) {
    testHardware();
  }
  
  // HELP
  else if (strcmp(cmd, "HELP") == 0) {
    printHelp();
    sendStatus("HELP", "Mostrado");
  }
  
  // STATUS
  else if (strcmp(cmd, "STATUS") == 0) {
    if (sensorBaud > 0) {
      sendStatus("CONNECTED", "Sensor OK");
      sendNum("BAUDRATE", sensorBaud);
      finger.getTemplateCount();
      sendNum("TEMPLATES", finger.templateCount);
    } else {
      sendError("DISCONNECTED", "Sin sensor");
    }
  }
  
  // ENROLL:ID
  else if (strncmp(cmd, "ENROLL:", 7) == 0) {
    if (sensorBaud == 0) {
      sendError("NO_SENSOR", "Sensor desconectado");
      return;
    }
    int id = atoi(cmd + 7);
    enrollFinger(id);
  }
  
  // VERIFY
  else if (strcmp(cmd, "VERIFY") == 0) {
    if (sensorBaud == 0) {
      sendError("NO_SENSOR", "Sensor desconectado");
      return;
    }
    verifyFinger();
  }
  
  // DELETE:ID
  else if (strncmp(cmd, "DELETE:", 7) == 0) {
    if (sensorBaud == 0) {
      sendError("NO_SENSOR", "Sensor desconectado");
      return;
    }
    int id = atoi(cmd + 7);
    deleteFinger(id);
  }
  
  // EMPTY
  else if (strcmp(cmd, "EMPTY") == 0) {
    if (sensorBaud == 0) {
      sendError("NO_SENSOR", "Sensor desconectado");
      return;
    }
    emptyDB();
  }
  
  // COUNT
  else if (strcmp(cmd, "COUNT") == 0) {
    if (sensorBaud == 0) {
      sendError("NO_SENSOR", "Sensor desconectado");
      return;
    }
    finger.getTemplateCount();
    sendNum("TEMPLATES", finger.templateCount);
  }
  
  // INFO
  else if (strcmp(cmd, "INFO") == 0) {
    if (sensorBaud == 0) {
      sendError("NO_SENSOR", "Sensor desconectado");
      return;
    }
    sendInfo();
  }
  
  // BUZZER:ON/OFF
  else if (strncmp(cmd, "BUZZER:", 7) == 0) {
    if (strcmp(cmd + 7, "ON") == 0) {
      buzzerOn = true;
      sendStatus("BUZZER", "ON");
      beep(1, 50);
    } else if (strcmp(cmd + 7, "OFF") == 0) {
      buzzerOn = false;
      sendStatus("BUZZER", "OFF");
    }
  }
  
  // Comando desconocido
  else {
    sendError("UNKNOWN_CMD", cmd);
  }
}

// ============================================
// OPERACIONES CON SENSOR
// ============================================

void enrollFinger(uint8_t id) {
  sendNum("ENROLL_START", id);
  setLed(LED_BLUE);
  
  sendStatus("ENROLL_STEP", "Coloque dedo");
  beep(1, 100);
  
  int p = waitForFinger();
  if (p != FINGERPRINT_OK) return;
  
  sendStatus("ENROLL_STEP", "Capturado");
  
  p = finger.image2Tz(1);
  if (p != FINGERPRINT_OK) {
    sendError("CONVERT_ERR", "Error convertir");
    allLedsOff();
    return;
  }
  
  sendStatus("ENROLL_STEP", "Primera OK");
  beep(2, 100);
  
  sendStatus("ENROLL_STEP", "Retire dedo");
  delay(2000);
  
  while (finger.getImage() != FINGERPRINT_NOFINGER) {
    delay(100);
  }
  
  sendStatus("ENROLL_STEP", "Coloque otra vez");
  beep(1, 100);
  
  p = waitForFinger();
  if (p != FINGERPRINT_OK) return;
  
  sendStatus("ENROLL_STEP", "Segunda captura");
  
  p = finger.image2Tz(2);
  if (p != FINGERPRINT_OK) {
    sendError("CONVERT_ERR", "Error convertir");
    allLedsOff();
    return;
  }
  
  sendStatus("ENROLL_STEP", "Creando modelo");
  p = finger.createModel();
  
  if (p == FINGERPRINT_ENROLLMISMATCH) {
    sendError("MISMATCH", "No coinciden");
    blinkLed(LED_RED, 3, 200);
    allLedsOff();
    return;
  } else if (p != FINGERPRINT_OK) {
    sendError("MODEL_ERR", "Error modelo");
    allLedsOff();
    return;
  }
  
  p = finger.storeModel(id);
  
  if (p == FINGERPRINT_OK) {
    sendNum("ENROLL_SUCCESS", id);
    blinkLed(LED_GREEN, 3, 200);
    beep(3, 100);
    
    finger.getTemplateCount();
    sendNum("TEMPLATES", finger.templateCount);
  } else {
    sendError("STORE_ERR", "Error guardar");
    blinkLed(LED_RED, 3, 200);
  }
  
  allLedsOff();
}

int waitForFinger() {
  unsigned long start = millis();
  
  while (millis() - start < TIMEOUT) {
    int p = finger.getImage();
    
    if (p == FINGERPRINT_OK) {
      return FINGERPRINT_OK;
    } else if (p == FINGERPRINT_NOFINGER) {
      continue;
    } else {
      sendError("IMAGE_ERR", "Error captura");
      allLedsOff();
      return p;
    }
  }
  
  sendError("TIMEOUT", "Tiempo agotado");
  allLedsOff();
  return -1;
}

void verifyFinger() {
  sendStatus("VERIFY_START", "Coloque dedo");
  setLed(LED_BLUE);
  
  int p = waitForFinger();
  if (p != FINGERPRINT_OK) return;
  
  p = finger.image2Tz();
  if (p != FINGERPRINT_OK) {
    sendError("CONVERT_ERR", "Error procesar");
    allLedsOff();
    return;
  }
  
  p = finger.fingerSearch();
  
  if (p == FINGERPRINT_OK) {
    Serial.print(F("VERIFY_SUCCESS:"));
    Serial.print(finger.fingerID);
    Serial.print(F(":"));
    Serial.println(finger.confidence);
    
    blinkLed(LED_GREEN, 2, 200);
    beep(1, 200);
  } else if (p == FINGERPRINT_NOTFOUND) {
    sendError("NOT_FOUND", "No registrada");
    blinkLed(LED_RED, 3, 100);
    beep(2, 50);
  } else {
    sendError("SEARCH_ERR", "Error busqueda");
    blinkLed(LED_RED, 2, 200);
  }
  
  allLedsOff();
}

void deleteFinger(uint8_t id) {
  setLed(LED_BLUE);
  
  uint8_t p = finger.deleteModel(id);
  
  if (p == FINGERPRINT_OK) {
    sendNum("DELETE_SUCCESS", id);
    blinkLed(LED_GREEN, 2, 200);
    beep(1, 100);
    
    finger.getTemplateCount();
    sendNum("TEMPLATES", finger.templateCount);
  } else {
    sendError("DELETE_ERR", "Error eliminar");
    blinkLed(LED_RED, 2, 200);
  }
  
  allLedsOff();
}

void emptyDB() {
  setLed(LED_BLUE);
  sendStatus("EMPTY_START", "Borrando BD");
  
  uint8_t p = finger.emptyDatabase();
  
  if (p == FINGERPRINT_OK) {
    sendStatus("EMPTY_SUCCESS", "BD limpia");
    blinkLed(LED_GREEN, 3, 200);
    beep(2, 150);
    
    finger.getTemplateCount();
    sendNum("TEMPLATES", 0);
  } else {
    sendError("EMPTY_ERR", "Error limpiar");
    blinkLed(LED_RED, 3, 200);
  }
  
  allLedsOff();
}

void sendInfo() {
  finger.getParameters();
  
  Serial.print(F("INFO:CAPACITY:"));
  Serial.println(finger.capacity);
  
  Serial.print(F("INFO:TEMPLATES:"));
  Serial.println(finger.templateCount);
  
  Serial.print(F("INFO:PACKET_LEN:"));
  Serial.println(finger.packet_len);
  
  Serial.print(F("INFO:SECURITY:"));
  Serial.println(finger.security_level);
  
  Serial.print(F("INFO:BAUDRATE:"));
  Serial.println(sensorBaud);
}

// ============================================
// UTILIDADES
// ============================================
void beep(int times, int dur) {
  if (!buzzerOn) return;
  for (int i = 0; i < times; i++) {
    digitalWrite(BUZZER_PIN, HIGH);
    delay(dur);
    digitalWrite(BUZZER_PIN, LOW);
    if (i < times - 1) delay(dur);
  }
}

void setLed(int pin) {
  allLedsOff();
  digitalWrite(pin, HIGH);
}

void allLedsOff() {
  digitalWrite(LED_RED, LOW);
  digitalWrite(LED_GREEN, LOW);
  digitalWrite(LED_BLUE, LOW);
}

void blinkLed(int pin, int times, int dur) {
  for (int i = 0; i < times; i++) {
    digitalWrite(pin, HIGH);
    delay(dur);
    digitalWrite(pin, LOW);
    delay(dur);
  }
}

// ============================================
// FIN v4.1 - OPTIMIZADO
// ============================================
