package gr.hua.dit.android.geofenceapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.util.Log;

public class MyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (MapsActivity.isServiceRunning()) {
            if (isGpsEnabled) { // if the service should be running and the gps just got enabled
                context.startService(new Intent().setClass(context, MyService.class)); // start the service (if it's already running it won't do anything)
                Log.d("Broadcast Receiver onReceive", "Service started");
            }
            else { // else if the gps just got disabled
                context.stopService(new Intent().setClass(context, MyService.class)); // stop the service till its back on
                Log.d("Broadcast Receiver onReceive", "Service stopped");
            }
        }
    }
}