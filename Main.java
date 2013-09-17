package com.example.tester;
GitHub Testing And More
import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.os.Vibrator;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;

public class Main extends Activity {
    private ImageButton weatherButton;
    private ImageButton locationButton;

    /**
     * 根据给定的url地址访问网络，得到响应内容(这里为GET方式访问)
     * @param savedInstanceState
     * @return web服务器响应的内容，为<code>String</code>类型，当访问失败时，返回为null
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        weatherButton = (ImageButton)findViewById(R.id.weatherButton);
        locationButton = (ImageButton)findViewById(R.id.locationButton);
        weatherButton.setOnTouchListener(imageButtonOnTouchListener);
        locationButton.setOnTouchListener(imageButtonOnTouchListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void onClickLocation(View view) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Sorry~");
        dialog.setMessage(getResources().getString(R.string.StillWorking));

        ImageView imageView = new ImageView(this);
        imageView.setImageResource(R.drawable.stillworking);
        dialog.setView(imageView);

        dialog.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //Do Nothing
            }
        });
        AlertDialog mDialog = dialog.create();
        mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        mDialog.show();
    }

    public void onClickWeather(View view) {
        Intent it = new Intent();
        it.setClass(Main.this, Weather.class);
        startActivity(it);
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
}