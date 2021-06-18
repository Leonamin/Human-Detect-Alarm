#include <Arduino.h>
#include "esp_camera.h"
#include "BluetoothSerial.h"
#include "driver/gpio.h"
#include "esp_intr_alloc.h"

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run `make menuconfig` to and enable it
#endif

#define STX                     0x5A
#define ETX                     0xA5
#define BT_CONNECT_EVENT        0x01
#define DETECT_EVENT            0x10
#define DETECT_PHOTO_EVENT      0x11

#define PROTOCOL_HEADER_SIZE    7

#define SEND_SIZE       4096
#define SEND_TIMEOUT    3000

#define LED_GPIO_NUM        33

#define PWDN_GPIO_NUM       32
#define RESET_GPIO_NUM      -1
#define XCLK_GPIO_NUM       0
#define SIOD_GPIO_NUM       26
#define SIOC_GPIO_NUM       27

#define Y9_GPIO_NUM         35
#define Y8_GPIO_NUM         34
#define Y7_GPIO_NUM         39
#define Y6_GPIO_NUM         36
#define Y5_GPIO_NUM         21
#define Y4_GPIO_NUM         19
#define Y3_GPIO_NUM         18
#define Y2_GPIO_NUM         5
#define VSYNC_GPIO_NUM      25
#define HREF_GPIO_NUM       23
#define PCLK_GPIO_NUM       22

/////////////////////////////////

// Declare types

/////////////////////////////////

