package com.example.casa.pruebabt;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Set;

public class DISPOSITbt extends AppCompatActivity {

    public TextView dispoenc;
    public ListView viewDispositivos;
    public Button btn_volver;
    public BluetoothAdapter mbtadapter;
    public ArrayList<String> mylistadispositivos;
    public Button btn_buscar;
    public ArrayAdapter<String> arrayAdapter;
    public String MACADDRESS;
    public static String EXTRA_ADDRESS = "device_address";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dispositbt);

        dispoenc = findViewById(R.id.IDTitulo);
        viewDispositivos = findViewById(R.id.IDListview);
        btn_volver = findViewById(R.id.IDbotonVolver);
        btn_buscar = findViewById(R.id.IDbotonbuscar);
        mylistadispositivos = new ArrayList<String>();

        mbtadapter = BluetoothAdapter.getDefaultAdapter();

        viewDispositivos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String elegido = mylistadispositivos.get(position);
                String[] mac = elegido.split("\n");
                MACADDRESS = mac[1];

                //DESACTIVO LA BUSQUEDA
                mbtadapter.cancelDiscovery();
                voyActivityConexion();
            }
        });

    }

    protected void onResume() {

        super.onResume();
        if (mbtadapter == null) {
            Toast.makeText(getApplicationContext(), "NO TENES BLUETOOTH", Toast.LENGTH_LONG).show();
            finish();
        }
        // SI EL BT NO ESTA ACTIVADO, MANDAR NOTIFICACION SI LO QUIERE ACTIVAR
        if (!mbtadapter.isEnabled()) {
            Intent EnableBTintent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(EnableBTintent, 1);
        }

        btn_volver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(mbtadapter.isEnabled())
                    mbtadapter.disable();
                finish();
            }
        });

        btn_buscar.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {

                if (!mbtadapter.isEnabled()) {
                    Intent EnableBTintent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(EnableBTintent, 1);

                } else if ((mbtadapter.isEnabled() && !mbtadapter.isDiscovering())) {
                    //CHECK BT PERMISSION
                    checkBTpermission();
                    mbtadapter.startDiscovery();
                }

                if (mbtadapter.isDiscovering()) {
                    mbtadapter.cancelDiscovery();
                    checkBTpermission();
                    mbtadapter.startDiscovery();
                }
            }
        });

        IntentFilter filtro = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(myReceiver, filtro);
        arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, mylistadispositivos);
        viewDispositivos.setAdapter(arrayAdapter);
    }

    // Instanciamos un BroadcastReceiver que se encargara de detectar cuando
    // un dispositivo es descubierto.

    BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice dispositivo = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mylistadispositivos.add(dispositivo.getName() + "\n" + dispositivo.getAddress());
                arrayAdapter.notifyDataSetChanged();
            }
        }
    };

    @TargetApi(Build.VERSION_CODES.M)
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkBTpermission() {

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");

                permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");

                if (permissionCheck != 0) {
                    this.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                } else
                    Toast.makeText(getApplicationContext(), "NO HAY NECESIDAD DE MIRAR PERMISOS", Toast.LENGTH_SHORT).show();
            }

        }

    }

    public void voyActivityConexion() {
        Intent i = new Intent(getApplicationContext(), ConexionBT.class);
        i.putExtra(EXTRA_ADDRESS, MACADDRESS);
        startActivity(i);
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

    protected void onPause() {

        super.onPause();
    }


}
