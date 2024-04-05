package com.example.avancada20;
import android.content.Context;
import android.util.Log;

import com.example.avancada20.ui.home.Region;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

public class FirebaseDataSaver implements Runnable {
    private static final String TAG = "FirebaseDataSaver";
    private DatabaseReference referencia = FirebaseDatabase.getInstance().getReference();
    private Context context;
    private List<Region> regions;
    private Semaphore semaphore;
    private int i = 0;
    private ExecutorService executorService;
    private volatile boolean running = true; // Flag para controlar a execução do loop
    private volatile boolean threadStarted = false; // Flag para indicar se a thread foi iniciada


    public FirebaseDataSaver(Context context, List<Region> regions, Semaphore semaphore) {
        this.context = context;
        this.regions = regions;
        this.semaphore = semaphore;
        this.executorService = executorService;
    }

    @Override
    /**
     * Executa a lógica principal da thread.
     * Define a flag threadStarted como true para indicar que a thread foi iniciada.
     * Executa um loop enquanto a flag running for true.
     * Dentro do loop, adquire o semáforo antes de salvar os dados.
     * Se a lista de regiões estiver vazia, libera o semáforo, aguarda até que a lista não esteja mais vazia e retoma a execução.
     * Se a lista não estiver vazia, salva os dados e, em seguida, libera o semáforo.
     * Registra mensagens de log para indicar ações realizadas ou exceções capturadas.
     */
    public void run() {
        threadStarted = true; // Define a flag threadStarted como true para indicar que a thread foi iniciada
        while (running) { // Executar o loop enquanto a flag running for true
            try {
                semaphore.acquire(); // Acquire semaphore before saving

                if (regions.isEmpty()) {
                    semaphore.release(); // Release semaphore after saving
                    synchronized (regions) {
                        regions.wait(); // Aguardar até que a lista não esteja mais vazia
                    }
                } else {
                    saveData();

                    semaphore.release(); // Release semaphore after saving
                    Log.d(TAG, "Semaphore released.");
                }

            } catch (InterruptedException e) {
                // Lidar com a exceção de interrupção
                Log.e(TAG, "InterruptedException: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }


    /**
     * Para a execução da thread.
     * Define a flag running como false para interromper o loop da thread.
     */
    public void stopThread() {
        running = false; // Método para parar o loop
    }


    /**
     * Salva os dados das regiões no Firebase Realtime Database.
     * Obtém uma referência para o nó "regioes" no banco de dados.
     * Percorre a lista de regiões e salva cada região como um nó filho sob o nó "regioes".
     * Limpa a lista de regiões após salvar com sucesso os dados.
     * Registra uma mensagem de log para indicar que os dados foram salvos com sucesso.
     */
    private void saveData() {
        DatabaseReference regiao = referencia.child("regioes"); // Obtém uma referência para o nó "regioes" no banco de dados

        for (Region region : regions) {
            // Salva cada região como um nó filho sob o nó "regioes"
            regiao.child(String.valueOf(i)).setValue(region);
            i++;
        }

        regions.clear(); // Limpa a lista de regiões após salvar com sucesso os dados
        Log.d(TAG, "Data saved successfully!"); // Registra uma mensagem de log para indicar que os dados foram salvos com sucesso
    }

    /**
     * Verifica se a thread está viva.
     * Retorna verdadeiro se a thread foi iniciada e ainda está em execução.
     *
     * @return True se a thread estiver viva, False caso contrário.
     */
    public boolean isAlive() {
        return threadStarted && running; // Retorna true se a thread foi iniciada e ainda está em execução
    }


}
