#include <ESP32Servo.h>
#include <HX711.h>		// incluye libreria HX711
#include <SPI.h>
#include <MFRC522.h>
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>
#include <freertos/semphr.h>
#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <PubSubClient.h>
#include <time.h>
#include <Preferences.h>

int check_RFID();
int check_renovar_comida();
int check_servir_comida();
int check_recargar_dispenser();
int check_conexion();
int check_proximity();

/* PINES */
#define BUZZER_PIN 25
#define SERVO_PIN 32
// Sensor proximidad
#define TRIG_PIN 18 // ESP32 pin connected to Ultrasonic Sensor's TRIG pin
#define ECHO_PIN 5 // ESP32 pin connected to Ultrasonic Sensor's ECHO pin
// Balanza
#define DT_PIN 23			// DT de HX711 a pin digital 2
#define SCK_PIN 22			// SCK de HX711 a pin digital 3
// RFID
#define SS_PIN 13
#define SCK_RFID_PIN 12
#define MOSI_PIN 14
#define MISO_PIN 27
#define RST_PIN 26

/* CONSTANTES */
#define FRECUENCIA_SERIAL 115200
#define VTASK_SLEEP_TIME 250
#define COMANDO_HORA_COMIDA 'h'
#define NTP_SERVER "pool.ntp.org"
#define GMT_OFFSET_SEC -10800
#define DAYLIGHT_OFFSET 0
// Balanza
#define POT_MIN 0
#define POT_MAX 4095
#define TO_POT 30000      // 10 seg para detectar que no hay mas alimento
#define UMBRAL_DIFERENCIA_DE_PESO 3
#define ESCALA_BALANZA 400.27f
// Servo
#define SERVO_TIMER 3
#define POS_MIN 0
#define POS_MAX 180
#define POS_APERTURA 60
// Sensor Distancia
#define TIEMPO_ACTIVACION_TRIGGER 10
#define PROX_MAX_CM 30.0
#define VALOR_TRANSFORMACION_CM_MS 0.017
#define TO_PROXIMITY 2000  // Timeout para volver a escanear por proximidad
// BUZZER
#define TIEMPO_BUZZER 1000
#define SHORT_BUZZ 200 // Duración corta en milisegundos
#define LONG_BUZZ 1000  // Duración larga en milisegundos
// Wifi
#define WIFI_SSID "Miro"
#define WIFI_PASS "agus1234"
// MQTT
#define MQTT_PORT 8883
#define MQTT_SERVER "y8ad1cae.ala.us-east-1.emqxsl.com"
#define MQTT_USER "PET_FEEDER_L6"
#define MQTT_PASSWORD "123456"
#define MQTT_TOPICO_HORA_COMIDA "/pet-feeder/hora-comida"
#define MQTT_TOPICO_ALIMENTACION "/pet-feeder/alimentacion"
#define MQTT_TOPICO_SINCRONIZAR "/pet-feeder/sincronizar"
#define MQTT_TOPICO_ESTADO "/pet-feeder/estado"
#define MQTT_TOPICO_ESTADISTICA "/pet-feeder/estadistica"

/**ESTADOS**/
#define CANT_ESTADOS 7
#define ESTADO_ESPERA 1000
#define ESTADO_DETECTA_PRESENCIA 1001
#define ESTADO_DETECTA_RFID 1002
#define ESTADO_RENOVAR_COMIDA 1003
#define ESTADO_SERVIR_COMIDA 1004
#define ESTADO_PEDIR_RECARGA 1005
#define ESTADO_INIT 1006

/**EVENTOS**/
#define CANT_EVENTOS 12
#define EVENTO_PRESENCIA_ON 2000
#define EVENTO_PRESENCIA_OFF 2001
#define EVENTO_DETECTA_RFID 2002
#define EVENTO_RFID_LEIDO 2003
#define EVENTO_RENOVAR_COMIDA 2004  // SI PESO = UMBRAL
#define EVENTO_COMIDA_RENOVADA 2005 // SI PESO = 0
#define EVENTO_HORA_COMIDA 2006     // SI HORA DE COMIDA Y PESO < UMBRAL
#define EVENTO_COMIDA_SERVIDA 2007
#define EVENTO_SIN_COMIDA 2009
#define EVENTO_RECARGA_COMIDA 2010
#define EVENTO_CONTINUE 2011
#define EVENTO_CONEXION_ON 2012
#define EVENTO_CONEXION_OFF 2013

