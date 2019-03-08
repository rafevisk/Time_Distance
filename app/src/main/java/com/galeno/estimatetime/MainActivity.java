package com.galeno.estimatetime;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private LocationManager locationManager;
    private LocationListener locationListener;
    private static final int REQUEST_PERMISSION_GPS = 1001;

    private static final NumberFormat numberFormaty = NumberFormat.getNumberInstance();

    private Button bt_requestGPS;
    private Button bt_enableGPS;
    private Button bt_disableGPS;
    private Button bt_startRoute;
    private Button bt_endRoute;
    private TextView resultTravelledDistance;
    private TextInputEditText txtinput_search;
    private double latitudeInicial =0, longitudeInicial=0;
    private double latitudeAtual, longitudeAtual;
    private int auxiliarDecisao =0;
    private FloatingActionButton bt_search;


    private AlertDialog alerta;


    private Chronometer resultPastTime;
    private boolean running = false;
    private  long pauseOfset;


    private TextView txt_inicial;
    private int auxProvider = 0;
    private int TimeRunning=0;
    private String EndTime;
    private String EndDistance;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bt_requestGPS = (Button) findViewById(R.id.bt_requestGPS);
        bt_enableGPS = (Button) findViewById(R.id.bt_enableGPS);
        bt_disableGPS = (Button) findViewById(R.id.bt_disableGPS);
        bt_startRoute = (Button) findViewById(R.id.bt_startRoute);
        bt_endRoute = (Button) findViewById(R.id.bt_endRoute);
        resultTravelledDistance = (TextView) findViewById(R.id.resultaTravelledDistance);
        txtinput_search = (TextInputEditText) findViewById(R.id.txtinput_search);
        resultPastTime = (Chronometer) findViewById(R.id.resultPastTime);


        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        bt_search = findViewById(R.id.bt_search);

        resultPastTime.setOnChronometerTickListener(chronometerWatcher);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if(auxiliarDecisao == 1) {
                    latitudeInicial = location.getLatitude();
                    longitudeInicial = location.getLongitude();

                    /*String exibir = String.format(Locale.getDefault(),"Lat: %f, Long: %f", latitudeInicial, longitudeInicial);
                    txt_inicial.setText(exibir);

                    auxiliarDecisao = 2;
                    buildAlert(String.format(String.valueOf(auxiliarDecisao)));
                    alerta.show();*/
                }
                if(auxiliarDecisao == 2){
                    double latAtual = location.getLatitude();
                    double lonAtual = location.getLongitude();
                    longitudeAtual = lonAtual;
                    latitudeAtual = latAtual;
                    float distance = 0;

                    Location inicialLocation = new Location("inicialLocation");
                    inicialLocation.setLatitude(latitudeInicial);
                    inicialLocation.setLongitude(longitudeInicial);

                    /*String exibir = String.format(Locale.getDefault(),"Lat: %f, Long: %f", latitudeInicial, longitudeInicial);
                    txt_inicial.setText(exibir);*/

                    Location newLocation = new Location("newLocation");
                    newLocation.setLatitude(latAtual);
                    newLocation.setLongitude(lonAtual);

                    /*String exibir2 = String.format(Locale.getDefault(),"Lat: %f, Long: %f", latAtual, lonAtual);
                    txt_atual.setText(exibir2);*/

                    distance = inicialLocation.distanceTo(newLocation);
                    resultTravelledDistance.setText(numberFormaty.format(distance));
                    EndDistance = String.format("%f",distance);
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        //Botão p/ procurar na região
        bt_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Editable local = txtinput_search.getText();
                Uri uri = Uri.parse(String.format(Locale.getDefault(),"geo:%f,%f?q=%s" , latitudeAtual, longitudeAtual,local));
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.setPackage("com.google.android.apps.maps");
                startActivity(intent);
            }
        });

        //Botão p/ pedir permissão
        bt_requestGPS.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                check_permission();
            }
        });

        //Botao ativar GPS
        bt_enableGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(locationManager.isProviderEnabled(locationManager.GPS_PROVIDER) == false){
                    buildAlertGPS(getString(R.string.Alert_locationOff));
                    alerta.show();
                }
                else{
                    check_permission_ativaGPS();
                }
            }
        });

        //Botao desativar gps
        bt_disableGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(locationManager.isProviderEnabled(locationManager.GPS_PROVIDER) == false){
                    buildAlertGPS(getString(R.string.Alert_locationOff));
                    alerta.show();
                }
                else {
                    if (auxProvider == 0) {
                        buildAlert(getString(R.string.Alert_gpsAlreadyOff));
                        alerta.show();

                    } else {
                        locationManager.removeUpdates(locationListener);
                        buildAlert(getString(R.string.Alert_gpsOff));
                        alerta.show();
                        auxProvider = 0;
                    }
                }
            }
        });

        //Botao inicia a rota e temporizador
        bt_startRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(auxProvider == 0){
                    buildAlert(getString(R.string.Alert_disableGPS2));
                    alerta.show();
                }else{
                    if(!running){
                        resultPastTime.setBase(SystemClock.elapsedRealtime() - pauseOfset);
                        resultPastTime.start();
                        running = true;
                    }
                    if(auxiliarDecisao == 0){
                        auxiliarDecisao = 1;
                    }
                }
            }
        });
        //Termina a rota e zera tudo
        bt_endRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(running){
                    //Mostra result final
                    mostraResultadoFinal();
                    //reseta o time
                    resultPastTime.stop();
                    resultPastTime.setBase(SystemClock.elapsedRealtime());
                    pauseOfset = 0;
                    running = false;
                    //reseta o Local Inicial
                    auxiliarDecisao = 0;
                    //reseta o campo distancia
                    resultTravelledDistance.setText("00.00");
                    //reseta a variavel EndDistance
                    EndDistance = " ";
                }
                else{
                    buildAlert(getString(R.string.Alert_routeNoStarted));
                    alerta.show();
                }
            }
        });
    }

    //Cria alerta
    private void buildAlert(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(msg);
        alerta = builder.create();
    }
    //Cria alerta com botão
    private void buildAlertGPS(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(msg);
        //define um botão como positivo.
        builder.setPositiveButton(getString(R.string.button_yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                abreConfGPS();
            }
        });
        //define um botão como negativo.
        builder.setNegativeButton(getString(R.string.button_no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {

            }
        });
        alerta = builder.create();
    }

    //Verifica permissão
    private void check_permission(){
        if (ActivityCompat.checkSelfPermission (this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            buildAlert(getString(R.string.Alert_permissionGranted));
            alerta.show();
        }
        else {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION},1001);
        }
    }
    //Verificar a permissao e ativa o update do gps
    private void check_permission_ativaGPS(){
        if (ActivityCompat.checkSelfPermission (this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 1, locationListener);
            buildAlert(getString(R.string.Alert_enableGPS));
            alerta.show();
            auxProvider = 1;
        }
        else {
            buildAlert(getString(R.string.Alert_requiredPermission));
            alerta.show();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_GPS){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //permissão concedida
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, getString(R.string.gps_on), Toast.LENGTH_SHORT).show();
                }
            }
            else{
                //usuário negou, não ativamos
                Toast.makeText(this, getString(R.string.no_gps_no_app), Toast.LENGTH_SHORT).show();
            }
        }
    }
    //Código p/ abrir a configuração do GPS
    public void abreConfGPS(){
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }

    //observer do time
    private Chronometer.OnChronometerTickListener chronometerWatcher = new Chronometer.OnChronometerTickListener() {
        @Override
        public void onChronometerTick(Chronometer chronometer) {
            TimeRunning += 1;
            int minut = TimeRunning / 60;
            int second = TimeRunning % 60;
            EndTime = String.format("Time: %d:%d", minut, second);
        }
    };

    private void mostraResultadoFinal(){
        Toast.makeText(this, String.format("Time: %s, Distance: %s meter", EndTime, EndDistance), Toast.LENGTH_SHORT).show();
        TimeRunning = 0;
    }

}
