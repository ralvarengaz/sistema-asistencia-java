/**
 * Sistema de Control de Asistencia - Arduino UNO Optimizado
 * Version: 3.1
 */

#include <Adafruit_Fingerprint.h>
#include <SoftwareSerial.h>

// Pines
#define RX_PIN 10
#define TX_PIN 11
#define BUZZER_PIN 6

// Timeouts
#define TIMEOUT_FINGER 15000
#define TIMEOUT_REMOVE 5000

SoftwareSerial mySerial(RX_PIN, TX_PIN);
Adafruit_Fingerprint finger = Adafruit_Fingerprint(&mySerial);

char cmdBuffer[32];
uint8_t bufferIndex = 0;

const char MSG_READY[] PROGMEM = "READY";
const char MSG_SENSOR_OK[] PROGMEM = "SENSOR_OK";
const char MSG_SENSOR_FAIL[] PROGMEM = "SENSOR_FAIL";
const char MSG_PLACE[] PROGMEM = "PLACE_FINGER";
const char MSG_REMOVE[] PROGMEM = "REMOVE_FINGER";
const char MSG_AGAIN[] PROGMEM = "PLACE_AGAIN";
const char MSG_TIMEOUT[] PROGMEM = "TIMEOUT";
const char MSG_SUCCESS[] PROGMEM = "SUCCESS";
const char MSG_CAPTURED[] PROGMEM = "CAPTURED";
const char MSG_CREATING[] PROGMEM = "CREATING_MODEL";
const char MSG_SAVING[] PROGMEM = "SAVING";
const char MSG_FOUND[] PROGMEM = "FOUND";
const char MSG_NOT_MATCH[] PROGMEM = "NOT_MATCH";

void printProgmem(const char* str) {
  char c;
  while ((c = pgm_read_byte(str++))) {
    Serial.write(c);
  }
  Serial.println();
}

void beep(uint8_t type) {
  if (type == 0) {
    tone(BUZZER_PIN, 500, 200);
    delay(250);
    tone(BUZZER_PIN, 400, 200);
    delay(250);
  } else if (type == 1) {
    tone(BUZZER_PIN, 2000, 100);
    delay(150);
  } else {
    tone(BUZZER_PIN, 1500, 50);
    delay(100);
  }
  noTone(BUZZER_PIN);
}

bool initSensor() {
  uint32_t rates[] = {57600, 9600, 115200};
  
  for (uint8_t i = 0; i < 3; i++) {
    mySerial.begin(rates[i]);
    delay(100);
    
    if (finger.verifyPassword()) {
      finger.getTemplateCount();
      Serial.print(F("TEMPLATES:"));
      Serial.println(finger.templateCount);
      return true;
    }
  }
  return false;
}

void setup() {
  Serial.begin(115200);
  while (!Serial);
  
  pinMode(BUZZER_PIN, OUTPUT);
  digitalWrite(BUZZER_PIN, LOW);
  
  delay(500);
  
  if (initSensor()) {
    printProgmem(MSG_SENSOR_OK);
    beep(1);
  } else {
    printProgmem(MSG_SENSOR_FAIL);
    beep(0);
  }
  
  printProgmem(MSG_READY);
}

void loop() {
  while (Serial.available() > 0) {
    char c = Serial.read();
    
    if (c == '\n') {
      cmdBuffer[bufferIndex] = '\0';
      processCommand();
      bufferIndex = 0;
    } 
    else if (c != '\r' && bufferIndex < 31) {
      cmdBuffer[bufferIndex++] = c;
    }
  }
}

void processCommand() {
  if (strcmp(cmdBuffer, "PING") == 0) {
    printProgmem(MSG_READY);
    return;
  }
  
  if (strcmp(cmdBuffer, "TEST") == 0) {
    testSensor();
    return;
  }
  
  if (strcmp(cmdBuffer, "CLEAR") == 0) {
    clearDatabase();
    return;
  }
  
  if (strcmp(cmdBuffer, "VERIFY") == 0) {
    verifyFinger();
    return;
  }
  
  if (strcmp(cmdBuffer, "COUNT") == 0) {
    finger.getTemplateCount();
    Serial.print(F("COUNT:"));
    Serial.println(finger.templateCount);
    return;
  }
  
  if (strncmp(cmdBuffer, "ENROLL:", 7) == 0) {
    uint8_t id = atoi(cmdBuffer + 7);
    if (id > 0 && id <= 255) {
      enrollFinger(id);
    } else {
      Serial.println(F("ERROR:ID_INVALID"));
    }
    return;
  }
  
  if (strncmp(cmdBuffer, "DELETE:", 7) == 0) {
    uint8_t id = atoi(cmdBuffer + 7);
    if (id > 0) {
      deleteFinger(id);
    }
    return;
  }
  
  Serial.println(F("ERROR:UNKNOWN_CMD"));
}

void testSensor() {
  Serial.println(F("TEST:START"));
  
  if (!finger.verifyPassword()) {
    Serial.println(F("TEST:NO_COMM"));
    beep(0);
    return;
  }
  
  printProgmem(MSG_PLACE);
  
  unsigned long start = millis();
  while (millis() - start < 15000) {
    if (finger.getImage() == FINGERPRINT_OK) {
      Serial.println(F("TEST:OK"));
      beep(1);
      return;
    }
    delay(100);
  }
  
  Serial.println(F("TEST:TIMEOUT"));
  beep(0);
}

