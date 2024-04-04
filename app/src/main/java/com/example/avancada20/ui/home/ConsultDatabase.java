package com.example.avancada20.ui.home;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.calculos.GeoCalculator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;

public class ConsultDatabase extends Thread{
    private List<Region> regions;
    private String locationName;
    private double latitude;
    private double longitude;

    private Semaphore semaphore;


    private DatabaseReference referencia = FirebaseDatabase.getInstance().getReference();
    private static final long TIMEOUT_MILLISECONDS = 5000;

    public ConsultDatabase(List<Region> regions, String locationName, double latitude, double longitude, Semaphore semaphore) {
        this.regions = regions;
        this.locationName = locationName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.semaphore = semaphore;

    }

    @Override
    public void run() {
        try {
            // Adquira a permissão do semáforo antes de acessar a lista
            semaphore.acquire();

            // Consultar o banco de dados para obter as regiões do banco
            consultarBanco(new ConsultaCallback() {
                @Override
                public void onRegionsLoaded(List<Region> regionsFromDatabase) {
                    // Realizar a comparação aqui
                    boolean regionExists = false;
                    for (Region region : regionsFromDatabase) {
                        Log.d("Consulta Banco de Dados", "Região do Banco de Dados - Nome: " + region.getName() );
                        if (region.getName().equals(locationName)) {
                            regionExists = true;
                            break;
                        }
                    }

                    if (!regionExists) {
                        // Verificar se a nova região está a menos de 30 metros de distância de outras regiões na lista local e do banco de dados
                        boolean tooClose = checkRegionProximity(latitude, longitude, regionsFromDatabase);
                        if (!tooClose) {
                            // Adicionar o objeto Region à lista de regiões local
                            RegionUpdaterThread thread = new RegionUpdaterThread(regions, locationName, latitude, longitude,semaphore);
                            semaphore.release();
                            thread.start();

                        } else {
                            // Se a nova região estiver muito próxima de outra região, registrar uma mensagem no log
                            Log.d("Consulta Banco de Dados ", "A nova região está muito próxima de outra região do Banco");
                        }
                    } else {
                        // Se a região já existir, registrar uma mensagem no log
                        Log.d("Consulta Banco de Dados", "Esta região já está na lista do Banco de Dados");
                    }

                    // Libere a permissão do semáforo após acessar a lista
                    semaphore.release();
                    Log.d("Consulta Banco de Dados", "Semáforo do Banco liberado");
                }

                @Override
                public void onCancelled() {
                    // Tratar o cancelamento da consulta
                    Log.d("Consulta Banco de Dados", "Consulta cancelada");
                    // Libere a permissão do semáforo após acessar a lista
                    semaphore.release();
                    Log.d("Consulta Banco de Dados", "Semáforo do Banco liberado");
                }
            });

        } catch (InterruptedException e) {
            // Handle any interruption exception that may occur
            e.printStackTrace();
        }
        Log.d("Consulta Banco de Dados", "Thread Finalizada");
    }



    private boolean checkRegionProximity(double latitude, double longitude, List<Region> regions) {
        GeoCalculator cal = new GeoCalculator();
        for (Region region : regions) {
            double distance = cal.calculateDistance(region.getLatitude(), region.getLongitude(), latitude, longitude);
            if (distance < 30) {
                return true;
            }
        }
        return false;
    }
    private void consultarBanco(final ConsultaCallback callback) {
        DatabaseReference regiao = referencia.child("regioes");
        List<Region> list = new ArrayList<>();
        regiao.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Region> lista = new ArrayList<>();
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    String name = childSnapshot.child("name").getValue(String.class);
                    double latitude = childSnapshot.child("latitude").getValue(Double.class);
                    double longitude = childSnapshot.child("longitude").getValue(Double.class);

                    Region region = new Region(name, latitude, longitude);
                    lista.add(region);
                }
                // Notificar o callback com a lista de regiões após a conclusão da consulta
                 callback.onRegionsLoaded(lista);


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.i ("Consulta Banco de Dados ", "Erro na leitura do Banco de Dados" + error);
                // Notificar o callback sobre o cancelamento da consulta
                callback.onCancelled();
            }
        });

    }


}

