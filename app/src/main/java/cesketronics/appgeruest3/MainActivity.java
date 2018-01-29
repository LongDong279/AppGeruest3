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
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.renderscript.ScriptGroup;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
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

    Button function1btn, function2btn, function3btn;
    TextView voltageView, currentView, energyView, maxEnergyView;

    WaveLoadingView mWaveLoadingView;
    static ArrayList<btData> btDataList;
    BluetoothHelper btHelperService;
    boolean mIsBound = false;

    static int maxEnergy = 0;
    int oPercent=0;
    int nPercent=0;
    int percentage = 0;

    static boolean UISetFirstTime = true;
    static boolean percentageDifference = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // declare all visual xml objects as java objects
        function1btn = (Button) findViewById(R.id.function1btn);
        function2btn = (Button) findViewById(R.id.function2btn);
        function3btn = (Button) findViewById(R.id.function3btn);
        voltageView = (TextView) findViewById(R.id.voltage_tv);
        currentView = (TextView) findViewById(R.id.current_tv);
        energyView = (TextView) findViewById(R.id.energy_tv);
        maxEnergyView = (TextView) findViewById(R.id.maxEnergy_tv);
        mWaveLoadingView = (WaveLoadingView) findViewById(R.id.waveLoadingView);

        btDataList = new ArrayList<>();
        btDataList = getArrayList("btDataList");

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


        final SharedPreferences mPrefsMaxCap = getSharedPreferences("label", 0);
        String mString = mPrefsMaxCap.getString("MaxCap", "0");
        maxEnergy = Integer.parseInt(mString);

        if (maxEnergy != 0){
            maxEnergyView.setText(mString);
        }

        function1btn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                //mConnectedThread.write("0"); // k
                btHelperService.writeToSerial("0");
                Toast.makeText(getBaseContext(), "Set Energy", Toast.LENGTH_SHORT).show();
                String s = maxEnergyView.getText().toString();
                maxEnergy = Integer.parseInt(s);
                Editor mEditor = mPrefsMaxCap.edit();
                mEditor.putString("MaxCap", s).commit();
            }
        });

        function2btn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                //mConnectedThread.write("1"); // r
                btHelperService.writeToSerial("1");
                Toast.makeText(getBaseContext(), "Reset Energy", Toast.LENGTH_SHORT).show();
                String s = "0";
                SharedPreferences.Editor mEditor = mPrefsMaxCap.edit();
                mEditor.putString("MaxCap", s).commit();
                maxEnergyView.setText("Enter max. E in");
            }
        });

        function3btn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, StatsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
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
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.d(TAG, "...In onResume()...");
        doBindService();

        /*

        if((btHelperService.getVoltage() != null) && (btHelperService.getCurrent()!= null) && (btHelperService.getEnergy() != null)){
            updateUi();
            updateDataList();
        }
        */
    }

    @Override
    public void onStop(){
        super.onStop();
        unbindService(mConnection);
        mIsBound = false;
    }

    private void updateDataList() {
        btDataList.add(new btData(btHelperService.getVoltage(),btHelperService.getCurrent(),btHelperService.getEnergy(),String.valueOf(maxEnergy),String.valueOf(percentage),Calendar.getInstance().getTimeInMillis()));
    }

    private void updateUi() {
        voltageView.setText("Spannung = " + btHelperService.getVoltage() + "V");    //update the textviews with sensor values
        currentView.setText("Strom = " + btHelperService.getCurrent() + "A");
        energyView.setText("Energie = " + btHelperService.getEnergy()+ "Wh");

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

    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.d(TAG, "...In onDestroy...");
        // Write to SharedPref.
        saveArrayList(btDataList, "btDataList");
        // btAdapter.disable();
        Log.d(TAG, "finishing APP");
        // stopBackground = true;
        finish();
    }

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



