package com.example.tester;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;
import android.os.Handler;

import java.util.Calendar;

/**
 * Created by toeknee on 13-8-3.
 */
public class AlarmSysService extends Service {
    private AlarmManager alarm;
    public final static String ACTION="android.intent.action.ALARM_SERVICE_START";
    private Handler handler;

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        handler = new Handler();
        handler.post(new Runnable(){
            public void run(){
                Toast.makeText(AlarmSysService.this, "Service is bind!", Toast.LENGTH_LONG).show();
            }
        });
        return null;
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
    }

    /**
     * 定时执行pendingIntent，每2小时一次
     * 为了演示方便，更改为每30s一次
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        // TODO Auto-generated method stub
        if(null == alarm){
            alarm = (AlarmManager)getSystemService(ALARM_SERVICE);
            Intent it = new Intent(AlarmSysService.this, AlarmReceiver.class);
            it.putExtras(intent.getExtras());
            it.setAction(ACTION);
            //第四个参数FLAG_UPDATE_CURRENT使得之前存在的PendingIntent也可更新（更换城市后）
            PendingIntent sender = PendingIntent.getBroadcast(AlarmSysService.this, 0, it, PendingIntent.FLAG_UPDATE_CURRENT);
            //每2小时重复执行一次,为方便演示，首次执行在创建服务的5秒后，实际上应该是在创建服务2小时后
            alarm.setRepeating(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis() + 1*5*1000,1*30*1000, sender);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }
}