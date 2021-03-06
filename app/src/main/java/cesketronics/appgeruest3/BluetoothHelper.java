package cesketronics.appgeruest3;

import android.app.IntentService;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.camera2.params.BlackLevelPattern;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static android.content.ContentValues.TAG;

/**
 * Created by walischewski on 29.01.2018.
 */
public class BluetoothHelper extends Service {

    final int handlerState = 0;                        //used to identify handler message
    Handler bluetoothIn;
    private BluetoothAdapter btAdapter = null;
    public static final long NOTIFY_INTERVAL = 10000;
    // run on another Thread to avoid crash

    private final IBinder mBinder = new btBinder();

    public boolean dataReceived = false;




    private ConnectingThread mConnectingThread;
    private ConnectedThread mConnectedThread;

    private boolean stopThread;
    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // String for MAC address
    //private static final String MAC_ADDRESS = "98:D3:31:FC:3A:25"; //your MAC Address here, should be changed to select different devices
    private String MAC_ADDRESS = "0";

    String resetArduinosEnergy = "0";
    boolean flagToReset = false;




    private StringBuilder recDataString = new StringBuilder();

    private String voltage = "0";
    private String current = "0";
    private String energy= "0";

    public String getVoltage() {
        return voltage;
    }

    public String getCurrent() {
        return current;
    }

    public String getEnergy() {
        return energy;
    }


    public class btBinder extends Binder {
        BluetoothHelper getService() {
            // Return this instance of LocalService so clients can call public methods
            return BluetoothHelper.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        stopThread = false;



        final SharedPreferences mPrefsMacAdd = getSharedPreferences("Mac", 0);
        String macString = mPrefsMacAdd.getString("MacAdd", "");
        MAC_ADDRESS = macString;

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();


        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        resetArduinosEnergy = sharedPrefs.getString((getString(R.string.preference_resetArduinoEnergy_key)), "0");

        if(resetArduinosEnergy.equals("1")){
            flagToReset = true;
            resetArduinosEnergy ="0";
            sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putString((getString(R.string.preference_resetArduinoEnergy_key)), "0");
            editor.commit();
        }


        bluetoothIn = new Handler() {

            public void handleMessage(android.os.Message msg) {
                Log.d("DEBUG", "handleMessage");
                if (msg.what == handlerState) {                                     //if message is what we want
                    String readMessage = (String) msg.obj;                                                                // msg.arg1 = bytes from connect thread
                    recDataString.append(readMessage); //enter code here
                    Log.d("RECORDED", recDataString.toString());
                    // Do stuff here with your data, like adding it to the database
                    int endOfLineIndex = recDataString.indexOf("~");                    // determine the end-of-line
                    if (endOfLineIndex > 0) {                                           // make sure there data before ~
                        String dataInPrint = recDataString.substring(0, endOfLineIndex);    // extract string


                        if (recDataString.charAt(0) == '#')                             //if it starts with # we know it is what we are looking for
                        {
                            dataInPrint = dataInPrint.substring(1);                     //erase the first character
                            Log.d("String:", dataInPrint);
                            String[] recSensorDataArray = dataInPrint.split(";");       //make an array out of the input string
                            String[] sensorArray = new String[recSensorDataArray.length];   //second sensor data array

                            for (int i = 0; i < recSensorDataArray.length; i++) {
                                sensorArray[i] = recSensorDataArray[i]; //give the string array into another string array, to simplify the name
                            }

                            voltage = sensorArray[0];
                            current = sensorArray[1];
                            energy = sensorArray[2];

                            dataReceived = true;
                        }
                    }
                }
                if(dataReceived){
                    recDataString.delete(0, recDataString.length());
                    dataReceived = false;
                }                   //clear all string data
            }
        };
        Log.d("BT SERVICE", "SERVICE CREATED");

    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("onStartCommand", "Startid: "+startId + ":" + intent);
        Log.d("BT SERVICE", "SERVICE STARTED");
        Log.d("BT Service", "Started as Sticky service");
        // Handler, btAdapter & checkBTState was here before
        //return START_STICKY;

        return super.onStartCommand(intent, flags, startId);
    }

    /*

    public void flush() {
        Log.d("Flush", "trying to flush");
        if (outStream != null) {
            try {
                outStream.flush();
            } catch (IOException e) {
                errorExit("Fatal Error", "In onStop() and failed to flush output stream: " + e.getMessage() + ".");
            }
        }
    }
    */


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("SERVICE", "onDestroy");
        bluetoothIn.removeCallbacksAndMessages(null);
        stopThread = true;
        if (mConnectedThread != null) {
            mConnectedThread.closeStreams();
        }
        if (mConnectingThread != null) {
            mConnectingThread.closeSocket();
        }
        Log.d("SERVICE", "nextstep stopself");
        stopSelf();
    }



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    //Checks that the Android device Bluetooth is available and prompts to be turned on if off
    private void checkBTState() {

        if (btAdapter == null) {
            Log.d("BT SERVICE", "BLUETOOTH NOT SUPPORTED BY DEVICE, STOPPING SERVICE");
            stopSelf();

        } else {
            if (btAdapter.isEnabled()) {
                Log.d("DEBUG BT", "BT ENABLED! BT ADDRESS : " + btAdapter.getAddress() + " , BT NAME : " + btAdapter.getName());
                try {
                    BluetoothDevice device = btAdapter.getRemoteDevice(MAC_ADDRESS);
                    Log.d("DEBUG BT", "ATTEMPTING TO CONNECT TO REMOTE DEVICE : " + MAC_ADDRESS);
                    mConnectingThread = new ConnectingThread(device);
                    mConnectingThread.start();
                } catch (IllegalArgumentException e) {
                    Log.d("DEBUG BT", "PROBLEM WITH MAC ADDRESS : " + e.toString());
                    Log.d("BT SEVICE", "ILLEGAL MAC ADDRESS, STOPPING SERVICE");

                    stopSelf();

                }
            } else {
                Log.d("BT SERVICE", "BLUETOOTH NOT ON, STOPPING SERVICE");
                //stopSelf();
                btAdapter.enable(); // enable Bluetooth when Bluetooth is off
                Toast.makeText(getBaseContext(), "Bluetooth enabling...", Toast.LENGTH_LONG).show();
                SystemClock.sleep(3000); // let phone enable bluetooth and wait some time
                checkBTState(); // checkBTState Again
            }
        }
    }

