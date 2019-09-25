package com.android.mb.mog;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import cn.jpush.android.api.JPushInterface;


/**
 * 起始页
 *
 * @author @author chenqm on 2018/1/15.
 */

public class SplashActivity extends Activity{

    //SharedPreferences
    public static final String KEY_REGISTRATION_ID = "KEY_REGISTRATION_ID";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        getRegisterId();
        new Handler().postDelayed(new Runnable() {

            public void run() {
                startActivity(new Intent(SplashActivity.this,MainActivity.class));
                finish();
            }

        }, 1500);
    }


    private void getRegisterId(){
        String rid = JPushInterface.getRegistrationID(getApplicationContext());
        if (Helper.isNotEmpty(rid)) {
            PreferencesHelper.getInstance().putString(KEY_REGISTRATION_ID,rid);
            Log.d("jpush rid:",rid);
        }
    }
}
