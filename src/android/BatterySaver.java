package io.electrosoft.helloworld;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

public class BatterySaver {

    private Context context;

    private JSONObject batteryStatus = new JSONObject();

    public BatterySaver(Context context) {
        this.context = context;
    }

    public JSONObject getBatteryStatus() {
        return batteryStatus;
    }

    public int batteryLevel()
    {
        if (Build.VERSION.SDK_INT >= 21)
        {
            BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        } else {
            IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, iFilter);
            int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
            int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;
            double batteryPct = level / (double) scale;
            return (int) (batteryPct * 100);
        }
    }

    public boolean batteryCharging() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);

        assert batteryStatus != null;
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        return isCharging;
    }

    public JSONObject batteryStatus() throws JSONException {

        Intent intent = new Intent();
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);

        batteryStatus.put("batteryPercentage", batteryLevel());
        batteryStatus.put("isCharging", batteryCharging());
        batteryStatus.put("remainingTime", batteryTimeLeft());
        batteryStatus.put("level", level);
        return batteryStatus;
    }

    public String batteryTimeLeft() {
        Intent intent = new Intent();
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);

        if(level <= 5)
        {
            return "3h 55m remaning";
        } else if(level > 5 && level<=10)
        {
            return "6h 0m remaning";
        } else if(level > 10 && level<=15)
        {
            return "8h 25m remaning";
        } else if(level > 15 && level<=25) {
            return "12h 55m remaning";
        } else if(level > 25 && level<=35)
        {
            return "19h 2m remaning";
        } else if(level > 35 && level<=50) {
            return "22h 0m remaning";
        } else if(level > 50 &&level<=65)
        {
            return "28h 15m remaning";
        } else if(level > 65 &&level<=75)
        {
            return "30h 55m remaning";
        } else if(level > 75 &&level<=85) {
            return "38h 5m remaning";
        } else if(level > 85 && level <= 100)
        {
            return "60h 0m remaning";
        }
        return "60h 0m remaning";
    }

    public void mode(int mode) {

        switch(mode) {



        }


    }

}
