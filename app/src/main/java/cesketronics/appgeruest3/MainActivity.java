package cesketronics.appgeruest3;

import java.lang.String;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import me.itangqi.waveloadingview.WaveLoadingView;
import pl.pawelkleczkowski.customgauge.CustomGauge;
import android.graphics.Color;
import android.app.Notification;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;


public class MainActivity extends Activity {
    private static final String TAG = "bluetooth1";

    static TextView voltageView, currentView;

    static WaveLoadingView mWaveLoadingView;
    static ArrayList<btData> btDataList;
    BluetoothHelper btHelperService;
    boolean mIsBound = false;

    private static CustomGauge gaugeVoltage;
    private static CustomGauge gaugeCurrent;
    double calculatingFactorVoltage = 1;
    double calculatingFactorCurrent = 1;

    Thread threadUpdateUi;
    boolean stopthreadUpdateUi = false;

    static int maxEnergy = 0;
    int oPercent=0;
    int nPercent=0;
    int percentage = 0;

    boolean UISetFirstTime = true;
    static boolean percentageDifference = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        voltageView = (TextView) findViewById(R.id.voltage_tv);
        currentView = (TextView) findViewById(R.id.current_tv);
        mWaveLoadingView = (WaveLoadingView) findViewById(R.id.waveLoadingView);

        mWaveLoadingView.setShapeType(WaveLoadingView.ShapeType.CIRCLE);
        mWaveLoadingView.setCenterTitleColor(Color.DKGRAY);
        mWaveLoadingView.setCenterTitleStrokeColor(Color.LTGRAY);
        mWaveLoadingView.setCenterTitleStrokeWidth(2);
        mWaveLoadingView.setProgressValue(20);
        mWaveLoadingView.setBorderWidth(10);
        mWaveLoadingView.setAmplitudeRatio(20);
        mWaveLoadingView.setWaveColor(Color.argb(255,255,0,0));
        mWaveLoadingView.setBorderColor(Color.DKGRAY);
        mWaveLoadingView.setCenterTitle("No BT-Device connected");
        mWaveLoadingView.setAnimDuration(5000);
        mWaveLoadingView.pauseAnimation();
        mWaveLoadingView.resumeAnimation();
        mWaveLoadingView.cancelAnimation();
        mWaveLoadingView.startAnimation();

        gaugeVoltage = findViewById(R.id.gaugeVoltage);
        gaugeCurrent = findViewById(R.id.gaugeCurrent);



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_mainactivity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Wir prüfen, ob Menü-Element mit der ID "action_daten_aktualisieren"
        // ausgewählt wurde und geben eine Meldung aus
        int id = item.getItemId();
        if (id == R.id.showStats) {
            startActivity(new Intent(this, StatsActivity.class));
            return true;
        }
        if(id == R.id.showSettings){
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void comparePercentage(int i) {
        percentageDifference = false;
        if (oPercent == 0){
            oPercent = i;
            showNotification(String.valueOf(oPercent));
        } else {
            nPercent = i;
            if (nPercent != oPercent){
                oPercent = nPercent;
                showNotification(String.valueOf(oPercent));
                percentageDifference = true;
            }
        }

    }

    private void showNotification(String s) {
        int c = Integer.parseInt(s);
        int ic = R.drawable.icon_battery5;
        if (c <= 20){ic = R.drawable.icon_battery1;}
        else if(c<=40){ic = R.drawable.icon_battery2;}
        else if(c<=60){ic = R.drawable.icon_battery3;}
        else if(c<=80){ic = R.drawable.icon_battery4;}
        else if(c<=100){ic = R.drawable.icon_battery5;}

        Resources r = getResources();
        long[] pattern = {0, 100, 500, 100, 500, 100, 500, 100, 500}; // to vibrate use .setVibration

        Notification notification = new NotificationCompat.Builder(this)
                .setTicker(r.getString(R.string.app_name))
                .setSmallIcon(ic)
                .setContentTitle(r.getString(R.string.app_name))
                .setContentText("Ladezustand: " + s+" %")
                .setAutoCancel(true)
                .setChannel("Ladezustand")
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification);
    }

    private int getPercentage(float E){
        float eNow = E;
        float percentage = 100*(1-(eNow/maxEnergy));
        int percentageReturn = Math.round(percentage);
        return percentageReturn;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "...In onPause()...");
        unbindService(mConnection);
        mIsBound = false;
        saveArrayList(btDataList, "btDataList");
        stopthreadUpdateUi = true;
        doUnBindService();
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.d(TAG, "...In onResume()...");

        btDataList = new ArrayList<>();
        btDataList = getArrayList("btDataList");

        if (btDataList == null){
            btDataList = new ArrayList<>();
        }


        mWaveLoadingView.setCenterTitle("No BT-Device connected");
        voltageView.setText("Voltage");    //update the textviews with sensor values
        currentView.setText("Current");

        // read the saved voltage of accumulator
        SharedPreferences sPrefsVoltage = PreferenceManager.getDefaultSharedPreferences(this);
        String prefAccumulatorVoltageKey = getString(R.string.preference_accumulator_voltage_key);
        String prefAccumulatorVoltageDefault = getString(R.string.preference_accumulator_voltage_default);
        String AccumulatorVoltage = sPrefsVoltage.getString(prefAccumulatorVoltageKey,prefAccumulatorVoltageDefault);
        Double maximumVoltage = Double.parseDouble(AccumulatorVoltage);
        calculatingFactorVoltage = 100/maximumVoltage;

