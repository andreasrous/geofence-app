package gr.hua.dit.android.geofenceapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

import gr.hua.dit.android.geofenceapp.databinding.ActivityResultsMapsBinding;

public class ResultsMapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private final int LOCATION_PERMISSION_CODE = 100;

    private GoogleMap mMap;
    private ActivityResultsMapsBinding binding;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private ArrayList<LatLng> entryPoints;
    private ArrayList<LatLng> exitPoints;
    private Marker currentMarker;

    // is used by MapsActivity to update the state of the pause/resume service button before entering this activity
    private static boolean checked = false;

    public ResultsMapsActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityResultsMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map2);
        mapFragment.getMapAsync(this);

        entryPoints = new ArrayList<>();
        exitPoints = new ArrayList<>();
        findViewById(R.id.returnButton).setOnClickListener(v -> finish()); // the "Return" button just sends us home
        ToggleButton restartButton = findViewById(R.id.restartButton);

        // by default checked = false, but if the user has already pressed the "Start" button from MapsActivity, checked will be true, so the button's state will be ON
        restartButton.setChecked(checked);
        restartButton.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if (MapsActivity.isRecording()) { // only pause/resume the service if a session is already recording
                if (isChecked) {
                    startService(new Intent(this, MyService.class)); // start the service if the toggle button's state is ON
                    MapsActivity.setServiceRunning(true);
                    checked = true;
                } else {
                    stopService(new Intent(this, MyService.class)); // stop the service if the toggle button's state is OFF
                    MapsActivity.setServiceRunning(false);
                    checked = false;
                }
            } else {
                restartButton.setChecked(false); // if the session is not recording then the toggle button's state will remain OFF
            }
        }));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
        } else {
            getCurrentLocation(); // if permissions are already granted by MapsActivity (or from before) just get the current location
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
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
        mMap.getUiSettings().setZoomControlsEnabled(true); // for better map navigation
        loadCurrentSession(); // load the current session when the google map is ready so it draws the circles saved in the latest session
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

    // puts a new marker in the current location of the user (if it changed more than 50m in the last 5") and deletes the old one
    private void getCurrentLocation() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = location -> {
            if (mMap != null) {
                if (currentMarker != null) {
                    currentMarker.remove();
                }
                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                currentMarker = mMap.addMarker(new MarkerOptions().position(currentLocation).title("Current Location"));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
            }
            showEntryExitPoints();
        };
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 50, locationListener);
        }
    }

    // gets the center points of the latest session (could be current) and displays a circle of 100m radius around them
    @SuppressLint("Range")
    private void loadCurrentSession() {
        ContentResolver contentResolver = this.getContentResolver();
        Uri uri = Uri.parse("content://gr.hua.dit.android.geofenceapp/session/latest");
        try (Cursor cursor = contentResolver.query(uri, new String[]{"center_points"},
                null, null, null)) {
            while (cursor.moveToNext()) {
                String key = cursor.getString(cursor.getColumnIndex("center_points"));
                ArrayList<LatLng> centerPoints = ArrayListConverter.fromString(key);
                if (centerPoints == null) {
                    break;
                }
                for (LatLng centerPoint : centerPoints) {
                    mMap.addCircle(new CircleOptions()
                            .center(centerPoint)
                            .radius(100)
                            .strokeColor(Color.BLUE)
                            .fillColor(Color.TRANSPARENT));
                }
            }
        }
    }

    // gets the entry and exit points of the latest session (could be current) and displays them on the map
    @SuppressLint("Range")
    private void showEntryExitPoints() {
        ContentResolver contentResolver = this.getContentResolver();
        Uri uri = Uri.parse("content://gr.hua.dit.android.geofenceapp/session/latest");
        try (Cursor cursor = contentResolver.query(uri, new String[]{"entry_points"},
                null, null, null)) {
            while (cursor.moveToNext()) {
                String key = cursor.getString(cursor.getColumnIndex("entry_points"));
                ArrayList<LatLng> points = ArrayListConverter.fromString(key);
                if (points != null) {
                    entryPoints = points;
                    for (LatLng entryPoint : entryPoints) {
                        mMap.addMarker(new MarkerOptions()
                                .position(entryPoint)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                                .title("Entry Point"));
                    }
                }
            }
        }
        try (Cursor cursor = contentResolver.query(uri, new String[]{"exit_points"},
                null, null, null)) {
            while (cursor.moveToNext()) {
                String key = cursor.getString(cursor.getColumnIndex("exit_points"));
                ArrayList<LatLng> points = ArrayListConverter.fromString(key);
                if (points != null) {
                    exitPoints = points;
                    for (LatLng exitPoint : exitPoints) {
                        mMap.addMarker(new MarkerOptions()
                                .position(exitPoint)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE))
                                .title("Exit Point"));
                    }
                }
            }
        }
    }

    public static boolean isChecked() {
        return checked;
    }

    public static void setChecked(boolean checked) {
        ResultsMapsActivity.checked = checked;
    }
}