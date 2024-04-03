package com.example.avancada20.ui.home;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.avancada20.FirebaseDataSaver;
import com.example.avancada20.MainActivity;


import com.example.avancada20.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;

public class HomeFragment extends Fragment implements OnMapReadyCallback, LocationCallbackListener {

    private static final int AUTOCOMPLETE_REQUEST_CODE = 1;

    private MapView mMapView;
    private GoogleMap mMap;
    private Marker currentLocationMarker;
    private CustomLocationManager customLocationManager;
    private AutoCompleteTextView locationSearchTextView;
    private PlacesAutoCompleteAdapter autoCompleteAdapter;
    private com.google.android.libraries.places.api.net.PlacesClient placesClient;
    private TextView currentLatTextView;
    private TextView currentLngTextView;
    private FirebaseDataSaver firebaseDataSaver = new FirebaseDataSaver();
    private Semaphore semaphore = new Semaphore(1);

    // Use your own API Key here
    private static final String PLACES_API_KEY = "AIzaSyAB11jVgtZNb0puLMS2gy-Slz25i2JYPqQ";

    @Nullable

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Places API
        Places.initialize(requireContext(), PLACES_API_KEY);

        // Initialize the MapView
        mMapView = root.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(this);

        // Initialize the CustomLocationManager and register this fragment as the callback listener
        customLocationManager = new CustomLocationManager(requireContext());
        customLocationManager.setLocationCallbackListener(this);

        if (!customLocationManager.checkLocationPermission()) {
            // Se a permissão de localização não foi concedida, solicite-a
            customLocationManager.requestLocationPermission();
        } else {
            // Se a permissão de localização foi concedida, inicie as atualizações de localização em segundo plano
            customLocationManager.startLocationUpdatesInBackground();
        }


        // Initialize AutoCompleteTextView for location search
        TextInputLayout locationSearchLayout = root.findViewById(R.id.editTextStartPoint);
        locationSearchTextView = root.findViewById(R.id.starting_point);
        locationSearchLayout.setStartIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAutocompleteActivity();
            }
        });

        // Initialize autoCompleteAdapter
        placesClient = com.google.android.libraries.places.api.Places.createClient(requireContext());
        autoCompleteAdapter = new PlacesAutoCompleteAdapter(requireContext(), placesClient);

        // Set the adapter for AutoCompleteTextView
        locationSearchTextView.setAdapter(autoCompleteAdapter);
        locationSearchTextView.setThreshold(1); // Set the minimum number of characters to trigger the suggestions

        // Set up item click listener for AutoCompleteTextView
        locationSearchTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedLocation = autoCompleteAdapter.getItem(position);
                if (selectedLocation != null) {
                    locationSearchTextView.setText(selectedLocation);
                    // Add any additional logic here if needed
                }
            }
        });

        // Initialize TextViews for displaying current latitude and longitude
        currentLatTextView = root.findViewById(R.id.latitudeTextView);
        currentLngTextView = root.findViewById(R.id.longitudeTextView);

        // Button to save current coordinates
        root.findViewById(R.id.buttonCoordenadas).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    saveCurrentLocationToRegionsList();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        // Button to save data to Firebase
        root.findViewById(R.id.buttonBancoDeDados).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCurrentLocationToFirebase();


            }
        });

        return root;
    }



    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        customLocationManager.stopLocationUpdates();

    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    @Override
    public void onNewLocationReceived(Location location) {
        updateMap(location);
        updateCurrentLocationTextViews(location.getLatitude(), location.getLongitude());
    }

    private void updateMap(Location location) {
        if (mMap != null) {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            if (currentLocationMarker == null) {
                // If the marker has not been created yet, create it and add it to the map
                currentLocationMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Minha Localização"));
            } else {
                // If the marker already exists, update its position
                currentLocationMarker.setPosition(latLng);
            }
            // Move the camera to the new position
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
        }
    }

    private void startAutocompleteActivity() {
        // Set the fields to specify which types of place data to
        // return after the user has made a selection.
        List<com.google.android.libraries.places.api.model.Place.Field> fields = Arrays.asList(com.google.android.libraries.places.api.model.Place.Field.ID, com.google.android.libraries.places.api.model.Place.Field.NAME);

        // Start the autocomplete intent.
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .build(requireContext());
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                com.google.android.libraries.places.api.model.Place place = Autocomplete.getPlaceFromIntent(data);
                locationSearchTextView.setText(place.getName());
                // Use place object to get details like place.getName(), place.getLatLng(), etc.
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                // TODO: Handle autocomplete error
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }
    }

    private void updateCurrentLocationTextViews(double latitude, double longitude) {
        currentLatTextView.setText("Lat: " + String.format("%.7f", latitude));
        currentLngTextView.setText("Long: " + String.format("%.7f", longitude));

    }

    private void saveCurrentLocationToRegionsList() throws InterruptedException {
        if (currentLocationMarker != null) {
            double latitude = Double.parseDouble(currentLatTextView.getText().toString().replace("Lat: ", "").replace(",", "."));
            double longitude = Double.parseDouble(currentLngTextView.getText().toString().replace("Long: ", "").replace(",", "."));

            // Get the region name using geocoding service
            String regionName = getRegionNameFromCoordinates(latitude, longitude);

            if (regionName != null) {
                // Accessing the list of regions directly from MainActivity
                List<Region> regions = ((MainActivity) requireActivity()).getRegions();

                // Start a new thread to add the region to the list

                ConsultDatabase thread = new ConsultDatabase(regions, regionName, latitude, longitude,semaphore);
                thread.start();
                try {
                    // Wait until the thread finishes its execution
                    thread.join();
                    Toast.makeText(requireContext(), "Current location saved to regions list. Size of the list: " + regions.size(), Toast.LENGTH_SHORT).show();
                } catch (InterruptedException e) {
                    // Handle any interruption exception that may occur during the wait
                    e.printStackTrace();
                }



            } else {
                Toast.makeText(requireContext(), "Region name not available", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(requireContext(), "Current location not available", Toast.LENGTH_SHORT).show();
        }
    }
    private void saveCurrentLocationToFirebase() {
        if (currentLocationMarker != null) {
            if (firebaseDataSaver == null || !firebaseDataSaver.isAlive()) { // Verifica se a thread ainda não foi iniciada
                // Criar uma instância de FirebaseDataSaver
                firebaseDataSaver = new FirebaseDataSaver(requireContext(), ((MainActivity) requireActivity()).getRegions(), semaphore);

                // Iniciar a execução da thread
                firebaseDataSaver.start();
            } else {
                Log.d("HomeFragment", "Thread já está em execução.");
            }

            // Notificar a thread quando a lista não estiver mais vazia
            synchronized (((MainActivity) requireActivity()).getRegions()) {
                ((MainActivity) requireActivity()).getRegions().notify();
            }

            // Notificar o usuário sobre a ação
            //Toast.makeText(requireContext(), "Dados salvos no Firebase.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "Localização atual não disponível.", Toast.LENGTH_SHORT).show();
        }
    }



    private String getRegionNameFromCoordinates(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        String regionName = null;
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    stringBuilder.append(address.getAddressLine(i)).append("\n");
                }
                regionName = stringBuilder.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return regionName;
    }
}