/** STRUCTS **/
struct Evento
{
  int (*handler)(void);
  int evento;
  char nombre[50];
};
struct Estado
{
  int estado;
  char nombre[50];
};

struct BuzzerParams 
{
    int duration;
    int repeats;
};

// DECLARO LOS ESTADOS PARA IMPRIMIR SU NOMBRE
struct Estado estados[CANT_ESTADOS] = {
    {ESTADO_ESPERA, "ESTADO_ESPERA"},
    {ESTADO_DETECTA_PRESENCIA, "ESTADO_DETECTA_PRESENCIA"},
    {ESTADO_DETECTA_RFID, "ESTADO_DETECTA_RFID"},
    {ESTADO_RENOVAR_COMIDA, "ESTADO_RENOVAR_COMIDA"},
    {ESTADO_SERVIR_COMIDA, "ESTADO_SERVIR_COMIDA"},
    {ESTADO_PEDIR_RECARGA, "ESTADO_PEDIR_RECARGA"},
    {ESTADO_INIT, "ESTADO_INIT"},
};
// DECLARO LOS EVENTOS CON SUS ACCIONES CORRESPONDIENTES PARA REALIZAR POOLING
struct Evento eventos[CANT_EVENTOS] = {
    {&check_RFID, EVENTO_DETECTA_RFID, "EVENTO_DETECTA_RFID"},
    {&check_RFID, EVENTO_RFID_LEIDO, "EVENTO_RFID_LEIDO"},
    {&check_proximity, EVENTO_PRESENCIA_ON, "EVENTO_PRESENCIA_ON"},
    {&check_proximity, EVENTO_PRESENCIA_OFF, "EVENTO_PRESENCIA_OFF"},
    {&check_renovar_comida, EVENTO_RENOVAR_COMIDA, "EVENTO_RENOVAR_COMIDA"},
    {&check_renovar_comida, EVENTO_COMIDA_RENOVADA, "EVENTO_COMIDA_RENOVADA"},
    {&check_recargar_dispenser, EVENTO_SIN_COMIDA, "EVENTO_SIN_COMIDA"},
    {&check_recargar_dispenser, EVENTO_RECARGA_COMIDA, "EVENTO_RECARGA_COMIDA"},
    {&check_servir_comida, EVENTO_HORA_COMIDA, "EVENTO_HORA_COMIDA"},
    {&check_conexion, EVENTO_CONEXION_OFF, "EVENTO_CONEXION_OFF"},
    {&check_conexion, EVENTO_CONEXION_ON, "EVENTO_CONEXION_ON"},
    {&check_servir_comida, EVENTO_COMIDA_SERVIDA, "EVENTO_COMIDA_SERVIDA"}};

