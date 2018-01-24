package cesketronics.appgeruest3;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.app.Activity;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class StatsActivity extends Activity {
    Button backButton, loadStatsButton;
    btData loadedData;
    private static final String TAG = "bluetooth1";
    ArrayList<btData> btDataArrayList = null;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);
        backButton = (Button) findViewById(R.id.backBtn);
        loadStatsButton = (Button) findViewById(R.id.loadDataBtn);


        backButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                /*
                startActivity(new Intent(StatsActivity.this, MainActivity.class));
                finish();
                */
                Intent intent = new Intent(StatsActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);

            }
        });

        loadStatsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
            btDataArrayList = getArrayList("btDataList");

            }
        });





    }

    public ArrayList<btData> getArrayList(String key){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(StatsActivity.this );
        Gson gson = new Gson();
        String json = prefs.getString(key, null);
        Type type = new TypeToken<ArrayList<String>>() {}.getType();
        return gson.fromJson(json, type);
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        Intent intent = new Intent(StatsActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        /*
        startActivity(new Intent(StatsActivity.this, MainActivity.class));
        finish();
        */
    }
}
