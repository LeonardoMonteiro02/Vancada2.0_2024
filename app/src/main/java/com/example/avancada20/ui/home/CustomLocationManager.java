/**
 * Classe responsável por gerenciar as atualizações de localização do dispositivo.
 *
 * Esta classe encapsula a lógica para solicitar permissões de localização, iniciar e parar as atualizações de localização em segundo plano,
 * e fornecer as informações de localização para outras partes do aplicativo por meio de um ouvinte de callback.
 *
 * Principais funcionalidades:
 * - Solicitação e verificação de permissões de localização.
 * - Inicialização e interrupção das atualizações de localização em segundo plano.
 * - Registro de mensagens de log para monitorar o status das atualizações de localização.
 * - Envio de informações de localização para um ouvinte de callback registrado.
 *
 * Autor: Leonardo Monteiro
 * Data: 05/04/2024
 */


package com.example.avancada20.ui.home;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;

public class CustomLocationManager {

    private static final String TAG = "CustomLocationManager";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final long UPDATE_INTERVAL = 5000; // 5 segundos
    private static final int FASTEST_UPDATE_INTERVAL = 2000; // 2 segundos

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private LocationCallbackListener callbackListener;
    private Handler handler;
    private Context context;

    public CustomLocationManager(Context context) {
        this.context = context;
        fusedLocationProviderClient = new FusedLocationProviderClient(context);
        createLocationCallback();
        handler = new Handler(Looper.getMainLooper());
        requestLocationPermission();
    }

    /**
     * Define o ouvinte de callback de localização.
     * Define o callbackListener como o ouvinte de callback de localização fornecido.
     * Isso permite que outras classes registrem ouvintes para receber atualizações de localização.
     */
    public void setLocationCallbackListener(LocationCallbackListener listener) {
        this.callbackListener = listener; // Define o callbackListener como o ouvinte de callback de localização fornecido
    }


    /**
     * Inicia as atualizações de localização em segundo plano.
     * Cria uma nova thread para executar a lógica de iniciar as atualizações de localização.
     * Dentro da nova thread, prepara o Looper para processar mensagens de localização.
     * Verifica se a permissão de localização foi concedida.
     * Se a permissão foi concedida, registra uma mensagem de log e inicia as atualizações de localização.
     * Se a permissão não foi concedida, registra uma mensagem de log informando que a permissão de localização não foi concedida.
     */
    public void startLocationUpdatesInBackground() {
        new Thread(() -> {
            Looper.prepare(); // Prepara o Looper para processar mensagens de localização
            if (checkLocationPermission()) { // Verifica se a permissão de localização foi concedida
                Log.d(TAG, "Location permission granted. Starting location updates..."); // Registra uma mensagem de log informando que a permissão de localização foi concedida
                startLocationUpdates(); // Inicia as atualizações de localização
            } else {
                Log.d(TAG, "Location permission not granted."); // Registra uma mensagem de log informando que a permissão de localização não foi concedida
            }
            Looper.loop(); // Inicia o loop do Looper para processar mensagens de localização
        }).start(); // Inicia a nova thread
    }


    /**
     * Verifica se a permissão de localização foi concedida.
     * Verifica se a permissão ACCESS_FINE_LOCATION ou ACCESS_COARSE_LOCATION foi concedida.
     * Retorna verdadeiro se pelo menos uma das permissões foi concedida, falso caso contrário.
     * Registra uma mensagem de log indicando se a permissão de localização foi concedida ou não.
     *
     * @return True se a permissão de localização foi concedida, false caso contrário.
     */
    public boolean checkLocationPermission() {
        // Verifica se a permissão ACCESS_FINE_LOCATION foi concedida
        boolean fineLocationPermissionGranted = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        // Verifica se a permissão ACCESS_COARSE_LOCATION foi concedida
        boolean coarseLocationPermissionGranted = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        // Verifica se pelo menos uma das permissões foi concedida
        boolean isPermissionGranted = fineLocationPermissionGranted || coarseLocationPermissionGranted;
        // Registra uma mensagem de log indicando se a permissão de localização foi concedida ou não
        Log.d(TAG, "Location permission granted: " + isPermissionGranted);

        return isPermissionGranted; // Retorna verdadeiro se pelo menos uma das permissões foi concedida, falso caso contrário
    }