void enrollFinger(uint8_t id) {
  Serial.print(F("ENROLL:START:"));
  Serial.println(id);
  
  uint8_t result = doEnroll(id);
  
  if (result == FINGERPRINT_OK) {
    Serial.print(F("ENROLL:OK:"));
    Serial.println(id);
    beep(1);
  } else {
    Serial.print(F("ENROLL:FAIL:"));
    Serial.println(result);
    beep(0);
  }
}

uint8_t doEnroll(uint8_t id) {
  printProgmem(MSG_PLACE);
  beep(2);
  
  unsigned long start = millis();
  uint8_t p = FINGERPRINT_NOFINGER;
  
  while (p != FINGERPRINT_OK) {
    p = finger.getImage();
    if (millis() - start > TIMEOUT_FINGER) {
      printProgmem(MSG_TIMEOUT);
      return FINGERPRINT_TIMEOUT;
    }
    if (p == FINGERPRINT_NOFINGER) {
      delay(50);
    } else if (p != FINGERPRINT_OK) {
      return p;
    }
  }
  
  printProgmem(MSG_CAPTURED);
  
  p = finger.image2Tz(1);
  if (p != FINGERPRINT_OK) return p;
  
  printProgmem(MSG_REMOVE);
  beep(2);
  delay(1000);
  
  start = millis();
  while (finger.getImage() != FINGERPRINT_NOFINGER) {
    if (millis() - start > TIMEOUT_REMOVE) {
      delay(500);
      start = millis();
    }
    delay(100);
  }
  
  printProgmem(MSG_AGAIN);
  beep(2);
  
  p = FINGERPRINT_NOFINGER;
  start = millis();
  
  while (p != FINGERPRINT_OK) {
    p = finger.getImage();
    if (millis() - start > TIMEOUT_FINGER) {
      printProgmem(MSG_TIMEOUT);
      return FINGERPRINT_TIMEOUT;
    }
    if (p == FINGERPRINT_NOFINGER) {
      delay(50);
    } else if (p != FINGERPRINT_OK) {
      return p;
    }
  }
  
  printProgmem(MSG_CAPTURED);
  
  p = finger.image2Tz(2);
  if (p != FINGERPRINT_OK) return p;
  
  printProgmem(MSG_CREATING);
  p = finger.createModel();
  if (p != FINGERPRINT_OK) {
    printProgmem(MSG_NOT_MATCH);
    return p;
  }
  
  printProgmem(MSG_SAVING);
  p = finger.storeModel(id);
  if (p == FINGERPRINT_OK) {
    printProgmem(MSG_SUCCESS);
  }
  
  return p;
}

void verifyFinger() {
  printProgmem(MSG_PLACE);
  
  int id = doVerify();
  
  if (id > 0) {
    Serial.print(F("VERIFY:OK:"));
    Serial.print(id);
    Serial.print(F(":"));
    Serial.println(finger.confidence);
    beep(1);
  } else if (id == 0) {
    Serial.println(F("VERIFY:FAIL:NOT_FOUND"));
    beep(0);
  } else {
    Serial.println(F("VERIFY:FAIL:ERROR"));
    beep(0);
  }
}

int doVerify() {
  unsigned long start = millis();
  uint8_t p = FINGERPRINT_NOFINGER;
  
  while (p != FINGERPRINT_OK) {
    p = finger.getImage();
    if (millis() - start > TIMEOUT_FINGER) {
      printProgmem(MSG_TIMEOUT);
      return -1;
    }
    if (p == FINGERPRINT_NOFINGER) {
      delay(50);
    } else if (p != FINGERPRINT_OK) {
      return -1;
    }
  }
  
  p = finger.image2Tz();
  if (p != FINGERPRINT_OK) return -1;
  
  p = finger.fingerSearch();
  if (p == FINGERPRINT_OK) {
    Serial.print(F("FOUND:"));
    Serial.println(finger.fingerID);
    return finger.fingerID;
  } else if (p == FINGERPRINT_NOTFOUND) {
    return 0;
  }
  
  return -1;
}

void deleteFinger(uint8_t id) {
  Serial.print(F("DELETE:START:"));
  Serial.println(id);
  
  uint8_t p = finger.deleteModel(id);
  
  if (p == FINGERPRINT_OK) {
    Serial.print(F("DELETE:OK:"));
    Serial.println(id);
    beep(1);
  } else {
    Serial.print(F("DELETE:FAIL:"));
    Serial.println(p);
    beep(0);
  }
}

void clearDatabase() {
  Serial.println(F("CLEAR:START"));
  
  uint8_t p = finger.emptyDatabase();
  
  if (p == FINGERPRINT_OK) {
    Serial.println(F("CLEAR:OK"));
    beep(1);
    delay(100);
    beep(1);
  } else {
    Serial.print(F("CLEAR:FAIL:"));
    Serial.println(p);
    beep(0);
  }
}
