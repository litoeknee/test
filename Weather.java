package com.example.tester;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by toeknee on 13-8-1.
 */
public class Weather extends Activity {
    private Spinner citySpinner;
    private Spinner provinceSpinner;
    private ImageButton contactButton;
    private ImageButton checkButton;
    private EditText phoneNumber;
    private EditText message;

    private ArrayAdapter<String> cityAdapter;
    private ArrayAdapter<String> provinceAdapter;

    private String[] provinces;
    private String[][] city;
    private String weatherInfo;
    private String cityName;
    final private String TAG = "CareHelper";

    private Intent intentService;

    /**
     * 初始化将资源文件raw目录下的数据库文件导入系统数据库
     * 在/data/data/com.example.tester/databases 目录下创建同名系统数据库
     */
    public void importInitDatabase() {
        //数据库的目录
        String dirPath="/data/data/com.example.tester/databases";
        File dir = new File(dirPath);
        if(!dir.exists()) {
            dir.mkdir();
        }
        //数据库文件
        File databaseFile = new File(dir, getResources().getString(R.string.DatabaseName));
        try {
            if(!databaseFile.exists()) {
                databaseFile.createNewFile();
            }
            //加载欲导入的数据库
            InputStream is = this.getApplicationContext().getResources().openRawResource(R.raw.db_weather);
            FileOutputStream fos = new FileOutputStream(databaseFile);
            byte[] buffer=new byte[is.available()];
            is.read(buffer);
            fos.write(buffer);
            is.close();
            fos.close();
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }catch(IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Activity创建时调用，初始化UI界面及数据库调用
     * @param savedInstanceState 若之前意外关闭/进入后台，则可从保存的bundle中恢复数据
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        intentService = new Intent(Weather.this, AlarmSysService.class);
        citySpinner = (Spinner)findViewById(R.id.citySpinner);
        provinceSpinner = (Spinner)findViewById(R.id.provinceSpinner);
        contactButton = (ImageButton)findViewById(R.id.contactButton);
        checkButton = (ImageButton)findViewById(R.id.checkButton);
        phoneNumber = (EditText)findViewById(R.id.phoneNumber);
        message = (EditText)findViewById(R.id.message);

        importInitDatabase();
        final DBHelper dbHelper = new DBHelper(Weather.this, getResources().getString(R.string.DatabaseName));
        Log.d("CareHelper", "Get provinces start!");
        provinces = dbHelper.getAllProvinces();
        Log.d("CareHelper", "Get provinces end!");

        //将可选内容与ArrayAdapter连接
        provinceAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,provinces);
        //设置下拉列表风格
        provinceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //将adapter添加到spinner中
        provinceSpinner.setAdapter(provinceAdapter);

        provinceSpinner.setOnItemSelectedListener(provinceSpinnerListener);
        citySpinner.setOnItemSelectedListener(citySpinnerListener);
        contactButton.setOnTouchListener(imageButtonOnTouchListener);
        checkButton.setOnTouchListener(imageButtonOnTouchListener);
        provinceSpinner.setOnTouchListener(spinnerOnTouchListener);
        citySpinner.setOnTouchListener(spinnerOnTouchListener);
        if(null != savedInstanceState)
        {
            phoneNumber.setText(savedInstanceState.getString("phone"));
            message.setText(savedInstanceState.getString("message"));
            provinceSpinner.setSelection(savedInstanceState.getInt("province"));

            cityAdapter = new ArrayAdapter<String>(Weather.this,
                    android.R.layout.simple_spinner_item,city[provinceSpinner.getSelectedItemPosition()]);
            cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            citySpinner.setAdapter(cityAdapter);
            citySpinner.setSelection(savedInstanceState.getInt("city"));
        }

        List<String[][]> result = dbHelper.getAllCityAndCode(provinces);
        city = result.get(0);
    }

