package kousei.radiomap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.telephony.CellIdentityLte;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.w3c.dom.Text;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.List;

public class MapsActivity extends FragmentActivity
        implements
        OnMapReadyCallback,
        LocationListener,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks {

    //google mMap
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    private static final int sRequestLocationSet = 123;
    private static final int sLocationRequestInterval = 5000;

    //database
    private DatabaseOpenHelper mDatabaseOpenHelper;
    private SQLiteDatabase mSdb;
    private Database mDb;
    private boolean isOpenedSQLiteDb;
    private boolean isInserted;
    private String mDbName;

    //signal strength
    private TelephonyManager mTelephonyManager = null;
    private int mRsrp;
    private int mMaxRsrp = -1;
    private int mMinRsrp = 200;
    private int mCellID;

    //Radio map
    private static final int sDefalutCameraZoom = 17;
    private CameraUpdate mCameraUpdate;
    private LatLng mCurrentLatLng;
    private LatLng mCenterLatLng = new LatLng(34.482254, 136.824878);

    private RadioMapImage mRadioMapImage;
    private Marker mMarker;
    private GroundOverlay overlayMapImage;
    private GroundOverlay overlayGridImage;
    private int mMapDiv;
    private int mMapSize;

    //layout
    private TextView mLteSsText, mLteLevelText;
    private TextView mRsrpText;
    private TextView mRsrqText;
    private TextView mRssnrText;
    private TextView mCqiText;
    private TextView mGsmSsText, mGsmLevelText, mGsmDBmText;
    private TextView mCdmaLevelText, mCdmaDBmText, mCdmaEcioText;
    private TextView mEvdoLevelText, mEvdoDBmText, mEvdoEcioText, mEvdoSnrText;
    private TextView mCurrentLatText, mCurrentLngText;
    private TextView mCenterLatText, mCenterLngText;
    private TextView mMaxRsrpText, mMinRsrpText;
    private TextView mPosText;
    private TextView mCellIdText, mBandText;

    private LinearLayout mOtherLayout;
    private Button mShowStateButton;
    private Button mDbStateButton;
    private Button mDbDropButton;
    private EditText mDbNameText;
    private EditText mMapDivText;
    private EditText mMapSizeText;

    private boolean isVisibled = false;

    private static final String[] sSignalStrengthLevel = {
            "none", "poor", "moderate", "good", "great"
    };

    public TelephonyManager getTelephonyManager() {
        if (mTelephonyManager == null) {
            mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        }
        return mTelephonyManager;
    }

    private PhoneStateListener mPhoneStateLister = new PhoneStateListener() {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            Method[] mMethod = SignalStrength.class.getMethods();
            mGsmSsText.setText(String.valueOf(signalStrength.getGsmSignalStrength()));
            mCdmaDBmText.setText(String.valueOf(signalStrength.getCdmaDbm()));
            mCdmaEcioText.setText(String.valueOf(signalStrength.getCdmaEcio()));
            mEvdoDBmText.setText(String.valueOf(signalStrength.getEvdoDbm()));
            mEvdoEcioText.setText(String.valueOf(signalStrength.getEvdoEcio()));
            mEvdoSnrText.setText(String.valueOf(signalStrength.getEvdoSnr()));

            try {
                int index;
                for (Method mthd : mMethod) {
                    String mthdName = mthd.getName();
                    switch (mthdName) {
                        case "getLteSignalStrength":
                            mLteSsText.setText(mthd.invoke(signalStrength).toString());
                            break;
                        case "getLteLevel":
                            index = (int) mthd.invoke(signalStrength);
                            mLteLevelText.setText(sSignalStrengthLevel[index]);
                            break;
                        case "getGsmLevel":
                            index = (int) mthd.invoke(signalStrength);
                            mGsmLevelText.setText(sSignalStrengthLevel[index]);
                            break;
                        case "getCdmaLevel":
                            index = (int) mthd.invoke(signalStrength);
                            mCdmaLevelText.setText(sSignalStrengthLevel[index]);
                            break;
                        case "getEvdoLevel":
                            index = (int) mthd.invoke(signalStrength);
                            mEvdoLevelText.setText(sSignalStrengthLevel[index]);
                            break;
                        case "getLteRsrp":
                            mRsrp = (int) mthd.invoke(signalStrength);
                            mRsrpText.setText(String.valueOf(mRsrp));
                            break;
                        case "getLteRsrq":
                            mRsrqText.setText(mthd.invoke(signalStrength).toString());
                            break;
                        case "getLteRssnr":
                            mRssnrText.setText(mthd.invoke(signalStrength).toString());
                            break;
                        case "getLteCqi":
                            mCqiText.setText(mthd.invoke(signalStrength).toString());
                            break;
                        case "getGsmDbm":
                            mGsmDBmText.setText(mthd.invoke(signalStrength).toString());
                            break;
                        default:
                            break;
                    }
                }
            } catch (Exception e) {
                Log.e("getPhoneState error", e.getMessage());
            }

            long time = System.currentTimeMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss SSS");
            Log.d("Update", "RSRP " + mRsrp + " " + sdf.format(time));

            checkMaxMin(mRsrp + 200);

            onCellInfoChanged(mTelephonyManager.getAllCellInfo());
        }

        @Override
        public void onCellInfoChanged(List<CellInfo> cellInfoList) {
            if (cellInfoList != null) {
                for (CellInfo cellInfo : cellInfoList) {
                    if (cellInfo instanceof CellInfoLte) {
                        CellInfoLte cellInfoLte = (CellInfoLte) cellInfo;
                        CellIdentityLte cellIdentityLte = cellInfoLte.getCellIdentity();
                        int ci = cellIdentityLte.getCi();

                        if (ci != Integer.MAX_VALUE) {
                            String s = cellIdentityLte.toString();
                            Log.d("Cell info", s);
                            mCellIdText.setText(String.valueOf(ci));
                            mCellID = ci;
                        }
                    }
                }
            }
        }
    };

    private LocationRequest createLocationRequest() {
        mLocationRequest = new LocationRequest()
                .setInterval(sLocationRequestInterval)
                .setFastestInterval(16)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        return mLocationRequest;
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        try {
            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        } catch (Exception e) {
            Log.e("Google MAp", e.getMessage().toString());
        }

        //GoogleApiClient connect
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(AppIndex.API).build();

        mGoogleApiClient.connect();
        checkLocationPreference();

        //電界強度取得
        getTelephonyManager().listen(mPhoneStateLister, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        //getTelephonyManager().listen(mPhoneStateLister, PhoneStateListener.LISTEN_CELL_INFO);

        //layout
        getLayout();

        //SQLite Open
        setDatabase();

    }

    private void checkLocationPreference() {
        // 1. ユーザが必要な位置情報設定を満たしているか確認する
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(
                mGoogleApiClient,
                new LocationSettingsRequest.Builder().addLocationRequest(createLocationRequest()).build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // 位置情報が利用できる
                        // FusedLocationApi.requestLocationUpdatesなどを呼び出す
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            // 2. ユーザに位置情報設定を変更してもらうためのダイアログを表示する
                            status.startResolutionForResult(MapsActivity.this, sRequestLocationSet);
                        } catch (IntentSender.SendIntentException e) {
                        }
                        break;

                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // 位置情報が取得できず、なおかつその状態からの復帰も難しい時呼ばれるらしい
                        break;
                }
            }
        });
    }

    private void setDatabase() {
        try {
            mDatabaseOpenHelper = new DatabaseOpenHelper(
                    this,
                    Environment.getExternalStorageDirectory() + "/" + mDbName + ".db",
                    null,
                    1
            );
            mSdb = mDatabaseOpenHelper.getReadableDatabase();
            mDb = new Database(mSdb);
            isOpenedSQLiteDb = true;
            isInserted = false;
        } catch (Exception e) {
            Log.e("DB Error", e.getMessage().toString());
        }

        mRadioMapImage = new RadioMapImage(mMapSize, mMapDiv, mCenterLatLng, mDatabaseOpenHelper);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == sRequestLocationSet) {
            // ユーザのダイアログに対する応答をここで確認できる
            switch (resultCode) {
                case Activity.RESULT_OK:
                    break;
                case Activity.RESULT_CANCELED:
                    break;
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //現在位置ボタンの表示
        if (mMap != null) {
            mMap.setMyLocationEnabled(true);
        }
        mCameraUpdate = CameraUpdateFactory.zoomTo(sDefalutCameraZoom);
        mMap.moveCamera(mCameraUpdate);
        setCenterMarker();
        gridImageOverlay(mRadioMapImage.getGridLineImage());
    }

    @Override
    public void onConnected(Bundle bundle) {
        //GoogleApiClient 接続成功
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        //GoogleApiClient　接続中断
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //GoogleApiClient　接続失敗
    }

    private void getLayout() {
        mLteSsText = (TextView) findViewById(R.id.ltess);
        mLteLevelText = (TextView) findViewById(R.id.lteLevel);
        mRsrpText = (TextView) findViewById(R.id.rsrp);
        mRsrqText = (TextView) findViewById(R.id.rsrq);
        mRssnrText = (TextView) findViewById(R.id.rssnr);
        mCqiText = (TextView) findViewById(R.id.cqi);
        mGsmSsText = (TextView) findViewById(R.id.gsmss);
        mGsmLevelText = (TextView) findViewById(R.id.gsmLebel);
        mGsmDBmText = (TextView) findViewById(R.id.gsmDBm);
        mCdmaLevelText = (TextView) findViewById(R.id.cdmaLevel);
        mCdmaDBmText = (TextView) findViewById(R.id.cdmaDBm);
        mCdmaEcioText = (TextView) findViewById(R.id.cdmaEcio);
        mEvdoLevelText = (TextView) findViewById(R.id.evdoLevel);
        mEvdoDBmText = (TextView) findViewById(R.id.evdoDBm);
        mEvdoEcioText = (TextView) findViewById(R.id.evdoEcio);
        mEvdoSnrText = (TextView) findViewById(R.id.evdoSnr);
        mCurrentLatText = (TextView) findViewById(R.id.currentLatitude);
        mCurrentLngText = (TextView) findViewById(R.id.currentLongitude);
        mCenterLatText = (TextView) findViewById(R.id.centerLatitude);
        mCenterLngText = (TextView) findViewById(R.id.centerLongitude);
        mMaxRsrpText = (TextView) findViewById(R.id.maxRsrp);
        mMinRsrpText = (TextView) findViewById(R.id.minRsrp);
        mPosText = (TextView) findViewById(R.id.CurrentPos);
        mCellIdText = (TextView) findViewById(R.id.CellId);
        mBandText = (TextView) findViewById(R.id.Band);

        mOtherLayout = (LinearLayout) findViewById(R.id.other);

        visibilityTextView(true);

        onChangedStateValue();
    }

    private void onChangedStateValue() {
        mShowStateButton = (Button) findViewById(R.id.showStateButton);
        mDbStateButton = (Button) findViewById(R.id.dbStateButton);
        mDbDropButton = (Button) findViewById(R.id.dropButton);
        mMapDivText = (EditText) findViewById(R.id.mapDivText);
        mDbNameText = (EditText) findViewById(R.id.dbNameText);
        mMapSizeText = (EditText) findViewById(R.id.mapSizeText);

        mDbName = mDbNameText.getText().toString();
        mMapDiv = Integer.parseInt(mMapDivText.getText().toString());
        mMapSize = Integer.parseInt(mMapSizeText.getText().toString());

        mShowStateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button button = (Button) v.findViewById(R.id.showStateButton);
                String state = button.getText().toString();
                switch (state) {
                    case "OFF":
                        isVisibled = false;
                        mShowStateButton.setText("ON");
                        break;
                    case "ON":
                        isVisibled = true;
                        mShowStateButton.setText("OFF");
                        break;
                }
                visibilityTextView(isVisibled);
            }

        });

        //DB開始・停止・削除
        mDbStateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button button = (Button) v.findViewById(R.id.dbStateButton);
                String state = button.getText().toString();

                switch (state) {
                    case "START":
                        if (mDatabaseOpenHelper.hasTable(mSdb)) {
                            mDatabaseOpenHelper.onCreate(mSdb);
                        }
                        button.setText("STOP");

                        isInserted = true;
                        Log.d("database", "Started Insert");
                        break;
                    case "STOP":
                        button.setText("START");
                        isInserted = false;
                        Log.d("database", "Stoped Insert");
                        break;
                }
            }

        });

        mDbDropButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((!isInserted) && isOpenedSQLiteDb) {
                    mDatabaseOpenHelper.onDrop(mSdb);
                }
            }
        });

        mDbNameText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    if (event.getAction() == KeyEvent.ACTION_UP) {
                        Log.d("onEditorAction", "check");
                        // ソフトキーボードを隠す
                        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
                        mDbName = mDbNameText.getText().toString();
                        setDatabase();
                    }
                    return true;
                }
                return false;
            }
        });

        mMapDivText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    if (event.getAction() == KeyEvent.ACTION_UP) {
                        Log.d("onEditorAction", "check");
                        // ソフトキーボードを隠す
                        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
                        mMapDiv = Integer.parseInt(mMapDivText.getText().toString());
                        if (mMapDiv != 0) {
                            mRadioMapImage.initialize(mMapSize, mMapDiv, mCenterLatLng);
                            gridImageOverlay(mRadioMapImage.getGridLineImage());
                        }
                    }
                    return true;
                }
                return false;
            }
        });

        mMapSizeText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    if (event.getAction() == KeyEvent.ACTION_UP) {
                        Log.d("onEditorAction", "check");
                        // ソフトキーボードを隠す
                        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
                        mMapSize = Integer.parseInt(mMapSizeText.getText().toString());
                        if (mMapSize != 0) {
                            mRadioMapImage.initialize(mMapSize, mMapDiv, mCenterLatLng);
                            gridImageOverlay(mRadioMapImage.getGridLineImage());
                        }
                    }
                    return true;
                }
                return false;
            }
        });
    }

    private void checkMaxMin(int value) {
        if (mMaxRsrp < value) {
            mMaxRsrp = value;
        }
        if (value < mMinRsrp) {
            mMinRsrp = value;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        mCameraUpdate = CameraUpdateFactory.newLatLng(mCurrentLatLng);
        mMap.moveCamera(mCameraUpdate);
        mCurrentLatText.setText(String.valueOf(mCurrentLatLng.latitude));
        mCurrentLngText.setText(String.valueOf(mCurrentLatLng.longitude));

        long time = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss SSS");
        Log.d("Update", "lat|lng " + mCurrentLatLng.latitude + "|" + mCurrentLatLng.longitude + " " + sdf.format(time));

        insertData();


        if (mRadioMapImage.hasCreatedBitmap()) {
            radioMapOverlay(
                    mRadioMapImage.onCreateColorMap(mRsrp + 200, mCurrentLatLng, isInserted, mCellID)
            );
            mPosText.setText(mRadioMapImage.getCurrentPos().toString());
        }
        mMaxRsrpText.setText(String.valueOf(mMaxRsrp));
        mMinRsrpText.setText(String.valueOf(mMinRsrp));

    }

    private void visibilityTextView(boolean flag) {
        if (flag) {
            mOtherLayout.setVisibility(View.GONE);
        } else {
            mOtherLayout.setVisibility(View.VISIBLE);
        }
    }

    private void insertData() {
        if (isOpenedSQLiteDb && isInserted)
            mDb.insert(mRsrp, mCurrentLatLng, mCellID);
    }

    public void setCenterMarker() {
        if (mMap != null) {
            mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {
                    if (mMarker == null) {
                        MarkerOptions options = new MarkerOptions();
                        options.position(latLng);
                        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                        options.draggable(true);
                        mMarker = mMap.addMarker(options);

                        mCenterLatLng = latLng;
                        mCenterLatText.setText(String.valueOf(latLng.latitude));
                        mCenterLngText.setText(String.valueOf(latLng.longitude));

                        mRadioMapImage.initialize(mMapSize, mMapDiv, mCenterLatLng);
                        gridImageOverlay(mRadioMapImage.getGridLineImage());
                        radioMapOverlay(mRadioMapImage.getBitMap());
                    }
                }
            });

            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    if (mMarker != null) {
                        mMarker.remove();
                        mMarker = null;
                    }
                    return false;
                }
            });

            mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                @Override
                public void onMarkerDragStart(Marker marker) {
                }

                @Override
                public void onMarkerDrag(Marker marker) {
                }

                @Override
                public void onMarkerDragEnd(Marker marker) {
                    Toast.makeText(MapsActivity.this, marker.getPosition().toString(), Toast.LENGTH_SHORT).show();
                    mCenterLatLng = marker.getPosition();
                    mCenterLatText.setText(String.valueOf(mCenterLatLng.latitude));
                    mCenterLngText.setText(String.valueOf(mCenterLatLng.longitude));
                    mRadioMapImage.onClearBitmap();
                }
            });
        }
    }

    private void radioMapOverlay(Bitmap bmp) {

        //画像および位置情報設定
        GroundOverlayOptions options = new GroundOverlayOptions();
        BitmapDescriptor bitmap = BitmapDescriptorFactory.fromBitmap(bmp);
        options.image(bitmap);
        options.anchor(0.5f, 0.5f);
        options.position(mCenterLatLng, mMapSize, mMapSize);

        //マップに画像をオーバーレイ
        if (overlayMapImage != null) {
            overlayMapImage.remove();
        }
        overlayMapImage = mMap.addGroundOverlay(options);
        overlayMapImage.setTransparency(0.7f);

    }

    private void gridImageOverlay(Bitmap bmp) {
        GroundOverlayOptions options = new GroundOverlayOptions();
        BitmapDescriptor bitmap = BitmapDescriptorFactory.fromBitmap(bmp);
        options.image(bitmap);
        options.anchor(0.5f, 0.5f);
        options.position(mCenterLatLng, mMapSize, mMapSize);

        if (overlayGridImage != null) {
            overlayGridImage.remove();
        }

        overlayGridImage = mMap.addGroundOverlay(options);
        overlayGridImage.setTransparency(0.7f);
    }

    public int getMaxRsrp() {
        return mMaxRsrp;
    }

    public int getMinRsrp() {
        return mMinRsrp;
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        mGoogleApiClient.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Maps Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://kousei.radiomap/http/host/path")
        );
        AppIndex.AppIndexApi.start(mGoogleApiClient, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Maps Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://kousei.radiomap/http/host/path")
        );
        AppIndex.AppIndexApi.end(mGoogleApiClient, viewAction);
        mGoogleApiClient.disconnect();
    }

}