/** VARIABLES GLOBALES **/
const char* root_ca = "-----BEGIN CERTIFICATE-----\n"
"MIIDrzCCApegAwIBAgIQCDvgVpBCRrGhdWrJWZHHSjANBgkqhkiG9w0BAQUFADBh\n"
"MQswCQYDVQQGEwJVUzEVMBMGA1UEChMMRGlnaUNlcnQgSW5jMRkwFwYDVQQLExB3\n"
"d3cuZGlnaWNlcnQuY29tMSAwHgYDVQQDExdEaWdpQ2VydCBHbG9iYWwgUm9vdCBD\n"
"QTAeFw0wNjExMTAwMDAwMDBaFw0zMTExMTAwMDAwMDBaMGExCzAJBgNVBAYTAlVT\n"
"MRUwEwYDVQQKEwxEaWdpQ2VydCBJbmMxGTAXBgNVBAsTEHd3dy5kaWdpY2VydC5j\n"
"b20xIDAeBgNVBAMTF0RpZ2lDZXJ0IEdsb2JhbCBSb290IENBMIIBIjANBgkqhkiG\n"
"9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4jvhEXLeqKTTo1eqUKKPC3eQyaKl7hLOllsB\n"
"CSDMAZOnTjC3U/dDxGkAV53ijSLdhwZAAIEJzs4bg7/fzTtxRuLWZscFs3YnFo97\n"
"nh6Vfe63SKMI2tavegw5BmV/Sl0fvBf4q77uKNd0f3p4mVmFaG5cIzJLv07A6Fpt\n"
"43C/dxC//AH2hdmoRBBYMql1GNXRor5H4idq9Joz+EkIYIvUX7Q6hL+hqkpMfT7P\n"
"T19sdl6gSzeRntwi5m3OFBqOasv+zbMUZBfHWymeMr/y7vrTC0LUq7dBMtoM1O/4\n"
"gdW7jVg/tRvoSSiicNoxBN33shbyTApOB6jtSj1etX+jkMOvJwIDAQABo2MwYTAO\n"
"BgNVHQ8BAf8EBAMCAYYwDwYDVR0TAQH/BAUwAwEB/zAdBgNVHQ4EFgQUA95QNVbR\n"
"TLtm8KPiGxvDl7I90VUwHwYDVR0jBBgwFoAUA95QNVbRTLtm8KPiGxvDl7I90VUw\n"
"DQYJKoZIhvcNAQEFBQADggEBAMucN6pIExIK+t1EnE9SsPTfrgT1eXkIoyQY/Esr\n"
"hMAtudXH/vTBH1jLuG2cenTnmCmrEbXjcKChzUyImZOMkXDiqw8cvpOp/2PV5Adg\n"
"06O/nVsJ8dWO41P0jmP6P6fbtGbfYmbW0W5BjfIttep3Sp+dWOIrWcBAI+0tKIJF\n"
"PnlUkiaY4IBIqDfv8NZ5YBberOgOzW6sRBc4L0na4UU+Krk2U886UAb3LujEV0ls\n"
"YSEY1QSteDwsOoBrp+uvFRTp2InBuThs4pFsiv9kuXclVzDAGySj4dzp30d8tbQk\n"
"CAUw7C29C79Fv1C5qfPrmAESrciIxpg0X40KPMbp1ZWVbd4=\n"
"-----END CERTIFICATE-----\n";

int estado_actual, estado_anterior, evento_actual, potValue, indexEvento,cant_comida_inicio_presencia, diferencia_cant_comida, gramosDeseados;
float duration_us, distance_cm;
bool alimentoSuficiente, proximidadDetectada, proximidadAnterior, esHoraComida, dispenserVacio, RFID_detectado, RFID_leido;
unsigned long currentTime, lastProximityTime, lastPotTime;
unsigned int umbralAlimentoMax = POT_MAX / 15;
unsigned int prevPotValue = UMBRAL_DIFERENCIA_DE_PESO;
String RFID_key_leido;
SemaphoreHandle_t xMutex;

Evento evento_poll;
Servo myServo;
HX711 celda;
MFRC522 mfrc522(SS_PIN, RST_PIN); 
WiFiClientSecure espClient;
PubSubClient client(espClient);
Preferences preferences;

void setup()
{
  // put your setup code here, to run once:
  Serial.begin(FRECUENCIA_SERIAL);
  WiFi.begin(WIFI_SSID, WIFI_PASS);
  client.setServer(MQTT_SERVER, MQTT_PORT);
  client.setCallback(callback);
  // Configurar la conexión segura SSL/TLS
  espClient.setCACert(root_ca); // Debes proporcionar el certificado raíz del servidor MQTT
  estado_actual = ESTADO_INIT;
  xMutex = xSemaphoreCreateMutex();
  // configure the trigger pin to output mode
  pinMode(TRIG_PIN, OUTPUT);
  // configure the echo pin to input mode
  pinMode(ECHO_PIN, INPUT);    
  pinMode(BUZZER_PIN, OUTPUT);
  digitalWrite(BUZZER_PIN, LOW);
  celda.begin(DT_PIN, SCK_PIN);		// inicializa objeto con los pines a utilizar
  celda.set_scale(ESCALA_BALANZA);	// establece el factor de escala obtenido del programa de calibracion
  celda.tare();	
  SPI.begin(SCK_RFID_PIN,MISO_PIN,MOSI_PIN); // Iniciar SPI bus
  mfrc522.PCD_Init();
  // Asignar un temporizador específico al servo
  ESP32PWM::allocateTimer(SERVO_TIMER); // Asigna TIMER3
  myServo.attach(SERVO_PIN);
  currentTime = millis();
  lastPotTime = currentTime;
  // setea el valor inicial para que de timeout
  lastProximityTime = currentTime - TO_PROXIMITY - 1;
  launchWeightReadTask();
  logFSM();
  close_door();
  configTime(GMT_OFFSET_SEC, DAYLIGHT_OFFSET, NTP_SERVER);
  printLocalTime();
}

