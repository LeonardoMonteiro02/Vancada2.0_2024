/**
 * Esta classe representa uma thread responsável por consultar o banco de dados Firebase para obter informações sobre as regiões armazenadas.
 * Ela implementa a lógica para verificar se uma nova região a ser adicionada já existe no banco de dados e se está muito próxima de outras regiões existentes.
 * Se a nova região não existir no banco de dados e não estiver muito próxima de outras regiões, inicia uma nova thread para atualizar as regiões.
 *
 * Principais funcionalidades:
 * - Consulta o banco de dados Firebase para obter informações sobre as regiões armazenadas.
 * - Verifica se uma nova região a ser adicionada já existe no banco de dados e se está muito próxima de outras regiões existentes.
 * - Inicia uma nova thread para atualizar as regiões, se necessário.
 * - Registra mensagens de log para monitorar o status da consulta ao banco de dados.
 *
 * Autor: Leonardo Monteiro
 * Data: 05/04/2024
 */


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

    /**
     * Executa a lógica principal da thread.
     * Adquire a permissão do semáforo antes de acessar a lista.
     * Consulta o banco de dados para obter as regiões do banco.
     * Realiza a comparação entre as regiões do banco e a nova região a ser adicionada.
     * Se a nova região não existir no banco e não estiver muito próxima de outras regiões na lista local e do banco de dados, inicia uma nova thread para atualizar as regiões.
     * Registra mensagens de log para indicar ações realizadas ou exceções capturadas.
     * Libera a permissão do semáforo após acessar a lista.
     */
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
                        Log.d("Consulta Banco de Dados", "Região do Banco de Dados - Nome: " + region.getName());
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
                            RegionUpdaterThread thread = new RegionUpdaterThread(regions, locationName, latitude, longitude, semaphore);
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
            // Lidar com qualquer exceção de interrupção que possa ocorrer
            e.printStackTrace();
        }
        Log.d("Consulta Banco de Dados", "Thread Finalizada");
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
        GeoCalculator cal = new GeoCalculator(); // Utiliza um objeto GeoCalculator para calcular a distância
        for (Region region : regions) { // Percorre todas as regiões na lista
            double distance = cal.calculateDistance(region.getLatitude(), region.getLongitude(), latitude, longitude); // Calcula a distância entre a nova região e a região atual na lista
            if (distance < 30) { // Se a distância for menor que 30 metros, retorna verdadeiro
                return true;
            }
        }
        return false; // Caso contrário, retorna falso
    }

    /**
     * Consulta o banco de dados para obter as regiões armazenadas.
     * Obtém uma referência para o nó "regioes" no banco de dados.
     * Adiciona um ouvinte de evento de valor único para a referência.
     * Para cada região encontrada no banco de dados, extrai os dados (nome, latitude, longitude, timestamp, usuário) e cria um objeto Region correspondente.
     * Adiciona cada objeto Region a uma lista.
     * Notifica o callback com a lista de regiões após a conclusão da consulta bem-sucedida.
     * Em caso de erro na leitura do banco de dados, registra uma mensagem de log e notifica o callback sobre o cancelamento da consulta.
     *
     * @param callback O objeto de callback para notificar sobre o resultado da consulta.
     */
    private void consultarBanco(final ConsultaCallback callback) {
        DatabaseReference regiao = referencia.child("regioes"); // Obtém uma referência para o nó "regioes" no banco de dados
        List<Region> list = new ArrayList<>(); // Cria uma lista para armazenar as regiões obtidas do banco de dados
        regiao.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Region> lista = new ArrayList<>(); // Cria uma lista temporária para armazenar as regiões obtidas do banco de dados
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    // Extrai os dados (nome, latitude, longitude, timestamp, usuário) de cada região no banco de dados
                    String name = childSnapshot.child("name").getValue(String.class);
                    double latitude = childSnapshot.child("latitude").getValue(Double.class);
                    double longitude = childSnapshot.child("longitude").getValue(Double.class);
                    Long timestamp = childSnapshot.child("timestamp").getValue(Long.class);
                    int user = Math.toIntExact(childSnapshot.child("user").getValue(Long.class));
                    String key = childSnapshot.getKey();
                    int Chave = Integer.parseInt(key); // Chave por rota do banco de dados 1, 2,3 ...

                    // Cria um objeto Region com os dados extraídos
                    Region region = new Region(name, latitude, longitude, timestamp, user);
                    lista.add(region); // Adiciona o objeto Region à lista temporária
                }
                // Notificar o callback com a lista de regiões após a conclusão da consulta
                callback.onRegionsLoaded(lista);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Em caso de erro na leitura do banco de dados, registra uma mensagem de log
                Log.i("Consulta Banco de Dados", "Erro na leitura do Banco de Dados" + error);
                // Notificar o callback sobre o cancelamento da consulta
                callback.onCancelled();
            }
        });
    }

}