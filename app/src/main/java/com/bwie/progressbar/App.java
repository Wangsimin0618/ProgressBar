package com.bwie.progressbar;

import android.app.Application;
import android.content.Context;

import com.umeng.analytics.MobclickAgent;
import com.umeng.commonsdk.UMConfigure;


/**
 * date: 2019/1/18.
 * Created by Administrator
 * function:
 */
public class App extends Application {
    private Context context;
    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        UMConfigure.init(context, "5c413464b465f59eae0012f7", "小米", UMConfigure.DEVICE_TYPE_PHONE, null);
        MobclickAgent.setScenarioType(context, MobclickAgent.EScenarioType.E_UM_NORMAL);
    }
}