    public void getBondedDevices(){
        Set<BluetoothDevice> all_devices = btAdapter.getBondedDevices();
        if (all_devices.size() > 0) {
            for (BluetoothDevice currentDevice : all_devices) {
                Log.d("Device Name ", currentDevice.getName());
            }
        }
    }


    public void writeToSerial(String s){
        mConnectedThread.write(s);
    }

    // New Class for Connecting Thread
    private class ConnectingThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectingThread(BluetoothDevice device) {
            Log.d("DEBUG BT", "IN CONNECTING THREAD");
            mmDevice = device;
            BluetoothSocket temp = null;
            Log.d("DEBUG BT", "MAC ADDRESS : " + MAC_ADDRESS);
            Log.d("DEBUG BT", "BT UUID : " + BTMODULEUUID);
            try {
                temp = mmDevice.createRfcommSocketToServiceRecord(BTMODULEUUID); // createRfcommSocketToServiceRecord changed to createInsecureRfcommSocketToServiceRecord
                Log.d("DEBUG BT", "SOCKET CREATED : " + temp.toString());
            } catch (IOException e) {
                Log.d("DEBUG BT", "SOCKET CREATION FAILED :" + e.toString());
                Log.d("BT SERVICE", "SOCKET CREATION FAILED, STOPPING SERVICE");
                stopSelf();
            }
            mmSocket = temp;
        }

        @Override
        public void run() {
            super.run();
            Log.d("DEBUG BT", "IN CONNECTING THREAD RUN");
            // Establish the Bluetooth socket connection.
            // Cancelling discovery as it may slow down connection
            btAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
                Log.d("DEBUG BT", "BT SOCKET CONNECTED");
                mConnectedThread = new ConnectedThread(mmSocket);
                mConnectedThread.start();
                Log.d("DEBUG BT", "CONNECTED THREAD STARTED");
                //I send a character when resuming.beginning transmission to check device is connected
                //If it is not an exception will be thrown in the write method and finish() will be called
                // mConnectedThread.write("x");
            } catch (IOException e) {
                try {
                    Log.d("DEBUG BT", "SOCKET CONNECTION FAILED : " + e.toString());
                    Log.d("BT SERVICE", "SOCKET CONNECTION FAILED, STOPPING SERVICE");
                    mmSocket.close();
                    stopSelf();
                } catch (IOException e2) {
                    Log.d("DEBUG BT", "SOCKET CLOSING FAILED :" + e2.toString());
                    Log.d("BT SERVICE", "SOCKET CLOSING FAILED, STOPPING SERVICE");
                    stopSelf();
                    //insert code to deal with this
                }
            } catch (IllegalStateException e) {
                Log.d("DEBUG BT", "CONNECTED THREAD START FAILED : " + e.toString());
                Log.d("BT SERVICE", "CONNECTED THREAD START FAILED, STOPPING SERVICE");
                stopSelf();
            }
        }

        public void closeSocket() {
            try {
                //Don't leave Bluetooth sockets open when leaving activity
                mmSocket.close();
            } catch (IOException e2) {
                //insert code to deal with this
                Log.d("DEBUG BT", e2.toString());
                Log.d("BT SERVICE", "SOCKET CLOSING FAILED, STOPPING SERVICE");
                stopSelf();
            }
        }
    }

    // New Class for Connected Thread
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            Log.d("DEBUG BT", "IN CONNECTED THREAD");
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.d("DEBUG BT", e.toString());
                Log.d("BT SERVICE", "UNABLE TO READ/WRITE, STOPPING SERVICE");
                stopSelf();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.d("DEBUG BT", "IN CONNECTED THREAD RUN");
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true && !stopThread) {
                try {
                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    Log.d("DEBUG BT PART", "CONNECTED THREAD " + readMessage);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                    if (flagToReset){
                        writeToSerial("1");
                        flagToReset =false;
                    }

                } catch (IOException e) {
                    Log.d("DEBUG BT", e.toString());
                    Log.d("BT SERVICE", "UNABLE TO READ/WRITE, STOPPING SERVICE");
                    stopSelf();
                    break;
                }
            }
        }

        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Log.d("DEBUG BT", "UNABLE TO READ/WRITE " + e.toString());
                Log.d("BT SERVICE", "UNABLE TO READ/WRITE, STOPPING SERVICE");
                stopSelf();
            }
        }

        public void closeStreams() {
            try {
                //Don't leave Bluetooth sockets open when leaving activity
                mmInStream.close();
                mmOutStream.close();
            } catch (IOException e2) {
                //insert code to deal with this
                Log.d("DEBUG BT", e2.toString());
                Log.d("BT SERVICE", "STREAM CLOSING FAILED, STOPPING SERVICE");
                stopSelf();
            }
        }
    }

}