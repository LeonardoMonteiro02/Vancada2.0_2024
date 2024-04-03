package com.example.avancada20.ui.home;

import android.location.Location;

public interface LocationCallbackListener {
    void onNewLocationReceived(Location location);
}