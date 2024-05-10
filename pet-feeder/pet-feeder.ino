#include <ESP32Servo.h>

int check_weight();
int check_proximity();
void door_control();
void buzzer_control(int tono);
void logFSM();
char* getEstado(int estado);
void leerComando();
int check_RFID();
void leer_RFID();

#define BUTTON_PIN 32
#define BUZZER_PIN 21
#define TRIG_PIN 12 // ESP32 pin connected to Ultrasonic Sensor's TRIG pin
#define ECHO_PIN 13 // ESP32 pin connected to Ultrasonic Sensor's ECHO pin
#define SERVO_PIN 14
#define POT_PIN 27
#define POT_MIN 0
#define POT_MAX 4095
#define POS_MIN 0
#define POS_MAX 180
#define PROX_MAX_CM 50.0
#define TONO_COMIDA_SERVIDA 420
#define TONO_PEDIR_RECARGA 630
#define TONO_RENOVAR_COMIDA 330
#define TO_PROXIMITY 3000 // Timeout para volver a escanear por proximidad
#define TO_POT 10000      // 10 seg para detectar que no hay mas alimento
#define CANT_EVENTOS 10
#define CANT_ESTADOS 6
#define COMANDO_HORA_COMIDA 'h'

/**ESTADOS**/
#define ESTADO_ESPERA 1000
#define ESTADO_DETECTA_PRESENCIA 1001
#define ESTADO_DETECTA_RFID 1002
#define ESTADO_RENOVAR_COMIDA 1003
#define ESTADO_SERVIR_COMIDA 1004
#define ESTADO_PEDIR_RECARGA 1005

/**EVENTOS**/
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
struct Estado estados[CANT_ESTADOS] = {
    {ESTADO_ESPERA, "ESTADO_ESPERA"},
    {ESTADO_DETECTA_PRESENCIA, "ESTADO_DETECTA_PRESENCIA"},
    {ESTADO_DETECTA_RFID, "ESTADO_DETECTA_RFID"},
    {ESTADO_RENOVAR_COMIDA, "ESTADO_RENOVAR_COMIDA"},
    {ESTADO_SERVIR_COMIDA, "ESTADO_SERVIR_COMIDA"},
    {ESTADO_PEDIR_RECARGA, "ESTADO_PEDIR_RECARGA"},
};
  struct Evento eventos[CANT_EVENTOS] = {
      {&check_RFID, EVENTO_DETECTA_RFID, "EVENTO_DETECTA_RFID"},
      {&check_RFID, EVENTO_RFID_LEIDO, "EVENTO_RFID_LEIDO"},
      {&check_proximity, EVENTO_PRESENCIA_ON, "EVENTO_PRESENCIA_ON"},
      {&check_proximity, EVENTO_PRESENCIA_OFF, "EVENTO_PRESENCIA_OFF"},
      {&check_weight, EVENTO_RENOVAR_COMIDA, "EVENTO_RENOVAR_COMIDA"},
      {&check_weight, EVENTO_COMIDA_RENOVADA, "EVENTO_COMIDA_RENOVADA"},
      {&check_weight, EVENTO_HORA_COMIDA, "EVENTO_HORA_COMIDA"},
      {&check_weight, EVENTO_SIN_COMIDA, "EVENTO_SIN_COMIDA"},
      {&check_weight, EVENTO_RECARGA_COMIDA, "EVENTO_RECARGA_COMIDA"},
      {&check_weight, EVENTO_COMIDA_SERVIDA, "EVENTO_COMIDA_SERVIDA"}};
int estado_actual, estado_anterior, evento_actual, potValue, indexEvento;
float duration_us, distance_cm;
bool alimentoSuficiente, proximidadDetectada, proximidadAnterior, esHoraComida, dispenserVacio, RFID_detectado, RFID_leido;
unsigned long currentTime, lastProximityTime, lastPotTime;
unsigned int umbralAlimentoMax = POT_MAX / 3;
unsigned int prevPotValue = POT_MAX;

Evento evento_poll;
Servo myServo;

void setup()
{
  // put your setup code here, to run once:
  Serial.begin(115200);
  estado_actual = ESTADO_ESPERA;
  // configure the trigger pin to output mode
  pinMode(TRIG_PIN, OUTPUT);
  // configure the echo pin to input mode
  pinMode(ECHO_PIN, INPUT);
  pinMode(BUZZER_PIN, OUTPUT);
  pinMode(BUTTON_PIN, INPUT_PULLUP);
  myServo.attach(SERVO_PIN);
  currentTime = millis();
  lastPotTime = currentTime;
  // setea el valor inicial para que de timeout
  lastProximityTime = currentTime - TO_PROXIMITY - 1;
  logFSM();
}

void loop()
{
  fsm();
  currentTime = millis(); // this speeds up the simulation
}

