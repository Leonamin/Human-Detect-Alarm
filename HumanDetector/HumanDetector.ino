#include <Arduino.h>

#include "BluetoothSerial.h"

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run `make menuconfig` to and enable it
#endif

BluetoothSerial SerialBT;

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

char msg = '1';

void IRAM_ATTR isr(void* arg) {
    DInput* s = static_cast<DInput*>(arg);
    s->numberStateChanges += 1;
    s->state = true;
}

void setup() {
    Serial.begin(115200);
    SerialBT.begin("Human Detector");

    pinMode(pirSensor.PIN, INPUT);
    pinMode(ledPin, OUTPUT);
    attachInterruptArg(pirSensor.PIN, isr, &pirSensor, RISING);
}

void loop() {
    if (pirSensor.state) {
        Serial.print("Detected");
        digitalWrite(ledPin, LOW);
        previousMS = millis();
        periodMS = 1000;
        pirSensor.state = false;
        SerialBT.write(msg);
        if (SerialBT.available()) {
            Serial.print("Detect info sending...");
            SerialBT.write(msg);
        }
    } else {
        if (millis() - previousMS >= periodMS) {
            digitalWrite(ledPin, HIGH);
        }
    }
}
