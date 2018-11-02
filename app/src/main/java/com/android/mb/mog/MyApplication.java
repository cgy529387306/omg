package com.android.mb.mog;

import android.app.Application;
import android.content.Context;

import com.mob.MobSDK;


public class MyApplication extends Application {

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;// 初始化
        MobSDK.init(this);
    }

    public static Context getAppContext() {
        return  mContext;
    }
}
