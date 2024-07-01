package soa.L6.pet_feeder.Activities;


import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

import soa.L6.pet_feeder.Model.FeederRecorder;
import soa.L6.pet_feeder.Model.FeederState;
import soa.L6.pet_feeder.Model.Food;
import soa.L6.pet_feeder.Model.Pet;
import soa.L6.pet_feeder.Model.PetRecorder;
import soa.L6.pet_feeder.R;
import soa.L6.pet_feeder.Utils.MQTTManager;
import soa.L6.pet_feeder.Utils.PetFeederConstants;
import soa.L6.pet_feeder.ui.dashboard.DashboardFragment;
import soa.L6.pet_feeder.ui.home.HomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import soa.L6.pet_feeder.databinding.ActivityMainBinding;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private SensorManager sensorManager;
    private SensorEventListener sensorEventListener;
    public HomeFragment homeFragment;
    public DashboardFragment dashboardFragment;
    public MQTTManager mqttManager;
    public PetRecorder petRecorder;
    public List<Pet> petList;
    public FeederState feederState;

    private final MqttCallback callback = new MqttCallback() {
        @Override
        public void connectionLost(Throwable cause) {
            Log.d(MainActivity.class.getName(), "Conexión perdida");
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            String messageContent = new String(message.getPayload());
            Log.d(MainActivity.class.getName(), "Mensaje recibido: " + messageContent);
            runOnUiThread(() -> {

                if (Objects.equals(topic, PetFeederConstants.SUB_TOPIC_ESTADOS)) {
                    feederState.UpdateEstado(messageContent);
                    callSetHomeDataInFragment();
                }
                if (Objects.equals(topic, PetFeederConstants.SUB_TOPIC_ESTADISTICA)) {
                    String[] messageWithSplit = messageContent.split(";");
                    // por defecto nuevo rfid detectado le pongo nombre mascota
                    Pet messageCat = new Pet("Mascota ",messageWithSplit[0]);
                    if(petRecorder.exists(messageCat)) // mascota ya existe
                    {
                        //agregar mascota con peso que comio
                        Pet modify = petRecorder.getPetList().stream().filter(x -> x.compareTo(messageCat) == 0).findAny().get();
                        modify.record_meal(Double.parseDouble(messageWithSplit[1]));
                        petRecorder.updatePet(modify);
                        callAddPetInFragment(modify);

                    }
                    else //crear nueva mascota
                    {
                        //agregar peso que comio
                        messageCat.record_meal(Double.parseDouble(messageWithSplit[1]));
                        //agregar mascota a lista
                        petRecorder.addPetToList(messageCat);

                        callAddPetInFragment(messageCat);

                    }

                    Log.d("topico estadistica",petRecorder.getPetList().toString());
                    petRecorder.savePetsToFile(MainActivity.this);
                }

            });


        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            Log.d(MainActivity.class.getName(), "Entrega completada");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        mqttManager = new MQTTManager(this,callback);
        mqttManager.connect();
        feederState = new FeederState(this);

        super.onCreate(savedInstanceState);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorEventListener = new SensorEventListener() {
            private static final float SHAKE_THRESHOLD = 500f; // Puedes ajustar este valor según tu necesidad
            private long lastUpdate = 0;
            private float lastX, lastY, lastZ;

            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    long currentTime = System.currentTimeMillis();
                    if ((currentTime - lastUpdate) > 100) {
                        long timeDifference = (currentTime - lastUpdate);
                        lastUpdate = currentTime;

                        float x = event.values[0];
                        float y = event.values[1];
                        float z = event.values[2];

                        float speed = Math.abs(x + y + z - lastX - lastY - lastZ) / timeDifference * 10000;

                        if (speed > SHAKE_THRESHOLD) {
                            System.out.println("SE DETECTO SHAKE!");
                            callAcceptDialog();
                        }

                        lastX = x;
                        lastY = y;
                        lastZ = z;
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // No necesitas implementar esto, pero es obligatorio
            }
        };



        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        getSupportActionBar().hide();

        petRecorder = new PetRecorder(PetFeederConstants.FILE_NAME_PETS);
        petRecorder.loadPetsFromFile(this);

        /*Pet newCat = new Pet("Pepe","CA");
        newCat.record_meal(60.1);
        newCat.record_meal(12.36);
        newCat.record_meal(2.36);
        newCat.record_meal(8.49);
        newCat.record_meal(24.94);
        petRecorder.addPetToList(newCat);

        Log.d("TEST PET",petRecorder.getPetList().toString());
        petRecorder.savePetsToFile(this);*/

        FeederRecorder feederRecorder = feederState.getFeederRecorder();
        /*
        Food newFood1 = new Food("12:00",50.00);
        Food newFood2 = new Food("14:00",50.00);
        Food newFood3 = new Food("15:00",50.00);
        Food newFood4 = new Food("16:00",50.00);
        Food newFood5 = new Food("17:00",50.00);
        Food newFood6 = new Food("18:00",50.00);
        Food newFood7 = new Food("19:00",50.00);
        Food newFood8 = new Food("20:00",50.00);
        Food newFood9 = new Food("22:00",50.00);

        feederRecorder.addFoodToList(newFood1);
        feederRecorder.addFoodToList(newFood2);
        feederRecorder.addFoodToList(newFood3);
        feederRecorder.addFoodToList(newFood4);
        feederRecorder.addFoodToList(newFood5);
        feederRecorder.addFoodToList(newFood6);
        feederRecorder.addFoodToList(newFood7);
        feederRecorder.addFoodToList(newFood8);
        feederRecorder.addFoodToList(newFood9);
        */
        Log.d("TEST FOOD",feederRecorder.getFoodList().toString());

        //feederRecorder.saveFoodToFile(this);
    }
    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorEventListener);
    }
    public MQTTManager getMQTTManager() {
        return mqttManager;
    }
    private void callSetHomeDataInFragment() {
        if (homeFragment != null && homeFragment.isAdded()) {
            homeFragment.setHomeData(feederState);
        } else {
            Log.d(MainActivity.class.getName(), "homeFragment no está listo, reintentando...");
            getSupportFragmentManager().executePendingTransactions();
            new android.os.Handler().postDelayed(this::callSetHomeDataInFragment, 1000); // Reintentar después de 1 segundo
        }
    }

    private void callAcceptDialog() {
        if (homeFragment != null && homeFragment.isAdded()) {
            homeFragment.acceptDialog();
        } else {
            Log.d(MainActivity.class.getName(), "homeFragment no está listo, reintentando...");
            getSupportFragmentManager().executePendingTransactions();
            new android.os.Handler().postDelayed(this::callAcceptDialog, 1000); // Reintentar después de 1 segundo
        }
    }
    private void callAddPetInFragment(Pet pet) {
        if (dashboardFragment != null && dashboardFragment.isAdded()) {
            dashboardFragment.addPetCard(pet);
        }
    }
}