    /**
     * Solicita permissão de localização.
     * Verifica se o contexto é uma instância de Activity.
     * Se for uma instância de Activity, solicita permissão de localização utilizando ActivityCompat.requestPermissions().
     * Se não for uma instância de Activity, registra uma mensagem de log informando que não é possível solicitar permissões.
     */
    public void requestLocationPermission() {
        if (context instanceof Activity) { // Verifica se o contexto é uma instância de Activity
            ActivityCompat.requestPermissions((Activity) context, // Se for uma instância de Activity, solicita permissão de localização
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, // Permissões a serem solicitadas
                    LOCATION_PERMISSION_REQUEST_CODE); // Código de solicitação de permissão
        } else {
            Log.e(TAG, "Context is not an instance of Activity. Unable to request permissions."); // Se não for uma instância de Activity, registra uma mensagem de log informando que não é possível solicitar permissões
        }
    }


    /**
     * Inicia as atualizações de localização.
     * Verifica se a permissão de localização foi concedida.
     * Se a permissão foi concedida, cria uma solicitação de localização com prioridade alta.
     * Define o intervalo de atualização e o intervalo mais rápido de atualização.
     * Solicita atualizações de localização ao provedor de localização fundida usando a solicitação de localização criada.
     * Registra uma mensagem de log informando que as atualizações de localização foram iniciadas.
     * Se a permissão de localização não foi concedida, registra uma mensagem de log informando que as atualizações de localização não podem ser iniciadas devido à falta de permissões.
     */
    private void startLocationUpdates() {
        if (checkLocationPermission()) { // Verifica se a permissão de localização foi concedida
            LocationRequest locationRequest = LocationRequest.create(); // Cria uma solicitação de localização
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // Define a prioridade da solicitação como alta precisão
            locationRequest.setInterval(UPDATE_INTERVAL); // Define o intervalo de atualização
            locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL); // Define o intervalo mais rápido de atualização

            // Solicita atualizações de localização ao provedor de localização fundida usando a solicitação de localização criada
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            Log.d(TAG, "Location updates started."); // Registra uma mensagem de log informando que as atualizações de localização foram iniciadas
        } else {
            Log.d(TAG, "Location updates cannot be started due to lack of permissions."); // Se a permissão de localização não foi concedida, registra uma mensagem de log informando que as atualizações de localização não podem ser iniciadas devido à falta de permissões
        }
    }


    /**
     * Para as atualizações de localização.
     * Verifica se a permissão de localização foi concedida.
     * Se a permissão foi concedida, remove as atualizações de localização registradas com o provedor de localização fundida.
     * Registra uma mensagem de log informando que as atualizações de localização foram interrompidas.
     * Se a permissão de localização não foi concedida, registra uma mensagem de log informando que as atualizações de localização não podem ser interrompidas devido à falta de permissões.
     */
    public void stopLocationUpdates() {
        if (checkLocationPermission()) { // Verifica se a permissão de localização foi concedida
            fusedLocationProviderClient.removeLocationUpdates(locationCallback); // Remove as atualizações de localização registradas com o provedor de localização fundida
            Log.d(TAG, "Location updates stopped."); // Registra uma mensagem de log informando que as atualizações de localização foram interrompidas
        } else {
            Log.d(TAG, "Location updates cannot be stopped due to lack of permissions."); // Se a permissão de localização não foi concedida, registra uma mensagem de log informando que as atualizações de localização não podem ser interrompidas devido à falta de permissões
        }
    }


    /**
     * Cria um callback de localização.
     * Cria um novo LocationCallback e substitui seu método onLocationResult().
     * No método onLocationResult(), verifica se o objeto LocationResult não é nulo.
     * Se não for nulo, obtém a última localização do objeto LocationResult.
     * Se a localização não for nula e o callbackListener não for nulo, envia a nova localização recebida para o callbackListener usando um Handler.
     */
    private void createLocationCallback() {
        // Cria um novo LocationCallback e substitui seu método onLocationResult()
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) { // Verifica se o objeto LocationResult não é nulo
                    final Location location = locationResult.getLastLocation(); // Obtém a última localização do objeto LocationResult
                    if (location != null && callbackListener != null) { // Verifica se a localização não é nula e se o callbackListener não é nulo
                        handler.post(() -> callbackListener.onNewLocationReceived(location)); // Envia a nova localização recebida para o callbackListener usando um Handler
                    }
                }
            }
        };
    }

}
