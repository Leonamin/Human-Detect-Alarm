#include <Arduino.h>

struct DInput {
  const uint8_t PIN;
  uint32_t numberStateChanges;
  bool state;
};

DInput pirSensor = {2, 0 , false};

// LED variables
int ledPin = 33;
unsigned long previousMS = 0;
unsigned long periodMS = 0;

void IRAM_ATTR isr(void* arg) {
    DInput* s = static_cast<DInput*>(arg);
    s->numberStateChanges += 1;
    s->state = true;
}

void setup() {
    Serial.begin(115200);
    pinMode(pirSensor.PIN, INPUT);
    pinMode(ledPin, OUTPUT);
    attachInterruptArg(pirSensor.PIN, isr, &pirSensor, RISING);
}

void loop() {
    if (pirSensor.state) {
        digitalWrite(ledPin, LOW);
        previousMS = millis();
        periodMS = 1000;
        pirSensor.state = false;
    } else {
        if (millis() - previousMS >= periodMS) {
            digitalWrite(ledPin, HIGH);
        }
    }
}
