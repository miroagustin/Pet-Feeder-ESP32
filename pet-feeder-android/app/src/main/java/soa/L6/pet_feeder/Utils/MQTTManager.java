package soa.L6.pet_feeder.Utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import info.mqtt.android.service.Ack;
import info.mqtt.android.service.MqttAndroidClient;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class MQTTManager
{

    private static final String CERTIFICATE_STRING = "-----BEGIN CERTIFICATE-----\n"
            + "MIIDrzCCApegAwIBAgIQCDvgVpBCRrGhdWrJWZHHSjANBgkqhkiG9w0BAQUFADBh\n"
            + "MQswCQYDVQQGEwJVUzEVMBMGA1UEChMMRGlnaUNlcnQgSW5jMRkwFwYDVQQLExB3\n"
            + "d3cuZGlnaWNlcnQuY29tMSAwHgYDVQQDExdEaWdpQ2VydCBHbG9iYWwgUm9vdCBD\n"
            + "QTAeFw0wNjExMTAwMDAwMDBaFw0zMTExMTAwMDAwMDBaMGExCzAJBgNVBAYTAlVT\n"
            + "MRUwEwYDVQQKEwxEaWdpQ2VydCBJbmMxGTAXBgNVBAsTEHd3dy5kaWdpY2VydC5j\n"
            + "b20xIDAeBgNVBAMTF0RpZ2lDZXJ0IEdsb2JhbCBSb290IENBMIIBIjANBgkqhkiG\n"
            + "9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4jvhEXLeqKTTo1eqUKKPC3eQyaKl7hLOllsB\n"
            + "CSDMAZOnTjC3U/dDxGkAV53ijSLdhwZAAIEJzs4bg7/fzTtxRuLWZscFs3YnFo97\n"
            + "nh6Vfe63SKMI2tavegw5BmV/Sl0fvBf4q77uKNd0f3p4mVmFaG5cIzJLv07A6Fpt\n"
            + "43C/dxC//AH2hdmoRBBYMql1GNXRor5H4idq9Joz+EkIYIvUX7Q6hL+hqkpMfT7P\n"
            + "T19sdl6gSzeRntwi5m3OFBqOasv+zbMUZBfHWymeMr/y7vrTC0LUq7dBMtoM1O/4\n"
            + "gdW7jVg/tRvoSSiicNoxBN33shbyTApOB6jtSj1etX+jkMOvJwIDAQABo2MwYTAO\n"
            + "BgNVHQ8BAf8EBAMCAYYwDwYDVR0TAQH/BAUwAwEB/zAdBgNVHQ4EFgQUA95QNVbR\n"
            + "TLtm8KPiGxvDl7I90VUwHwYDVR0jBBgwFoAUA95QNVbRTLtm8KPiGxvDl7I90VUw\n"
            + "DQYJKoZIhvcNAQEFBQADggEBAMucN6pIExIK+t1EnE9SsPTfrgT1eXkIoyQY/Esr\n"
            + "hMAtudXH/vTBH1jLuG2cenTnmCmrEbXjcKChzUyImZOMkXDiqw8cvpOp/2PV5Adg\n"
            + "06O/nVsJ8dWO41P0jmP6P6fbtGbfYmbW0W5BjfIttep3Sp+dWOIrWcBAI+0tKIJF\n"
            + "PnlUkiaY4IBIqDfv8NZ5YBberOgOzW6sRBc4L0na4UU+Krk2U886UAb3LujEV0ls\n"
            + "YSEY1QSteDwsOoBrp+uvFRTp2InBuThs4pFsiv9kuXclVzDAGySj4dzp30d8tbQk\n"
            + "CAUw7C29C79Fv1C5qfPrmAESrciIxpg0X40KPMbp1ZWVbd4=\n"
            + "-----END CERTIFICATE-----\n";

    private static final String TAG = "MQTTManager";
    private MqttAndroidClient mqttAndroidClient;
    private Context context;
    private final String clientId = MqttClient.generateClientId();

    public MQTTManager(Context context, MqttCallback callback)
    {
        this.context = context;
        mqttAndroidClient = new MqttAndroidClient(context, PetFeederConstants.MQTT_SERVER_URI, clientId, Ack.AUTO_ACK);
        Log.d(TAG, "Internet conectado? " + isInternetConnected(context));
        mqttAndroidClient.setCallback(callback);
    }
    public boolean isInternetConnected(Context context)
    {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network nw = connectivityManager.getActiveNetwork();
        if (nw == null) return false;
        NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
        return actNw != null && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
    }
    public void connect()
    {
        try
        {

            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(PetFeederConstants.USER_NAME_MQTT);
            options.setPassword(PetFeederConstants.PASSWORD_MQTT.toCharArray());
            options.setSocketFactory(getSocketFactory());

            IMqttToken token = mqttAndroidClient.connect(options);
            Log.d(TAG, "Conectando...");
            token.setActionCallback(new IMqttActionListener()
            {
                @Override
                public void onSuccess(IMqttToken asyncActionToken)
                {
                    Log.d(TAG, "Conectado exitosamente");
                    subscribeToTopic(PetFeederConstants.SUB_TOPIC_ESTADOS);
                    subscribeToTopic(PetFeederConstants.SUB_TOPIC_ESTADISTICA);
                    showToast("Conexión exitosa");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception)
                {
                    Log.d(TAG, "Fallo al conectar: " + exception.toString());
                    showToast("Fallo al conectar: " + exception.getMessage());
                }
            });
        } catch (Exception e)
        {
            Log.e(TAG, "Error al conectar a MQTT: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void subscribeToTopic(String topic)
    {
        try
        {
            mqttAndroidClient.subscribe(topic, 0);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void publishMessage(String topic, String payload)
    {
        try
        {
            if (mqttAndroidClient != null && mqttAndroidClient.isConnected())
            {
                MqttMessage message = new MqttMessage();
                message.setPayload(payload.getBytes());
                mqttAndroidClient.publish(topic, message);
            } else
            {
                Log.d(TAG, "Cliente no conectado");
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public boolean isConnected()
    {
        return mqttAndroidClient != null && mqttAndroidClient.isConnected();
    }

    public void disconnect()
    {
        try {
            mqttAndroidClient.disconnect();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private SSLSocketFactory getSocketFactory()
    {
        try
        {
            // Load CAs from an InputStream
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream caInput = new ByteArrayInputStream(CERTIFICATE_STRING.getBytes());
            Certificate ca;

            try
            {
                ca = cf.generateCertificate(caInput);
                Log.d(TAG, "ca=" + ((java.security.cert.X509Certificate) ca).getSubjectDN());
            } finally
            {
                caInput.close();
            }

            // Create a KeyStore containing our trusted CAs
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            // Create a TrustManager that trusts the CAs in our KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            // Create an SSLContext that uses our TrustManager
            SSLContext context = SSLContext.getInstance("TLSv1.2");
            context.init(null, tmf.getTrustManagers(), null);

            return context.getSocketFactory();
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    private void showToast(String message)
    {
        new Handler(Looper.getMainLooper()).post(() ->
        {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        });
    }

}
