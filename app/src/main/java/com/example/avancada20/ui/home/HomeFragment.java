/**
 * Classe responsável por controlar a lógica da tela inicial do aplicativo.
 *
 * Esta classe implementa um Fragment do Android que exibe um mapa, permite a busca de locais usando o Google Places API,
 * mostra a localização atual do usuário e oferece opções para salvar a localização atual em uma lista de regiões e para salvar os dados no Firebase.
 *
 * Principais funcionalidades:
 * - Inicialização do MapView e configuração do mapa.
 * - Gerenciamento de permissões de localização e atualizações de localização em segundo plano.
 * - Implementação da funcionalidade de busca de locais usando o Google Places API e preenchimento automático de locais.
 * - Exibição da localização atual do usuário no mapa.
 * - Salvamento da localização atual em uma lista de regiões e envio dos dados para o Firebase.
 * - Utilização de threads para operações assíncronas de salvamento de dados no Firebase.
 * - Geocodificação para obter o nome da região a partir das coordenadas de latitude e longitude.
 *
 * Autor: Leonardo Monteiro da Sé
 * Data: 05/04/2024
 */

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
import com.example.avancada20.ui.home.ConsultDatabase;
import com.example.avancada20.ui.home.Region;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private FirebaseDataSaver firebaseDataSaver;
    private  ConsultDatabase cosultdata;
    private Semaphore semaphore = new Semaphore(1);
    private ExecutorService executorService = Executors.newFixedThreadPool(2); // Dois threads para as duas operações


    // Use sua própria chave de API aqui
    private static final String PLACES_API_KEY = "AIzaSyAB11jVgtZNb0puLMS2gy-Slz25i2JYPqQ";