void loop()
{
  if (client.connected()) 
  {
    client.loop();
  }
  fsm();
  currentTime = millis(); 
}

void fsm()
{
  estado_anterior = estado_actual;
  generaEvento();
  switch (estado_actual)
  {
    case ESTADO_INIT:
    {
      switch (evento_actual) 
      {
        case EVENTO_CONEXION_ON:
        {
          estado_actual = ESTADO_ESPERA;
        }
        break;
      }
    }
    break;
    case ESTADO_ESPERA:
    {
      switch (evento_actual)
      {
        case EVENTO_CONEXION_OFF:
        {
          estado_actual = ESTADO_INIT;
        }
        break;
        case EVENTO_PRESENCIA_ON:
        {
          // TODO ACCION
          estado_actual = ESTADO_DETECTA_PRESENCIA;
          cant_comida_inicio_presencia = potValue;
        }
        break;
        case EVENTO_RENOVAR_COMIDA:
        {
          launchBuzzerTask(SHORT_BUZZ, 2);  // Sonido corto 3 veces
          estado_actual = ESTADO_RENOVAR_COMIDA;
        }
        break;
        case EVENTO_HORA_COMIDA:
        {
          // TODO ACCION
          open_door(); 
          estado_actual = ESTADO_SERVIR_COMIDA;
        }
        break;
      }
    }
    break;
    case ESTADO_DETECTA_PRESENCIA:
    {
      switch (evento_actual)
      {
        case EVENTO_CONEXION_OFF:
        {
          estado_actual = ESTADO_INIT;
        }
        break;
        case EVENTO_PRESENCIA_OFF:
        {
          // TODO ACCION
          diferencia_cant_comida = cant_comida_inicio_presencia - potValue;

          if(diferencia_cant_comida > UMBRAL_DIFERENCIA_DE_PESO)
          {
            String mensaje = String(RFID_key_leido) + ";" + String(diferencia_cant_comida);
            
            //publica en el topico de estadistica
            client.publish(MQTT_TOPICO_ESTADISTICA,mensaje.c_str());
          }
          
          estado_actual = ESTADO_ESPERA;
        }
        break;
        case EVENTO_DETECTA_RFID:
        {
          // TODO ACCION
          leer_RFID();
          estado_actual = ESTADO_DETECTA_RFID;
        }
        break;
      }
    }
    break;
    case ESTADO_DETECTA_RFID:
    {
      switch (evento_actual)
      {
        case EVENTO_CONEXION_OFF:
        {
          estado_actual = ESTADO_INIT;
        }
        break;
        case EVENTO_RFID_LEIDO:
        {
          // TODO ACCION
          RFID_leido = false;
          estado_actual = ESTADO_DETECTA_PRESENCIA;
        }
        break;
      }
    }
    break;
    case ESTADO_RENOVAR_COMIDA:
    {
      switch (evento_actual)
      {
        case EVENTO_CONEXION_OFF:
        {
          estado_actual = ESTADO_INIT;
        }
        break;
        case EVENTO_COMIDA_RENOVADA:
        {
          // TODO ACCION
          estado_actual = ESTADO_ESPERA;
        }
        break;
      }
    }
    break;
    case ESTADO_SERVIR_COMIDA:
    {
      switch (evento_actual)
      {
        case EVENTO_CONEXION_OFF:
        {
          estado_actual = ESTADO_INIT;
        }
        break;
        case EVENTO_COMIDA_SERVIDA:
        {
          // Cierro la puerta, prendo el buzzer y dejo de estar en horario comida
          esHoraComida = false;
          close_door(); 
          launchBuzzerTask(LONG_BUZZ, 1);  // Sonido largo una vez
          estado_actual = ESTADO_ESPERA;
        }
        break;
        case EVENTO_SIN_COMIDA:
        {
          launchBuzzerTask(SHORT_BUZZ, 5);  // Sonido corto una vez     
          estado_actual = ESTADO_PEDIR_RECARGA;
        }
        break;
      }
    }
    break;
    case ESTADO_PEDIR_RECARGA:
    {
      switch (evento_actual)
      {
        case EVENTO_CONEXION_OFF:
        {
          estado_actual = ESTADO_INIT;
        }
        break;
        case EVENTO_RECARGA_COMIDA:
        {
          // TODO ACCION)
          estado_actual = ESTADO_SERVIR_COMIDA;
        }
        break;
      }
    }
    break;
  }
  logFSM();
}

