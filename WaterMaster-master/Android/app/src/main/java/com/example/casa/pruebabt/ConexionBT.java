package com.example.casa.pruebabt;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class ConexionBT extends AppCompatActivity implements SensorEventListener {

    public String address;

    BluetoothAdapter myBluetooth = null;
    BluetoothSocket SocketConexion = null;

    private ProgressDialog progress;

    // Flag que indica si el bluetooth está conectado
    private boolean isBtConnected = false;

    //SPP UUID ESPECIAL PARA LA CONEXION
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    boolean modoBalanza = false;
    boolean aguaCaliente = false;
    Switch swAF;
    Switch swAC;
    TextView af;
    TextView ac;
    TextView tvTemp;
    TextView tvdatosTemp;
    Button balanza;
    Button btn_volver;
    TextView tvDatospeso;
    TextView tvPeso;
    String[] cadena;

    //SENSORES
    private SensorManager senSensorManager;
    //acelerometro
    private Sensor senAccelerometer;
    //proximidad
    private Sensor senProximidad;
    //giroscopio
    private Sensor giroscopio;

    // variables for shake detection
    private static final float SHAKE_THRESHOLD = 3.25f; // m/S**2
    private static final int MIN_TIME_BETWEEN_SHAKES_MILLISECS = 1000;
    public long mLastShakeTime;

    String shake = "s";
    String proximidad = "p";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conexion_bt);

        af = (TextView) findViewById(R.id.IDtvAguafria);
        ac = (TextView) findViewById(R.id.IDtvAguacaliente);

        swAC = (Switch) findViewById(R.id.IDSwitchAC);
        swAF = (Switch) findViewById(R.id.IDSwitchaf);

        tvTemp = (TextView) findViewById(R.id.IDtvTemp);
        tvdatosTemp = (TextView) findViewById(R.id.IDtvDatostemp);

        balanza = (Button) findViewById(R.id.IDbtnBalanza);
        tvPeso = (TextView) findViewById(R.id.IDtvpeso);
        tvDatospeso = (TextView) findViewById(R.id.IDtvdatosPeso);

        btn_volver = (Button) findViewById(R.id.IDbtnVolver);


        //SACO DEL INTENT LA MAC A LA QUE ME VOY A CONECTAR
        Intent myintent = getIntent();
        address = myintent.getStringExtra(DISPOSITbt.EXTRA_ADDRESS);

        //CREACION Y CONEXION DE HILOS
        new ConexionaDispositivo().execute();

        // Ejecuta el thread que lee y escribe el bluetooth
        LeerYEscribirBT LeeyEscribeBT = new LeerYEscribirBT();
        LeeyEscribeBT.execute();

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        //acelerometro
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //sensor de proximidad
        senProximidad = senSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        //Giroscopio
        giroscopio = senSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        //Si hay acelerometro, registro el listener, sino finalizo
        if (senAccelerometer != null)
            senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        else {
            Toast.makeText(getApplicationContext(), "NO TENES ACELEROMETRO", Toast.LENGTH_SHORT).show();
            finish(); // Close app
        }
        //Si no hay Sensor de proximidad, finalizo, sino registro el listener
        if (senProximidad == null) {
            Toast.makeText(getApplicationContext(), "NO TENES SENSOR DE PROXIMIDAD", Toast.LENGTH_SHORT).show();
            finish(); // Close app
        } else {
            senSensorManager.registerListener(this,
                    senProximidad, SensorManager.SENSOR_DELAY_NORMAL);
        }

        //SI NO HAY GIROSCOPIO, FINALIZO, SINO REGISTRO EL LISTENER
        if (giroscopio == null)
            Toast.makeText(getApplicationContext(), "NO TENES GIROSCOPIO", Toast.LENGTH_SHORT).show();
        else
            senSensorManager.registerListener(this, giroscopio, SensorManager.SENSOR_DELAY_NORMAL);


    }

    protected void onResume() {
        super.onResume();

        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        // Register it, specifying the polling interval in
        // microseconds
        senSensorManager.registerListener(this,
                senProximidad, SensorManager.SENSOR_DELAY_NORMAL);
        // LISTENER DEL GIROSCOPIO
        senSensorManager.registerListener(this, giroscopio, SensorManager.SENSOR_DELAY_NORMAL);


        btn_volver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EnviarporBT("z");
                try {
                    SocketConexion.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                finish();
            }
        });


        swAF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swAC.setChecked(false);
                aguaCaliente = false;
                modoBalanza=false;
                tvDatospeso.setText("");
                EnviarporBT("f");
                getWindow().getDecorView().setBackgroundColor(Color.WHITE);

            }
        });

        swAC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swAF.setChecked(false);
                aguaCaliente = true;
                modoBalanza=false;
                tvDatospeso.setText("");
                EnviarporBT("c");
                getWindow().getDecorView().setBackgroundColor(Color.WHITE);

            }
        });

        balanza.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                modoBalanza=true;
                swAF.setChecked(false);
                swAC.setChecked(false);
                tvdatosTemp.setText("");
                aguaCaliente = false;
                EnviarporBT("b");
                getWindow().getDecorView().setBackgroundColor(Color.WHITE);

            }
        });
    }


    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && aguaCaliente == false) {
            long curTime = System.currentTimeMillis();
            if ((curTime - mLastShakeTime) > MIN_TIME_BETWEEN_SHAKES_MILLISECS) {

                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                double acceleration = Math.sqrt(Math.pow(x, 2) +
                        Math.pow(y, 2) +
                        Math.pow(z, 2)) - SensorManager.GRAVITY_EARTH;

                if (acceleration > SHAKE_THRESHOLD) {
                    mLastShakeTime = curTime;
                    EnviarporBT(shake);
                    Toast.makeText(getApplicationContext(),"SHAKE",Toast.LENGTH_SHORT).show();
                }

            }
        }

        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            if (event.values[0] < senProximidad.getMaximumRange()) {
                // Detected something nearby
                EnviarporBT(proximidad);
               Toast.makeText(getApplicationContext(),"PROXIMIDAD",Toast.LENGTH_SHORT).show();
            }
        }

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE && aguaCaliente == true) {
            if (event.values[2] > 0.5f) { // anticlockwise
                EnviarporBT("i");
                getWindow().getDecorView().setBackgroundColor(Color.CYAN);


            } else if (event.values[2] < -0.5f) { // clockwise
                    EnviarporBT("d");
                    getWindow().getDecorView().setBackgroundColor(Color.YELLOW);
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    protected void onPause() {
        super.onPause();
        senSensorManager.unregisterListener(this);

    }

    protected void onStop() {
        SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        sm.unregisterListener(this);
        super.onStop();
    }


    private class ConexionaDispositivo extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true;

        @Override
        protected void onPreExecute()
        {
            // Envío mensaje de please wait.
            progress = ProgressDialog.show(ConexionBT.this, "Conectando...", "Por favor, espere!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices)
        {
            try
            {
                // Si el socket es nulo y no está conectado
                if (SocketConexion == null)// || !isBtConnected)
                {
                    // Comienzo la rutina de conexión
                    //obtengo el dispositivo movil bluetooth
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();

                    //conecto a la dirección del dispositivo y chequeo si está disponible
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);

                    //creo el socket
                    //SocketConexion = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);
                    SocketConexion = dispositivo.createRfcommSocketToServiceRecord(myUUID);

                    //CANCELO LA BUSQUEDA QUE TENGO A QUE CONECTARME Y ADEMAS ME CONSUME RECURSOS
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    SocketConexion.connect();//comienzo la conexión

                }
            }
            catch (IOException e)
            {
                ConnectSuccess = false;
            }
            return null;
        }


        @Override
        protected void onPostExecute(Void result)
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                // Si no puede conectar, avisa con un mensaje y cierra el activity y vuelve a listar dispositivos
                Toast.makeText(getApplicationContext(),"Fallo la Conexión. Intente de nuevo",Toast.LENGTH_SHORT).show();
                finish();
            }
            else
            {
                // Sino, avisa que se ha conectado
                Toast.makeText(getApplicationContext(),"¡Conexión establecida!",Toast.LENGTH_SHORT).show();

                isBtConnected = true;
            }
            progress.dismiss();
        }
    }


    public class LeerYEscribirBT extends AsyncTask<Void, String, Void>
    {
        private boolean LeeYEscribe = true;

        @Override
        protected Void doInBackground(Void... devices) // Se ejecuta todo el tiempo.
        {
            try
            {
                byte[] buffer = new byte[1024];
                int bytes = 0;

                // Recibe los valores de arduino todo el tiempo, hasta que termine la aplicación
                while(true) {

                    // Leo el inputstram del Bluetooth
                    bytes += SocketConexion.getInputStream().read(buffer, bytes, buffer.length - bytes);

                    // Convierto a string los datos recibidos
                    String strReceived = new String(buffer, 0, bytes);

                    // Publico el progreso
                publishProgress(strReceived);

                    // Reinicio el buffer
                    buffer = new byte[1024];
                    bytes = 0;

               }
            }
            catch (IOException e)
            {
                LeeYEscribe = false;//Si el try falla, podemos tratarlo aquí
            }
            return null;
        }


        @Override
        protected void onProgressUpdate(String... values) {

                if (modoBalanza == false) {
                    cadena = values[0].split("t");
                    tvdatosTemp.setText(cadena[0] + " °C");
                } else {
                    cadena = values[0].split("p");
                    tvDatospeso.setText(cadena[0] + " gramos");
                }

        }


        @Override
        protected void onPostExecute(Void result)
        {
            super.onPostExecute(result);

            if (!LeeYEscribe)
            {
                Toast.makeText(getApplicationContext(),"Se dejo de leer y escribir el BT",Toast.LENGTH_SHORT).show();
                finish();
            }
            progress.dismiss();
        }


    }

    //Metodo para enviar por el bt
    private void EnviarporBT(String s)
    {
        // Si el socket está conectado escribe el output del socket del bluetooth
        if (SocketConexion!=null)
        {
            try
            {
                SocketConexion.getOutputStream().write(s.getBytes());
            }
            catch (IOException e)
            {
                //msg("Error");
            }
        }
    }

}
