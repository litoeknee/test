package com.example.tester;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by toeknee on 13-8-3.
 */
public class WeatherUpdateService extends Service {
    private final static String TAG = "CareHelper";
    private String cityCode;
    private String cityName;
    private String date;
    private String temperature;
    private String weather1;
    private String weather2;
    private String suggestion;
    private String phoneNumber;
    private String message;
    private String info;
    private final String[] checkDate = {"img1", "img2", "img3", "img4"};

    private NotificationManager mNotificationManager;
    private PowerManager pm;
    private PowerManager.WakeLock wakeLock;
    public final static String ACTION="android.intent.action.ALARM_SERVICE_START";

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        cityCode = intent.getExtras().getString("cityCode");
        phoneNumber = intent.getExtras().getString("phoneNumber");
        message =intent.getExtras().getString("message");
        doTask();
        return START_STICKY;
    }

    /**
     * 通过json获取到的天气信息，判断当天及第二天天气是否恶劣，返回boolean值
     * 1、雷电天气
     * 2、中雨以上天气
     * 3、中雪以上天气
     * 4、沙尘暴等
     * 5、高温（大于30度）
     * @param json 处理json文档的对象
     * @return boolean 是否要弹出天气信息窗口
     */
    private boolean weatherChangeChecker(JSONObject json) {
        boolean returnHotValue = false;
        boolean[] todayAndTomorrow = {false, false};
        try {
            //炎热天气
            String temp = temperature.replaceAll(getResources().getString(R.string.celsius),"");
            String[] fromToTemp = temp.split("~");
            if (fromToTemp[0].startsWith("3") || fromToTemp[1].startsWith("3") ||
                    fromToTemp[0].startsWith("4") || fromToTemp[1].startsWith("4") ||
                    fromToTemp[0].startsWith("5") || fromToTemp[1].startsWith("5")) {
                info = getResources().getString(R.string.HotWeather);
                returnHotValue = true;
            }
            for (int i = 0; i < 4; i ++) { //雷电天气
                if (json.getString(checkDate[i]).equals("4") ||
                        json.getString(checkDate[i]).equals("5")) {
                    todayAndTomorrow[i/2] = true;
                }
            }
            for (int i = 0; i < 4; i ++) { //中雨以上天气
                if (json.getString(checkDate[i]).equals("6") ||
                        json.getString(checkDate[i]).equals("8") ||
                        json.getString(checkDate[i]).equals("9") ||
                        json.getString(checkDate[i]).equals("10") ||
                        json.getString(checkDate[i]).equals("11") ||
                        json.getString(checkDate[i]).equals("12") ||
                        json.getString(checkDate[i]).equals("22") ||
                        json.getString(checkDate[i]).equals("23") ||
                        json.getString(checkDate[i]).equals("24") ||
                        json.getString(checkDate[i]).equals("25")) {
                    todayAndTomorrow[i/2] = true;
                }
            }
            for (int i = 0; i < 4; i ++) { //中雪以上天气
                if (json.getString(checkDate[i]).equals("15") ||
                        json.getString(checkDate[i]).equals("16") ||
                        json.getString(checkDate[i]).equals("17") ||
                        json.getString(checkDate[i]).equals("27") ||
                        json.getString(checkDate[i]).equals("28")) {
                    todayAndTomorrow[i/2] = true;
                }
            }
            for (int i = 0; i < 4; i ++) { //沙尘天气
                if (json.getString(checkDate[i]).equals("20") ||
                        json.getString(checkDate[i]).equals("30") ||
                        json.getString(checkDate[i]).equals("31")) {
                    todayAndTomorrow[i/2] = true;
                }
            }
            if (todayAndTomorrow[0])
                info += " " + getResources().getString(R.string.Today) + weather1;
            if (todayAndTomorrow[1])
                info += " " + getResources().getString(R.string.Tomorrow) + weather2;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return (returnHotValue || todayAndTomorrow[0] || todayAndTomorrow[1]);
    }

    private Handler handler = new Handler(){
        @Override
        //当有消息发送出来的时候就执行Handler的这个方法
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            Bundle data = msg.getData();
            String val = data.getString("value");
            try {
                JSONObject json = new JSONObject(val).getJSONObject("weatherinfo");
                cityName = json.getString("city");
                date = json.getString("date_y") ;
                date = date + "("+json.getString("week")+")";
                temperature = json.getString("temp1");
                weather1 = json.getString("weather1");
                weather2 = json.getString("weather2");
                suggestion = json.getString("index_d");
                if (weatherChangeChecker(json)) {
                    pushDialog();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * 天气恶劣时的提示，同时还执行
     * 1、点亮屏幕
     * 2、在通知栏显示并震动
     * 弹出提示有3种选项
     * 1、确定并发送信息，同时停止监听服务
     * 2、取消此次发送并继续监听
     * 3、取消此次发送并停止监听
     */
    private void pushDialog() {
        lightOn();
        addNotification(cityName + getResources().getString(R.string.ReadyToSendMessage) + info,true,true);

        AlertDialog.Builder dialog = new AlertDialog.Builder(WeatherUpdateService.this);
        dialog.setTitle(cityName + getResources().getString(R.string.ReadyToSendMessage) + info);
        dialog.setMessage(getResources().getString(R.string.SendTo) + phoneNumber);

        EditText editText = new EditText(WeatherUpdateService.this);
        editText.setText(message);
        dialog.setView(editText);

        dialog.setPositiveButton(R.string.Send, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                sendMessage();

                //停止AlarmSysService定时提醒服务
                AlarmManager alarm = (AlarmManager)getSystemService(ALARM_SERVICE);
                Intent it = new Intent(WeatherUpdateService.this, AlarmReceiver.class);
                it.setAction(ACTION);
                PendingIntent alarmIntent = PendingIntent.getBroadcast(WeatherUpdateService.this, 0, it, 0);
                alarm.cancel(alarmIntent);

                handler = new Handler();
                handler.post(new Runnable(){
                    public void run(){
                        Toast.makeText(WeatherUpdateService.this, R.string.CancelSuccess,
                                Toast.LENGTH_SHORT).show();
                    }
                });

                //关闭自身服务
                stopSelf();
            }
        });
        dialog.setNegativeButton(R.string.CancelAndStopListening, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //停止AlarmSysService定时提醒服务
                AlarmManager alarm = (AlarmManager)getSystemService(ALARM_SERVICE);
                Intent it = new Intent(WeatherUpdateService.this, AlarmReceiver.class);
                it.setAction(ACTION);
                PendingIntent alarmIntent = PendingIntent.getBroadcast(WeatherUpdateService.this, 0, it, 0);
                alarm.cancel(alarmIntent);

                handler = new Handler();
                handler.post(new Runnable(){
                    public void run(){
                        Toast.makeText(WeatherUpdateService.this, R.string.CancelSuccess,
                                Toast.LENGTH_SHORT).show();
                    }
                });
                //关闭自身服务
                stopSelf();
            }
        });
        dialog.setNeutralButton(R.string.CancelAndContinueListening, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Do Nothing
            }
        });
        AlertDialog mDialog = dialog.create();
        mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        mDialog.show();
    }

    /**
     * 获得天气情况，并把得到的天气信息传给Handler进行JSON处理
     * 由于不能在主线程中连接网络故新建一个线程
     */
    private void doTask() {
        new Thread(){
            @Override
            public void run() {
                try{
                    String info = "http://m.weather.com.cn/data/"+ cityCode +".html";
                    String localWeatherInfo = new WebAccessTools().getWebContent(info);
                    Message msg = new Message();
                    Bundle data = new Bundle();
                    data.putString("value",localWeatherInfo);
                    msg.setData(data);
                    handler.sendMessage(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * 发送信息，同时根据信息发送情况弹出不同提示
     */
    private void sendMessage() {
        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";
        PendingIntent sentPI = PendingIntent.getBroadcast(WeatherUpdateService.this, 0,
                new Intent(SENT), 0);
        PendingIntent deliveredPI = PendingIntent.getBroadcast(WeatherUpdateService.this, 0,
                new Intent(DELIVERED), 0);

        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        handler = new Handler();
                        handler.post(new Runnable(){
                            public void run(){
                                Toast.makeText(WeatherUpdateService.this, R.string.SendSuccess,
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                        break;
                    default:
                        handler = new Handler();
                        handler.post(new Runnable(){
                            public void run(){
                                Toast.makeText(WeatherUpdateService.this, R.string.SendFailure,
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                        break;
                }
            }
        }, new IntentFilter(SENT));

        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        handler = new Handler();
                        handler.post(new Runnable(){
                            public void run(){
                                Toast.makeText(WeatherUpdateService.this, "SMS delivered",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                        break;
                    case Activity.RESULT_CANCELED:
                        handler = new Handler();
                        handler.post(new Runnable(){
                            public void run(){
                                Toast.makeText(WeatherUpdateService.this, "SMS not delivered",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                        break;
                }
            }
        }, new IntentFilter(DELIVERED));

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber,null,message,sentPI,deliveredPI);
    }

    /**
     * 点亮屏幕的方法
     * 原先还有解锁设定，但考虑到用户体验而取消
     */
    private void lightOn() {
        // 获取电源的服务
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        // 获取键盘系统服务
        //mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        // 点亮亮屏
        wakeLock = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        wakeLock.acquire();
        Log.i("Log : ", "------>mKeyguardLock");
        // 初始化键盘锁，可以锁定或解开键盘锁
        //mKeyguardLock = mKeyguardManager.newKeyguardLock("");
        // 禁用显示键盘锁定
        //mKeyguardLock.disableKeyguard();
    }

    /**
     * 通知栏设定方法
     * @param myMessage 通知栏显示的文字
     * @param sound 是否发出声音
     * @param vibrate 是否产生震动
     */
    public void addNotification(String myMessage,boolean sound,boolean vibrate){
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.small_launcher)
                        .setContentTitle(getResources().getString(R.string.CareHelper))
                        .setContentText(myMessage);

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, Main.class);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, resultIntent, 0);
        mBuilder.setContentIntent(contentIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        Notification notification = mBuilder.build();
        if(sound)
            notification.defaults |= Notification.DEFAULT_SOUND;
        if(vibrate)
            notification.defaults |= Notification.DEFAULT_VIBRATE;
        notification.flags|=Notification.FLAG_AUTO_CANCEL;

        mNotificationManager.notify(22, notification);
    }

    /**
     * 服务销毁时要释放唤醒手机Wakelock
     */
    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        wakeLock.release();
        super.onDestroy();
    }
}