String obtenerHoraActual()
{
  struct tm timeinfo;
  if(!getLocalTime(&timeinfo))
  {
    Serial.println("Error al obtener la hora");
    return String(); // Devuelve una cadena vacía en caso de error
  }
  
  char horaActual[6]; // HH:MM tiene 5 caracteres + 1 para el terminador nulo
  strftime(horaActual, sizeof(horaActual), "%H:%M", &timeinfo);
  
  return String(horaActual); // Convierte el char[] a String y lo devuelve
}

void obtener_dia()
{
    preferences.begin("my-app", false);

    char fecha[10]; // Buffer para almacenar la fecha en formato dd/mm

    // Obtener la estructura de tiempo actual
    struct tm timeinfo;
    if(!getLocalTime(&timeinfo))
    {
      Serial.println("Failed to obtain time");
      return;
    }

    // Formatear la fecha en dd/mm
    strftime(fecha, sizeof(fecha), "%d/%m", &timeinfo);

    // Leer la fecha almacenada previamente
    String fechaAnterior = preferences.getString("fecha");

    // Comparar la fecha actual con la almacenada y guardar si es diferente
    if(fechaAnterior.indexOf(fecha) == -1)
    {
        if (!preferences.putString("fecha", fecha)) 
        {          
          Serial.println("Error al guardar la fecha.");
        }

      Serial.println("Fecha actualizada: " + String(fecha));
    }

    preferences.end();
}

bool check_procesados(String entryTime)
{
  preferences.begin("my-app", false);
    
  String fecha = preferences.getString("fecha", "");
  
  if(fecha.indexOf(entryTime) == -1)
  {
    Serial.print("ENTRO");
    fecha += ",";
    fecha += entryTime;
    preferences.putString("fecha", fecha);
    return true;
  }
  else 
  {
    return false;
  }
  preferences.end();
}
// Función para comprobar la hora y ejecutar la acción correspondiente
void checkAndExecute() 
{
  obtener_dia();

  String horaActual = obtenerHoraActual();
  
  if (horaActual == "") return;

  preferences.begin("my-app", true);

  String data = preferences.getString("data", "");
  preferences.end();

  if (data.length() > 0)
  {
    int startIndex = 0;
    int endIndex = data.indexOf(',');
    String fecha;

    while (endIndex != -1) 
    {
      String entry = data.substring(startIndex, endIndex);
      String entryTime = entry.substring(0, entry.indexOf(';'));
      String grams = entry.substring(entry.indexOf(';') + 1);

      if (entryTime == horaActual) 
      {
        if(check_procesados(entryTime))
        {
          esHoraComida = true; 
          gramosDeseados = grams.toInt();
        }
       
      }

      startIndex = endIndex + 1;
      endIndex = data.indexOf(',', startIndex);
    }

    // Comprobar el último o único elemento
    String entry = data.substring(startIndex);
    String entryTime = entry.substring(0, entry.indexOf(';'));
    String grams = entry.substring(entry.indexOf(';') + 1);

    if (entryTime == horaActual) 
    {
      if(check_procesados(entryTime))
      {
        esHoraComida = true; 
        gramosDeseados = grams.toInt();
      }
    }
  }
}

