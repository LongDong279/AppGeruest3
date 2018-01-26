package cesketronics.appgeruest3;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by walischewski on 23.01.2018.
 */

public class btData {
    String btDataVoltage;
    String btDataCurrent;
    String btDataEnergy;
    String btDataMaxEnergySet;
    String btDataEnergyPercetage;
    long btTime;

    public btData(String btDataVoltage, String btDataCurrent, String btDataEnergy, String btDataMaxEnergySet, String btDataEnergyPercetage, long btTime) {
        this.btDataVoltage = btDataVoltage;
        this.btDataCurrent = btDataCurrent;
        this.btDataEnergy = btDataEnergy;
        this.btDataMaxEnergySet = btDataMaxEnergySet;
        this.btDataEnergyPercetage = btDataEnergyPercetage;
        this.btTime = btTime;
    }

    public String getBtDataVoltage() {
        return btDataVoltage;
    }

    public String getBtDataCurrent() {
        return btDataCurrent;
    }

    public String getBtDataEnergy() {
        return btDataEnergy;
    }

    public String getBtDataMaxEnergySet() {
        return btDataMaxEnergySet;
    }

    public String getBtDataEnergyPercetage() {
        return btDataEnergyPercetage;
    }

    public long getBtTime() {
        return btTime;
    }

    public void setBtDataVoltage(String btDataVoltage) {
        this.btDataVoltage = btDataVoltage;
    }

    public void setBtDataCurrent(String btDataCurrent) {
        this.btDataCurrent = btDataCurrent;
    }

    public void setBtDataEnergy(String btDataEnergy) {
        this.btDataEnergy = btDataEnergy;
    }

    public void setBtDataMaxEnergySet(String btDataMaxEnergySet) {
        this.btDataMaxEnergySet = btDataMaxEnergySet;
    }

    public void setBtDataEnergyPercetage(String btDataEnergyPercetage) {
        this.btDataEnergyPercetage = btDataEnergyPercetage;
    }

    public void setBtTime(long btTime) {
        this.btTime = btTime;
    }

    public static String getDate(long milliSeconds, String dateFormat)
    {
        // Create a DateFormatter object for displaying date in specified format.
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }
}