void fsm()
{
  estado_anterior = estado_actual;
  generaEvento();
  switch (estado_actual)
  {
  case ESTADO_ESPERA:
  {
    switch (evento_actual)
    {
    case EVENTO_PRESENCIA_ON:
    {
      // TODO ACCION
      estado_actual = ESTADO_DETECTA_PRESENCIA;
    }
    break;
    case EVENTO_RENOVAR_COMIDA:
    {
      // TODO ACCION
      buzzer_control(TONO_RENOVAR_COMIDA);
      estado_actual = ESTADO_RENOVAR_COMIDA;
    }
    break;
    case EVENTO_HORA_COMIDA:
    {
      // TODO ACCION
      door_control();
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
    case EVENTO_PRESENCIA_OFF:
    {
      // TODO ACCION
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
    case EVENTO_RFID_LEIDO:
    {
      // TODO ACCION
      RFID_leido = false;
      Serial.println("Se leyo el RFID 1A2B3C4D5E6F7A de prueba.");
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
    case EVENTO_COMIDA_SERVIDA:
    {
      // Cierro la puerta, prendo el buzzer y dejo de estar en horario comida
      esHoraComida = false;
      buzzer_control(TONO_COMIDA_SERVIDA);
      door_control();
      estado_actual = ESTADO_ESPERA;
    }
    break;
    case EVENTO_SIN_COMIDA:
    {
      // TODO ACCION
      buzzer_control(TONO_PEDIR_RECARGA);
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
    case EVENTO_RECARGA_COMIDA:
    {
      // TODO ACCION
      estado_actual = ESTADO_SERVIR_COMIDA;
    }
    break;
    }
  }
  break;
  }
  logFSM();
}
void generaEvento()
{
  check_weight();
  check_proximity();
  leerComando();
  check_RFID();
  // DECLARO CONDICIONES PARA GENERAR EVENTOS Y SE CHECKEAN DE A UNA POR POLLING

  evento_poll = eventos[indexEvento];
  indexEvento = (++indexEvento) % (CANT_EVENTOS);
  if (evento_poll.evento == evento_poll.handler())
  {
    evento_actual = evento_poll.evento;
  }
}
// Funciones de manejo de sensores y actuadores
int check_weight()
{
  int eventoReturn = EVENTO_CONTINUE;
  leerComando();
  // solo importa el timeout cuando es el momento de servir la comida
  if (!esHoraComida) lastPotTime = currentTime;
  unsigned long timediff = currentTime - lastPotTime;
  // si no hay suficiente peso abre la compuerta
  potValue = analogRead(POT_PIN);
  // Checkeo que haya cambiado el valor anterior leido
  if (prevPotValue != potValue)
  {
    alimentoSuficiente = potValue > umbralAlimentoMax;
    // La compuerta permanece abierta mientras el peso actual sea menor al alimento deseado
    if (!alimentoSuficiente)
    {
      Serial.print(potValue);
      Serial.println(" gr      ");
      dispenserVacio = false;
    }
    prevPotValue = potValue;
    lastPotTime = currentTime;
  }
  else if (timediff > TO_POT) // si supero el timeout con el mismo peso significa que no hay mas alimento
  {
    dispenserVacio = true;
  }


  if(!dispenserVacio) 
    eventoReturn = EVENTO_RECARGA_COMIDA;
  if(alimentoSuficiente)
    eventoReturn = EVENTO_COMIDA_SERVIDA;
  if(alimentoSuficiente && esHoraComida)
    eventoReturn = EVENTO_RENOVAR_COMIDA;
  if(!alimentoSuficiente && esHoraComida)
    eventoReturn = EVENTO_HORA_COMIDA;
  if(dispenserVacio)
    eventoReturn = EVENTO_SIN_COMIDA;
  if(!potValue)
    eventoReturn = EVENTO_COMIDA_RENOVADA;

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
    delayMicroseconds(10);
    digitalWrite(TRIG_PIN, LOW);

    // measure duration of pulse from ECHO pin
    duration_us = pulseIn(ECHO_PIN, HIGH);

    // calculate the distance
    distance_cm = 0.017 * duration_us;
    proximidadDetectada = distance_cm < PROX_MAX_CM;

    lastProximityTime = currentTime;
  }
  if(!proximidadAnterior && proximidadDetectada)
    eventoReturn = EVENTO_PRESENCIA_ON;
  if(proximidadAnterior && !proximidadDetectada)
    eventoReturn = EVENTO_PRESENCIA_OFF;
  
  return eventoReturn;
  
}

void door_control()
{
  if (esHoraComida)
  {
    // Abrir compuerta
    myServo.write(POS_MAX / 3);
  }
  else
  {
    // Cerrar compuerta
    myServo.write(POS_MIN);
  }
}
void buzzer_control(int tono)
{
  tone(BUZZER_PIN, tono, 2000);
}
void logFSM()
{
  if (estado_anterior != estado_actual)
  {
    Serial.println("---------");
    Serial.print("Distancia: ");
    Serial.print(distance_cm);
    Serial.print(" Estado Anterior: ");
    Serial.print(getEstado(estado_anterior));
    Serial.print(" Evento Actual: ");
    Serial.print(evento_poll.nombre);
    Serial.print(" Estado Actual: ");
    Serial.println(getEstado(estado_actual));
    Serial.println("---------");
  }
}
char* getEstado(int estado) 
{
    for (int i=0; i < CANT_ESTADOS; i++)
    {
      if(estados[i].estado == estado)
        return estados[i].nombre;
    }
    return "";
}
void leerComando() 
{
  if (Serial.available() > 0) {
    // read the incoming byte:
    int incomingByte = Serial.read();
    if(incomingByte == COMANDO_HORA_COMIDA)
    {
      esHoraComida = true;
      Serial.print("He recibido el comando: ");
      Serial.print(incomingByte);
      Serial.println(". Hora de comida activada.");
    }
  }

}
int check_RFID() 
{
  int eventoReturn = EVENTO_CONTINUE;
  // Se simula RFID con un boton
  RFID_detectado = digitalRead(BUTTON_PIN)== LOW;
  if(RFID_leido) 
    eventoReturn = EVENTO_RFID_LEIDO;
  if(RFID_detectado) 
    eventoReturn = EVENTO_DETECTA_RFID;
  
  return eventoReturn;

}
void leer_RFID() 
{
  // Logica para leer RFID
  RFID_leido = true;
}