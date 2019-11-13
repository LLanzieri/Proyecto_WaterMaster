package com.example.casa.pruebabt;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity{

    private Button conectardispo;
    ImageView foto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        foto = (ImageView) findViewById(R.id.IDimagen);
        conectardispo = (Button) findViewById(R.id.IDConectaraDispositivo);

        conectardispo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "COMENCEMOS CON LA BUSQUEDA", Toast.LENGTH_SHORT).show();
                voyaActivity2();
            }

            public void voyaActivity2() {
                Intent a = new Intent(getApplicationContext(), DISPOSITbt.class);
                startActivity(a);
            }
        });
    }



    protected void onPause() {
        super.onPause();

    }

    protected void onResume() {
        super.onResume();

    }

    protected void onStop() {

        super.onStop();
    }


}