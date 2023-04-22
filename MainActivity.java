package com.example.pointgpstracker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.SensorEvent;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
//-------------------------------------------------
// This PointGPSTracker app works:
// Determines GPS and Network coordinates, shows altitude, calculates bearing between two points
// Let's make two applications out of it - an SMS tracker that will send coordinates to the specified phone every 30 minutes
// and an application for the autopilot of an aircraft-type UAV
//
// (c) by Valery Shmelev (Oflameron) https://www.linkedin.com/in/valery-shmelev-479206227/
// GitHUB Repository  https://github.com/vallshmeleff
//
//-------------------------------------------------
public class MainActivity extends AppCompatActivity {
    TextView tvEnabledGPS;
    TextView tvStatusGPS;
    TextView tvLocationGPS;
    TextView tvEnabledNet;
    TextView tvStatusNet;
    TextView tvLocationNet;
    TextView tvAltitude;
    TextView tvAzimut;
    TextView Coord4Azimut; // Start and end coordinates for azimuth
    TextView tvGSMSpeed;
    TextView tvCalcSpeed;
    private LocationManager locationManager;
    StringBuilder sbGPS = new StringBuilder();
    StringBuilder sbNet = new StringBuilder();

    public double sorseLat = 0; // Where are you now
    public double sorseLong = 0; // Where are you now
    public double sorseLat2 = 0; // Where are you now - Old. Saved
    public double sorseLong2 = 0; // Where are you now - Old. Saved
    public double OldTime = 0;
    public double NewTime = 0.1;

