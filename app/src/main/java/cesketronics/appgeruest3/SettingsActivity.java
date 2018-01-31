package cesketronics.appgeruest3;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;


public class SettingsActivity extends PreferenceActivity
implements Preference.OnPreferenceChangeListener {

    ArrayList<btData> btDataStatsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        btDataStatsList = getArrayList("btDataList");


        Preference accumulatorEnergy = findPreference(getString(R.string.preference_accumulator_energy_key));
        accumulatorEnergy.setOnPreferenceChangeListener(this);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String savedAccumulatorEnergy = sharedPrefs.getString(accumulatorEnergy.getKey(), "");
        onPreferenceChange(accumulatorEnergy, savedAccumulatorEnergy);

        Preference resetRecordedDataBtn = findPreference(getString(R.string.preference_resetRecordedData_key));
        resetRecordedDataBtn.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialog.Builder(SettingsActivity.this )
                        .setMessage("Are you sure you want to reset all data?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this);
                                preferences.edit().remove("btDataList").commit();
                                if(MainActivity.btDataList != null){
                                    MainActivity.btDataList.clear();
                                }
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
                return true;
            }
        });


        Preference resetMacAddressBtn = findPreference(getString(R.string.preference_resetMacAddress_key));
        resetMacAddressBtn.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(SettingsActivity.this, selectBtDeviceActivity.class));
                return true;
            }
        });

        Preference resetArduinoEnergyBtn = findPreference(getString(R.string.preference_resetArduinoEnergy_key));
        resetArduinoEnergyBtn.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialog.Builder(SettingsActivity.this )
                        .setMessage("Are you sure you want to reset all data?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this);
                                SharedPreferences.Editor editor = sharedPrefs.edit();
                                editor.putString((getString(R.string.preference_resetArduinoEnergy_key)), "1");
                                editor.commit();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
                return true;
            }
        });

        if(btDataStatsList.size() > 1){
            resetArduinoEnergyBtn.setSummary((btDataStatsList.get(btDataStatsList.size()-1).btDataEnergy)+" Wh");
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        preference.setSummary(value.toString());

        return true;
    }

    public ArrayList<btData> getArrayList(String key){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this );
        Gson gson = new Gson();
        String json = prefs.getString(key, null);
        Type type = new TypeToken<ArrayList<btData>>() {}.getType();
        return gson.fromJson(json, type);
    }
}
