#include <Arduino.h>

#include "BluetoothSerial.h"

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run `make menuconfig` to and enable it
#endif

#define BT_CONNECT_EVENT        0x01
#define DETECT_EVENT            0x10
#define DETECT_PHOTO_EVENT      0x11

#define PROTOCOL_HEADER_SIZE    2

/////////////////////////////////

// Declare types

/////////////////////////////////

typedef struct _DInput {
  const uint8_t PIN;
  uint32_t numberStateChanges;
  bool state;
} DInput;

/**
 *  Protocol_h
 *  Protocol header
 * 
 *  event_type      : event type of data sending
 *  data_length     : data length
 */
typedef struct _header {
    uint8_t event_type;
    uint8_t data_length;
} Protocol_h;

/**
 * Protocol
 * Whole protocol
 * 
 * protocol_header: Protocol_h
 * data: it depends on event type
 */
typedef struct _Protocol {
    Protocol_h protocol_header;
    uint8_t* data;
} Protocol;

/////////////////////////////////

// Declare hardware control varibles

/////////////////////////////////

BluetoothSerial SerialBT;

DInput pirSensor = {2, 0 , false};

int ledPin = 33;
unsigned long previousMS = 0;
unsigned long periodMS = 0;

/////////////////////////////////

// Declare operation variables

/////////////////////////////////

bool photo_enable = false;
Protocol protocol;
uint8_t detect_count_num = 0;

/**
 * buf: Whole protocol
 * size: protocol data length
 */
void makePacketAndSend(int event_type, uint8_t* buf, int size) {
    uint8_t *txPacket = (uint8_t*) malloc(size + PROTOCOL_HEADER_SIZE);
	if (NULL == txPacket) 
        return;
	txPacket[0] = event_type;
	txPacket[1] = size;
	memcpy(&txPacket[PROTOCOL_HEADER_SIZE], buf, size);
	SerialBT.write(txPacket, size + PROTOCOL_HEADER_SIZE);
	free(txPacket);
}

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
        Serial.println("Detected");

        digitalWrite(ledPin, LOW);
        previousMS = millis();
        periodMS = 1000;
        
        pirSensor.state = false;
        detect_count_num++;

        if (!photo_enable) {
            protocol.protocol_header.event_type = DETECT_EVENT;
            protocol.protocol_header.data_length = 1;
            protocol.data = (uint8_t*) malloc(sizeof(uint8_t) * 1);
            protocol.data[0] = detect_count_num;
        } else {
            
        }

        makePacketAndSend(protocol.protocol_header.event_type, protocol.data, protocol.protocol_header.data_length);
        // TODO SerialBT.available() is not working
        // if (SerialBT.available()) {
            
        // }
    } else {
        if (millis() - previousMS >= periodMS) {
            digitalWrite(ledPin, HIGH);
        }
    }
}
