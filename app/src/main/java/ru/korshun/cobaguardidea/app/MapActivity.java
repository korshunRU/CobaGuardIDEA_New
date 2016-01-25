package ru.korshun.cobaguardidea.app;


import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.vision.barcode.Barcode;


public class MapActivity
        extends FragmentActivity
        implements OnMapReadyCallback {


    private GoogleMap mMap;
    private double myLongitude = 0, myLatitude = 0;
    private GPSTracker gpsTracker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        gpsTracker = new GPSTracker(getApplicationContext());

        myLatitude = gpsTracker.getLatitude();
        myLongitude = gpsTracker.getLongitude();

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        Barcode.GeoPoint geoPoint1 = gpsTracker.getLocationFromAddress("Екатеринбург, Черепанова 4");
        Barcode.GeoPoint geoPoint2 = gpsTracker.getLocationFromAddress("Екатеринбург, Ленина 43");
        Barcode.GeoPoint geoPoint3 = gpsTracker.getLocationFromAddress("Екатеринбург, Космонавтов 43");

        LatLng ekb1 = new LatLng(geoPoint1.lat / 1E6, geoPoint1.lng / 1E6);
        mMap.addMarker(new MarkerOptions().position(ekb1).title("Екатеринбург, Черепанова 4"));
//        marker1.showInfoWindow();

        LatLng ekb2 = new LatLng(geoPoint2.lat / 1E6, geoPoint2.lng / 1E6);
        mMap.addMarker(new MarkerOptions().position(ekb2).title("Екатеринбург, Ленина 43"));

        LatLng ekb3 = new LatLng(geoPoint3.lat / 1E6, geoPoint3.lng / 1E6);
        mMap.addMarker(new MarkerOptions().position(ekb3).title("Екатеринбург, Космонавтов 43"));

        if(myLatitude > 0 && myLongitude > 0) {
            LatLng my = new LatLng(myLatitude, myLongitude);
            Marker marker1 = mMap.addMarker(new MarkerOptions().position(my).title("Вы тут").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

            marker1.showInfoWindow();

            mMap.moveCamera(CameraUpdateFactory.zoomTo(11));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(my));
        }

        else {

            mMap.moveCamera(CameraUpdateFactory.zoomTo(11));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(ekb1));

        }

        System.out.println("myLog: onMapReady");

    }
}
