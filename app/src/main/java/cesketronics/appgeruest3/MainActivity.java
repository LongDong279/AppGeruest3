package cesketronics.appgeruest3;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.lang.String;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
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
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.renderscript.ScriptGroup;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import me.itangqi.waveloadingview.WaveLoadingView;
import android.graphics.Color;
import android.app.Notification;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;


public class MainActivity extends Activity {
    private static final String TAG = "bluetooth1";

    TextView voltageView, currentView;
    //energyView;

    WaveLoadingView mWaveLoadingView;
    static ArrayList<btData> btDataList;
    BluetoothHelper btHelperService;
    boolean mIsBound = false;
    String resetArduinosEnergy = "0";

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



        // declare all visual xml objects as java objects
        voltageView = (TextView) findViewById(R.id.voltage_tv);
        currentView = (TextView) findViewById(R.id.current_tv);
        //energyView = (TextView) findViewById(R.id.energy_tv);
        mWaveLoadingView = (WaveLoadingView) findViewById(R.id.waveLoadingView);

        btDataList = new ArrayList<>();
        btDataList = getArrayList("btDataList");

        if (btDataList == null){
            btDataList = new ArrayList<>();
        }

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


        // read the saved maximum energy for accumulator
        SharedPreferences sPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String prefAccumulatorEnergyKey = getString(R.string.preference_accumulator_energy_key);
        String prefAccumulatorEnergyDefault = getString(R.string.preference_accumulator_energy_default);
        String AccumulatorEnergy = sPrefs.getString(prefAccumulatorEnergyKey,prefAccumulatorEnergyDefault);
        maxEnergy = Integer.parseInt(AccumulatorEnergy);

        //check if reset Arduinos Energy in BT Helper


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
            if (nPercent < oPercent){
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
        doBindService();

        /*
        btDataList.add(new btData("12,5", "3,5","0,01","3500","99",Calendar.getInstance().getTimeInMillis()));
        SystemClock.sleep(2000);
        btDataList.add(new btData("12,5", "3,5","0,02","3500","98",Calendar.getInstance().getTimeInMillis()));
        SystemClock.sleep(2000);
        btDataList.add(new btData("12,5", "3,1","0,03","3500","97",Calendar.getInstance().getTimeInMillis()));
        SystemClock.sleep(2000);
        btDataList.add(new btData("12,5", "3,7","0,04","3500","96",Calendar.getInstance().getTimeInMillis()));
*/

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
                    }
                }
            }
        });
        threadUpdateUi.start();





    }

    @Override
    public void onStop(){
        super.onStop();
        /*
        unbindService(mConnection);
        mIsBound = false;
        saveArrayList(btDataList, "btDataList");
        stopthreadUpdateUi = true;
        doUnBindService();
        */
    }

    private void updateDataList() {
        btDataList.add(new btData(btHelperService.getVoltage(),btHelperService.getCurrent(),btHelperService.getEnergy(),String.valueOf(maxEnergy),String.valueOf(percentage),Calendar.getInstance().getTimeInMillis()));
    }

    private void updateUi() {
        voltageView.setText("Spannung = " + btHelperService.getVoltage() + "V");    //update the textviews with sensor values
        currentView.setText("Strom = " + btHelperService.getCurrent() + "A");
        //energyView.setText("Energie = " + btHelperService.getEnergy()+ "Wh");

        Float E = Float.parseFloat(btHelperService.getEnergy());
        if (maxEnergy == 0) {
            mWaveLoadingView.setCenterTitle("Enter E first");
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
            Toast.makeText(getBaseContext(), "Service connected", Toast.LENGTH_SHORT).show();
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


}