void generaEvento()
{
  // COMPARO CONDICIONES POR POOLING CICLICO
  evento_poll = eventos[indexEvento];
  indexEvento = (++indexEvento) % (CANT_EVENTOS);
  if (evento_poll.evento == evento_poll.handler())
  {
    evento_actual = evento_poll.evento;
  }
}
void check_weight()
{
  // solo importa el timeout cuando es el momento de servir la comida
  if (!esHoraComida)
    lastPotTime = currentTime;
  unsigned long timediff = currentTime - lastPotTime;
  // guardamos la variable compartida para evitar lecturas sucias en la ejecucion de la funcion
  xSemaphoreTake(xMutex, portMAX_DELAY);
  int pesoActual = potValue;
  xSemaphoreGive(xMutex);
  int diferenciaPeso = (prevPotValue - pesoActual);

  // Checkeo que haya cambiado el valor anterior leido
  if (abs(diferenciaPeso) > UMBRAL_DIFERENCIA_DE_PESO) 
  {
    alimentoSuficiente = pesoActual > gramosDeseados;
    Serial.print(pesoActual);
    Serial.println(" gr      ");
    // La compuerta permanece abierta mientras el peso actual sea menor al alimento deseado
    if (!alimentoSuficiente)
    {
      dispenserVacio = false;
    }
    prevPotValue = pesoActual;
    lastPotTime = currentTime;
  }
  else if (timediff > TO_POT) // si supero el timeout con el mismo peso significa que no hay mas alimento
  {
    dispenserVacio = true;
  }
}


int check_servir_comida()
{
  checkAndExecute();
  check_weight();

  int eventoReturn = EVENTO_CONTINUE;
  if (alimentoSuficiente)
    eventoReturn = EVENTO_COMIDA_SERVIDA;
  if (!alimentoSuficiente && esHoraComida && potValue < gramosDeseados)
    eventoReturn = EVENTO_HORA_COMIDA;
  return eventoReturn;
}

int check_recargar_dispenser()
{
  check_weight();
  int eventoReturn = EVENTO_CONTINUE;
  if (dispenserVacio) //es un time out en check weight
    eventoReturn = EVENTO_SIN_COMIDA;
  if (!dispenserVacio)
    eventoReturn = EVENTO_RECARGA_COMIDA;
  return eventoReturn;
}

int check_renovar_comida()
{
  check_weight();
  int eventoReturn = EVENTO_CONTINUE;
  if (potValue < UMBRAL_DIFERENCIA_DE_PESO)
    eventoReturn = EVENTO_COMIDA_RENOVADA;
  if (alimentoSuficiente && esHoraComida)
    eventoReturn = EVENTO_RENOVAR_COMIDA;
  return eventoReturn;
}

int check_proximity()
{
  int eventoReturn = EVENTO_CONTINUE;
  // solo checkea cuando pasa el time out de proximidad TO_PROXIMITY
  unsigned long timediff = currentTime - lastProximityTime;
  if (timediff > TO_PROXIMITY)
  {
    proximidadAnterior = proximidadDetectada;

    // generate 10-microsecond pulse to TRIG pin
    digitalWrite(TRIG_PIN, HIGH);
    delayMicroseconds(TIEMPO_ACTIVACION_TRIGGER);
    digitalWrite(TRIG_PIN, LOW);

    // measure duration of pulse from ECHO pin
    duration_us = pulseIn(ECHO_PIN, HIGH);

    // calculate the distance
    distance_cm = VALOR_TRANSFORMACION_CM_MS * duration_us;
    proximidadDetectada = distance_cm < PROX_MAX_CM;

    lastProximityTime = currentTime;
  }
  if (!proximidadAnterior && proximidadDetectada)
    eventoReturn = EVENTO_PRESENCIA_ON;
  if (proximidadAnterior && !proximidadDetectada)
    eventoReturn = EVENTO_PRESENCIA_OFF;

  return eventoReturn;
}


