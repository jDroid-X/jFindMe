package com.jdroidx.jfindme.ui.map;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.jdroidx.jfindme.R;
import com.jdroidx.jfindme.databinding.ActivityMapV2Binding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class MapActivity2 extends AppCompatActivity implements OnMapReadyCallback {

    private ActivityMapV2Binding binding;
    private GoogleMap googleMap;
    private BottomSheetBehavior<View> bottomSheetBehavior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapV2Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_container);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Initialize Bottom Sheet
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        // Fab Action
        binding.fabMyLocation.setOnClickListener(v -> {
            if (googleMap != null) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this).getLastLocation().addOnSuccessListener(location -> {
                        if (location != null) {
                            googleMap.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(new com.google.android.gms.maps.model.LatLng(location.getLatitude(), location.getLongitude()), 16.5f));
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.googleMap = map;
        // Apply default settings or styling here
    }
}
