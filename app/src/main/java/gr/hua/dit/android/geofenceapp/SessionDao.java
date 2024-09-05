package gr.hua.dit.android.geofenceapp;

import android.database.Cursor;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface SessionDao {

    @Query("SELECT * FROM session")
    Cursor getAll();

    @Query("SELECT * FROM session WHERE id = :id")
    Session findSessionById(int id);

    @Query("SELECT * FROM session WHERE id = :id")
    Cursor findSessionByIdCursor(int id);

    @Query("SELECT * FROM session WHERE id = (SELECT MAX(id) FROM session)")
    Session findLatestSession();

    @Query("SELECT * FROM session WHERE id = (SELECT MAX(id) FROM session)")
    Cursor findLatestSessionCursor();

    @Query("SELECT center_points FROM session WHERE id = :id")
    Cursor findCenterPointsById(int id);

    @Query("SELECT entry_points FROM session WHERE id = :id")
    Cursor findEntryPointsById(int id);

    @Query("SELECT exit_points FROM session WHERE id = :id")
    Cursor findExitPointsById(int id);

    @Insert
    void insertSession(Session session);

    @Update
    void updateSession(Session session);
}
