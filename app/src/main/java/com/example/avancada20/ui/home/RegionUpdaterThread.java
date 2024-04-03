package com.example.avancada20.ui.home;

import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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
        for (Region region : regions) {
            double distance = calculateDistance(region.getLatitude(), region.getLongitude(), latitude, longitude);
            if (distance < 30) {
                return true;
            }
        }
        return false;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Implemente a lógica para calcular a distância entre duas coordenadas geográficas
        // Aqui está um exemplo simplificado usando a fórmula de Haversine:
        double R = 6371000; // Raio da Terra em metros
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c;
        return distance;
    }

}
