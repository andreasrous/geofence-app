package gr.hua.dit.android.geofenceapp;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

@Entity
public class Session { // represents a table inside the db called "session"

    @PrimaryKey(autoGenerate = true) // the id auto increments every time we insert a new record
    private int id;

    @ColumnInfo(name = "entry_points")
    private ArrayList<LatLng> entryPoints; // with the help of our ArrayListConverter this column will be saved as a string inside the db

    @ColumnInfo(name = "exit_points")
    private ArrayList<LatLng> exitPoints;

    @ColumnInfo(name = "center_points")
    private ArrayList<LatLng> centerPoints;

    public Session() {
    }

    @Ignore
    public Session(ArrayList<LatLng> entryPoints, ArrayList<LatLng> exitPoints, ArrayList<LatLng> centerPoints) {
        this.entryPoints = entryPoints;
        this.exitPoints = exitPoints;
        this.centerPoints = centerPoints;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ArrayList<LatLng> getEntryPoints() {
        return entryPoints;
    }

    public void setEntryPoints(ArrayList<LatLng> entryPoints) {
        this.entryPoints = entryPoints;
    }

    public ArrayList<LatLng> getExitPoints() {
        return exitPoints;
    }

    public void setExitPoints(ArrayList<LatLng> exitPoints) {
        this.exitPoints = exitPoints;
    }

    public ArrayList<LatLng> getCenterPoints() {
        return centerPoints;
    }

    public void setCenterPoints(ArrayList<LatLng> centerPoints) {
        this.centerPoints = centerPoints;
    }

    @NonNull
    @Override
    public String toString() {
        return "Session{" +
                "id=" + id +
                ", entryPoints=" + entryPoints +
                ", exitPoints=" + exitPoints +
                ", centerPoints=" + centerPoints +
                '}';
    }
}
