package kousei.radiomap;

import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by kousei on 2015/12/15.
 */
public class RadioMapImage {
    private Bitmap mBmp;
    private int mDiv = 100;
    private int mWidth = 0;
    private int mHeight = 0;
    private int mOffset = 0;

    // ブラシリスト
    private int BrushListWhite;
    private int BrushListBlue;
    private int BrushListRed;
    private int[] BrushList = new int[257];
    private int mColorMax = 255;
    private double mMax = 150;
    private double mMin = 200;

    private LatLng mCenterLatLng;
    private double mAreaSize = 1000.0;
    private double mRange = 0.5;
    private double mStep = 0.000002;  //lat0.00001 赤道上での差が約1m

    //座標変換
    private double sLatitude = 0.004;
    private double sLongitude = 0.004492;
    private double divLat = 0;
    private double divLng = 0;
    private Point mCurrentPos = new Point(0, 0);

    private ArrayList[][] mAveList;
    private int[] mValues;

    private int[] mPixels;

    private DatabaseOpenHelper mDatabaseOpenHelper;
    private SQLiteDatabase mSdb;
    private Database mDb;

    public RadioMapImage(int size, int div, LatLng center, DatabaseOpenHelper databaseOpenHelper) {
        setBrushList();
        initialize(size, div, center);

        mDatabaseOpenHelper = databaseOpenHelper;
        try {
            mSdb = mDatabaseOpenHelper.getReadableDatabase();
            mDb = new Database(mSdb);
        } catch (Exception e) {
            Log.e("DB Error", e.getMessage().toString());
        }
    }

    public void initialize(int size, int div, LatLng center) {
        mCenterLatLng = center;
        mAreaSize = size;
        mDiv = div;
        mWidth = mDiv;
        mHeight = mDiv;
        mOffset = mDiv / 2;
        sLatitude = 0.004;
        sLongitude = 0.004492;

        mAveList = new ArrayList[mWidth][mHeight];
        mValues = new int[mWidth * mHeight];
        mPixels = new int[mWidth * mHeight];

        float[] results = new float[3];
        double distance = 0;

        double startLatitude;
        double startLongitude;
        double endLatitude;
        double endLongitude;

        double minDistance = mAreaSize - mRange;
        double maxDistance = mAreaSize + mRange;

        Arrays.fill(mValues, 0);
        Arrays.fill(mPixels, 0);

        for (int i = 0; i < mDiv; i++) {
            for (int j = 0; j < mDiv; j++) {
                mAveList[i][j] = new ArrayList<Integer>();
            }
        }

        //width
        while (distance < minDistance || maxDistance < distance) {
            startLatitude = center.latitude - sLatitude;
            startLongitude = center.longitude;
            endLatitude = center.latitude + sLatitude;
            endLongitude = center.longitude;

            try {
                Location.distanceBetween(
                        startLatitude,
                        startLongitude,
                        endLatitude,
                        endLongitude,
                        results
                );
                if (results != null && results.length > 0) {
                    distance = (double) results[0];
                }
            } catch (IllegalArgumentException e) {
                Log.e("distance between", e.getMessage());
                break;
            }


            if (distance < minDistance) {
                sLatitude = sLatitude + mStep;
            }
            if (maxDistance < distance) {
                sLatitude = sLatitude - mStep;
            }
        }

        distance = 0;
        //height
        while (distance < minDistance || maxDistance < distance) {
            startLatitude = center.latitude;
            startLongitude = center.longitude - sLongitude;
            endLatitude = center.latitude;
            endLongitude = center.longitude + sLongitude;

            try {
                Location.distanceBetween(
                        startLatitude,
                        startLongitude,
                        endLatitude,
                        endLongitude,
                        results
                );
                if (results != null && results.length > 0) {
                    distance = (double) results[0];
                }
            } catch (IllegalArgumentException e) {
                Log.e("distance between", e.getMessage());
                break;
            }

            if (distance < minDistance) {
                sLongitude = sLongitude + mStep;
            }
            if (maxDistance < distance) {
                sLongitude = sLongitude - mStep;
            }
        }

        divLat = sLatitude * 2 / mDiv;
        divLng = sLongitude * 2 / mDiv;

        onClearBitmap();

    }


    public Bitmap onCreateColorMap(int value, LatLng latLng, boolean isInserted, int cell) {
        Point p = new Point(onChangedLatLngToPixel(latLng));
        int ave;
        if ((0 <= p.x && p.x < mDiv) && (0 <= p.y && p.y < mDiv)) {

            if (isInserted) {
                mDb.insert(value, p, cell);
            }
            ave = onSavedValue(value, p);
            DrawPoint(ave, p);

        }
        return mBmp;
    }

    private int onSavedValue(int value, Point p) {
        mAveList[p.x][p.y].add(value);

        int avg = 0;
        List<Integer> list = mAveList[p.x][p.y];
        for (int n : list) {
            avg += n;
        }
        avg = avg / list.size();

        if (10 < list.size()) {
            mAveList[p.x][p.y].clear();
        }

        return avg;
    }

