package gr.hua.dit.android.geofenceapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import gr.hua.dit.android.geofenceapp.databinding.ActivityMapsBinding;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private final int LOCATION_PERMISSION_CODE = 100;
    private static boolean recording = false; // helps us determine whether a session is currently recording
    private static boolean serviceRunning = false; // only used by MyReceiver to check if it should pause or resume the service

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Map<LatLng, Circle> map; // holds all center points and their corresponding circles
    private ArrayList<LatLng> entryPoints;
    private ArrayList<LatLng> exitPoints;
    private Marker currentMarker; // used to update the marker of the current location (helps remove the old one)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map1);
        mapFragment.getMapAsync(this);

        map = new HashMap<>();
        entryPoints = new ArrayList<>();
        exitPoints = new ArrayList<>();
        Button cancelButton = findViewById(R.id.cancelButton);

        if (recording) {
            cancelButton.setText("Return"); // if the session is recording then change text to "Return" for more clearance
        } else {
            cancelButton.setText("Cancel");
        }

        findViewById(R.id.cancelButton).setOnClickListener(v -> finish()); // the "Cancel" button just sends us home
        findViewById(R.id.startButton).setOnClickListener(v -> {
            if (!recording) { // if the user presses "Start" for the first time
                insert(); // create a brand new session
                if (!map.isEmpty()) {
                    update(); // update the table with the center points saved so far in map only if it's not empty
                }
                recording = true; // the session is now recording
                serviceRunning = true; // the service should be running
                startService(new Intent().setClass(getApplicationContext(), MyService.class)); // starts the service
                cancelButton.setText("Return");
                ResultsMapsActivity.setChecked(true); // it automatically updates the pause/resume service button of ResultsMapsActivity to "Running"
            }
        });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
        } else {
            getCurrentLocation(); // if permissions are already granted by ResultsMapsActivity (or from before) just get the current location
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true); // for easier map navigation
        loadCurrentSession(); // load the current session when the google map is ready so it draws the circles saved so far

        mMap.setOnMapLongClickListener(latLng -> { // if the user presses and holds a part of the map
            boolean alreadyExists = false; // helps check if the user presses on an existing area
            ArrayList<LatLng> centerPoints = new ArrayList<>(map.keySet());

            for (LatLng centerPoint : centerPoints) { // for every center points so far, if the user pressed on an existing area
                if (HaversineDistanceCalculator.isPointInsideCircle(latLng, centerPoint)) {
                    Circle circle = map.get(centerPoint);
                    circle.remove(); // remove the circle from the google map
                    map.remove(centerPoint); // remove the the center point (and its circle value) from the hash map
                    alreadyExists = true; // note that the user pressed on an existing area
                    break;
                }
            }

            // if the user pressed on a new part of the map save it in the hash map and display it as a circle
            if (!alreadyExists) {
                Circle currentCircle = mMap.addCircle(new CircleOptions()
                        .center(latLng)
                        .radius(100)
                        .strokeColor(Color.BLUE)
                        .fillColor(Color.TRANSPARENT));
                map.put(latLng, currentCircle);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            }

            if (recording) { // if the current session is recording
                int updatedRows = update(); // update the db for every new change
                Log.d("Update Row", "Rows updated = " + updatedRows);
            }
        });
    }

    @SuppressLint("Range")
    private void loadCurrentSession() { // loads the center, entry and exit points of the current session
        if (recording) { // only load a session if its recording
            ContentResolver contentResolver = this.getContentResolver();
            Uri uri = Uri.parse("content://gr.hua.dit.android.geofenceapp/session/latest");
            try (Cursor cursor = contentResolver.query(uri, new String[]{"center_points"},
                    null, null, null)) {
                while (cursor.moveToNext()) {
                    String key = cursor.getString(cursor.getColumnIndex("center_points")); // get the center points as a string
                    ArrayList<LatLng> centerPoints = ArrayListConverter.fromString(key); // convert them to an ArrayList of LatLng using the TypeConverter
                    if (centerPoints == null) {
                        break;
                    }
                    for (LatLng centerPoint : centerPoints) { // draws a circle around each center point fetched
                        Circle currentCircle = mMap.addCircle(new CircleOptions()
                                .center(centerPoint)
                                .radius(100)
                                .strokeColor(Color.BLUE)
                                .fillColor(Color.TRANSPARENT));
                        map.put(centerPoint, currentCircle); // saved both the center points and their circles in a map
                    }
                }
            }
            try (Cursor cursor = contentResolver.query(uri, new String[]{"entry_points"},
                    null, null, null)) {
                while (cursor.moveToNext()) {
                    String key = cursor.getString(cursor.getColumnIndex("entry_points"));
                    ArrayList<LatLng> points = ArrayListConverter.fromString(key);
                    if (points != null) {
                        entryPoints = points; // if there are any entry points associated with the current session fetch them
                    }
                }
            }
            try (Cursor cursor = contentResolver.query(uri, new String[]{"exit_points"},
                    null, null, null)) {
                while (cursor.moveToNext()) {
                    String key = cursor.getString(cursor.getColumnIndex("exit_points"));
                    ArrayList<LatLng> points = ArrayListConverter.fromString(key);
                    if (points != null) {
                        exitPoints = points; // if there are any exit points associated with the current session fetch them also
                    }
                }
            }
        }
    }

    private Uri insert() { // creates a brand new session
        ContentResolver contentResolver = this.getContentResolver();
        Uri uri = Uri.parse("content://gr.hua.dit.android.geofenceapp/session/insert");
        ArrayList<LatLng> centerPoints = new ArrayList<>(map.keySet()); // get the center points saved so far in the map
        String json = ArrayListConverter.fromArrayList(centerPoints); // convert them to JSON because ContentValues won't accept them otherwise
        ContentValues contentValues = new ContentValues();
        contentValues.put("center_points", json); // pass the center points to contentValues as string
        return contentResolver.insert(uri, contentValues); // insert auto increments the session id
    }

    private int update() { // updates the database table with new center points similarly to insert
        ContentResolver contentResolver = this.getContentResolver();
        Uri uri = Uri.parse("content://gr.hua.dit.android.geofenceapp/session/update");
        ArrayList<LatLng> centerPoints = new ArrayList<>(map.keySet()); // get the updated center points from the map
        String jsonCenterPoints = ArrayListConverter.fromArrayList(centerPoints); // convert them to JSON
        String jsonEntryPoints = ArrayListConverter.fromArrayList(entryPoints); // do the same for entry/exit points so they don't get erased
        String jsonExitPoints = ArrayListConverter.fromArrayList(exitPoints);
        ContentValues contentValues = new ContentValues();
        contentValues.put("center_points", jsonCenterPoints);
        contentValues.put("entry_points", jsonEntryPoints);
        contentValues.put("exit_points", jsonExitPoints);
        return contentResolver.update(uri, contentValues, null, null); // update the current session with the new values (while keeping entry/exit points intact)
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation(); // gets the current location immediately upon being granted permissions
            }
        }
    }

    private void getCurrentLocation() { // gets the current location if it changed more than 50m in the last 5"
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = location -> {
            if (mMap != null) {
                if (currentMarker != null) {
                    currentMarker.remove(); // remove the old marker from the map
                }
                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                currentMarker = mMap.addMarker(new MarkerOptions().position(currentLocation).title("Current Location")); // add a new marker to the current location
                mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
            }
        };
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 50, locationListener);
        }
    }

    public static boolean isRecording() {
        return recording;
    }

    public static void setRecording(boolean recording) {
        MapsActivity.recording = recording;
    }

    public static boolean isServiceRunning() {
        return serviceRunning;
    }

    public static void setServiceRunning(boolean serviceRunning) {
        MapsActivity.serviceRunning = serviceRunning;
    }
}