package com.vimo.network.manager;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;

import java.io.File;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * File created by vimo on 28/03/18.
 */

public class DataManager {
    private static final DataManager instanceManager = new DataManager();
    private static Context applicationContext;
    private static String vimoKey = null;
    private static String myDeviceId;
    private static String versionName = null;
    private static int buildNumber = -1;

    public static DataManager getManager() {
        return instanceManager;
    }

    private DataManager() {
    }

    public static void setApplicationContext(Context applicationContext) {
        DataManager.applicationContext = applicationContext;
    }

    public static void setVimoKey(String vimoKey) {
        DataManager.vimoKey = vimoKey;
    }

    public static String getSystemLanguage() {
        return Locale.getDefault().getLanguage();
    }

    /**
     * Method to get application version detail
     * @return  returns app version info
     */
    public static String getAppVersionNumber() {
        if (versionName == null) {
            try {
                PackageInfo pInfo = applicationContext.getPackageManager().getPackageInfo(applicationContext.getPackageName(), 0);
                return pInfo.versionName;
            } catch (Exception e) {
                return null;
            }
        } else {
            return versionName;
        }
    }

    /**
     * Method to get application build no
     * @return  returns app build version info
     */
    public static int getBuildNumber() {
        if (buildNumber == -1) {
            try {
                PackageInfo pInfo = applicationContext.getPackageManager().getPackageInfo(applicationContext.getPackageName(), 0);
                return pInfo.versionCode;
            } catch (Exception e) {
                return -1;
            }
        } else {
            return buildNumber;
        }
    }

    /**
     * Method to get device ID
     * @return          it returns device IMEI number
     */
    public static String getDeviceId() {
        if (myDeviceId == null || myDeviceId.trim().length() == 0) {
            myDeviceId = Settings.Secure.getString(applicationContext.getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        return myDeviceId;
    }

    public static boolean isRightToLeft() {
        String language = getSystemLanguage();
        return (language != null && (language.equalsIgnoreCase("ar") || language.equalsIgnoreCase("fa")));
    }

    @SuppressWarnings("deprecation")
    public static long getAvailableInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize;
        long availableBlocks;
        if (Build.VERSION.SDK_INT >= 18) {
            blockSize = stat.getBlockSizeLong();
            availableBlocks = stat.getAvailableBlocksLong();
        } else {
            blockSize = stat.getBlockSize();
            availableBlocks = stat.getAvailableBlocks();
        }
        return (availableBlocks * blockSize);
    }

    public static boolean hasFreeSpace() {
        long available = getAvailableInternalMemorySize();
        return (available > 2000);
    }

    public String getFormattedDate(double timestamp) {
        Calendar cal = Calendar.getInstance(Locale.UK);
        cal.setTimeInMillis((long)timestamp * 1000);
        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, Locale.GERMAN);
        return df.format(cal.getTime());
    }

    public String getFormattedTime(double timestamp) {
        Calendar cal = Calendar.getInstance(Locale.UK);
        cal.setTimeInMillis((long)timestamp * 1000);
        DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.UK);
        return df.format(cal.getTime());
    }

//    public int getDifferenceInDays(double timestamp) {
//        if (timestamp == 0) {
//            return 0;
//        }
//        Calendar now = Calendar.getInstance();
//        now.setTimeZone(TimeZone.getTimeZone("utc"));
//        Calendar next = Calendar.getInstance();
//        next.setTimeZone(TimeZone.getTimeZone("utc"));
//        next.setTimeInMillis((long) timestamp);
//        return next.get(Calendar.DAY_OF_YEAR) - now.get(Calendar.DAY_OF_YEAR);
//    }
}