    private Point onChangedLatLngToPixel(LatLng latLng) {
        double lat = (latLng.latitude - mCenterLatLng.latitude) / divLat - mOffset;
        double lng = (latLng.longitude - mCenterLatLng.longitude) / divLng + mOffset;
        if (lat < 0) {
            lat = Math.ceil(lat);
        } else {
            lat = Math.floor(lat);
        }
        if (lng < 0) {
            lng = Math.ceil(lng);
        } else {
            lng = Math.floor(lng);
        }

        int x = (int) lng - mOffset;
        int y = (int) lat + mOffset;
        int offsetX = 0, offsetY = 0;
        if (y <= 0) offsetY = 1;
        if (0 <= x) offsetX = 1;
        mCurrentPos = new Point(x + offsetX, y - offsetY);
        return new Point((int) lng, (int) -lat);
    }

    private void DrawPoint(int value, Point p) {
        double a;
        if (value < mMin) {
            mMin = value;
        }
        if (mMax < value) {
            mMax = value;
        }

        a = mColorMax / (mMax - mMin);
        if (mMin == mMax) {
            a = mColorMax;
        }
        mValues[p.x + p.y * mWidth] = value;

        long start = System.nanoTime();
        int i, length;
        length = mWidth * mHeight;

        for (i = 0; i < length; i++) {
            switch (mValues[i]) {
                case 0:
                    mPixels[i] = Color.WHITE;
                    break;
                default:
                    mPixels[i] = BrushList[(int) (a * (mValues[i] - mMin))];
                    break;
            }
        }
        long end = System.nanoTime();
        long time = end - start;
        Log.d("for time", String.valueOf(time));
        mBmp.setPixels(mPixels, 0, mWidth, 0, 0, mWidth, mHeight);
    }

    public void onClearBitmap() {
        mBmp = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
    }


    public boolean hasCreatedBitmap() {
        if (mBmp != null) {
            return true;
        }
        return false;
    }

    public Bitmap getBitMap() {
        return mBmp;
    }

    public Bitmap getGridLineImage() {
        int imageSize = (int) mAreaSize;
        double divSize = imageSize / mDiv;
        Bitmap bmp = Bitmap.createBitmap(imageSize, imageSize, Bitmap.Config.RGB_565);

        int pixels[] = new int[imageSize * imageSize];

        for (double y = 0; y < imageSize; y++) {
            for (double x = 0; x < imageSize; x++) {
                pixels[(int) (x + y * imageSize)] = Color.WHITE;
                if ((x % divSize == 0 && x != 0) || (y % divSize == 0 && y != 0)) {
                    pixels[(int) (x + y * imageSize)] = Color.GRAY;
                }
            }

        }

        bmp.setPixels(pixels, 0, imageSize, 0, 0, imageSize, imageSize);

        return bmp;
    }

    public Point getCurrentPos() {
        return mCurrentPos;
    }

    private void setBrushList() {

        //ブラシの色を変えるときはここ
        for (int i = 0; i <= mColorMax; i++) {
            int[] t = HSVtoRGB(255 - i, 255, 255); //Hueは変化させるけど他はそのまま．255-iとしているのは青を最小値とするため
            BrushList[i] = Color.argb(255, t[0], t[1], t[2]); //argb 0～255の色のブラシを作る
        }
        BrushList[256] = Color.WHITE;
        BrushListWhite = BrushList.length - 1;
        BrushListBlue = 0;
        BrushListRed = BrushList.length - 2;
    }

    private int[] HSVtoRGB(int h, int s, int v) {
        double f;
        int i, p, q, t;
        int[] rgb = new int[3];

        i = (int) Math.floor(h / 60.0) % 6;
        f = (float) (h / 60.0) - (float) Math.floor(h / 60.0);
        p = (int) Math.round(v * (1.0 - (s / 255.0)));
        q = (int) Math.round(v * (1.0 - (s / 255.0) * f));
        t = (int) Math.round(v * (1.0 - (s / 255.0) * (1.0 - f)));

        switch (i) {
            case 0:
                rgb[0] = v;
                rgb[1] = t;
                rgb[2] = p;
                break;
            case 1:
                rgb[0] = q;
                rgb[1] = v;
                rgb[2] = p;
                break;
            case 2:
                rgb[0] = p;
                rgb[1] = v;
                rgb[2] = t;
                break;
            case 3:
                rgb[0] = p;
                rgb[1] = q;
                rgb[2] = v;
                break;
            case 4:
                rgb[0] = t;
                rgb[1] = p;
                rgb[2] = v;
                break;
            case 5:
                rgb[0] = v;
                rgb[1] = p;
                rgb[2] = q;
                break;
        }

        return rgb;
    }
}
