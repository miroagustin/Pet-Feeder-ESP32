#define TRIG_PIN 12 // ESP32 pin GIOP23 connected to Ultrasonic Sensor's TRIG pin
#define ECHO_PIN 13 // ESP32 pin GIOP22 connected to Ultrasonic Sensor's ECHO pin

float duration_us, distance_cm;
void setup() {
  // put your setup code here, to run once:
  Serial.begin(115200);
  // configure the trigger pin to output mode
  pinMode(TRIG_PIN, OUTPUT);
  // configure the echo pin to input mode
  pinMode(ECHO_PIN, INPUT);
}

void loop() {
  // generate 10-microsecond pulse to TRIG pin
  digitalWrite(TRIG_PIN, HIGH);
  delayMicroseconds(10);
  digitalWrite(TRIG_PIN, LOW);

  // measure duration of pulse from ECHO pin
  duration_us = pulseIn(ECHO_PIN, HIGH);
  
  // calculate the distance
  distance_cm = 0.017 * duration_us;
 
 
  // print the value to Serial Monitor
  Serial.print("distanceA: ");
  Serial.print(distance_cm);
  Serial.println(" cm      ");
  delay(500);
}
