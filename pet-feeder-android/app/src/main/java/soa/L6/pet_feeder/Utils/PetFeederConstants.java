package soa.L6.pet_feeder.Utils;

public class PetFeederConstants
{
    // Definir las constantes
    public static final String SUB_TOPIC_ESTADOS = "/pet-feeder/estado";
    public static final String SUB_TOPIC_ESTADISTICA = "/pet-feeder/estadistica";
    public static final String PUB_TOPIC_ALIMENTACION = "/pet-feeder/alimentacion";
    public static final String PUB_TOPIC_HORA_COMIDA = "/pet-feeder/hora-comida";
    public static final String PUB_TOPIC_SINCRONIZAR = "/pet-feeder/sincronizar";
    public static final String MQTT_SERVER_URI = "ssl://y8ad1cae.ala.us-east-1.emqxsl.com:8883";
    public static final String USER_NAME_MQTT = "PET_FEEDER_L6";
    public static final String PASSWORD_MQTT = "123456";
    public static final String ESTADO_ESPERA = "ESTADO_ESPERA";
    public static final String ESTADO_DETECTA_PRESENCIA = "ESTADO_DETECTA_PRESENCIA";
    public static final String ESTADO_DETECTA_RFID = "ESTADO_DETECTA_RFID";
    public static final String ESTADO_RENOVAR_COMIDA = "ESTADO_RENOVAR_COMIDA";
    public static final String ESTADO_SERVIR_COMIDA = "ESTADO_SERVIR_COMIDA";
        public static final String ESTADO_PEDIR_RECARGA = "ESTADO_PEDIR_RECARGA";
    public static final String FILE_NAME_PETS = "pets.dat";
    public static final String FILE_NAME_FOODS = "foods.dat";

}