    /**
     * 当 Activity
     * 1、当用户按下HOME键时
     * 2、长按HOME键，选择运行其他的程序时。
     * 3、按下电源按键（关闭屏幕显示）时。
     * 4、从activity A中启动一个新的activity时。
     * 5、屏幕方向切换时，
     * 调用，保存UI界面上的内容
     * @param savedInstanceState 将数据存入该Bundle中
     */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save away the original text, so we still have it if the activity
        // needs to be killed while paused.
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("province", provinceSpinner.getSelectedItemPosition());
        savedInstanceState.putInt("city", citySpinner.getSelectedItemPosition());
        savedInstanceState.putString("phone", phoneNumber.getText().toString());
        savedInstanceState.putString("message", message.getText().toString());
        /*Toast.makeText(getApplicationContext(),provinceSpinner.getSelectedItemPosition() + " "
                + citySpinner.getSelectedItemPosition() + " "
                + phoneNumber.getText().toString() + " "
                + message.getText().toString() + " ",Toast.LENGTH_LONG).show();*/

        Log.e(TAG, "onSaveInstanceState");
    }

    private AdapterView.OnItemSelectedListener provinceSpinnerListener = new Spinner.OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView <?> arg0, View arg1,
                                   int arg2, long arg3) {
            // TODO Auto-generated method stub
            arg0.setVisibility(View.INVISIBLE);
            TextView tv = (TextView) arg1;
            tv.setTextSize(22f);
            tv.setTextColor(Color.WHITE);
            Log.d("CareHelper", "Get cities" + arg2 + " " + arg3);

            cityAdapter = new ArrayAdapter<String>(Weather.this,android.R.layout.simple_spinner_item,city[arg2]);
            cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            citySpinner.setAdapter(cityAdapter);

            arg0.setVisibility(View.VISIBLE);
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub
        }
    };

    private AdapterView.OnItemSelectedListener citySpinnerListener = new Spinner.OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView <?> arg0, View arg1,
                                    int arg2, long arg3) {
            // TODO Auto-generated method stub
            TextView tv = (TextView) arg1;
            tv.setTextSize(22f);
            tv.setTextColor(Color.WHITE);
            cityName = ((TextView) arg1).getText().toString();
            Log.d("Weather", "Get city name: " + cityName);
            DBHelper dbHelper = new DBHelper(Weather.this, "db_weather.db");
            String cityCode = dbHelper.getCityCodeByName(cityName);
            Log.d("Weather", "Get city code: " + cityCode);
            setWeatherSituation(cityCode);

            arg1.setVisibility(View.VISIBLE);
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub

        }
    };

    public void onClickContact(View view) {
        Intent i = new Intent(Intent.ACTION_PICK);
        i.setType("vnd.android.cursor.dir/phone");
        startActivityForResult(i, 0);
    }

    /**
     * 当之前设定Intent让其他程序返回结果时调用
     * @param requestCode 之前设定的Intent ID。识别为何应用返回
     * @param resultCode
     * @param data 返回带有数据的Intent
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 0:
                if (data == null) {
                    Log.d("GetContact", "No");
                    return;
                }
                Uri uri = data.getData();
                Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                cursor.moveToFirst();
                String number = cursor.getString(cursor.getColumnIndexOrThrow("number"));
                Log.d("GetContact", "number" + number);
                phoneNumber.setText(number);
                phoneNumber.setSelection(number.length());
                break;
            default:
                break;
        }
    }

    public void onClickCheck(View view) {
        stopService(intentService);

        String mobile = phoneNumber.getText().toString().trim();
        String content = message.getText().toString();
        if (validate(mobile, content)) {
            pushAlertDialog(mobile, content);
        }
    }

    /**
     * 弹出提示窗口，显示是否发送信息content给mobile
     * @param content 信息内容
     * @param mobile 对方手机号码
     */
    public void pushAlertDialog(final String mobile, final String content) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(cityName.toString() + getResources().getString(R.string.WhenWeatherChange));
        dialog.setMessage(mobile);

        TextView textView = new TextView(this);
        textView.setText(content);
        textView.setTextColor(Color.DKGRAY);
        dialog.setView(textView);

        dialog.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Bundle msg = new Bundle();
                Log.d("Weather", "Get city name: " + cityName);

                DBHelper dbHelper = new DBHelper(Weather.this, "db_weather.db");
                msg.putString("cityCode", dbHelper.getCityCodeByName(cityName));
                msg.putString("phoneNumber", mobile);
                msg.putString("message", content);

                Log.d("Weather", "Get city code: " + msg);
                Toast.makeText(getApplicationContext(),R.string.SetOK,Toast.LENGTH_LONG).show();

                intentService.putExtras(msg);
                startService(intentService);
            }
        });
        dialog.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //Do nothing
            }
        });
        AlertDialog mDialog = dialog.create();
        mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        mDialog.show();
    }

    //定义Handler对象
    private Handler handler = new Handler(){
        @Override
        //当有消息发送出来的时候就执行Handler的这个方法
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            //处理UI
            Bundle data = msg.getData();
            String val = data.getString("value");
            weatherInfo = val;
            try {
                //==========================解析JSON得到天气===========================
                JSONObject json = new JSONObject(weatherInfo).getJSONObject("weatherinfo");
                //得到城市
                String cityName = json.getString("city");
                //得到阳历日期
                String date = json.getString("date_y");
                //得到温度
                String temperature= json.getString("temp1");
                //得到天气
                String weather= json.getString("weather1");
                //得到建议
                String suggestion= json.getString("index_d");
                String changeLine = System.getProperty("line.separator");
                message.setText(cityName + changeLine
                        + temperature + "  "
                        + weather + changeLine
                        + suggestion);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * 由城市码获得天气情况，由于不能再主线程中连接网络故新建一个线程
     * @param cityCode 城市代码
     */
    public void setWeatherSituation(final String cityCode) {
        new Thread(){
            @Override
            public void run(){
                String info = "http://m.weather.com.cn/data/"+ cityCode +".html";
                String localWeatherInfo = new WebAccessTools().getWebContent(info);
                Message msg = new Message();
                Bundle data = new Bundle();
                data.putString("value",localWeatherInfo);
                msg.setData(data);
                handler.sendMessage(msg);
            }
        }.start();
    }

    /**
     * 短信内容合法性验证，内容包括：
     * 1、判断手机号码是否为空并弹出Toast警告
     * 2、判断短信内容是否为空并弹出Toast警告
     * 3、判断手机号码是否符合大陆要求，由checkMobile方法实现
     * @param mobile 手机号码
     * @param content 短信内容
     */
    private boolean validate(String mobile, String content){
        if(mobile.equals("")){
            Toast toast=Toast.makeText(getApplicationContext(), R.string.PhoneNumberBlank,Toast.LENGTH_LONG);
            toast.show();
            return false;
        } else if(!checkMobile(mobile)){
            Toast toast=Toast.makeText(getApplicationContext(), R.string.PhoneNumberError,Toast.LENGTH_LONG);
            toast.show();
            return false;
        } else if(content.equals("")){
            Toast toast=Toast.makeText(getApplicationContext(), R.string.ContentBlank,Toast.LENGTH_LONG);
            toast.show();
            return false;
        }else{
            return true;
        }
    }

    /**
     * 中国移动134.135.136.137.138.139.150.151.152.157.158.159.187.188 ,147(数据卡不验证)
     * 中国联通130.131.132.155.156.185.186
     * 中国电信133.153.180.189
     * CDMA   133,153
     * 手机号码验证 适合目前所有的手机
     * @param mobile 手机号码
     */
    public boolean checkMobile(String mobile){
        if (mobile.startsWith("+86"))
            mobile = mobile.substring(3);
        mobile = mobile.replaceAll("-","");
        String regex = "^1(3[0-9]|5[012356789]|8[0789])\\d{8}$";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(mobile);
        return m.find();
    }

    private View.OnTouchListener imageButtonOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            ImageButton imageButton = (ImageButton)v;
            Vibrator vv = (Vibrator)getApplication().getSystemService(Service.VIBRATOR_SERVICE);
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                imageButton.getDrawable().setAlpha(150);//设置图片透明度0~255，0完全透明，255不透明
                imageButton.invalidate();
                vv.vibrate(10000);//震到放开
            } else {
                imageButton.getDrawable().setAlpha(255);//还原图片
                imageButton.invalidate();
                vv.cancel();
            }
            return false;
        }
    };

    private View.OnTouchListener spinnerOnTouchListener= new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            Vibrator vv = (Vibrator)getApplication().getSystemService(Service.VIBRATOR_SERVICE);
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                vv.vibrate(10000);//震到放开
            } else {
                vv.cancel();
            }
            return false;
        }
    };
}