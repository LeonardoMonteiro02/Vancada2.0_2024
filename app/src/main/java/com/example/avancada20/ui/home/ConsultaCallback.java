package com.example.avancada20.ui.home;

import java.util.List;

public interface ConsultaCallback {
   void onRegionsLoaded(List<Region> regions);
    void onCancelled();
}

