/**
 * Adaptador responsável por fornecer sugestões de autocompletar para um campo de texto baseado em locais.
 *
 * Este adaptador extende ArrayAdapter<String> e implementa Filterable para fornecer funcionalidade de autocompletar.
 * Utiliza o Places API da Google para buscar previsões de autocompletar com base no texto de restrição fornecido.
 *
 * Principais funcionalidades:
 * - Fornecimento de previsões de autocompletar com base na consulta de pesquisa fornecida.
 * - Implementação de um filtro para o AutoCompleteTextView que executa a filtragem de previsões de autocompletar com base no texto de restrição fornecido.
 * - Utilização de Tasks para buscar previsões de autocompletar de forma assíncrona e aguardar o resultado da tarefa por até 30 segundos.
 * - Registro de erros no log e exibição de Toasts em caso de exceções durante a execução da tarefa de busca de previsões de autocompletar.
 *
 * Autor: Leonardo Monteiro
 * Data: 05/04/2024
 */


package com.example.avancada20.ui.home;


import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PlacesAutoCompleteAdapter extends ArrayAdapter<String> implements Filterable {

    private static final String TAG = "PlacesAutoCompleteAdap"; // Reduzido para 23 caracteres

    private List<String> predictions = new ArrayList<>();
    private PlacesClient placesClient;

    public PlacesAutoCompleteAdapter(Context context, PlacesClient placesClient) {
        super(context, android.R.layout.simple_dropdown_item_1line);
        this.placesClient = placesClient;
    }

    @Override
    public int getCount() {
        return predictions.size();
    }

    @Override
    public String getItem(int position) {
        return predictions.get(position);
    }

    @Override
    /**
     * Obtém o filtro para o AutoCompleteTextView.
     * Retorna um novo filtro que executa a filtragem de previsões de autocompletar com base no texto de restrição fornecido.
     * Se o texto de restrição não for nulo, chama o método getAutocomplete() para obter previsões de autocompletar com base no texto de restrição.
     * Define os resultados do filtro com as novas previsões e seu número correspondente.
     * Se houver resultados válidos, atualiza a lista de previsões e notifica o adaptador para atualizar a exibição.
     * Se não houver resultados válidos, notifica o adaptador que o conjunto de dados é inválido.
     */
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                if (constraint != null) { // Verifica se o texto de restrição não é nulo
                    List<String> newPredictions = getAutocomplete(constraint.toString()); // Obtém previsões de autocompletar com base no texto de restrição
                    results.values = newPredictions; // Define os resultados do filtro com as novas previsões
                    results.count = newPredictions.size(); // Define o número de resultados
                }
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) { // Verifica se há resultados válidos
                    predictions = (List<String>) results.values; // Atualiza a lista de previsões com os novos valores
                    notifyDataSetChanged(); // Notifica o adaptador para atualizar a exibição
                } else {
                    notifyDataSetInvalidated(); // Notifica o adaptador que o conjunto de dados é inválido
                }
            }
        };
    }


    /**
     * Obtém previsões de autocompletar para uma consulta de pesquisa fornecida.
     * Cria uma nova lista para armazenar as previsões de autocompletar.
     * Cria uma tarefa para buscar previsões de autocompletar com base na consulta de pesquisa fornecida.
     * Tenta aguardar o resultado da tarefa por até 30 segundos.
     * Se a resposta não for nula, itera sobre as previsões de autocompletar na resposta e as adiciona à lista de previsões.
     * Se ocorrer uma exceção durante a execução da tarefa, registra um erro no log e exibe um Toast informando sobre o erro.
     * Retorna a lista de previsões de autocompletar.
     */
    private List<String> getAutocomplete(@NonNull String query) {
        List<String> predictionsList = new ArrayList<>(); // Cria uma nova lista para armazenar as previsões de autocompletar
        Task<FindAutocompletePredictionsResponse> task = placesClient.findAutocompletePredictions(com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest.builder()
                .setQuery(query) // Define a consulta de pesquisa
                .build());

        try {
            FindAutocompletePredictionsResponse response = Tasks.await(task, 30, TimeUnit.SECONDS); // Tenta aguardar o resultado da tarefa por até 30 segundos
            if (response != null) { // Verifica se a resposta não é nula
                for (AutocompletePrediction prediction : response.getAutocompletePredictions()) { // Itera sobre as previsões de autocompletar na resposta
                    predictionsList.add(prediction.getFullText(null).toString()); // Adiciona as previsões à lista de previsões
                }
            }
        } catch (ExecutionException | InterruptedException | TimeoutException e) { // Trata possíveis exceções
            Log.e(TAG, "Error getting autocomplete prediction", e); // Registra um erro no log
            Toast.makeText(getContext(), "Error getting autocomplete prediction API call", Toast.LENGTH_SHORT).show(); // Exibe um Toast informando sobre o erro
        }

        return predictionsList; // Retorna a lista de previsões de autocompletar
    }

}
