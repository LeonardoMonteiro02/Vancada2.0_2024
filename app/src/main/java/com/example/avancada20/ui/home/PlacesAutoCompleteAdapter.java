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
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                if (constraint != null) {
                    List<String> newPredictions = getAutocomplete(constraint.toString());
                    results.values = newPredictions;
                    results.count = newPredictions.size();
                }
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    predictions = (List<String>) results.values;
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        };
    }

    private List<String> getAutocomplete(@NonNull String query) {
        List<String> predictionsList = new ArrayList<>();
        Task<FindAutocompletePredictionsResponse> task = placesClient.findAutocompletePredictions(com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .build());

        try {
            FindAutocompletePredictionsResponse response = Tasks.await(task, 30, TimeUnit.SECONDS);
            if (response != null) {
                for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                    predictionsList.add(prediction.getFullText(null).toString());
                }
            }
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            Log.e(TAG, "Error getting autocomplete prediction", e);
            Toast.makeText(getContext(), "Error getting autocomplete prediction API call", Toast.LENGTH_SHORT).show();
        }

        return predictionsList;
    }
}
