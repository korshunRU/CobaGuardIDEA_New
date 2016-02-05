package ru.korshun.cobaguardidea.app;


import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.vision.barcode.Barcode;

import java.util.HashMap;
import java.util.Map;

import ru.korshun.cobaguardidea.app.fragments.FragmentPassports;


public class MapActivity
        extends FragmentActivity
        implements OnMapReadyCallback {


//    private GoogleMap mMap;
    //    private double myLongitude = 0, myLatitude = 0;
    private GPSTracker gpsTracker;
    private HashMap<String, String> objectAddressMap = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Intent intent = getIntent();


        objectAddressMap = (HashMap<String, String>) intent.getSerializableExtra(FragmentPassports.OBJECT_ADDRESS_MAP_INTENT_KEY);

        gpsTracker = new GPSTracker(getApplicationContext());

    }


    @Override
    protected void onStop() {
        super.onStop();
        gpsTracker.stopListening();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

//        GoogleMap mMap = googleMap;

        gpsTracker.setGoogleMap(googleMap);

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
        }
        googleMap.setMyLocationEnabled(true);

        googleMap.getUiSettings().setZoomControlsEnabled(true);

        if(gpsTracker.isCanGetLocation()) {

            gpsTracker.setMyPosition(true);

            for (Map.Entry<String, String> item : objectAddressMap.entrySet()) {

                Barcode.GeoPoint geoPoint = gpsTracker.getLocationFromAddress("Свердловская область, Екатеринбург, " + item.getValue());

                if(geoPoint != null) {

                    LatLng latLng = new LatLng(geoPoint.lat / 1E6, geoPoint.lng / 1E6);
                    googleMap.addMarker(new MarkerOptions().position(latLng).title(item.getKey() + ", " + item.getValue()));

//                System.out.println("myLog: " + geoPoint.lat + ", " + geoPoint.lng);
                }

            }

        }

    }

}
