package com.android.mb.mog;

import android.app.Application;
import android.content.Context;

import com.mob.MobSDK;

import cn.jpush.android.api.JPushInterface;


public class MBApplication extends Application {

    private static Context sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;// 初始化
        MobSDK.init(this);
        initJPush();
    }

    /**
     * 获取全局Application对象
     * @since 2013.08.02 修改错误提示内容为Application by pcqpcq
     * @return
     */
    public static Context getInstance(){
        if (sInstance == null) {
            throw new RuntimeException("Application must be init");
        }
        return sInstance;
    }

    private void initJPush(){
        JPushInterface.setDebugMode(true); 	// 设置开启日志,发布时请关闭日志
        JPushInterface.init(this);
    }

}