/***************************************************************************************************************************************/


    /**
     * Cria e retorna a exibição hierárquica associada ao fragmento.
     * Infla o layout do fragmento, inicializa e configura os elementos da interface do usuário,
     * como MapView, CustomLocationManager, AutoCompleteTextView para pesquisa de localização,
     * TextViews para exibir latitude e longitude atuais e botões para salvar localização e dados no Firebase.
     * Inicializa o Places API e configura o adaptador de autocompletar para AutoCompleteTextView.
     * Define os ouvintes de clique e item para AutoCompleteTextView e botões.
     *
     * @param inflater           O LayoutInflater usado para inflar o layout do fragmento.
     * @param container          O ViewGroup no qual a exibição do fragmento será inserida.
     * @param savedInstanceState Um Bundle que contém os dados do estado anterior do fragmento.
     * @return A exibição hierárquica associada ao fragmento.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflar o layout do fragmento
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        // Inicializar o Places API
        Places.initialize(requireContext(), PLACES_API_KEY);

        // Inicializar o MapView
        mMapView = root.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(this);

        // Inicializar o CustomLocationManager e registrar este fragmento como ouvinte de retorno de chamada
        customLocationManager = new CustomLocationManager(requireContext());
        customLocationManager.setLocationCallbackListener(this);

        // Verificar se a permissão de localização foi concedida
        if (!customLocationManager.checkLocationPermission()) {
            // Se a permissão de localização não foi concedida, solicitar permissão
            customLocationManager.requestLocationPermission();
        } else {
            // Se a permissão de localização foi concedida, iniciar atualizações de localização em segundo plano
            customLocationManager.startLocationUpdatesInBackground();
        }

        // Inicializar AutoCompleteTextView para pesquisa de localização
        TextInputLayout locationSearchLayout = root.findViewById(R.id.editTextStartPoint);
        locationSearchTextView = root.findViewById(R.id.starting_point);
        locationSearchLayout.setStartIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAutocompleteActivity();
            }
        });

        // Inicializar placesClient e autoCompleteAdapter
        placesClient = com.google.android.libraries.places.api.Places.createClient(requireContext());
        autoCompleteAdapter = new PlacesAutoCompleteAdapter(requireContext(), placesClient);

        // Configurar o adaptador para AutoCompleteTextView
        locationSearchTextView.setAdapter(autoCompleteAdapter);
        locationSearchTextView.setThreshold(1); // Definir o número mínimo de caracteres para acionar as sugestões

        // Configurar o ouvinte de clique de item para AutoCompleteTextView
        locationSearchTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedLocation = autoCompleteAdapter.getItem(position);
                if (selectedLocation != null) {
                    locationSearchTextView.setText(selectedLocation);
                    // Adicionar lógica adicional aqui, se necessário
                }
            }
        });

        // Inicializar TextViews para exibir latitude e longitude atuais
        currentLatTextView = root.findViewById(R.id.latitudeTextView);
        currentLngTextView = root.findViewById(R.id.longitudeTextView);

        // Botão para salvar as coordenadas atuais
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

        // Botão para salvar dados no Firebase
        root.findViewById(R.id.buttonBancoDeDados).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCurrentLocationToFirebase();
            }
        });

        return root;
    }


    // Este método é chamado quando o fragmento é retomado e resume o MapView.
    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    // Este método é chamado quando o fragmento é pausado e pausa o MapView.
    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    // Este método é chamado quando o fragmento é destruído e destrói o MapView e interrompe as atualizações de localização.
    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        customLocationManager.stopLocationUpdates();
        firebaseDataSaver.stopThread();
        executorService.shutdown();

    }

    // Este método é chamado quando a memória está baixa e notifica o MapView.
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    // Este método é chamado quando o mapa está pronto para uso.
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    // Este método é chamado quando uma nova localização é recebida e atualiza o mapa e os TextViews de localização atual.
    @Override
    public void onNewLocationReceived(Location location) {
        updateMap(location);
        updateCurrentLocationTextViews(location.getLatitude(), location.getLongitude());
    }


    /**
     * Atualiza o mapa com a localização atual do dispositivo.
     * Verifica se o mapa está disponível e, se estiver, atualiza o marcador da localização atual
     * para refletir a nova posição. Se o marcador ainda não existe, ele é criado e adicionado ao mapa.
     * Em seguida, move a câmera do mapa para a nova posição com um nível de zoom específico.
     *
     * @param location A localização atual do dispositivo.
     */
    private void updateMap(Location location) {
        // Verifica se o mapa está disponível
        if (mMap != null) {
            // Converte a localização em coordenadas de latitude e longitude
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            // Verifica se o marcador da localização atual ainda não foi criado
            if (currentLocationMarker == null) {
                // Se o marcador ainda não existe, cria um novo marcador na posição atual e adiciona ao mapa
                currentLocationMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Minha Localização"));
            } else {
                // Se o marcador já existe, atualiza apenas sua posição
                currentLocationMarker.setPosition(latLng);
            }

            // Move a câmera do mapa para a nova posição com um nível de zoom de 15
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
        }
    }



    /**
     * Inicia a atividade de autocompletar para permitir que o usuário selecione um local.
     * Define os campos para especificar quais tipos de dados de local devem ser retornados
     * após o usuário fazer uma seleção. Em seguida, inicia a intenção de autocompletar,
     * que permite ao usuário pesquisar e selecionar um local. O resultado é retornado através
     * do método onActivityResult com o código de solicitação AUTOCOMPLETE_REQUEST_CODE.
     */
    private void startAutocompleteActivity() {
        // Defina os campos para especificar quais tipos de dados de local devem ser retornados após o usuário fazer uma seleção.
        List<com.google.android.libraries.places.api.model.Place.Field> fields = Arrays.asList(com.google.android.libraries.places.api.model.Place.Field.ID, com.google.android.libraries.places.api.model.Place.Field.NAME);

        // Inicie a intenção de autocompletar.
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .build(requireContext());
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
    }




    /**
     * Método chamado quando o resultado de uma atividade é retornado.
     * Verifica se o resultado corresponde à solicitação de autocompletar.
     * Se for, verifica se a operação foi concluída com sucesso.
     * Se for bem-sucedida, obtém o local selecionado do intent e atualiza o TextView com o nome do local.
     * Se houver um erro durante a operação de autocompletar, ele pode ser tratado aqui.
     * Se o usuário cancelou a operação, não é realizada nenhuma ação adicional.
     *
     * @param requestCode O código de solicitação original enviado para a atividade.
     * @param resultCode  O código de resultado devolvido pela atividade filho.
     * @param data        O intent que contém o resultado da atividade.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Verifica se o resultado corresponde à solicitação de autocompletar.
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            // Verifica se a operação foi concluída com sucesso.
            if (resultCode == Activity.RESULT_OK) {
                // Obtém o local selecionado do intent.
                com.google.android.libraries.places.api.model.Place place = Autocomplete.getPlaceFromIntent(data);
                // Atualiza o TextView com o nome do local.
                locationSearchTextView.setText(place.getName());
                // Use o objeto place para obter detalhes como place.getName(), place.getLatLng(), etc.
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                // TODO: Lidar com erro de autocompletar
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // O usuário cancelou a operação.
            }
        }
    }




    /**
     * Atualiza os TextViews que exibem a latitude e longitude da localização atual.
     * Formata os valores de latitude e longitude e os define nos TextViews correspondentes.
     *
     * @param latitude  A latitude atual.
     * @param longitude A longitude atual.
     */
    private void updateCurrentLocationTextViews(double latitude, double longitude) {
        // Formata os valores de latitude e longitude e os define nos TextViews correspondentes.
        currentLatTextView.setText("Lat: " + String.format("%.7f", latitude));
        currentLngTextView.setText("Long: " + String.format("%.7f", longitude));
    }


    /**
     * Salva a localização atual na lista de regiões.
     * Verifica se o marcador da localização atual não é nulo.
     * Se não for nulo, extrai as coordenadas de latitude e longitude dos TextViews correspondentes.
     * Em seguida, obtém o nome da região usando o serviço de geocodificação.
     * Se o nome da região estiver disponível, adiciona a região à lista de regiões acessada através da MainActivity,
     * iniciando uma nova thread para realizar esta operação.
     * Espera até que a thread termine sua execução antes de continuar.
     * Se o nome da região não estiver disponível, exibe um Toast informando sobre a indisponibilidade.
     * Se a localização atual não estiver disponível, exibe um Toast informando sobre a indisponibilidade.
     *
     * @throws InterruptedException Se ocorrer uma interrupção enquanto aguarda a conclusão da thread.
     */
    private void saveCurrentLocationToRegionsList() throws InterruptedException {
        // Verifica se o marcador da localização atual não é nulo
        if (currentLocationMarker != null) {
            // Extrai as coordenadas de latitude e longitude dos TextViews correspondentes
            double latitude = Double.parseDouble(currentLatTextView.getText().toString().replace("Lat: ", "").replace(",", "."));
            double longitude = Double.parseDouble(currentLngTextView.getText().toString().replace("Long: ", "").replace(",", "."));

            // Obter o nome da região usando o serviço de geocodificação
            String regionName = getRegionNameFromCoordinates(latitude, longitude);

            if (regionName != null) {
                // Acessando a lista de regiões diretamente da MainActivity
                List<Region> regions = ((MainActivity) requireActivity()).getRegions();

                // Iniciar uma nova thread para adicionar a região à lista
                ConsultDatabase thread = new ConsultDatabase(regions, regionName, latitude, longitude, semaphore);
                thread.start();
                try {
                    // Aguardar até que a thread termine sua execução
                    thread.join();
                } catch (InterruptedException e) {
                    Log.e("Home Fragment", "Erro execução da Thread de Consulta de Banco  " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                // Exibe um Toast informando sobre a indisponibilidade do nome da região
                Toast.makeText(requireContext(), "Nome da região não disponível", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Exibe um Toast informando sobre a indisponibilidade da localização atual
            Toast.makeText(requireContext(), "Localização atual não disponível", Toast.LENGTH_SHORT).show();
        }
    }




    /**
     * Salva a localização atual no Firebase.
     * Verifica se o marcador da localização atual não é nulo.
     * Se não for nulo e a thread FirebaseDataSaver não estiver em execução, cria uma nova instância de FirebaseDataSaver,
     * vinculando-a à lista de regiões acessada através da MainActivity, e inicia sua execução em uma nova thread.
     * Se a thread já estiver em execução, registra uma mensagem de log.
     * Em seguida, notifica a thread quando a lista de regiões não estiver mais vazia.
     * Exibe um Toast para informar o usuário sobre a ação realizada.
     * Se a localização atual não estiver disponível, exibe um Toast informando ao usuário.
     */
    private void saveCurrentLocationToFirebase() {
        // Verifica se o marcador da localização atual não é nulo
        if (currentLocationMarker != null) {
            // Verifica se a thread FirebaseDataSaver ainda não foi iniciada
            if (firebaseDataSaver == null || !firebaseDataSaver.isAlive()) {
                // Criar uma instância de FirebaseDataSaver
                firebaseDataSaver = new FirebaseDataSaver(requireContext(), ((MainActivity) requireActivity()).getRegions(), semaphore);

                // Iniciar a execução da thread
                executorService.execute(firebaseDataSaver);
            } else {
                // Registra uma mensagem de log se a thread já estiver em execução
                Log.d("HomeFragment", "Thread já está em execução.");
            }

            // Notificar a thread quando a lista não estiver mais vazia
            synchronized (((MainActivity) requireActivity()).getRegions()) {
                ((MainActivity) requireActivity()).getRegions().notify();
            }

        } else {
            // Exibe um Toast informando ao usuário sobre a indisponibilidade da localização atual
            Toast.makeText(requireContext(), "Localização atual não disponível.", Toast.LENGTH_SHORT).show();
        }
    }





    /**
     * Obtém o nome da região a partir das coordenadas de latitude e longitude.
     * Utiliza um objeto Geocoder para realizar uma consulta reversa e obter o endereço correspondente.
     * Primeiro, tenta obter o endereço usando o método getFromLocation().
     * Se a primeira tentativa falhar, aguarda por 1 segundo e tenta novamente.
     * Se a segunda tentativa também falhar, retorna uma string vazia.
     *
     * @param latitude  A latitude das coordenadas.
     * @param longitude A longitude das coordenadas.
     * @return O nome da região correspondente às coordenadas ou uma string vazia se não puder ser obtido.
     */
    private String getRegionNameFromCoordinates(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        String fullAddress = "";

        try {
            // Esperar 1 segundo antes de tentar novamente
            Thread.sleep(1000);
            // Tentar obter o endereço correspondente às coordenadas
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            // Se houver endereços retornados e não estiver vazio
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                StringBuilder stringBuilder = new StringBuilder();
                // Construir o endereço completo a partir das linhas do endereço
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    stringBuilder.append(address.getAddressLine(i));
                    // Adicionar vírgula entre as linhas do endereço, exceto para a última linha
                    if (i < address.getMaxAddressLineIndex()) {
                        stringBuilder.append(", ");
                    }
                }

                // Remover espaços em branco extras e definir como o nome da região
                fullAddress = stringBuilder.toString().trim();
            }
        } catch (IOException e) {
            // Lidar com exceções de E/S, como falha na conexão de rede
            Log.e("Home Fragment", "Erro na primeira tentativa de obter o endereço completo a partir das coordenadas: " + e.getMessage());
            try {
                // Tentar novamente após um curto intervalo de tempo
                Thread.sleep(1000);
                // Nova tentativa de obter o endereço
                List<Address> addressesRetry = geocoder.getFromLocation(latitude, longitude, 1);
                if (addressesRetry != null && !addressesRetry.isEmpty()) {
                    Address addressRetry = addressesRetry.get(0);

                    StringBuilder stringBuilderRetry = new StringBuilder();
                    // Construir o endereço completo a partir das linhas do endereço
                    for (int i = 0; i <= addressRetry.getMaxAddressLineIndex(); i++) {
                        stringBuilderRetry.append(addressRetry.getAddressLine(i));
                        // Adicionar vírgula entre as linhas do endereço, exceto para a última linha
                        if (i < addressRetry.getMaxAddressLineIndex()) {
                            stringBuilderRetry.append(", ");
                        }
                    }

                    // Remover espaços em branco extras e definir como o nome da região
                    fullAddress = stringBuilderRetry.toString().trim();
                }
            } catch (InterruptedException | IOException ex) {
                // Lidar com exceções ao tentar novamente
                Log.e("Home Fragment", "Erro na segunda tentativa de obter o endereço completo: " + ex.getMessage());
            }
        } catch (Exception e) {
            // Lidar com outras exceções imprevistas
            Log.e("Home Fragment", "Erro ao obter o endereço completo a partir das coordenadas: " + e.getMessage());
        }

        // Retorna o nome da região correspondente ou uma string vazia se não puder ser obtido
        return fullAddress;
    }



}
