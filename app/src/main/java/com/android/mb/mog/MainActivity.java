package com.android.mb.mog;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.alipay.sdk.app.H5PayCallback;
import com.alipay.sdk.app.PayTask;
import com.alipay.sdk.util.H5PayResultModel;
import com.android.mb.mog.download.Constant;
import com.android.mb.mog.download.DownloadInfo;
import com.android.mb.mog.download.DownloadManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import cn.jpush.android.api.JPushInterface;
import cn.sharesdk.tencent.qq.QQ;
import cn.sharesdk.wechat.friends.Wechat;

public class MainActivity extends AppCompatActivity implements JavaScriptInterface.JsCallbackHandler{
    private WebView webView;
    private String webUrl = "http://manougou.5979wenhua.com/mobile/index.html";
//    private String webUrl = "http://manougou.5979wenhua.com/api/down.html";
    public static final int FILE_CHOOSER_RESULT_CODE = 5173;
    private ValueCallback<Uri> uploadMessage;
    private ValueCallback<Uri[]> uploadMessageAboveL;
    private LocalBroadcastManager mLocalBroadcastManager;
    private DownloadInfo downloadInfo;
    private MaterialDialog mMaterialDialog;
    /**
     * 更新用户信息广播接受者
     */
    private BroadcastReceiver mReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            String jsStr = "javascript:aliPayComplete()";
            loadJs(jsStr);
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        mLocalBroadcastManager.unregisterReceiver(mReceiver);
        webView.destroy();
        webView = null;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EventBus.getDefault().register(this);
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mLocalBroadcastManager.registerReceiver(mReceiver, new IntentFilter("WXPay_Result"));
        initWebView();
        setWebChromeClient();
    }

    private void showDialog(){
        mMaterialDialog = new MaterialDialog.Builder(this)
                .title("下载")
                .content("下载中，请稍等...")
                .progress(true, 0)
                .progressIndeterminateStyle(true)
                .show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void update(DownloadInfo info){
        if (DownloadInfo.DOWNLOAD.equals(info.getDownloadStatus())){
            downloadInfo = info;
            if (info.getTotal() == 0){
                mMaterialDialog.setProgress(0);
            }else{
                float progress = info.getProgress() * mMaterialDialog.getMaxProgress() / info.getTotal();
                mMaterialDialog.setProgress((int) progress);
            }
        }else if (DownloadInfo.DOWNLOAD_OVER.equals(info.getDownloadStatus())){
            Toast.makeText(this,"下载成功",Toast.LENGTH_SHORT).show();
            mMaterialDialog.setProgress(mMaterialDialog.getMaxProgress());
            mMaterialDialog.dismiss();
            try {
                //刷新相册
                File imgFile = new File(Constant.FILE_PATH, info.getFileName());
                if (imgFile.exists()) {
                    Uri uri = Uri.fromFile(imgFile);
                    Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    intent.setData(uri);
                    sendBroadcast(intent);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }else if (DownloadInfo.DOWNLOAD_PAUSE.equals(info.getDownloadStatus())){
            Toast.makeText(this,"下载暂停",Toast.LENGTH_SHORT).show();
        }else if (DownloadInfo.DOWNLOAD_CANCEL.equals(info.getDownloadStatus())){
            Toast.makeText(this,"下载取消",Toast.LENGTH_SHORT).show();
        }else if (DownloadInfo.DOWNLOAD_ERROR.equals(info.getDownloadStatus())){
            Toast.makeText(this,"下载出错",Toast.LENGTH_SHORT).show();
        }
    }


    private WebViewClient webViewClient = new WebViewClient() {
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode,
                                    String description, String failingUrl) {
            webView.loadUrl("about:blank");
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler,
                                       SslError error) {
            handler.proceed();
        }

        @Override
        public boolean shouldOverrideUrlLoading(final WebView view, String url) {
            if (TextUtils.isEmpty(url)) {
                return true;
            }
            final PayTask task = new PayTask(MainActivity.this);
            boolean isIntercepted = task.payInterceptorWithUrl(url, true, new H5PayCallback() {
                @Override
                public void onPayResult(final H5PayResultModel result) {
                    final String url = result.getReturnUrl();
                    if(!TextUtils.isEmpty(url)){
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                view.loadUrl(url);
                            }
                        });
                    }
                }
            });
            if(!isIntercepted){
                view.loadUrl(url);
            }
            return true;
        }
    };


    // region 双击返回
    private static final long DOUBLE_CLICK_INTERVAL = 2000;
    private long mLastClickTimeMills = 0;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) ) {
            if (webView.canGoBack()) {
                webView.goBack(); //goBack()表示返回WebView的上一页面
                return true;
            }else {
                if (System.currentTimeMillis() - mLastClickTimeMills > DOUBLE_CLICK_INTERVAL) {
                    Toast.makeText(MainActivity.this,"再按一次返回退出",Toast.LENGTH_SHORT).show();
                    mLastClickTimeMills = System.currentTimeMillis();
                    return true;
                }
                finish();
                return true;
            }
        }
        return false;
    }


    @RequiresApi(api = Build.VERSION_CODES.ECLAIR_MR1)
    @SuppressLint({"CommitPrefEdits", "AddJavascriptInterface", "SetJavaScriptEnabled"})
    private void initWebView(){
        webView = (WebView) findViewById(R.id.webView);
        webView.setWebViewClient(webViewClient);
        WebSettings webSettings = webView.getSettings();
        webSettings.setSavePassword(true);
        webSettings.setSaveFormData(true);
        webSettings.setJavaScriptEnabled(true);//允许使用js
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        Map<String,String> headerMap = new HashMap<>();
        headerMap.put("deviceType","1");
        webView.loadUrl(webUrl,headerMap);
        webView.addJavascriptInterface(new JavaScriptInterface(this,this),
                "android");
    }



    private void setWebChromeClient(){
        webView.setWebChromeClient(new WebChromeClient(){
            // For 3.0+ Devices (Start)
            protected void openFileChooser(ValueCallback uploadMsg, String acceptType) {
                uploadMessage = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image");
                startActivityForResult(Intent.createChooser(i, "File Browser"), FILE_CHOOSER_RESULT_CODE);
            }
            // For Lollipop 5.0+ Devices
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            public boolean onShowFileChooser(WebView mWebView,
                                             ValueCallback<Uri[]> filePathCallback,
                                             WebChromeClient.FileChooserParams fileChooserParams) {

                if (uploadMessageAboveL != null) {
                    uploadMessageAboveL.onReceiveValue(null);
                    uploadMessageAboveL = null;
                }
                uploadMessageAboveL = filePathCallback;
                Intent intent = fileChooserParams.createIntent();
                try {
                    startActivityForResult(intent, FILE_CHOOSER_RESULT_CODE);
                } catch (ActivityNotFoundException e) {
                    uploadMessageAboveL = null;
                    Toast.makeText(getBaseContext(), "Cannot Open File Chooser", Toast.LENGTH_LONG).show();
                    return false;
                }
                return true;
            }

            //For Android 4.1 only
            protected void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                uploadMessage = uploadMsg;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image");
                startActivityForResult(Intent.createChooser(intent, "File Browser"), FILE_CHOOSER_RESULT_CODE);
            }

            //for Android <3.0
            protected void openFileChooser(ValueCallback<Uri> uploadMsg) {
                uploadMessage = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image");
                startActivityForResult(Intent.createChooser(i, "File Chooser"), FILE_CHOOSER_RESULT_CODE);
            }

        });
    }

    @Override
    public void onLoginComplete(String plat, JSONObject jsonObject) {
        if (jsonObject!=null){
            if (Wechat.NAME.equals(plat)){
                wxLoginComplete(jsonObject);
            }else if (QQ.NAME.equals(plat)){
                qqLoginComplete(jsonObject);
            }
        }
    }

    @Override
    public void aliPayResult(int code) {
        //1:成功 0:失败
        String jsStr = "javascript:aliPayComplete()";
        loadJs(jsStr);
    }

    @Override
    public void loginSuccess() {
        String rid = PreferencesHelper.getInstance().getString(SplashActivity.KEY_REGISTRATION_ID);
        if (Helper.isEmpty(rid)){
            rid = JPushInterface.getRegistrationID(getApplicationContext());
        }
        String jsStr = "javascript: jiguanreg('" + rid + "')";
        loadJs(jsStr);
    }

    @Override
    public void downLoadVideo(String url) {
        if (Helper.isNotEmpty(url)){
            showDialog();
            DownloadManager.getInstance().download(url);
        }
    }

    @Override
    public void downLoadImage(String url) {
        if (Helper.isNotEmpty(url)){
            showDialog();
            DownloadManager.getInstance().download(url);
        }
    }

    private void wxLoginComplete(JSONObject jsonObject){
        JSONObject jsonObj = new JSONObject();
        try {
            HashMap<String,String> userInfo = new HashMap<>();
            userInfo.put("nickname",jsonObject.getString("nickname"));
            userInfo.put("unionid",jsonObject.getString("unionid"));
            userInfo.put("headingurl",jsonObject.getString("icon"));
            userInfo.put("token",jsonObject.getString("token"));
            jsonObj = new JSONObject(userInfo);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String jsStr = "javascript:wxLoginComplete('" + jsonObj.toString() + "')";
        loadJs(jsStr);
    }

    private void qqLoginComplete(JSONObject jsonObject){
        JSONObject jsonObj = new JSONObject();
        try {
            HashMap<String,String> userInfo = new HashMap<>();
            userInfo.put("nickname",jsonObject.getString("nickname"));
            userInfo.put("openid",jsonObject.getString("userID"));
            userInfo.put("figureurl_1",jsonObject.getString("icon"));
            userInfo.put("figureurl_2",jsonObject.getString("iconQzone"));
            userInfo.put("token",jsonObject.getString("token"));
            jsonObj = new JSONObject(userInfo);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String jsStr = "javascript:qqLoginComplete('" + jsonObj.toString() + "')";
        loadJs(jsStr);
    }

    private void loadJs(final String jsStr){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl(jsStr);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.ECLAIR_MR1)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode == FILE_CHOOSER_RESULT_CODE) {
                if (uploadMessageAboveL == null)
                    return;
                uploadMessageAboveL.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, intent));
                uploadMessageAboveL = null;
            }
        } else if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (null == uploadMessage)
                return;
            // Use MainActivity.RESULT_OK if you're implementing WebView inside Fragment
            // Use RESULT_OK only if you're implementing WebView inside an Activity
            Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
            uploadMessage.onReceiveValue(result);
            uploadMessage = null;
        } else {
            Toast.makeText(getBaseContext(), "Failed to Upload Image", Toast.LENGTH_LONG).show();
        }
    }


}
