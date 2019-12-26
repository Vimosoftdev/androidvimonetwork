package com.vimo.network.model;

import android.os.Build;

import com.vimo.network.helper.Logger;
import com.vimo.network.manager.DataManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * File created by vimo on 28/03/18.
 */

public class RequestParam {

    private JSONObject object;

    public static RequestParam myRequest() {
        return new RequestParam();
    }

    public RequestParam() {
        object = new JSONObject();
        addParam("versionNumber", DataManager.getAppVersionNumber());
        addParam("appBuildNumber", DataManager.getBuildNumber());
        addParam("deviceType", "Android");
        addParam("language", DataManager.getSystemLanguage());
        addParam("manufacturer", Build.MANUFACTURER);
        addParam("deviceModel", Build.MODEL);
        addParam("osVersion", Build.VERSION.SDK_INT);
        addParam("deviceId", DataManager.getDeviceId());
    }

    public void addParam(String key, JSONArray value) {
        try {
            object.put(key, value);
        } catch (JSONException e) {
            Logger.error(e);
        }
    }

    public void addParam(String key, JSONObject value) {
        try {
            object.put(key, value);
        } catch (JSONException e) {
            Logger.error(e);
        }
    }

    public void addParam(String key, List value) {
        try {
            object.put(key, new JSONArray(value));
        } catch (Exception e) {
            Logger.error("Unable to add String param");
        }
    }

    public void addParam(String key, String value) {
        try {
            object.put(key, value);
        } catch (JSONException e) {
            Logger.error(e);
        }
    }

    public void addParam(String key, int value) {
        try {
            object.put(key, value);
        } catch (JSONException e) {
            Logger.error(e);
        }
    }

    public void addParam(String key, float value) {
        try {
            object.put(key, value);
        } catch (Exception e) {
            Logger.error("Unable to add int param");
        }
    }

    public void addParam(String key, boolean value) {
        try {
            object.put(key, value);
        } catch (JSONException e) {
            Logger.error(e);
        }
    }

    public void addObjectItem(String payload, String key, String value) {
        try {
            if (!object.has(payload)) {
                object.put(payload, new JSONObject());
            }
            ((JSONObject) object.get(payload)).put(key, value);
        } catch (JSONException e) {
            Logger.error(e);
        }
    }

    public void addObjectListItem(String payload, String key, String value) {
        try {
            if (!object.has(payload)) {
                object.put(payload, new JSONObject());
            }
            if (!((JSONObject) object.get(payload)).has(key)) {
                ((JSONObject) object.get(payload)).put(key, new JSONArray());
            }
            ((JSONArray) ((JSONObject) object.get(payload)).get(key)).put(value);
        } catch (JSONException e) {
            Logger.error(e);
        }
    }

    public boolean isAvailable(String key) {
        if (object.has(key)) {
            try {
                return object.getBoolean(key);
            } catch (JSONException e) {
                Logger.error("RequestParam :: getString :: Error :: " + e.getMessage());
            }
        }
        return false;
    }

    public void removeValue(String key) {
        if (object.has(key)) {
            object.remove(key);
        }
    }

    public JSONObject getObject() {
        return object;
    }

//    public JSONObject getParam() {
//        return object;
//    }

    public String json() {
        return object.toString();
    }
}
