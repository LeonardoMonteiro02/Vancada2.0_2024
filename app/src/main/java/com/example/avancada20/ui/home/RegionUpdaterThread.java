/**
 * Classe responsável por atualizar a lista de regiões com base em novos dados de localização.
 *
 * Esta classe implementa uma thread que executa a lógica para adicionar uma nova região à lista de regiões.
 * Ao receber uma nova localização, adquire a permissão de um semáforo antes de acessar a lista de regiões.
 * Verifica se a região já existe na lista. Se não existir, verifica se a nova região está a menos de 30 metros de distância de outras regiões na lista.
 * Se a nova região não estiver muito próxima, cria um objeto Region com os dados da localização e o adiciona à lista de regiões.
 * Registra mensagens no log para indicar as ações realizadas ou situações encontradas.
 * Libera a permissão do semáforo após acessar a lista de regiões.
 * Utiliza uma classe GeoCalculator para calcular a distância entre a nova região e as regiões existentes na lista.
 *
 * Autor: Leonardo Monteiro
 * Data: 05/04/2024
 */


package com.example.avancada20.ui.home;



import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.example.calculos.GeoCalculator;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class RegionUpdaterThread extends Thread {
    private List<Region> regions;
    private String locationName;
    private double latitude;
    private double longitude;

    private Semaphore semaphore;
    Random random = new Random();

    private DatabaseReference referencia = FirebaseDatabase.getInstance().getReference();

    public RegionUpdaterThread(List<Region> regions, String locationName, double latitude, double longitude, Semaphore semaphore) {
        this.regions = regions;
        this.locationName = locationName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.semaphore = semaphore;
    }

    /**
     * Executa a lógica para adicionar uma nova região à lista de regiões.
     * Adquire a permissão do semáforo antes de acessar a lista.
     * Verifica se a região já existe na lista. Se não existir, verifica se a nova região está a menos de 30 metros de distância de outras regiões na lista.
     * Se não estiver muito próxima, cria um objeto Region com os dados da localização e o adiciona à lista de regiões.
     * Registra mensagens no log para indicar as ações realizadas ou situações encontradas.
     * Finalmente, libera a permissão do semáforo após acessar a lista.
     */
    @Override
    public void run() {
        try {
            // Adquira a permissão do semáforo antes de acessar a lista
            semaphore.acquire();

            // Verificar se a região já existe na lista
            boolean regionExists = false;
            for (Region region : regions) {
                Log.d("Consulta Na Lista", "Região do Banco de Dados - Nome: " + region.getName());
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
                    Region newRegion = new Region(locationName, latitude, longitude, System.nanoTime(), Math.abs(random.nextInt()));

                    // Adicionar o objeto Region à lista de regiões
                    regions.add(newRegion);

                    // Exibir o tamanho atual da lista de regiões no log
                    Log.d("Consulta Na Lista", "Região Adicionada na Lista " + "Size lista:  " + newRegion.getuser());
                } else {
                    // Se a nova região estiver muito próxima de outra região, registrar uma mensagem no log
                    Log.d("Consulta Na Lista", "A nova região está muito próxima de outra região da Lista");
                }
            } else {
                // Se a região já existir, registrar uma mensagem no log
                Log.d("Consulta Na Lista", "Esta região já está na lista");
            }
        } catch (InterruptedException e) {
            // Lidar com qualquer exceção de interrupção que possa ocorrer
            e.printStackTrace();
        } finally {
            // Libere a permissão do semáforo após acessar a lista
            semaphore.release();
            Log.d("Consulta Na Lista", "Semáforo da Lista liberado");
        }
        Log.d("Consulta Na Lista", "Thread Finalizada");
    }


    /**
     * Verifica se a nova região está muito próxima de outras regiões na lista.
     * Utiliza um objeto GeoCalculator para calcular a distância entre a nova região e as regiões existentes na lista.
     * Percorre todas as regiões na lista e calcula a distância entre cada uma delas e a nova região.
     * Se a distância entre a nova região e qualquer região na lista for menor que 30 metros, retorna verdadeiro.
     * Caso contrário, retorna falso.
     *
     * @param latitude  A latitude da nova região.
     * @param longitude A longitude da nova região.
     * @param regions   A lista de regiões existentes.
     * @return True se a nova região estiver muito próxima de outras regiões na lista, false caso contrário.
     */
    private boolean checkRegionProximity(double latitude, double longitude, List<Region> regions) {
        GeoCalculator cal = new GeoCalculator();
        for (Region region : regions) {
            // Calcula a distância entre a nova região e a região atual na lista
            double distance = cal.calculateDistance(region.getLatitude(), region.getLongitude(), latitude, longitude);
            // Se a distância for menor que 30 metros, retorna verdadeiro
            if (distance < 30) {
                return true;
            }
        }
        // Se nenhuma região na lista estiver muito próxima da nova região, retorna falso
        return false;
    }

}