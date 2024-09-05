package gr.hua.dit.android.geofenceapp;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {Session.class}, exportSchema = false, version = 1)
@TypeConverters({ArrayListConverter.class}) // lets the database use the ArrayListConverter we created
public abstract class AppDatabase extends RoomDatabase { // creates a room database
    public abstract SessionDao sessionDao();
}
