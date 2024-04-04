package com.example.avancada20.ui.home;

import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.example.calculos.GeoCalculator;

import java.util.List;
import java.util.concurrent.Semaphore;

public class RegionUpdaterThread extends Thread {
    private List<Region> regions;
    private String locationName;
    private double latitude;
    private double longitude;

    private Semaphore semaphore;

    private DatabaseReference referencia = FirebaseDatabase.getInstance().getReference();

    public RegionUpdaterThread(List<Region> regions, String locationName, double latitude, double longitude, Semaphore semaphore) {
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

            // Verificar se a região já existe na lista
            boolean regionExists = false;
            for (Region region : regions) {
                Log.d("Consulta Na Lista", "Região do Banco de Dados - Nome: " + region.getName() );
                if (region.getName().equals(locationName)) {
                    regionExists = true;
                    break;
                }
            }

            if (!regionExists) {
                // Verificar se a nova região está a menos de 30 metros de distância de outras regiões na lista
                boolean tooClose = checkRegionProximity(latitude, longitude, regions);

                if (!tooClose) {
                    // Criar um objeto Region com os dados da localização
                    Region newRegion = new Region(locationName, latitude, longitude);

                    // Adicionar o objeto Region à lista de regiões
                    regions.add(newRegion);

                    // Exibir o tamanho atual da lista de regiões no log
                    Log.d("Consulta Na Lista", "Região Adicionada na Lista "+ "Size lista:  "+ regions.size());
                } else {
                    // Se a nova região estiver muito próxima de outra região, registrar uma mensagem no log
                    Log.d("Consulta Na Lista", "A nova região está muito próxima de outra região da Lista");
                }
            } else {
                // Se a região já existir, registrar uma mensagem no log
                Log.d("Consulta Na Lista", "Esta região já está na lista");
            }
        } catch (InterruptedException e) {
            // Handle any interruption exception that may occur
            e.printStackTrace();
        } finally {
            // Libere a permissão do semáforo após acessar a lista
            semaphore.release();
            Log.d("Consulta Na Lista", "Semafaro da Lista liberado");
        }
        Log.d("Consulta Na Lista", "Thread Finalizada");
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



}
