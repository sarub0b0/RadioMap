package kousei.radiomap;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by kousei on 2015/12/11.
 */
public class DatabaseOpenHelper extends SQLiteOpenHelper {

    private static final String CREATE_TABLE_SQL_SS =
            "CREATE TABLE SignalStrength (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "RSRP INTEGER," +
                    "Lat TEXT," +
                    "Lng TEXT," +
                    "Cell_ID INTEGER" +
                    ");";

    private static final String DROP_TABLE_SQL_SS = "DROP TABLE IF EXISTS SignalStrength;";
    private static final String CHECK_TABLE_SQL_SS = "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='SignalStrength';";

    private static final String CREATE_TABLE_SQL_MAP =
            "CREATE TABLE Map (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "RSRP INTEGER," +
                    "X INTEGER," +
                    "Y INTEGER," +
                    "Cell_ID INTEGER" +
                    ");";

    private static final String DROP_TABLE_SQL_MAP = "DROP TABLE IF EXISTS Map;";
    private static final String CHECK_TABLE_SQL_MAP = "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='Map';";
    public DatabaseOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SQL_SS);
        db.execSQL(CREATE_TABLE_SQL_MAP);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_TABLE_SQL_SS);
        db.execSQL(CREATE_TABLE_SQL_SS);
        db.execSQL(DROP_TABLE_SQL_MAP);
        db.execSQL(CREATE_TABLE_SQL_MAP);
    }

    public void onDrop(SQLiteDatabase db) {
        db.execSQL(DROP_TABLE_SQL_SS);
        db.execSQL(DROP_TABLE_SQL_MAP);
        if(hasTable(db)){
            Log.d("database","Droped table");
        }
    }

    public boolean hasTable(SQLiteDatabase db) {
        Cursor c = db.rawQuery(CHECK_TABLE_SQL_SS, null);
        c.moveToFirst();
        String ss = c.getString(0);

        c = db.rawQuery(CHECK_TABLE_SQL_MAP,null);
        c.moveToFirst();

        String map = c.getString(0);
        if(ss.equals("0") && map.equals("0")){
            return true;
        }else{
            return false;
        }
    }
}
