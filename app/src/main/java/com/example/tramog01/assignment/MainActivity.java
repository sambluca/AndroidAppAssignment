package com.example.tramog01.assignment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private MapView map;
    private GoogleMap gMap;
    public LatLng latLng = null;
    Marker youMarker = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        map = findViewById(R.id.mapView);
        map.onCreate(savedInstanceState);
        if(map != null) {
            map.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    gMap = googleMap;
                    map.onResume();
                }
            });
        }
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    "android.permission.ACCESS_FINE_LOCATION",
                    "android.permission.ACCESS_COARSE_LOCATION"
            }, 1);
            return;
        }
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                latLng = new LatLng(location.getLatitude(), location.getLongitude());
                if(youMarker != null) {
                    youMarker.remove();
                }
                youMarker = gMap.addMarker(new MarkerOptions().position(latLng).title("You!"));
                gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));

            }
            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {}
            @Override
            public void onProviderEnabled(String s) {}
            @Override
            public void onProviderDisabled(String s) {}
        });
    }

    public void apiCall(View v) {
        TextView stationList = findViewById(R.id.stationsList);

        if(latLng != null) {new SearchAsync().execute(); }
        else {
            stationList.append("The app can't seem to find your location! \n");
            stationList.append("Please make sure the app can access your location.");
        }
    }

    class SearchAsync extends AsyncTask<Void, Void, Void> {
        private TextView stationList = findViewById(R.id.stationsList);

        ArrayList<JSONObject> stationsData = new ArrayList<>();

        @Override
        protected Void doInBackground(Void... voids) {

            HttpURLConnection urlConnection = null;

            try {
                URL url = new URL("http://zebedee.kriswelsh.com:8080/stations?lat="+ latLng.latitude +"&lng=" + latLng.longitude);
                urlConnection = (HttpURLConnection) url.openConnection();

                InputStreamReader ins = new InputStreamReader(urlConnection.getInputStream());
                BufferedReader in = new BufferedReader(ins);
                String line = "";

                while((line = in.readLine()) != null) {
                    JSONArray ja = new JSONArray(line);
                    for(int i = 0; i < ja.length(); i++) {
                        JSONObject jo = (JSONObject) ja.get(i);

                        stationsData.add(jo);
                    }
                }

            } catch (IOException ioe){

            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if(urlConnection != null ){
                    urlConnection.disconnect();
                }
            }

            return null;
        }


        @Override
        protected void onPreExecute() {
            stationList.setText("Searching");
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            stationList.setText("");
            ArrayList<Marker> markers = new ArrayList<Marker>();
            markers.add(youMarker);
            youMarker.showInfoWindow();
            for(JSONObject stationData : stationsData ) {
                try {
                    float[] distanceBetween = new float[2];
                    LatLng stationLatLng = new LatLng(Double.parseDouble((String) stationData.get("Latitude")), Double.parseDouble((String) stationData.get("Longitude")));
                    Location.distanceBetween(
                            latLng.latitude,
                            latLng.longitude,
                            stationLatLng.latitude,
                            stationLatLng.longitude,
                            distanceBetween);
                    int distance = (int) Math.round(distanceBetween[0]);

                    Marker marker = gMap.addMarker(new MarkerOptions()
                            .position(stationLatLng)
                            .title(stationData.get("StationName").toString() + " - " + distance + "m")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
                            );

                    stationList.append("Station: " + stationData.get("StationName").toString() + " - " + distance + "m" + '\n');

                    markers.add(marker);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            LatLngBounds.Builder markerBuilder = new LatLngBounds.Builder();
            for (Marker marker : markers) {
                markerBuilder.include(marker.getPosition());
            }
            LatLngBounds bounds = markerBuilder.build();

            gMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));

            super.onPostExecute(aVoid);
        }
    }
}
