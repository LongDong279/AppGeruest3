package cesketronics.appgeruest3;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.ColorSpace;
import android.icu.text.MessageFormat;
import android.icu.text.TimeZoneFormat;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.app.Activity;
import android.widget.Toast;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import java.util.Calendar;
import java.util.Locale;


public class StatsActivity extends Activity {
    Button loadStatsButton, voltageEnableBtn, currentEnableBtn, energyEnableBtn, exportCSVBtn ;
    private static final String TAG = "bluetooth1";
    ArrayList<btData> btDataStatsList;
    GraphView graph;
    LineGraphSeries<DataPoint> dataSeriesVoltage;
    LineGraphSeries<DataPoint> dataSeriesCurrent;
    LineGraphSeries<DataPoint> dataSeriesEnergy;
    boolean voltageEnable, currentEnable, energyEnable, dataAlreadyLoaded = false;
    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM HH:mm:ss");






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);
        loadStatsButton = (Button) findViewById(R.id.loadDataBtn);
        energyEnableBtn =(Button)findViewById(R.id.enableEnergyBtn);
        currentEnableBtn =(Button)findViewById(R.id.enableCurrentBtn);
        voltageEnableBtn =(Button)findViewById(R.id.enableVoltageBtn);
        exportCSVBtn = (Button) findViewById(R.id.exportCSVBtn);

        graph = (GraphView) findViewById(R.id.graph);
        dataSeriesCurrent = new LineGraphSeries<>();
        dataSeriesEnergy = new LineGraphSeries<>();


        energyEnableBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(energyEnable == true) {
                    graph.removeSeries(dataSeriesEnergy);
                    energyEnable = false;
                } else {
                    graph.addSeries(dataSeriesEnergy);
                    energyEnable = true;
                }

            }
        });

        voltageEnableBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(voltageEnable == true) {
                    graph.removeSeries(dataSeriesVoltage);
                    voltageEnable = false;
                } else {
                    graph.addSeries(dataSeriesVoltage);
                    voltageEnable = true;
                }
            }
        });

        currentEnableBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentEnable == true) {
                    graph.removeSeries(dataSeriesCurrent);
                    currentEnable = false;
                } else {
                    graph.addSeries(dataSeriesCurrent);
                    currentEnable = true;
                }
            }
        });

        exportCSVBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                btDataStatsList = getArrayList("btDataList");


                String baseDir = android.os.Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();

                String filePath = baseDir + File.separator;

                // Create the file.
                File file = new File(filePath, "AkkuExportData.csv");


                String[] stringArray = new String[btDataStatsList.size()];

                    for (int z = 0; z<btDataStatsList.size(); z++) {
                        String timeAndDay = sdf.format(btDataStatsList.get(z).btTime);
                        stringArray[z] = String.valueOf(timeAndDay)+","+String.valueOf(btDataStatsList.get(z).btDataVoltage)+","+String.valueOf(btDataStatsList.get(z).btDataCurrent)+","+String.valueOf(btDataStatsList.get(z).btDataEnergy)+","+String.valueOf(btDataStatsList.get(z).btDataMaxEnergySet)+"\r\n";
                    }

                try
                {
                    file.createNewFile();
                    FileOutputStream fOut = new FileOutputStream(file);
                    OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                    myOutWriter.append("Datum,Spannung,Strom,Energie,Akkuenergie"+"\r\n");
                    for (int z = 0; z< stringArray.length ; z++) {
                        myOutWriter.append(stringArray[z]);
                    }
                    myOutWriter.close();
                    fOut.flush();
                    fOut.close();

                    Toast.makeText(getBaseContext(), "Data successfully exported to Download folder", Toast.LENGTH_LONG).show();
                }
                catch (IOException e)
                {
                    Log.e("Exception", "File write failed: " + e.toString());
                }




            }
        });

        loadStatsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                btDataStatsList = getArrayList("btDataList");
                Log.d(TAG, "Data loaded");

                if((btDataStatsList != null) && (btDataStatsList.size()>0)){
                    DataPoint[] valuesVoltage = new DataPoint[btDataStatsList.size()];
                    DataPoint[] valuesCurrent = new DataPoint[btDataStatsList.size()];
                    DataPoint[] valuesEnergy = new DataPoint[btDataStatsList.size()];

                    if (dataAlreadyLoaded == true){
                        graph.removeAllSeries();
                    }

                    for (int i = 0; i < btDataStatsList.size(); i++) {
                        DataPoint pVoltage = new DataPoint(btDataStatsList.get(i).btTime, (Double.parseDouble(btDataStatsList.get(i).btDataVoltage)));
                        DataPoint pCurrent = new DataPoint(btDataStatsList.get(i).btTime, (Double.parseDouble(btDataStatsList.get(i).btDataCurrent)));
                        DataPoint pEnergy = new DataPoint(btDataStatsList.get(i).btTime, (Double.parseDouble(btDataStatsList.get(i).btDataMaxEnergySet)-(Double.parseDouble(btDataStatsList.get(i).btDataEnergy))));

                        valuesVoltage[i] = pVoltage;
                        valuesCurrent[i] = pCurrent;
                        valuesEnergy[i] = pEnergy;
                    }


                    dataSeriesVoltage = new LineGraphSeries<>(valuesVoltage);
                    dataSeriesVoltage.setThickness(5);
                    dataSeriesVoltage.setColor(Color.BLUE);
                    dataSeriesVoltage.setDrawDataPoints(false);
                    dataSeriesVoltage.setTitle("Voltage");


                    dataSeriesCurrent = new LineGraphSeries<>(valuesCurrent);
                    dataSeriesCurrent.setThickness(5);
                    dataSeriesCurrent.setColor(Color.RED);
                    dataSeriesCurrent.setDrawDataPoints(false);
                    dataSeriesCurrent.setTitle("Current");

                    dataSeriesEnergy = new LineGraphSeries<>(valuesEnergy);
                    dataSeriesEnergy.setThickness(5);
                    dataSeriesEnergy.setColor(Color.YELLOW);
                    dataSeriesEnergy.setDrawDataPoints(false);
                    dataSeriesEnergy.setTitle("Accumulator Energy");



                    graph.addSeries(dataSeriesVoltage);
                    voltageEnable = true;
                    graph.addSeries(dataSeriesCurrent);
                    currentEnable = true;
                    graph.addSeries(dataSeriesEnergy);
                    energyEnable = true;

                    graph.getLegendRenderer().setVisible(true);
                    graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);


                    graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter(){
                        public String formatLabel(double value, boolean isValueX) {
                            if (isValueX) {
                                Date d = new Date((long) (value));
                                return (sdf.format(d));
                            } return "" + (int) value;
                        }
                    });

                    graph.getGridLabelRenderer().setNumHorizontalLabels(3);


                    graph.getViewport().setScrollable(true);
                    graph.getViewport().setXAxisBoundsManual(true);

                    graph.getViewport().setMinX(btDataStatsList.get(0).btTime);
                    graph.getViewport().setMaxX(btDataStatsList.get(btDataStatsList.size()-1).btTime);

                    dataAlreadyLoaded = true;
                }else{
                    Toast.makeText(getBaseContext(), "no data to display", Toast.LENGTH_LONG).show();
                }

            }
        });


    }

    public ArrayList<btData> getArrayList(String key){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(StatsActivity.this );
        Gson gson = new Gson();
        String json = prefs.getString(key, null);
        Type type = new TypeToken<ArrayList<btData>>() {}.getType();
        return gson.fromJson(json, type);
    }



}
