package com.example.tester;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.util.logging.Logger;

/**
 * Created by toeknee on 13-8-3.
 */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null) {
            if (intent.getAction().equals(AlarmSysService.ACTION)) {
                Intent mIntent = new Intent(context, WeatherUpdateService.class);
                mIntent.putExtras(intent.getExtras());
                //String cityCode = mIntent.getExtras().getString("cityCode");
                //Toast.makeText(context, "AR: " + cityCode, Toast.LENGTH_LONG).show();
                context.startService(mIntent);
            }
        }
    }
}