void open_door()
{
  // Abrir compuerta
  myServo.write(POS_APERTURA);
}
void close_door()
{
  // Cerrar compuerta
  myServo.write(POS_MIN);
}

void buzzer_control(int duration, int repeats)
{
  for (int i = 0; i < repeats; i++) 
  {
      digitalWrite(BUZZER_PIN, HIGH);
      vTaskDelay(duration / portTICK_PERIOD_MS);
      digitalWrite(BUZZER_PIN, LOW);
      vTaskDelay(100 / portTICK_PERIOD_MS);  // Pequeña pausa entre sonidos
  }
}

void logFSM()
{
  if (estado_anterior != estado_actual)
  {
    // Construir la cadena de caracteres con toda la información
    String mensaje = "Distancia: " + String(distance_cm) + " Peso: " + String(potValue) + " Estado Anterior: " + getEstado(estado_anterior) + " Evento Actual: " + evento_poll.nombre + " Estado Actual: " + getEstado(estado_actual);
    String mensajeMQTT = String(distance_cm) + ";" + String(potValue) + ";" + getEstado(estado_anterior) + ";" + evento_poll.nombre + ";" + getEstado(estado_actual);

    Serial.println("---------");
    Serial.println(mensaje.c_str());
    Serial.println("---------");

    // Publicar al tópico /pet-feeder/estado
    client.publish(MQTT_TOPICO_ESTADO, mensajeMQTT.c_str());
  }
}
char *getEstado(int estado)
{
  for (int i = 0; i < CANT_ESTADOS; i++)
  {
    if (estados[i].estado == estado)
      return estados[i].nombre;
  }
  return "";
}

int check_RFID()
{
  int eventoReturn = EVENTO_CONTINUE;
  // Se simula RFID con un boton
  RFID_detectado = mfrc522.PICC_IsNewCardPresent();
  if (RFID_leido)
    eventoReturn = EVENTO_RFID_LEIDO;
  if (RFID_detectado)
    eventoReturn = EVENTO_DETECTA_RFID;
  return eventoReturn;
}
void leer_RFID()
{

  mfrc522.PICC_ReadCardSerial();
  // Seleccionar una tarjeta
  // Mostrar UID en el monitor serie
  Serial.print("UID de la tarjeta:");
  RFID_key_leido = "";
  for (byte i = 0; i < mfrc522.uid.size; i++) 
  {
    Serial.print(mfrc522.uid.uidByte[i] < 0x10 ? " 0" : " ");
    Serial.print(mfrc522.uid.uidByte[i], HEX);
    char hexString[3]; // Buffer to store hex string
    sprintf(hexString, "%02X", mfrc522.uid.uidByte[i]);
    RFID_key_leido += hexString;
  }
  Serial.println();

  // Detener comunicación con la tarjeta
  mfrc522.PICC_HaltA();
  // Logica para leer RFID
  RFID_leido = true;
}
void launchWeightReadTask() //FreeRTOS
{
  xTaskCreatePinnedToCore(
    get_units_task,      // Función de la tarea
    "Get Units Task",    // Nombre de la tarea
    10000,               // Tamaño de la pila
    NULL,                // Parámetro de la tarea
    2,                   // Prioridad de la tarea (ajustar según sea necesario)
    NULL,                // Puntero a la tarea
    1                    // Núcleo donde se ejecutará la tarea (0 = primer núcleo, 1 = segundo núcleo)
  );
}


void launchBuzzerTask(int duration, int repeats) // FreeRTOS
{
  BuzzerParams* params = new BuzzerParams{duration, repeats};
  xTaskCreatePinnedToCore(
    buzzerTask,    // Función de la tarea
    "Buzzer Task", // Nombre de la tarea
    10000,         // Tamaño de la pila (stack size)
    params,        // Parámetro que se pasa a la tarea
    1,             // Prioridad de la tarea
    NULL,          // Puntero a la tarea
    1              // Núcleo donde se ejecutará la tarea (0 = primer núcleo, 1 = segundo núcleo)
  );
}

