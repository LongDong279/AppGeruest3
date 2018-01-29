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

        btDataList = new ArrayList<>();
        btDataList = getArrayList("btDataList");


        mWaveLoadingView = (WaveLoadingView) findViewById(R.id.waveLoadingView);

        mWaveLoadingView.setShapeType(WaveLoadingView.ShapeType.CIRCLE);
        // mWaveLoadingView.setTopTitle("Ladezustand");
        mWaveLoadingView.setCenterTitleColor(Color.DKGRAY);
        //mWaveLoadingView.setCenterTitleSize(10);
        mWaveLoadingView.setCenterTitleStrokeColor(Color.LTGRAY);
        mWaveLoadingView.setCenterTitleStrokeWidth(2);

        //mWaveLoadingView.setBottomTitleSize(18);
        mWaveLoadingView.setProgressValue(20);
        mWaveLoadingView.setBorderWidth(10);
        mWaveLoadingView.setAmplitudeRatio(20);

        mWaveLoadingView.setWaveColor(Color.argb(255,255,0,0));


        mWaveLoadingView.setBorderColor(Color.DKGRAY);
        // mWaveLoadingView.setTopTitleStrokeColor(Color.MAGENTA);
        // mWaveLoadingView.setTopTitleStrokeWidth(5);
        //mWaveLoadingView.setBottomTitle("Bottom Title");
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

    /*

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return  device.createRfcommSocketToServiceRecord(MY_UUID);
        //creates secure outgoing connecetion with BT device using UUID
    }

    */

    private int getPercentage(float E){
        float eNow = E;
        float percentage = 100*(1-(eNow/maxEnergy));
        int percentageReturn = Math.round(percentage);
        return percentageReturn;
    }

    @Override
    public void onStart() {
        super.onStart();

        //Intent intent = new Intent(this, BluetoothHelper.class);
        //bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

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
        // stopWorker = false;
        /*
        new Thread (new Runnable() {
            public void run() {
                // a potentially  time consuming task
                Log.d(TAG, "in Thread");
                // openBT();
            }
        }).start();
        */

        doBindService();
        startService(new Intent(this, BluetoothHelper.class));

       // updateUi();
       // updateDataList();
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

    /*
    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "...In onStop()...");

        if (outStream != null) {
            try {
                outStream.flush();
            } catch (IOException e) {
                errorExit("Fatal Error", "In onStop() and failed to flush output stream: " + e.getMessage() + ".");
            }
        }
        //closeBT();
        //receiveDataInBackground();

    }
    */

    /*
    private void closeBT() {
        try     {
            btSocket.close();
            btAdapter.disable();
        } catch (IOException e2) {
            errorExit("Fatal Error", "In closeBT() and failed to close socket." + e2.getMessage() + ".");
        }
    }
    */

    /*
    private void receiveDataInBackground() {
        Log.d(TAG, "receive Data in Background");
        // NEW SHIT
        stopBackground = false;
        backgroundThread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !stopBackground) {
                    try {
                        Log.d(TAG, "in Background Thread");
                        openBT();
                        bluetoothIn = new Handler() {
                            public void handleMessage(Message msg) {
                                if (msg.what == handlerState) {                                     //if message is what we want
                                    String readMessage = (String) msg.obj;                             // msg.arg1 = bytes from connect thread
                                    recDataString.append(readMessage);                                //keep appending to string until ~
                                    int endOfLineIndex = recDataString.indexOf("~");                    // determine the end-of-line
                                    if (endOfLineIndex > 0) {                                           // make sure there data before ~
                                        String dataInPrint = recDataString.substring(0, endOfLineIndex);    // extract string

                                        if (recDataString.charAt(0) == '#')                             //if it starts with # we know it is what we are looking for
                                        {
                                            dataInPrint = dataInPrint.substring(1);                     //erase the first character
                                            String[] recSensorDataArray = dataInPrint.split(";");       //make an array out of the input string
                                            String[] sensorArray = new String[recSensorDataArray.length];   //second sensor data array

                                            for (int i = 0; i < recSensorDataArray.length; i++) {
                                                sensorArray[i] = recSensorDataArray[i]; //give the string array into another string array, to simplify the name
                                            }

                                            Float E = Float.parseFloat(sensorArray[2]);
                                            int percentage = getPercentage(E);
                                            comparePercentage(percentage);

                                        }
                                        recDataString.delete(0, recDataString.length());                    //clear all string data
                                        dataReceived = true;
                                    }
                                }
                            }
                        };
                        //
                        if(dataReceived==true){closeBT();}
                        backgroundThread.sleep(60000);
                    } catch (Exception ex) {
                        stopBackground = true;
                    }
                }
            }
        });
    }

    */
    /*
    private void openBT(){
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();
        Log.d(TAG, "...openBT - try connect...");
        // Set up a pointer to the remote node using it's address.
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e1) {
            errorExit("Fatal Error", "In openBT() and socket create failed: " + e1.getMessage() + ".");
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        Log.d(TAG, "...Connecting...");
        try {
            btSocket.connect();
            Log.d(TAG, "...Connection ok...");
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                errorExit("Fatal Error", "In openBT() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        // Create a data stream so we can talk to server.
        Log.d(TAG, "...in openBT() Create Socket...");

        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            errorExit("Fatal Error", "In onStart() and output stream creation failed:" + e.getMessage() + ".");
        }
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();
    }
    */

    @Override
    public void onRestart() {
        super.onRestart();
        Log.d(TAG, "...In onRestart()...");
        // closeBT();
    }

    /*

    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if(btAdapter==null) {
            errorExit("Fatal Error", "Bluetooth not supported");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...in checkBTState() Bluetooth ON...");
            } else {
                btAdapter.enable();
                SystemClock.sleep(2000);
                // Prompt user to turn on Bluetooth
                // Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                // startActivityForResult(enableBtIntent, 1);
            }
        }
    }
    */

    private void errorExit(String title, String message){
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }

    /*
    private void sendData(String message) {
        byte[] msgBuffer = message.getBytes();

        Log.d(TAG, "...Send data: " + message + "...");

        try {
            outStream.write(msgBuffer);
        } catch (IOException e) {
            String msg = "In onResume() and an exception occurred during write: " + e.getMessage();
            if (address.equals("00:00:00:00:00:00"))
                msg = msg + ".\n\nUpdate your server address from 00:00:00:00:00:00 to the correct address on line 35 in the java code";
            msg = msg +  ".\n\nCheck that the SPP UUID: " + MY_UUID.toString() + " exists on server.\n\n";

            errorExit("Fatal Error", msg);
        }
    }
    */

    // create new class for connect thread
    /*
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }
        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            // comboxView.append(input);
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
                finish();

            }
        }


    }
    */

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



