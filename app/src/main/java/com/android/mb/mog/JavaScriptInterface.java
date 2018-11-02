package com.android.mb.mog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.alipay.sdk.app.PayTask;
import com.android.mb.mog.alipay.PayResult;
import com.android.mb.mog.wxpay.WXPayUtils;

import org.json.JSONObject;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.PlatformActionListener;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.onekeyshare.OnekeyShare;
import cn.sharesdk.tencent.qq.QQ;
import cn.sharesdk.wechat.friends.Wechat;

public class JavaScriptInterface {

    private Context mContext;

    private JsCallbackHandler mCallbackHandler;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @SuppressWarnings("unused")
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SDK_PAY_FLAG: {
                    @SuppressWarnings("unchecked")
                    PayResult payResult = new PayResult((Map<String, String>) msg.obj);
                    String resultInfo = payResult.getResult();// 同步返回需要验证的信息
                    String resultStatus = payResult.getResultStatus();
                    // 判断resultStatus 为9000则代表支付成功
                    if (TextUtils.equals(resultStatus, "9000")) {
                        // 该笔订单是否真实支付成功，需要依赖服务端的异步通知。
                        mCallbackHandler.aliPayResult(1);
                        Log.d("aliPay","支付成功: " + payResult);
                    } else {
                        // 该笔订单真实的支付结果，需要依赖服务端的异步通知。
                        mCallbackHandler.aliPayResult(0);
                        Log.d("aliPay","支付失败: " + payResult);
                    }
                    break;
                }
                default:
                    break;
            }
        };
    };

    private static final int SDK_PAY_FLAG = 1;
    public interface JsCallbackHandler {
        void onLoginComplete(String plat,JSONObject jsonObject);

        void aliPayResult(int code);
    }

    public JavaScriptInterface(Context context,JsCallbackHandler callbackHandler) {
        mContext = context;
        mCallbackHandler = callbackHandler;
    }

    @JavascriptInterface
    public void wxlogin() {
        doLogin(Wechat.NAME);
    }


    @JavascriptInterface
    public void qqlogin() {
        doLogin(QQ.NAME);
    }

//    @JavascriptInterface
//    public void share(String json) {
//        try {
//            if (!TextUtils.isEmpty(json)){
//                JSONObject jsonObject = new JSONObject(json);
//                String title = jsonObject.getString("title");
//                String content = jsonObject.getString("content");
//                String url = jsonObject.getString("url");
//                String imageUrl = jsonObject.getString("imageUrl");
//                showShare(title,content,url,imageUrl);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    @JavascriptInterface
    public void share(String title,String content,String url,String imgurl) {
        showShare(title,content,url,imgurl);
    }


    @JavascriptInterface
    public void wechatpay(String orderInfo) {
        try {
            if (!TextUtils.isEmpty(orderInfo)){
                String json = URLDecoder.decode(orderInfo);
                JSONObject jsonObject = new JSONObject(json);
                String AppId = jsonObject.getString("AppId");
                String NonceStr = jsonObject.getString("NonceStr");
                String Package = jsonObject.getString("Package");
                String PartnerId = jsonObject.getString("PartnerId");
                String PrepayId = jsonObject.getString("PrepayId");
                String Sign = jsonObject.getString("Sign");
                String TimeStamp = jsonObject.getString("TimeStamp");
                doWxPay(AppId,NonceStr,Package,PartnerId,PrepayId,Sign,TimeStamp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void alipay(String orderInfo) {
        try {
            doAliPay(orderInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doLogin(final String plat){
        Platform platform = ShareSDK.getPlatform(plat);
        platform.setPlatformActionListener(new PlatformActionListener() {

            @Override
            public void onError(Platform arg0, int arg1, Throwable arg2) {
                arg2.printStackTrace();
            }

            @Override
            public void onComplete(Platform arg0, int arg1, HashMap<String, Object> arg2) {
                if (arg0!=null && arg0.getDb()!=null && !TextUtils.isEmpty(arg0.getDb().exportData())){
                    String userInfo = arg0.getDb().exportData();
                    try {
                        JSONObject jsonObject = new JSONObject(userInfo);
                        mCallbackHandler.onLoginComplete(plat,jsonObject);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onCancel(Platform arg0, int arg1) {

            }
        });
        platform.authorize();
    }


    private void showShare(String title,String content,String url,String imageUrl) {
        OnekeyShare oks = new OnekeyShare();
        //关闭sso授权
        oks.disableSSOWhenAuthorize();
        // title标题，微信、QQ和QQ空间等平台使用
        oks.setTitle(title);
        // titleUrl QQ和QQ空间跳转链接
        oks.setTitleUrl(url);
        // text是分享文本，所有平台都需要这个字段
        oks.setText(content);
        // url在微信、微博，Facebook等平台中使用
        oks.setUrl(url);
        oks.setImageUrl(imageUrl);
        // 启动分享GUI
        oks.show(mContext);
    }

    private void doWxPay(String AppId,String NonceStr,String Package,String PartnerId,String PrepayId,String Sign,String TimeStamp){
        WXPayUtils.WXPayBuilder builder = new WXPayUtils.WXPayBuilder();
        builder.setAppId(AppId)
                .setPartnerId(PartnerId)
                .setPrepayId(PrepayId)
                .setPackageValue(Package)
                .setNonceStr(NonceStr)
                .setTimeStamp(TimeStamp)
                .setSign(Sign)
                .build().toWXPayNotSign(mContext);
    }

    private void doAliPay(final String orderInfo){
        Runnable payRunnable = new Runnable() {

            @Override
            public void run() {
                PayTask aliPay = new PayTask((Activity) mContext);
                Map<String, String> result = aliPay.payV2(orderInfo, true);
                Message msg = new Message();
                msg.what = SDK_PAY_FLAG;
                msg.obj = result;
                mHandler.sendMessage(msg);
            }
        };

        Thread payThread = new Thread(payRunnable);
        payThread.start();
    }

}
