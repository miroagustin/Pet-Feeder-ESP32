#include <ESP32Servo.h>

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
#define TO_PROXIMITY 3000 // Timeout para volver a escanear por proximidad
#define TO_POT 30000      // 30 seg para detectar que no hay mas alimento
#define CANT_EVENTOS 8
#define CANT_ESTADOS 7

/**ESTADOS**/
#define ESTADO_ESPERA 1000
#define ESTADO_DETECTA_PRESENCIA 1001
#define ESTADO_DETECTA_RFID 1002
#define ESTADO_RENOVAR_COMIDA 1003
#define ESTADO_SERVIR_COMIDA 1004
#define ESTADO_PEDIR_RECARGA 1005
#define ESTADO_FINALIZADO 1006

/**EVENTOS**/
#define EVENTO_PRESENCIA_ON 2000
#define EVENTO_PRESENCIA_OFF 2001
#define EVENTO_DETECTA_RFID 2002
#define EVENTO_RFID_LEIDO 2003
#define EVENTO_RENOVAR_COMIDA 2004  // SI PESO = UMBRAL
#define EVENTO_COMIDA_RENOVADA 2005 // SI PESO = 0
#define EVENTO_HORA_COMIDA 2006     // SI HORA DE COMIDA Y PESO < UMBRAL
#define EVENTO_COMIDA_SERVIDA 2007
#define EVENTO_FINALIZADO 2008
#define EVENTO_SIN_COMIDA 2009
#define EVENTO_RECARGA_COMIDA 2010

struct Evento
{
  bool condicion;
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
    {ESTADO_FINALIZADO, "ESTADO_FINALIZADO"},
};
int estado_actual, estado_anterior, evento_actual, potValue, indexEvento;
float duration_us, distance_cm;
bool alimentoSuficiente, proximidadDetectada, proximidadAnterior, esHoraComida, dispenserVacio;
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
  myServo.attach(SERVO_PIN);
  currentTime = millis();
  lastPotTime = currentTime;
  // setea el valor inicial para que de timeout
  lastProximityTime = currentTime - TO_PROXIMITY - 1;
  // HARDCODE HORARIO DE COMER PERMANENTE
  esHoraComida = true;
}

void loop()
{
  fsm();
  delay(100);
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
      // TODO ACCION
      door_control();
      buzzer_control();
      estado_actual = ESTADO_ESPERA;
    }
    break;
    case EVENTO_SIN_COMIDA:
    {
      // TODO ACCION
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
  case ESTADO_FINALIZADO:
  {
    switch (evento_actual)
    {
    case EVENTO_FINALIZADO:
    {
      // TODO ACCION
      estado_actual = ESTADO_ESPERA;
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
  // TODO EVENTO_FINALIZADO, EVENTO_DETECTA_RFID y EVENTO_RFID_LEIDO
  check_weight();
  check_proximity();
  // DECLARO CONDICIONES PARA GENERAR EVENTOS Y SE CHECKEAN DE A UNA POR POLLING
  struct Evento eventos[CANT_EVENTOS] = {
      {!proximidadAnterior && proximidadDetectada, EVENTO_PRESENCIA_ON, "EVENTO_PRESENCIA_ON"},
      {proximidadAnterior && !proximidadDetectada, EVENTO_PRESENCIA_OFF, "EVENTO_PRESENCIA_OFF"},
      {prevPotValue == potValue && potValue == POT_MAX, EVENTO_RENOVAR_COMIDA, "EVENTO_RENOVAR_COMIDA"},
      {!potValue, EVENTO_COMIDA_RENOVADA, "EVENTO_COMIDA_RENOVADA"},
      {!alimentoSuficiente && esHoraComida, EVENTO_HORA_COMIDA, "EVENTO_HORA_COMIDA"},
      {dispenserVacio, EVENTO_SIN_COMIDA, "EVENTO_SIN_COMIDA"},
      {!dispenserVacio, EVENTO_RECARGA_COMIDA, "EVENTO_RECARGA_COMIDA"},
      {alimentoSuficiente, EVENTO_COMIDA_SERVIDA, "EVENTO_COMIDA_SERVIDA"}};
  evento_poll = eventos[indexEvento];
  indexEvento = (++indexEvento) % (CANT_EVENTOS);
  if (evento_poll.condicion)
  {
    evento_actual = evento_poll.evento;
  }
}
// Funciones de manejo de sensores y actuadores
void check_weight()
{
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
}

void check_proximity()
{
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

    // print the value to Serial Monitor
    Serial.print("distance: ");
    Serial.print(distance_cm);
    Serial.println(" cm      ");
    Serial.print("timestamp: ");
    Serial.print(currentTime);
    Serial.println(" ms      ");
    lastProximityTime = currentTime;
  }
}

void door_control()
{
  if (!alimentoSuficiente)
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
void buzzer_control()
{
  tone(BUZZER_PIN, 420, 2000);
}
void logFSM()
{
  if (estado_anterior != estado_actual)
  {
    Serial.println("---------");
    Serial.print("Estado Anterior: ");
    Serial.print(getEstado(estado_anterior));
    Serial.print(" Evento Actual: ");
    Serial.print(evento_poll.nombre);
    Serial.print(" Estado Actual: ");
    Serial.println(getEstado(estado_actual));
    Serial.println("---------");
  }
}
char* getEstado(int estado) {
    for (int i=0; i < CANT_ESTADOS; i++)
    {
      if(estados[i].estado == estado)
        return estados[i].nombre;
    }
    return "";
}