        // read the saved current of accumulator
        SharedPreferences sPrefsCurrent = PreferenceManager.getDefaultSharedPreferences(this);
        String prefAccumulatorCurrentKey = getString(R.string.preference_accumulator_current_key);
        String prefAccumulatorCurrentDefault = getString(R.string.preference_accumulator_current_default);
        String AccumulatorCurrent = sPrefsCurrent.getString(prefAccumulatorCurrentKey,prefAccumulatorCurrentDefault);
        Double maximumCurrent = Double.parseDouble(AccumulatorCurrent);
        calculatingFactorCurrent = 100/maximumCurrent;


        // read the saved maximum energy for accumulator
        SharedPreferences sPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String prefAccumulatorEnergyKey = getString(R.string.preference_accumulator_energy_key);
        String prefAccumulatorEnergyDefault = getString(R.string.preference_accumulator_energy_default);
        String AccumulatorEnergy = sPrefs.getString(prefAccumulatorEnergyKey,prefAccumulatorEnergyDefault);
        maxEnergy = Integer.parseInt(AccumulatorEnergy);

        UISetFirstTime = true;
        stopthreadUpdateUi = false;


        doBindService();

        threadUpdateUi = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !stopthreadUpdateUi) {
                    try {
                        if (btHelperService != null) {
                            if (btHelperService.dataReceived) {
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateUi();
                                        updateDataList();

                                    }
                                });
                            }
                        }

                    } catch (Exception ex) {
                        stopthreadUpdateUi = true;
                        mWaveLoadingView.setCenterTitle("Connection Error");
                        mWaveLoadingView.setProgressValue(100);
                        mWaveLoadingView.setWaveColor(Color.argb(255, 255, 0, 0));
                    }
                }
            }
        });
        threadUpdateUi.start();


    }

    @Override
    public void onStop(){
        super.onStop();
    }

    private void updateDataList() {
        btDataList.add(new btData(btHelperService.getVoltage(),btHelperService.getCurrent(),btHelperService.getEnergy(),String.valueOf(maxEnergy),String.valueOf(percentage),Calendar.getInstance().getTimeInMillis()));
    }

    private void updateUi() {
        String voltage = btHelperService.getVoltage();
        String current = btHelperService.getCurrent();

        voltageView.setText(voltage + "V");    //update the textviews with sensor values
        currentView.setText(current + "A");

        Double dV = (calculatingFactorVoltage*(Double.parseDouble(voltage)));
        int iV = dV.intValue();
        Double dC = (calculatingFactorCurrent*(Double.parseDouble(current)));
        int iC = dC.intValue();

        gaugeVoltage.setValue(iV);
        gaugeCurrent.setValue(iC);

        Float E = Float.parseFloat(btHelperService.getEnergy());
            if (maxEnergy == 0) {
                mWaveLoadingView.setCenterTitle("Enter maximum E first");
            } else {
                percentage = getPercentage(E);
                comparePercentage(percentage);
                if(UISetFirstTime == true){
                    if (percentage <= 25) {
                        mWaveLoadingView.setWaveColor(Color.argb(255, 255, 0, 0));
                    } else if (percentage <= 50) {
                        mWaveLoadingView.setWaveColor(Color.argb(255, 255, 140, 0));
                    } else if (percentage <= 75) {
                        mWaveLoadingView.setWaveColor(Color.argb(255, 255, 255, 0));
                    } else if (percentage <= 100) {
                        mWaveLoadingView.setWaveColor(Color.argb(255, 50, 205, 50));
                    }
                    mWaveLoadingView.setCenterTitle(String.valueOf(percentage) + " %");
                    mWaveLoadingView.setProgressValue(percentage);
                    UISetFirstTime = false;
                } else{
                    if(percentageDifference == true){
                        if (percentage <= 25) {
                            mWaveLoadingView.setWaveColor(Color.argb(255, 255, 0, 0));
                        } else if (percentage <= 50) {
                            mWaveLoadingView.setWaveColor(Color.argb(255, 255, 140, 0));
                        } else if (percentage <= 75) {
                            mWaveLoadingView.setWaveColor(Color.argb(255, 255, 255, 0));
                        } else if (percentage <= 100) {
                            mWaveLoadingView.setWaveColor(Color.argb(255, 50, 205, 50));
                        }
                        mWaveLoadingView.setCenterTitle(String.valueOf(percentage) + " %");
                        mWaveLoadingView.setProgressValue(percentage);
                    }
                }
            }
    }

    @Override
    public void onRestart() {
        super.onRestart();
        Log.d(TAG, "...In onRestart()...");
        // closeBT();
    }

    private void errorExit(String title, String message){
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }

    /*
    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.d(TAG, "...In onDestroy...");
        // Write to SharedPref.
        // btAdapter.disable();
        Log.d(TAG, "finishing APP");
        finish();
    }
    */

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.super.onBackPressed();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    public void saveArrayList(ArrayList<btData> list, String key){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(list);
        editor.putString(key, json);
        editor.apply();     // This line is IMPORTANT !!!
    }

    public ArrayList<btData> getArrayList(String key){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this );
        Gson gson = new Gson();
        String json = prefs.getString(key, null);
        Type type = new TypeToken<ArrayList<btData>>() {}.getType();
        return gson.fromJson(json, type);
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            btHelperService = ((BluetoothHelper.btBinder)service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            btHelperService = null;
        }


    };

    void doBindService() {
        bindService(new Intent(MainActivity.this, BluetoothHelper.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnBindService(){
        if(mIsBound){
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}



