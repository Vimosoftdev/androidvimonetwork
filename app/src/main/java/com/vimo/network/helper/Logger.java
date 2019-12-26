package com.vimo.network.helper;

import android.content.Context;
import android.util.Log;

import com.vimo.network.BuildConfig;
import com.vimo.network.ViMoNetApplication;
import com.vimo.network.manager.DataManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * File created by vimo on 28/03/18.
 */

public class Logger {
    private final static String logFileName = "ApplicationLog.txt";
    private final static String METHOD = "METHOD";
    private final static String MESSAGE = "MESSAGE";
    private final static String DATA = "DATA";
    private final static String ERROR = "ERROR";
    private final static String SIPLOG = "SIPLOG";

    private static Context applicationContext = null;
    private static SimpleDateFormat dateFormat;
    private static Logger ourInstance = new Logger();
    private static StringBuffer oldLogStringBuilder = null;
    private static StringBuffer newLogStringBuilder = null;
    private static int logLength = 0;

    public static Logger getLogger() {
        return ourInstance;
    }

    private Logger() {
        applicationContext = ViMoNetApplication.getApplication();
        if (DataManager.isRightToLeft()) {
            dateFormat = new SimpleDateFormat("dd.MM.yyyy kk:mm:ss", Locale.ENGLISH);
        } else {
            dateFormat = new SimpleDateFormat("dd.MM.yyyy kk:mm:ss", Locale.getDefault());
        }
        readExistingLogs();
    }

    private File getCacheDir(String filename) {
        if (applicationContext == null) {
            return null;
        }
        return (new File(applicationContext.getCacheDir(), filename));
    }

    public static void setApplicationContext(Context context) {
        applicationContext = context;
        //
        getLogger().readExistingLogs();
    }

    public void writeLogIntoFile(boolean isAppTerminating) {
        Log.i("Logger", "writeLogIntoFile :: " + isAppTerminating);    // Changed from Logger.method(this, "writeLogIntoFile") to avoid crash
        if (!DataManager.hasFreeSpace()) {
            Log.i("Logger", "No free space for logs");
            return;
        }
        File logFile = getCacheDir(logFileName);
        if (logFile == null) {
            Log.e("Logger", "writeLogIntoFile :: Unable to get log file");
            return;
        }
        if (logFile.exists() && logFile.delete()) {
            Log.i("Logger", "Log file is removed");
        }
        try {
            logFile = getCacheDir(logFileName);
            if (logFile.createNewFile()) {
                Log.i("Logger", "Log file has been created");
                BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true));
                if (isAppTerminating) {
                    if (oldLogStringBuilder == null) {
                        oldLogStringBuilder = new StringBuffer(newLogStringBuilder);
                    } else {
                        oldLogStringBuilder.append(newLogStringBuilder);
                    }
                }
                writer.write(oldLogStringBuilder.toString());
                writer.flush();
                writer.close();
            } else {
                Log.e("Logger", "Unable to create the log file");
            }
        } catch (Exception e) {
            Log.e("Logger", "Error occurred while writing logs into file : " + e.getLocalizedMessage());
        }
    }

    public static String getLogFilePath() {
        File temp = getLogger().getCacheDir(logFileName);
        if (temp.exists()) {
            return temp.getAbsolutePath();
        }
        return logFileName;
    }

//    public void resetLogger() {
//        File logFile = getLogger().getCacheDir(logFileName);
//        if (logFile.exists() && logFile.delete()) {
//            Log.i("Logger", "Log file is removed");
//        }
//    }

    public static void method(Object c, String method) {
        if (c != null)
            getLogger().log(METHOD, c.getClass().getSimpleName() + " --> " + method);
    }

    public static void message(String message) {
        getLogger().log(MESSAGE, message);
    }

    public static void data(Object object) {
        getLogger().log(DATA, object.toString());
    }

    public static void sipLog(Object object) {
        getLogger().log(SIPLOG, object.toString());
    }

    public static void sipTrace(String tag, String string) {
        getLogger().log(SIPLOG, tag + " : " + string);
    }

    public static void error(Object error) {
        getLogger().log(ERROR, error.toString());
    }

    public static void error(String error) {
        getLogger().log(ERROR, error);
    }

    private void log(String type, String data) {
        if (data == null) {
            return;
        }
        if (newLogStringBuilder == null) {
            newLogStringBuilder = new StringBuffer();
            logLength = 0;
        }
        try {
            if (type == null) {
                logLength += data.length();
                newLogStringBuilder.append(data);
            } else {
                String temp = String.format(Locale.ENGLISH, "%1s [%2s]:%3s\r\n", getDateTimeStamp(), type, data);
                logLength += temp.length();
                newLogStringBuilder.append(temp);
            }
        } catch (Exception e) {
            Log.e("Logger", "Unable to write app logs");
        }
        if (logLength / 1024 > 400) {
            oldLogStringBuilder = new StringBuffer(newLogStringBuilder);
            writeLogIntoFile(false);
            newLogStringBuilder = new StringBuffer();
            logLength = 0;
        }
        if (BuildConfig.isDeveloperMode && type != null) {
            switch (type) {
                case METHOD: {
                    Log.w("testapplog" + type, data);
                    break;
                }
                case MESSAGE: {
                    Log.i("testapplog" + type, data);
                    break;
                }
                case DATA: {
                    Log.d("testapplog" + type, data);
                    break;
                }
                default: {
                    Log.e("testapplog" + type, data);
                    break;
                }
            }
        }
    }

    private void readExistingLogs() {
        File logFile = getCacheDir(logFileName);
        if (logFile == null) {
            Log.e("Logger", "readExistingLogs :: Unable to get log file");
            return;
        }
        BufferedReader buffer = null;
        try {
            buffer = new BufferedReader(new FileReader(logFile));
            newLogStringBuilder = new StringBuffer();
            String line = buffer.readLine();
            while (line != null) {
                newLogStringBuilder.append(line);
                newLogStringBuilder.append("\n");
                line = buffer.readLine();
            }
        } catch (FileNotFoundException e) {
            try {
                if (logFile.createNewFile()) {
                    Log.i("Logger", "Log file has been created");
                }
            } catch (IOException io) {
                io.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("Logger", "Error occurred while reading log file");
        } finally {
            if (buffer != null) {
                try {
                    buffer.close();
                } catch (IOException e) {
                    Log.e("Logger", "Unable to close log file");
                }
            }
        }
    }

    private static String getDateTimeStamp() {
        Date dateNow = Calendar.getInstance().getTime();
        return dateFormat.format(dateNow);
    }
}