    public float azimuth;
    public float baseAzimuth;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvEnabledGPS = (TextView) findViewById(R.id.tvEnabledGPS);
        tvStatusGPS = (TextView) findViewById(R.id.tvStatusGPS);
        tvLocationGPS = (TextView) findViewById(R.id.tvLocationGPS); // GPS Positioning
        tvEnabledNet = (TextView) findViewById(R.id.tvEnabledNet);
        tvStatusNet = (TextView) findViewById(R.id.tvStatusNet);
        tvLocationNet = (TextView) findViewById(R.id.tvLocationNet); // Position determination by base stations
        tvAltitude = (TextView) findViewById(R.id.tvAltitude); // Height above sea level
        Coord4Azimut = (TextView) findViewById(R.id.Coord4Azimut); // Bearing
        tvAzimut = (TextView) findViewById(R.id.tvAzimut);
        tvGSMSpeed = (TextView) findViewById(R.id.tvGSMSpeed);
        tvCalcSpeed = (TextView) findViewById(R.id.tvCalcSpeed); // Calculate the speed by changing the GPS coordinates

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        Coord4Azimut.setText("Azimut for : \n Dest Latitude : " + "\n Dest Longitude : " +  "\n This Latitude : " +  "\n Dest Longitude : ");

    }  // OnCreate



    @Override
    protected void onResume() {
        super.onResume();
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
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                1000 * 10, 10, locationListener);
        locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, 1000 * 10, 10,
                locationListener);
        checkEnabled();
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(locationListener);
    }

    private LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            showLocation(location);
        }

        @Override
        public void onProviderDisabled(String provider) {
            checkEnabled();
        }

        @Override
        public void onProviderEnabled(String provider) {
            checkEnabled();
            Log.i("CHECK ENABLE 0", " == == onProviderEnabled == == ");
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

        @Override @SuppressWarnings("deprecation")
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (provider.equals(LocationManager.GPS_PROVIDER)) {
                tvStatusGPS.setText("Status %: " + String.valueOf(status));
            } else if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
                tvStatusNet.setText("Status %: " + String.valueOf(status));
            }
        }
    };

    public void onSensorChanged(SensorEvent event) {
        azimuth = event.values[0];
        baseAzimuth = azimuth;

    }

        private void showLocation(Location location) {
            double currentSpeed,kmphSpeed;
        if (location == null)
            return;
        if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            tvLocationGPS.setText(formatLocation(location));
            tvAltitude.setText("Altitude: " + location.getAltitude());
                                // location.getAccuracy() - The smaller the returned number, the higher the precision
                                // bearingTo - bearingTo(Location dest) - Shows the bearing to the specified point dest
                                //
            sorseLat2 = sorseLat; // Where are you now - Previous value
            sorseLong2 = sorseLong; // Where are you now - Previous value
            OldTime = NewTime; // Save Time
            NewTime = location.getTime(); // New Time

            sorseLat = location.getLatitude(); // Where are you now
            sorseLong = location.getLongitude(); // Where are you now

            //------ In this example, the destination is given by numbers --------
            double destLat = 55.9242; // Korolev Point
            double destLong = 10.8593;
            Coord4Azimut.setText("Azimut : \n Dest Latitude : " + String.valueOf(destLat) +  "\n Dest Longitude : " + String.valueOf(destLong) +  "\n This Latitude : " + String.valueOf(sorseLat) +  "\n Dest Longitude : " + String.valueOf(sorseLong));

            Location from = new Location(LocationManager.GPS_PROVIDER);
            Location to = new Location(LocationManager.GPS_PROVIDER);
            from.setLatitude(sorseLat); // This Point
            from.setLongitude(sorseLong);
            to.setLatitude(destLat); // Destination Point
            to.setLongitude(destLong);
            //------ Calculate azimuth (bearing) --------
            float bearingTo = from.bearingTo(to);
            Coord4Azimut.setText("GPS Azimut :" + String.valueOf(bearingTo));
            // The bearing is calculated for a perfect ball. There are no corrections for the height of the position and the non-sphericity of the Earth.
            //------ GSM Speed --------
            if (location.hasSpeed()) {
                double speed = location.getSpeed();
                currentSpeed = round(speed,3,BigDecimal.ROUND_HALF_UP);
                kmphSpeed = round((currentSpeed*3.6),3,BigDecimal.ROUND_HALF_UP);
                tvGSMSpeed.setText("GPS Speed :" + String.valueOf(kmphSpeed));
            } else {
                tvGSMSpeed.setText("GPS Speed : No data");
            //------ Calculate the speed by changing the GPS coordinates --------
                double radius = 6371000;
                double dLat = Math.toRadians(sorseLat-sorseLat2);
                double dLon = Math.toRadians(sorseLong-sorseLong2);
                double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                        Math.cos(Math.toRadians(sorseLat)) * Math.cos(Math.toRadians(sorseLat2)) *
                                Math.sin(dLon/2) * Math.sin(dLon/2);
                double c = 2 * Math.asin(Math.sqrt(a));
                double distance =  Math.round(radius * c);

                double timeDifferent = NewTime - OldTime;
                tvCalcSpeed.setText("Calc Speed : " + String.valueOf(distance/timeDifferent));

            }

        } else if (location.getProvider().equals(
                LocationManager.NETWORK_PROVIDER)) {
            tvLocationNet.setText(formatLocation(location));
            tvAltitude.setText("Altitude: " + location.getAltitude());
        }
    }

    public static double round(double unrounded, int precision, int roundingMode) {
        BigDecimal bd = new BigDecimal(unrounded);
        BigDecimal rounded = bd.setScale(precision, roundingMode);
        return rounded.doubleValue();
    }

    private String formatLocation(Location location) {
        if (location == null)
            return "";
        return String.format(
                "Source Coordinates: lat = %1$.4f, lon = %2$.4f, time = %3$tF %3$tT",
                location.getLatitude(), location.getLongitude(),
                        location.getTime());

    }

    private void checkEnabled() {
        tvEnabledGPS.setText("Enabled: "
                + locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER));
        tvEnabledNet.setText("Enabled: "
                + locationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    public void onClickLocationSettings(View view) {
        startActivity(new Intent(
                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    };





} // MainActivity

