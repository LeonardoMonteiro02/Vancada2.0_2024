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

    public void setLocationCallbackListener(LocationCallbackListener listener) {
        this.callbackListener = listener;
    }

    public void startLocationUpdatesInBackground() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                if (checkLocationPermission()) {
                    Log.d(TAG, "Location permission granted. Starting location updates...");
                    startLocationUpdates();
                } else {
                    Log.d(TAG, "Location permission not granted.");
                }
                Looper.loop();
            }
        }).start();
    }

    public boolean checkLocationPermission() {
        boolean fineLocationPermissionGranted = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarseLocationPermissionGranted = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        boolean isPermissionGranted = fineLocationPermissionGranted || coarseLocationPermissionGranted;
        Log.d(TAG, "Location permission granted: " + isPermissionGranted);

        return isPermissionGranted;
    }
    public void requestLocationPermission() {
        // Verifica se a Activity está disponível
        if (context instanceof Activity) {
            // Solicita permissões de localização
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            Log.e(TAG, "Context is not an instance of Activity. Unable to request permissions.");
        }
    }
    private void startLocationUpdates() {
        if (checkLocationPermission()) {
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(5000); // Atualiza a cada 5 segundos

            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            Log.d(TAG, "Location updates started.");
        } else {
            Log.d(TAG, "Location updates cannot be started due to lack of permissions.");
        }
    }

    public void stopLocationUpdates() {
        if (checkLocationPermission()) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            Log.d(TAG, "Location updates stopped.");
        } else {
            Log.d(TAG, "Location updates cannot be stopped due to lack of permissions.");
        }
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    final Location location = locationResult.getLastLocation();
                    if (location != null && callbackListener != null) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callbackListener.onNewLocationReceived(location);
                            }
                        });
                    }
                }
            }
        };
    }
}