void buzzerTask(void *parameter) 
{
  BuzzerParams* params = static_cast<BuzzerParams*>(parameter);
  buzzer_control(params->duration, params->repeats);
  delete params; // Liberar la memoria asignada para los parámetros
  vTaskDelete(NULL);  // Termina la tarea cuando se complete
}

void get_units_task(void *parameter) 
{
  while(true)
  {  
    celda.power_up();
    // Realizar la lectura bloqueante
    int newValue = celda.get_units(10);
    celda.power_down();	
    xSemaphoreTake(xMutex, portMAX_DELAY);
    potValue = newValue;
    xSemaphoreGive(xMutex);
    // Dormir un tiempo para permitir otras operaciones (ajustar según sea necesario)
    vTaskDelay(VTASK_SLEEP_TIME / portTICK_PERIOD_MS);
  }
}
void callback(char* topic, byte* payload, unsigned int length)
{
  Serial.print("Mensaje recibido en el tópico: ");
  Serial.print(topic);
  Serial.print(". Length: ");
  Serial.print(length);
  Serial.print(". Contenido: ");

  String message;
  for (unsigned int i = 0; i < length; i++) 
  {
    message += (char)payload[i];
  }
  Serial.println(message);

  preferences.begin("my-app", false);

  if (strcmp(topic, MQTT_TOPICO_ALIMENTACION) == 0) 
  { 
    // Concatenar el nuevo string al existente
    String currentData = preferences.getString("data", "");
    if (currentData.length() > 0)
    {
      currentData += ",";
    }
    currentData += message;
    preferences.putString("data", currentData);
    Serial.println("Datos guardados en NVS: " + currentData);
  } 
  else if (strcmp(topic, MQTT_TOPICO_SINCRONIZAR) == 0) 
  {
    // Reemplazar el contenido de "data" con el nuevo payload
    preferences.putString("data", message);
    Serial.println("Datos sincronizados en NVS: " + message);
  } 
  else if (strcmp(topic, MQTT_TOPICO_HORA_COMIDA) == 0) 
  {
    Serial.print(". ES HORA COMIDA POR MQTT"); //acá tengo q hacer lo de la balanza
    preferences.putString("horaComida", message); // hace falta? 
    
    size_t pos = message.indexOf(';');
    String numberPart = message.substring(pos + 1); // Obtiene la subcadena a partir del delimitador

    gramosDeseados = numberPart.toInt();

    esHoraComida = true;
  }
  
  preferences.end();
}

int check_conexion() 
{
  bool mqtt = false;
  bool wifi = false;
  bool time = false;

  struct tm timeinfo;
  if (WiFi.status() != WL_CONNECTED) 
  {
    Serial.println("Connecting to WiFi...");
  } 
  else
  {
    wifi = true;
    if (!getLocalTime(&timeinfo)) 
    {
      Serial.println("Fallo al obtener la hora");
    }
    else 
    {
      time = true;
    }
    mqtt = client.connected();
    if (!mqtt) 
    {
      Serial.println("Conectando al servidor MQTT...");
      if (client.connect("ESP32Client", MQTT_USER, MQTT_PASSWORD))
      {
        mqtt = true;
        Serial.println("Conectado al servidor MQTT");
        client.subscribe(MQTT_TOPICO_HORA_COMIDA);
        client.subscribe(MQTT_TOPICO_SINCRONIZAR);
        client.subscribe(MQTT_TOPICO_ALIMENTACION);
      } 
      else 
      {
        Serial.print("Fallo en la conexión, rc=");
        Serial.println(client.state());
      }
    }
  }

  if (wifi && mqtt && time)
  {
    return EVENTO_CONEXION_ON;
  }
  return EVENTO_CONEXION_OFF;
}
void printLocalTime() 
{
  struct tm timeinfo;
  if (!getLocalTime(&timeinfo)) 
  {
    Serial.println("Fallo al obtener la hora");
    return;
  }
  Serial.println(&timeinfo, "%A, %B %d %Y %H:%M:%S");
}