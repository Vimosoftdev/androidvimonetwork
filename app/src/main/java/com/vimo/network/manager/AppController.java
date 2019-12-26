package com.vimo.network.manager;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;

/**
 * File created by ViMo Software Development Pvt Ltd on 2019-12-24.
 */
public class AppController {
    private static final AppController ourInstance = new AppController();

    public static AppController getInstance() {
        return ourInstance;
    }

    private AppController() {
    }

    public static boolean isRunningInForeground() {
        ActivityManager.RunningAppProcessInfo appProcessInfo = new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(appProcessInfo);
        return (appProcessInfo.importance == IMPORTANCE_FOREGROUND || appProcessInfo.importance == IMPORTANCE_VISIBLE);
    }

    /**
     * Method to check the app permission status
     *
     * @param permission permission properly
     * @return returns true if the permission is not granted. else false.
     */
    private boolean permissionNotGranted(final Activity activity, final String permission) {
        return (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED);
    }
}
