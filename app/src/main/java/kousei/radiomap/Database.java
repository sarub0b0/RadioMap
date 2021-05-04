package kousei.radiomap;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Point;
import android.util.Log;


import com.google.android.gms.maps.model.LatLng;

/**
 * Created by kousei on 2015/12/11.
 */
public class Database {

    private static final String TABLE_NAME = "SignalStrength";
    private static final String COLUMN_RSRP = "RSRP";
    private static final String COLUMN_LAT = "Lat";
    private static final String COLUMN_LNG = "Lng";
    private static final String COLUMN_CELLID = "Cell_ID";

    private static final String COLUMN_X = "X";
    private static final String COLUMN_Y = "Y";

    private SQLiteDatabase db;

    public Database(SQLiteDatabase db) {
        this.db = db;
    }

    //データ登録
    public void insert(int value, LatLng latLng, int cell) {
        ContentValues values = new ContentValues();
        String lat = String.valueOf(latLng.latitude);
        String lng = String.valueOf(latLng.longitude);
        values.put(COLUMN_RSRP, value);
        values.put(COLUMN_LAT, lat);
        values.put(COLUMN_LNG, lng);
        values.put(COLUMN_CELLID, cell);
        try {
            db.insert(TABLE_NAME, null, values);
        } catch (Exception e) {
            Log.e("database", e.getMessage());
        }
    }

    public void insert(int value, Point p, int cell) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_X, p.x);
        values.put(COLUMN_Y, p.y);
        values.put(COLUMN_RSRP, value);
        values.put(COLUMN_CELLID, cell);
        try {
            db.insert("Map", null, values);
        } catch (Exception e) {
            Log.e("database", e.getMessage());
        }
    }
}
