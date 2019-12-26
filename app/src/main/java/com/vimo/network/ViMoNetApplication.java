package com.vimo.network;

import android.app.Application;

import com.vimo.network.helper.Logger;
import com.vimo.network.manager.DataManager;

import androidx.appcompat.app.AppCompatActivity;

/**
 * File created by vimo on 28/03/18.
 */

public class ViMoNetApplication extends Application {

    private static ViMoNetApplication myApplication = null;
    private AppCompatActivity currentActivity = null;
    protected String vimoKey = "";
    protected String preferenceName = "";

    public static ViMoNetApplication getApplication() {
        return myApplication;
    }

    public String getVimoKey() {
        return vimoKey;
    }

    public String getPreferenceName() {
        return preferenceName;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        myApplication = this;
        Logger.setApplicationContext(this);
        DataManager.setApplicationContext(this);
    }

    @Override
    public void onTerminate() {
        Logger.getLogger().writeLogIntoFile(true);
        super.onTerminate();
    }

    public AppCompatActivity getCurrentActivity() {
        return currentActivity;
    }

    public void setCurrentActivity(AppCompatActivity currentActivity) {
        this.currentActivity = currentActivity;
    }
}