typedef struct _DInput {
  const uint8_t PIN;
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
    uint32_t data_length;
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

DInput pirSensor = {GPIO_NUM_13, false};

unsigned long previous_ms = 0;
unsigned long period_ms = 0;

/////////////////////////////////

// Declare operation variables

/////////////////////////////////
camera_config_t config;
bool photo_enable = false;

Protocol protocol;
uint8_t detect_count_num = 0;

static void IRAM_ATTR detectIntr(void* arg) {
    // TODO if arg is used, esp32 calls core error and reboot it must be changed
    // DInput* s = static_cast<DInput*>(arg);
    // s->state = true;
    pirSensor.state = true;
}

/**
 * event_type
 * data: Whole protocol
 * size: protocol data length (4 BYTE)
 */
void makePacketAndSend(int event_type, uint8_t* data, uint32_t size) {
    uint8_t *headerPacket = (uint8_t*) malloc(PROTOCOL_HEADER_SIZE);
    if (NULL == headerPacket) {
        Serial.printf("makePacketAndSend::Memory allocation failed\n");
        return;
    }

    headerPacket[0] = STX;
    headerPacket[1] = event_type;
	headerPacket[2] = (uint8_t) ((size >> 24) & 0xff);
    headerPacket[3] = (uint8_t) ((size >> 16) & 0xff);
	headerPacket[4] = (uint8_t) ((size >> 8) & 0xff);
    headerPacket[5] = (uint8_t) (size & 0xff);
    headerPacket[6] = ETX;
    
    Serial.printf("makePacketAndSend::txPacket header\n");
    for(int i = 0; i < PROTOCOL_HEADER_SIZE; i++) {
        Serial.printf("0x%02x ", headerPacket[i]);
        if (0 == (i % 10) && (i != 0)) {
            Serial.println();
        }
    }
    Serial.println();

    long ret = SerialBT.write(headerPacket, PROTOCOL_HEADER_SIZE);
    Serial.printf("makePacketAndSend::Header sending write byte size = %d\n", ret);
	
    uint32_t ptr = 0;
    uint32_t sendPacketSize = size;

    unsigned long timeoutCheck = 0;

    while (true) {
        if (sendPacketSize >= SEND_SIZE) {
            long ret = SerialBT.write(&data[ptr], SEND_SIZE);
            if (ret != 0) {
                Serial.printf("makePacketAndSend::write byte size = %d\n", ret);
                timeoutCheck = 0;
            } else {
                if (0 == timeoutCheck)
                    timeoutCheck = millis();
                else if (timeoutCheck <= millis() - SEND_TIMEOUT) {
                    Serial.printf("makePacketAndSend::write timeout!\n");
                    break;
                }
            }
            sendPacketSize -= ret;
            ptr += ret;
        } else {
            long ret = SerialBT.write(&data[ptr], sendPacketSize);
            if (ret == sendPacketSize) {
                Serial.printf("makePacketAndSend::data sending completed write byte size = %d\n", ret);
                break;
            } else if (0 == ret) {
                if (0 == timeoutCheck)
                    timeoutCheck = millis();
                else if (timeoutCheck <= millis() - SEND_TIMEOUT) {
                    Serial.printf("makePacketAndSend::write timeout!\n");
                    break;
                }
            } else {
                Serial.printf("makePacketAndSend::write byte size = %d\n", ret);
                timeoutCheck = 0;
                sendPacketSize -= ret;
                ptr += ret;
            }
        }
    }
    
	// long ret = SerialBT.write(txPacket, size + PROTOCOL_HEADER_SIZE);
    // Serial.printf("makePacketAndSend::write byte size = %d\n", ret);
	free(headerPacket);
}

void setupGpio() {
    pinMode(pirSensor.PIN, INPUT);
    pinMode(LED_GPIO_NUM, OUTPUT);

    esp_err_t err;
    err = gpio_isr_handler_add(GPIO_NUM_13, &detectIntr, &pirSensor);
    if (err != ESP_OK) {
        Serial.printf("handler add failed with error 0x%x \r\n", err);
    }
    err = gpio_set_intr_type(GPIO_NUM_13, GPIO_INTR_POSEDGE); //Rising edge
    if (err != ESP_OK) {
        Serial.printf("set intr type failed with error 0x%x \r\n", err);
    }
    // TODO add this code if sleep mode is used and this code has error "GPIO wakeup only supports level mode, but edge mode set. gpio_num:13"
    // err = gpio_wakeup_enable(GPIO_NUM_13, GPIO_INTR_POSEDGE);
    // if (err != ESP_OK) {
    //     Serial.printf("set wakeup enable failed with error 0x%x \r\n", err);
    // }
}

bool setupCamera() {
    config.ledc_channel = LEDC_CHANNEL_0;
    config.ledc_timer = LEDC_TIMER_0;
    config.pin_d0 = Y2_GPIO_NUM;
    config.pin_d1 = Y3_GPIO_NUM;
    config.pin_d2 = Y4_GPIO_NUM;
    config.pin_d3 = Y5_GPIO_NUM;
    config.pin_d4 = Y6_GPIO_NUM;
    config.pin_d5 = Y7_GPIO_NUM;
    config.pin_d6 = Y8_GPIO_NUM;
    config.pin_d7 = Y9_GPIO_NUM;
    config.pin_xclk = XCLK_GPIO_NUM;
    config.pin_pclk = PCLK_GPIO_NUM;
    config.pin_vsync = VSYNC_GPIO_NUM;
    config.pin_href = HREF_GPIO_NUM;
    config.pin_sscb_sda = SIOD_GPIO_NUM;
    config.pin_sscb_scl = SIOC_GPIO_NUM;
    config.pin_pwdn = PWDN_GPIO_NUM;
    config.pin_reset = RESET_GPIO_NUM;
    config.xclk_freq_hz = 20000000;
    config.pixel_format = PIXFORMAT_JPEG;

    if(psramFound()){
        config.frame_size = FRAMESIZE_UXGA;
        config.jpeg_quality = 10;
        config.fb_count = 2;
    } else {
        config.frame_size = FRAMESIZE_SVGA;
        config.jpeg_quality = 12;
        config.fb_count = 1;
    }

    esp_err_t err = esp_camera_init(&config);
    if (err != ESP_OK) {
        Serial.printf("Camera init failed with error 0x%x", err);
        return false;
    }

    return true;
}


void setup() {
    Serial.begin(115200);
    SerialBT.begin("Human Detector");

    photo_enable = setupCamera();
    Serial.printf("Photo enable: %s\n", photo_enable ? "true" : "false");
    setupGpio();            //gpio_install_isr_service() is called setupCamera() -> esp_camera_init() It must be declared after calling setupCamera()
}

void loop() {
    if (pirSensor.state) {
        Serial.println("Detected");

        digitalWrite(LED_GPIO_NUM, LOW);
        previous_ms = millis();
        period_ms = 1000;
        
        pirSensor.state = false;
        detect_count_num++;

        if (!photo_enable) {
            protocol.protocol_header.event_type = DETECT_EVENT;
            protocol.protocol_header.data_length = 1;
            protocol.data = (uint8_t*) malloc(sizeof(uint8_t) * 1);
            protocol.data[0] = detect_count_num;

            makePacketAndSend(protocol.protocol_header.event_type, protocol.data, protocol.protocol_header.data_length);
            free(protocol.data);
        } else {
            protocol.protocol_header.event_type = DETECT_PHOTO_EVENT;
        
            camera_fb_t* fb = NULL;
            fb = esp_camera_fb_get();
            if (!fb) {
                Serial.println("Camera capture failed");
                protocol.protocol_header.data_length = 0;
            } else {
                Serial.printf("Camera capture successed picture size: %d\n", fb->len);
                //TODO 1 byte can only save to 255 but picture size is much larger than 1 byte
                protocol.protocol_header.data_length = fb->len;
                // memory waste
                // protocol.data = (uint8_t*) malloc(sizeof(uint8_t) * fb->len);
                // memcpy(protocol.data, fb->buf, fb->len);
            }

            makePacketAndSend(protocol.protocol_header.event_type, fb->buf, protocol.protocol_header.data_length);
            esp_camera_fb_return(fb);
            free(protocol.data);
        }

        // TODO SerialBT.available() is not working
        // if (SerialBT.available()) {
            
        // }
    } else {
        if (millis() - previous_ms >= period_ms) {
            digitalWrite(LED_GPIO_NUM, HIGH);
        }
    }
}
