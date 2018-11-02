package com.android.mb.mog;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;



/**
 * 起始页
 *
 * @author @author chenqm on 2018/1/15.
 */

public class SplashActivity extends Activity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        new Handler().postDelayed(new Runnable() {

            public void run() {
                startActivity(new Intent(SplashActivity.this,MainActivity.class));
                finish();
            }

        }, 1500);
    }


}
