package gr.hua.dit.android.geofenceapp;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class MyService extends Service {

    private LocationManager locationManager;
    private LocationListener locationListener;
    private ArrayList<LatLng> centerPoints;
    private ArrayList<LatLng> entryPoints;
    private ArrayList<LatLng> exitPoints;
    private LatLng currentLocation; // stores the current location of the user
    private LatLng previousLocation; // stores the previous location of the user

    public MyService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        centerPoints = new ArrayList<>();
        entryPoints = new ArrayList<>();
        exitPoints = new ArrayList<>();

        locationListener = location -> {
            loadCurrentSession(); // get the center, entry and exit points for every location update
            previousLocation = currentLocation;
            currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
            for (LatLng centerPoint : centerPoints) {
                if (previousLocation != null) { // if it's not the first location update
                    if (HaversineDistanceCalculator.isPointInsideCircle(previousLocation, centerPoint)
                            && !HaversineDistanceCalculator.isPointInsideCircle(currentLocation, centerPoint)) {
                        exitPoints.add(currentLocation); // exit point if the user was inside a circle before but now isn't
                    }
                    else if (!HaversineDistanceCalculator.isPointInsideCircle(previousLocation, centerPoint)
                            && HaversineDistanceCalculator.isPointInsideCircle(currentLocation, centerPoint)) {
                        entryPoints.add(currentLocation); // entry point if the user wasn't inside a previous circle but now is
                    }
                }
            }
            save(); // save the new changes for every location update (so they don't get lost if the user decides to bounce)
        };

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // records the device's current location if it changed more than 50m in the last 5 seconds.
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 50, locationListener);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // gets the center, entry and exit points of the latest session (could be current)
    @SuppressLint("Range")
    private void loadCurrentSession() {
        if (MapsActivity.isRecording()) {
            ContentResolver contentResolver = this.getContentResolver();
            Uri uri = Uri.parse("content://gr.hua.dit.android.geofenceapp/session/latest");
            try (Cursor cursor = contentResolver.query(uri, new String[]{"center_points"},
                    null, null, null)) {
                while (cursor.moveToNext()) {
                    String key = cursor.getString(cursor.getColumnIndex("center_points"));
                    ArrayList<LatLng> points = ArrayListConverter.fromString(key);
                    if (points != null) {
                        centerPoints = points;
                    }
                }
            }
            try (Cursor cursor = contentResolver.query(uri, new String[]{"entry_points"},
                    null, null, null)) {
                while (cursor.moveToNext()) {
                    String key = cursor.getString(cursor.getColumnIndex("entry_points"));
                    ArrayList<LatLng> points = ArrayListConverter.fromString(key);
                    if (points != null) {
                        entryPoints = points;
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
                    }
                }
            }
        }
    }

    // saves the current entry/exit points to the db (also saves the center points so they don't get erased)
    private void save() {
        ContentResolver contentResolver = this.getContentResolver();
        Uri uri = Uri.parse("content://gr.hua.dit.android.geofenceapp/session/update");
        String jsonCenterPoints = ArrayListConverter.fromArrayList(centerPoints);
        String jsonEntryPoints = ArrayListConverter.fromArrayList(entryPoints);
        String jsonExitPoints = ArrayListConverter.fromArrayList(exitPoints);
        ContentValues contentValues = new ContentValues();

        // the center points will always by the same, but without this line they would get erased in the next update statement and turned to null
        contentValues.put("center_points", jsonCenterPoints);
        contentValues.put("entry_points", jsonEntryPoints);
        contentValues.put("exit_points", jsonExitPoints);

        // update the latest session with the new entry/exit points (while keeping the center points intact)
        contentResolver.update(uri, contentValues, null, null);
    }
}