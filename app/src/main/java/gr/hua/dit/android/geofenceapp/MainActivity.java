package gr.hua.dit.android.geofenceapp;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BroadcastReceiver broadcastReceiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);
        registerReceiver(broadcastReceiver, intentFilter);

        findViewById(R.id.addAreasButton).setOnClickListener(v -> { // "Add Areas" button
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), MapsActivity.class);
            startActivity(intent); // sends user to the MapsActivity
        });

        // "End Recording" button - resets static variables and stops the service
        findViewById(R.id.endRecordingButton).setOnClickListener(v -> {
            if (MapsActivity.isRecording()) { // should only work if the session is actually recording
                MapsActivity.setRecording(false);
                MapsActivity.setServiceRunning(false);
                stopService(new Intent(this, MyService.class)); // stops the service
                ResultsMapsActivity.setChecked(false);
            }
        });

        findViewById(R.id.showEntryPointsButton).setOnClickListener(v -> { // "Show Entry/Exit Points" button
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), ResultsMapsActivity.class);
            startActivity(intent); // sends user to ResultsMapsActivity
        });
    }
}