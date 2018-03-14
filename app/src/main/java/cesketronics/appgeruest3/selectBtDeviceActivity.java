package cesketronics.appgeruest3;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class selectBtDeviceActivity extends Activity {

    private ListView listView;
    private BluetoothAdapter mBluetoothAdapter;
    private Set<BluetoothDevice> pairedDevices;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_bt_device);

        listView = (ListView) findViewById(R.id.listView);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //mBluetoothAdapter.startDiscovery();

        pairedDevices = mBluetoothAdapter.getBondedDevices();
        ArrayList<String> list = new ArrayList<String>();
        for(BluetoothDevice bt : pairedDevices)
            list.add(bt.getName() + " " + bt.getAddress());

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>
                (this,android.R.layout.simple_list_item_1, list);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int pos, long id){
                String devicep=listView.getAdapter().getItem(pos).toString();
                String address = devicep.substring(devicep.length() - 17);

                final SharedPreferences mPrefsMacAdd = getSharedPreferences("Mac", 0);
                SharedPreferences.Editor mEditor = mPrefsMacAdd.edit();
                mEditor.putString("MacAdd", address).commit();

                Toast.makeText(getBaseContext(), "Device successfully changed", Toast.LENGTH_LONG).show();


            }
        });

    }




    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
