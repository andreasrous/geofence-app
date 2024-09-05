package gr.hua.dit.android.geofenceapp;

import androidx.room.TypeConverter;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

// allows us to use ArrayList<LatLng> as a column type in the database by converting the column to and from JSON
public class ArrayListConverter {

    @TypeConverter // converts JSON to ArrayList<LatLng>
    public static ArrayList<LatLng> fromString(String value) {
        Type listType = new TypeToken<ArrayList<LatLng>>() {
        }.getType();
        return new Gson().fromJson(value, listType);
    }

    @TypeConverter // converts ArrayList<LatLng> to JSON
    public static String fromArrayList(ArrayList<LatLng> list) {
        Gson gson = new Gson();
        return gson.toJson(list);
    }
}
