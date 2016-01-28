package ru.korshun.cobaguardidea.app;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.vision.barcode.Barcode;

import java.io.IOException;
import java.util.List;


public class GPSTracker
        implements LocationListener {


    private Context mContext;

    // Flag for GPS status
    private boolean isGPSEnabled = false;

    // Flag for network status
    private boolean isNetworkEnabled = false;

    // Flag for GPS status
    private boolean canGetLocation = false;

    //    private Location location; // Location
//    private double myLatitude; // Latitude
//    private double myLongitude; // Longitude

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000 * 30; // 30 seconds

    // Declaring a Location Manager
    private LocationManager locationManager;

    private GoogleMap googleMap;

    private Marker myMarker = null;

    private Barcode.GeoPoint myGeoPoint = null;


    public GPSTracker(Context context) {

        this.mContext = context;

        getLocation();

    }


    public void stopListening() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
        locationManager.removeUpdates(this);
    }


    private Location getLocation() {
        Location location = null;

        try {
            locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

            // Getting GPS status
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            // Getting network status
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                Toast.makeText(mContext, R.string.no_internet_or_gps, Toast.LENGTH_LONG).show();
                return null;
            } else {
                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                    if (ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return null;
                    }
                }
//                this.canGetLocation = true;
                if (isNetworkEnabled) {

                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    if (locationManager != null) {
                        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
//                            myLatitude = (int) (location.getLatitude() * 1E6);
//                            myLongitude = (int) (location.getLongitude() * 1E6);
                            myGeoPoint = new Barcode.GeoPoint(1, (int) (location.getLatitude() * 1E6), (int) (location.getLongitude() * 1E6));
                        }
                    }
                }
                // If GPS enabled, get myLatitude/myLongitude using GPS Services
                if (isGPSEnabled) {
                    if (location == null) {
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                        if (locationManager != null) {
                            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
//                                myLatitude = (int) (location.getLatitude() * 1E6);
//                                myLongitude = (int) (location.getLongitude() * 1E6);
                                myGeoPoint = new Barcode.GeoPoint(1, (int) (location.getLatitude() * 1E6), (int) (location.getLongitude() * 1E6));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(mContext, R.string.err_data, Toast.LENGTH_LONG).show();
            return null;
        }

        setCanGetLocation(true);
        return location;
    }


//    /**
//     * Function to get myLatitude
//     * */
//    public double getMyLatitude(){
////        if(location != null){
////            myLatitude = location.getMyLatitude();
////        }
//
//        // return myLatitude
//        return myLatitude;
//    }


//    /**
//     * Function to get myLongitude
//     * */
//    public double getMyLongitude(){
////        if(location != null){
////            myLongitude = location.getMyLongitude();
////        }
//
//        // return myLongitude
//        return myLongitude;
//    }



    public Barcode.GeoPoint getLocationFromAddress(String strAddress) {

        Geocoder coder = new Geocoder(mContext);
        List<Address> address;
        Address location;

        try {
            address = coder.getFromLocationName(strAddress, 5);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        if(address.size() > 0) {
            location = address.get(0);
            location.getLatitude();
            location.getLongitude();

            return new Barcode.GeoPoint(1, (int) (location.getLatitude() * 1E6), (int) (location.getLongitude() * 1E6));
        }

        else {
            return null;
        }


    }


    public boolean isCanGetLocation() {
        return canGetLocation;
    }

    public void setCanGetLocation(boolean canGetLocation) {
        this.canGetLocation = canGetLocation;
    }


    public void setMyPosition() {

        if(myMarker != null) {
            myMarker.remove();
        }

        LatLng my = new LatLng(myGeoPoint.lat / 1E6, myGeoPoint.lng / 1E6);
        myMarker = getGoogleMap().addMarker(
                new MarkerOptions()
                        .position(my)
                        .title(mContext.getString(R.string.me_marker_title))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

//        Barcode.GeoPoint geoPoint = getLocationFromAddress("Свердловская область, Екатеринбург");
//        if(geoPoint != null) {
//            LatLng latLng = new LatLng(geoPoint.lat / 1E6, geoPoint.lng / 1E6);
//            getGoogleMap().moveCamera(CameraUpdateFactory.zoomTo(11));
//            getGoogleMap().moveCamera(CameraUpdateFactory.newLatLng(latLng));
//        }
//        System.out.println("myLog: setMyPosition " + myGeoPoint.lat / 1E6 + ", " + myGeoPoint.lng / 1E6);
//        getGoogleMap().moveCamera(CameraUpdateFactory.zoomTo(11));
//        getGoogleMap().moveCamera(CameraUpdateFactory.newLatLng(my));
    }

    public void setMyPosition(boolean moveCamera) {

        setMyPosition();

        if(moveCamera) {
            Barcode.GeoPoint geoPoint = getLocationFromAddress("Свердловская область, Екатеринбург");
            if (geoPoint != null) {
                LatLng latLng = new LatLng(geoPoint.lat / 1E6, geoPoint.lng / 1E6);
                getGoogleMap().moveCamera(CameraUpdateFactory.zoomTo(11));
                getGoogleMap().moveCamera(CameraUpdateFactory.newLatLng(latLng));
            }
        }
    }

    public GoogleMap getGoogleMap() {
        return googleMap;
    }

    public void setGoogleMap(GoogleMap googleMap) {
        this.googleMap = googleMap;
    }

    @Override
    public void onLocationChanged(Location location) {

//        myLatitude = location.getLatitude();
//        myLongitude = location.getLongitude();

        myGeoPoint = new Barcode.GeoPoint(1, (int) (location.getLatitude() * 1E6), (int) (location.getLongitude() * 1E6));

        setMyPosition(false);

//        System.out.println("myLog: onLocationChanged " + location.getProvider() + " " + location.getMyLatitude() + " " + location.getMyLongitude());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
