package gr.hua.dit.android.geofenceapp;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Room;

import java.util.Objects;

public class DbProvider extends ContentProvider {

    private static final String AUTHORITY = "gr.hua.dit.android.geofenceapp";

    private UriMatcher uriMatcher;
    private AppDatabase appDatabase;
    private SessionDao sessionDao;

    public DbProvider() {
    }

    @Override
    public boolean onCreate() {
        appDatabase = Room.databaseBuilder(getContext(), AppDatabase.class, "mydb").allowMainThreadQueries().build(); // create the db
        sessionDao = appDatabase.sessionDao(); // create a new dao
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, "session/#", 1);       // get a specific session
        uriMatcher.addURI(AUTHORITY, "session/latest", 2);  // get the latest session
        uriMatcher.addURI(AUTHORITY, "session/insert", 3);  // insert a new session record
        uriMatcher.addURI(AUTHORITY, "session/update", 4);  // update an existing session
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        Cursor cursor = null;
        int id;
        if (uriMatcher.match(uri) == 1) {
            id = Integer.parseInt(Objects.requireNonNull(uri.getLastPathSegment())); // get the session id that's written in the uri
        }
        else if (uriMatcher.match(uri) == 2) {
            id = sessionDao.findLatestSession().getId(); // get the latest session id (could also be current)
        }
        else {
            return null; // return if the query does not involve any of the first two uris
        }

        if (projection == null) {
            return sessionDao.findSessionByIdCursor(id); // if there are no specific columns defined just get everything
        }

        // get the specific column defined in the projection list and return is as a cursor
        switch (projection[0]) {
            case "entry_points":
                cursor = sessionDao.findEntryPointsById(id);
                break;
            case "exit_points":
                cursor = sessionDao.findExitPointsById(id);
                break;
            case "center_points":
                cursor = sessionDao.findCenterPointsById(id);
        }

        return cursor;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        if (uriMatcher.match(uri) == 3) {
            Session session = new Session();
            if (values != null) { // get every value from the values list and create a session object
                session.setEntryPoints(ArrayListConverter.fromString(String.valueOf(values.get("entry_points"))));
                session.setExitPoints(ArrayListConverter.fromString(String.valueOf(values.get("exit_points"))));
                session.setCenterPoints(ArrayListConverter.fromString(String.valueOf(values.get("center_points"))));
            }
            sessionDao.insertSession(session); // create a brand new session record using the values provided (auto increments session id)
            int id = sessionDao.findLatestSession().getId();
            return Uri.parse("content://" + AUTHORITY + "/session/" + id); // return the uri of the new session for confirmation
        }
        else {
            return null;
        }
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        if (uriMatcher.match(uri) == 4) {
            // if there are no selection args get the latest session id, else get the one defined in the selectionArgs list
            int id = selectionArgs == null ? sessionDao.findLatestSession().getId() : Integer.parseInt(selectionArgs[0]);
            Session session = sessionDao.findSessionById(id);
            if (values != null) {
                session.setEntryPoints(ArrayListConverter.fromString(String.valueOf(values.get("entry_points"))));
                session.setExitPoints(ArrayListConverter.fromString(String.valueOf(values.get("exit_points"))));
                session.setCenterPoints(ArrayListConverter.fromString(String.valueOf(values.get("center_points"))));
            }
            sessionDao.updateSession(session); // update the session with the new values
            return 1;
        }
        else {
            return 0;
        }
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }
}