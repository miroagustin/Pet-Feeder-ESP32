# Pet Feeder Inteligente

## Autores
- Fragassi Donatella
- Garin Matias
- Miró Agustin
- Romero Ignacio

## Grupo
- L6

## Institución
- Universidad Nacional de La Matanza
- Departamento de Ingeniería e Investigaciones Tecnológicas
- Florencio Varela 1903 - San Justo, Argentina

## Resumen
El Pet Feeder inteligente permite a los usuarios programar horarios o activar manualmente la dispensación de comida para sus mascotas. Una aplicación móvil proporciona información sobre los animales que se están alimentando y permite el control remoto del dispensador. El dispositivo emite un sonido al servir la comida y utiliza un sensor de proximidad para detectar cuánto come cada mascota. La mascota es identificada mediante una etiqueta RFID en su collar. El dispositivo puede rastrear el cambio de peso en el recipiente para determinar cuánto ha comido la mascota, permitiendo un seguimiento del consumo de calorías y nutrientes.

## Palabras Claves
- Pet Feeder
- RFID
- IoT
- ESP32
- MQTT
- freeRTOS

## Introducción
El Pet Feeder es un dispositivo inteligente y configurable para alimento balanceado donde el usuario puede:
- Activar manualmente el Pet Feeder para llenar el recipiente de comida con el peso configurado.
- Configurar un horario para que automáticamente se active y cargue el recipiente.
- Ver estadísticas de cada animal.
- Gestionar los horarios de alimentación.
- Verificar el estado del dispositivo.
- Activar la alimentación manual desde la aplicación.

Cuando se sirve la comida, el Pet Feeder emite un sonido. La mascota es identificada mediante una etiqueta RFID en su collar y el consumo de comida se monitorea con un sensor de proximidad y una balanza para rastrear el consumo de calorías y nutrientes.

## Desarrollo
### Diagrama de Estados
- Inicio del equipo: Estado INIT, esperando la conexion WiFi y a los servicios MQTT y NTP
- Conexion exitosa: El equipo pasa a Estado “En espera” con dos funcionalidades principales: Servir Comida y Detectar Animal.

#### Servir Comida
- Se inicia desde la aplicación de Android indicando la cantidad de gramos a servir.
- Verifica la cantidad de comida con el sensor de peso.
- Si la cantidad de comida está dentro del umbral, se abre la compuerta mediante un servo motor.
- Si no hay suficiente comida, se pasa al estado “Pedir recarga”.

#### Detectar Animal
- Verifica los sensores de proximidad y RFID.
- Registra la presencia de la mascota y su identificación en la aplicación a través de MQTT.

#### Gestionar Horarios de Comida
- Alta de nuevos horarios de comida desde la aplicación.
- Eliminación de horarios individuales o todos los datos.
- Sincronización con el dispositivo.

### Manual de Usuario
- Iniciar el equipo y activar las funcionalidades desde la aplicación.
- Configurar horarios y gestionar el estado del dispositivo.

## Estructura del Repositorio
El repositorio contiene las siguientes carpetas:
- `pet-feeder-android`: donde se encuentra el código de la aplicación Android.
- `pet-feeder-embebido-fisico`: donde se encuentra el archivo .ino del ESP32.
- `pet-feeder-embebido-simulador`: donde se encuentra la prueba de concepto en el simulador Wokwi.


## Proyecto en Wokwi
- [Proyecto Wokwi](https://wokwi.com/projects/395699196580858881)

## Repositorio
- [Repositorio GitHub](https://github.com/miroagustin/Pet-Feeder-ESP32)

## Conclusión
Este sistema permite saber exactamente cuándo y cuánto come cada animal. Los mayores desafíos en el desarrollo fueron el uso de la memoria no volátil del ESP32, problemas de operaciones bloqueantes, y compatibilidad con Android 12.

## Referencias
1. [Documentación Simulador Wokwi](https://docs.wokwi.com/)
2. [Wiki de la cátedra](https://www.soa-unlam.com.ar/wiki/)
3. [Documentación oficial de Android](https://developer.android.com/)
