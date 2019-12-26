package com.vimo.network;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.util.SparseArray;

import com.google.gson.Gson;
import com.vimo.network.helper.Logger;
import com.vimo.network.listener.ConnectionListener;
import com.vimo.network.listener.NetworkCodes;
import com.vimo.network.listener.SocketIoManagerListener;
import com.vimo.network.manager.DsDomainManager;
import com.vimo.network.manager.VimoEncryption;
import com.vimo.network.model.ComponentInfo;
import com.vimo.network.model.RequestInfo;
import com.vimo.network.model.RequestParam;
import com.vimo.network.model.RpcResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * File created by vimo on 28/03/18.
 */

public class NetworkManager implements SocketIoManagerListener, ConnectionListener, NetworkCodes {
    private static final NetworkManager instanceManager = new NetworkManager();
    private static final String dsComponentName = "ds";
    private static final String dsComponentRpc = "servicedirectory.getComponent";
    private Context applicationContext = null;
    private SparseArray<String> localizedString = new SparseArray<>();
    private Map<String, SocketIoManager> socketManager = new HashMap<>();
    private Map<Integer, RequestInfo> pendingRequests = new HashMap<>();
    private SparseArray<String> requestedComponents = new SparseArray<>();
    private List<Integer> exceptionalRequestId = null;
    private List<Integer> processInBackground = null;
    private ComponentInfo directoryServiceInfo = null;

    public static NetworkManager getManager() {
        return instanceManager;
    }

    private NetworkManager() {
    }

    /**
     * Method to check network availability
     * @return true if the network is available. Else false.
     */
    public boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = (manager == null) ? null : manager.getActiveNetworkInfo();
        return (info != null && info.isConnected());
    }

    public boolean isAirplaneModeOn(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return Settings.System.getInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) != 0;
        } else {
            return Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        }
    }

    public static String getDsComponentName() {
        return dsComponentName;
    }

    public static String getDsComponentRpc() {
        return dsComponentRpc;
    }

    /**
     * Method to configure the network manager with required info
     * @param context              context to configure the network connection manager
//     * @param directoryServiceIp   ip to connect with directory service
//     * @param directoryServicePort port to connect with directory service
     * @param localizedString      localized string for network error with predefined error codes
     * @param exceptionalRequestId network manager will not destroy the socket connection if the particular request is available in this stack
     * @param processInBackground  network manager will continue the process in background even the app is in background
     */
    public void configureNetworkManager(Context context, SparseArray<String> localizedString, List<Integer> exceptionalRequestId, List<Integer> processInBackground) {
        this.applicationContext = context;
        this.directoryServiceInfo = new ComponentInfo();
//        this.directoryServiceInfo.setPublicIp(directoryServiceIp);
//        this.directoryServiceInfo.setDomain(directoryServiceIp);
        this.directoryServiceInfo.setNport(DsDomainManager.getManager().myPortNo());
        this.localizedString = localizedString;
        if (exceptionalRequestId != null) {
            this.exceptionalRequestId = Collections.unmodifiableList(exceptionalRequestId);
        } else {
            this.exceptionalRequestId = new ArrayList<>();
        }
        if (processInBackground != null) {
            this.processInBackground = Collections.unmodifiableList(processInBackground);
        } else {
            this.processInBackground = new ArrayList<>();
        }
    }

    private List<Integer> getExceptionalTaskId() {
        return exceptionalRequestId;
    }

    /**
     * Method to get the background task ids
     * @return a stack with ids
     */
    public List<Integer> getBackgroundTaskId() {
        return processInBackground;
    }

    /**
     * Method to get the localized string for the particular key
     * @param key to get the localization string
     * @return returns the localized string
     */
    public String getLocalizedString(int key) {
        return localizedString.get(key);
    }

    /**
     * Method to send rpc request to server
     * @param param         request param
     * @param requestId     request id to map with response
     * @param timeout       timeout for the request
     * @param rpc           rpc to call
     * @param componentName component to send request
     * @param listener      listener for callback
     */
    public static void send(final RequestParam param, final int requestId, final int timeout, final String rpc, final String componentName, final ConnectionListener listener) {
        getManager().sendRequest(param, requestId, timeout, rpc, componentName, listener);
    }

    public void destroySocketComponent(String componentName) {
        Logger.method(this, "destroySocketComponent :: " + componentName);
        SocketIoManager ioManager = socketManager.get(componentName);
        if (ioManager == null) {
            Logger.error("destroySocketComponent :: " + componentName + " component is already destroyed");
        } else {
            if (ioManager.hasSocketInstance()) {
                if (!ioManager.isActive() && !ioManager.isConnecting()) {
                    Logger.message("NetworkManager :: Socket io manager has the died " + componentName + " component connection");
                    ioManager.clearSocketManager();
                    socketManager.remove(componentName);
                } else if (ioManager.anyBackgroundTask()) {
                    Logger.message("NetworkManager :: There are some background task going on. Can not destroy " + componentName + " component.");
                } else {
                    Logger.message("NetworkManager :: Socket io manager (" + componentName + ") is not having any job. Need to cleanup.");
                    ioManager.disconnectSocket();
                }
            } else {
                Logger.message("NetworkManager :: There is not active socket connection for " + componentName + ". Need to cleanup.");
                ioManager.clearSocketManager();
            }
        }
    }

    public void destroyAllSocketConnections() {
        Logger.method(this, "destroyAllSocketConnections");
        if (socketManager.size() == 0) {
            Logger.message("NetworkManager :: There is no socket connection to destroy");
            return;
        }
        final Map<String, SocketIoManager> temp = new HashMap<>(socketManager);
        final Set<String> allKeys = temp.keySet();
        for (String key : allKeys) {
            Logger.message("NetworkManager :: Found key " + key + " to destroy");
            destroySocketComponent(key);
        }
    }

    @Override
    public void didDisconnected(String componentName, String error, int errorCode) {
        Logger.method(this, "didDisconnected :: " + componentName);
        SocketIoManager ioManager = socketManager.get(componentName);
        if (ioManager != null) {
            if (error != null) {
                Map<Integer, RequestInfo> tempPendingRequest = new HashMap<>(pendingRequests);
                Logger.message("NetworkManager :: pending request :: " + tempPendingRequest);
                Logger.error("NetworkManager :: onDisconnected :: Sending failure message to all available callback :: " + componentName);
                if (componentName.equals(dsComponentName)) {
                    pendingRequests.clear();
                    Set<Integer> keys = tempPendingRequest.keySet();
                    for (Integer key : keys) {
                        RequestInfo request = tempPendingRequest.get(key);
                        if (request != null) {
                            Logger.message("NetworkManager :: onDisconnected :: ds :: requestInfo " + request.getComponentName() + " reqId " + request.getRequestId());
                            sendFailureCallback(request, error, errorCode);
                        } else {
                            Logger.error("NetworkManager :: onDisconnected :: ds :: no request reference found for " + key);
                        }
                        // remove the request info from io manager
                        ioManager.removeRequest(key);
                    }
                    requestedComponents.clear();
                } else {
                    Set<Integer> keys = tempPendingRequest.keySet();
                    for (Integer key : keys) {
                        RequestInfo request = tempPendingRequest.get(key);
                        if (request == null || request.getComponentName().equals(componentName)) {
                            pendingRequests.remove(key);
                        }
                    }
                    for (Integer key : keys) {
                        RequestInfo request = tempPendingRequest.get(key);
                        if (request != null) {
                            Logger.message("NetworkManager :: onDisconnected :: requestInfo " + request.getComponentName() + " reqId " + request.getRequestId());
                            if (request.getComponentName().equals(componentName)) {
                                sendFailureCallback(request, error, errorCode);
                                // remove the request info from io manager
                                ioManager.removeRequest(key);
                            }
                        } else {
                            Logger.error("NetworkManager :: onDisconnected :: no request reference found for " + key);
                        }
                    }
                }
            }
            Logger.data("NetworkManager :: didDisconnected :: pendingRequests :: " + pendingRequests);
            ioManager.clearSocketManager();
        } else {
            Logger.error("NetworkManager :: Socket io manager is not found for " + componentName);
        }
        socketManager.remove(componentName);
    }

    @Override
    public void onSuccess(Object response, int requestId) {
        Logger.method(this, "onSuccess for " + requestId);
        final String componentName = requestedComponents.get(requestId);
        // going to parse the response
        final Object object = getJsonObject(response);
        // got the parsed response
        if (object != null && object instanceof ComponentInfo) {
            Map<Integer, RequestInfo> tempPendingRequest = new HashMap<>(pendingRequests);
            Set<Integer> keys = tempPendingRequest.keySet();
            for (Integer key : keys) {
                RequestInfo request = tempPendingRequest.get(key);
                if (request == null) {
                    pendingRequests.remove(key);
                    Logger.error("NetworkManager :: onSuccess :: no request reference found for " + key);
                } else if (request.getComponentName().equals(componentName)) {
                    pendingRequests.remove(key);
                }
            }
            // remove component info from requested component stack
            requestedComponents.remove(requestId);
            Logger.message("NetworkManager :: Received component info for " + componentName);
            ComponentInfo componentInfo = (ComponentInfo) object;
            if (componentInfo.getSipPort() > 0) {
                for (Integer key : keys) {
                    RequestInfo request = tempPendingRequest.get(key);
                    if (request != null && request.getComponentName().equals(componentName)) {
                        Logger.message("NetworkManager :: onDisconnected :: requestInfo " + request.getComponentName() + " reqId " + request.getRequestId());
                        Logger.data("Sending callback for " + request.getListener());
                        sendSuccessCallback(request, componentInfo);
                        break;
                    }
                }
            } else {
                // creating socket io manager to send request
                SocketIoManager ioManager = new SocketIoManager(componentInfo, componentName, this);
                socketManager.put(componentName, ioManager);
                Logger.message("NetworkManager :: Going to send pending request to " + componentName);
                for (Integer key : keys) {
                    RequestInfo request = tempPendingRequest.get(key);
                    if (request != null && request.getComponentName().equals(componentName)) {
                        Logger.message("NetworkManager :: " + request.getComponentName() + " :: Sending request for request id :: " + request.getRequestId());
                        ioManager.send(request);
                    }
                }
            }
        } else {
            Logger.error("NetworkManager :: onSuccess :: onFailure :: " + object);
            if (object == null) {
                onFailure(getLocalizedString(EC_RESPONSE_ERROR), EC_RESPONSE_ERROR, requestId);
            } else {
                RpcResponse rpc = (RpcResponse) object;
                onFailure(rpc.getMsg(), rpc.getStatus(), requestId);
            }
        }
    }

    @Override
    public void onFailure(final String error, final int errorCode, final int requestId) {
        Logger.method(this, "onFailure for " + requestId);
        Logger.data("NetworkManager :: Available requested components :: " + requestedComponents);
        final String componentName = requestedComponents.get(requestId);
        requestedComponents.remove(requestId);
        Logger.error("NetworkManager :: happened for " + componentName);
        Logger.error("NetworkManager :: onFailure :: Sending failure message to all available callback :: " + componentName);
        //
        Map<Integer, RequestInfo> tempPendingRequest = new HashMap<>(pendingRequests);
        Set<Integer> keys = new HashSet<>(tempPendingRequest.keySet());
        for (Integer key : keys) {
            RequestInfo request = tempPendingRequest.get(key);
            if (request == null) {
                pendingRequests.remove(key);
                tempPendingRequest.remove(key);
                Logger.error("NetworkManager :: onFailure :: no request reference found for " + key);
            } else if (request.getComponentName().equals(componentName)) {
                pendingRequests.remove(key);
            } else if (!request.getComponentName().equals(componentName)) {
                tempPendingRequest.remove(key);
            }
        }
        Logger.data("NetworkManager :: pendingRequests :: " + pendingRequests);
        sendFailureCallback(tempPendingRequest, error, errorCode);
    }

    public void sendSuccessCallback(final RequestInfo requestInfo, final Object response) {
        Logger.method(this, "sendSuccessCallback :: " + requestInfo.getRequestId());
        if (getExceptionalTaskId().contains(requestInfo.getRequestId())) {
            Logger.error("NetworkManager :: sendSuccessCallback :: Sending to exceptional available callback");
            requestInfo.getListener().onSuccess(response, requestInfo.getRequestId());
            return;
        }
        Activity activity = ViMoNetApplication.getApplication().getCurrentActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Logger.error("NetworkManager :: sendSuccessCallback :: Sending to available callback");
                    requestInfo.getListener().onSuccess(response, requestInfo.getRequestId());
                }
            });
        } else {
            Logger.error("NetworkManager :: sendSuccessCallback :: Looks like application is running in background");
            if (getBackgroundTaskId().contains(requestInfo.getRequestId())) {
                requestInfo.getListener().onSuccess(response, requestInfo.getRequestId());
            }
        }
    }

    public void sendFailureCallback(final Map<Integer, RequestInfo> requestList, final String error, final int errorCode) {
        Logger.method(this, "sendFailureCallback :: Request List :: " + requestList);
        final Map<Integer, RequestInfo> temp = new HashMap<>(requestList);
        Set<Integer> keys = requestList.keySet();
        for (Integer key : keys) {
            RequestInfo info = requestList.get(key);
            if (getExceptionalTaskId().contains(info.getRequestId())) {
                Logger.error("NetworkManager :: sendFailureCallback :: Sending to exceptional available callback :: " + info.getListener());
                info.getListener().onFailure(error, errorCode, info.getRequestId());
                temp.remove(key);
            }
        }
        if (temp.size() == 0) {
            Logger.message("NetworkManager :: No more request found for callback");
            return;
        }
        Activity activity = ViMoNetApplication.getApplication().getCurrentActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Logger.error("NetworkManager :: sendFailureCallback :: Sending failure message to available callback");
                    Set<Integer> keys = temp.keySet();
                    for (Integer key : keys) {
                        RequestInfo request = temp.get(key);
                        Logger.error("NetworkManager :: sendFailureCallback :: Sending to available callback (foreground) :: " + request.getListener());
                        request.getListener().onFailure(error, errorCode, request.getRequestId());
                    }
                }
            });
        } else {
            Logger.error("NetworkManager :: sendFailureCallback :: Looks like application is running in background");
            Set<Integer> tempKeys = temp.keySet();
            for (Integer key : tempKeys) {
                RequestInfo request = temp.get(key);
                if (getBackgroundTaskId().contains(request.getRequestId())) {
                    Logger.error("NetworkManager :: sendFailureCallback :: Sending to available callback (background) :: " + request.getListener());
                    request.getListener().onFailure(error, errorCode, request.getRequestId());
                }
            }
        }
    }

    public void sendFailureCallback(final RequestInfo requestInfo, final String error, final int errorCode) {
        Logger.method(this, "sendFailureCallback :: " + requestInfo.getRequestId());
        if (getExceptionalTaskId().contains(requestInfo.getRequestId())) {
            Logger.error("NetworkManager :: sendFailureCallback :: Sending to exceptional available callback :: " + requestInfo.getRequestId());
            requestInfo.getListener().onFailure(error, errorCode, requestInfo.getRequestId());
            return;
        }
        Activity activity = ViMoNetApplication.getApplication().getCurrentActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Logger.error("NetworkManager :: sendFailureCallback :: Sending failure message to available callback for req id :: " + requestInfo.getRequestId());
                    requestInfo.getListener().onFailure(error, errorCode, requestInfo.getRequestId());
                }
            });
        } else {
            Logger.error("NetworkManager :: sendFailureCallback :: Looks like application is running in background");
            if (getBackgroundTaskId().contains(requestInfo.getRequestId())) {
                Logger.message("NetworkManager :: sendFailureCallback :: Sending background callback :: req :: " + requestInfo.getRequestId());
                requestInfo.getListener().onFailure(error, errorCode, requestInfo.getRequestId());
            }
        }
    }

    /**
     * Method to send rpc request to server
     * @param param         request param
     * @param requestId     request id to map with response
     * @param timeout       timeout for the request
     * @param rpc           rpc to call
     * @param componentName component to send request
     * @param listener      listener for callback
     */
    private synchronized void sendRequest(final RequestParam param, final int requestId, final int timeout, final String rpc, final String componentName, final ConnectionListener listener) {
        Logger.method(this, "sendRequest :: " + requestId + " ::    component :: " + componentName);
        if (!isNetworkAvailable()) {
            Logger.error("NetworkManager :: Network is not available");
            listener.onFailure(getLocalizedString(EC_NETWORK_DOWN), EC_NETWORK_DOWN, requestId);
            return;
        }
        if (isAirplaneModeOn(applicationContext)) {
            Logger.error("NetworkManager :: Looks like user kept the phone in airplane mode");
            listener.onFailure(getLocalizedString(EC_NETWORK_DOWN), EC_NETWORK_DOWN, requestId);
            return;
        }
        // check whether the component is available or not
        SocketIoManager ioManager = socketManager.get(componentName);
        if (ioManager != null) {
            if (!ioManager.hasSocketInstance() || (!ioManager.isActive() && !ioManager.isConnecting())) {
                Logger.message("NetworkManager :: Socket io manager is available with died " + componentName + " connection. Need to clear it.");
                ioManager.clearSocketManager();
                socketManager.remove(componentName);
                ioManager = null;
            }
        }
        if (ioManager == null) {
            Logger.message("NetworkManager :: Stack doesn't have " + componentName + " component connection in network manager");
            // add the request to pending request list to do the job later
            // First, app should connect to the particular socket component
            boolean isDevComponent = param.isAvailable(IS_DEV_COMPONENT);
            param.removeValue(IS_DEV_COMPONENT);
            if (!pendingRequests.containsKey(requestId)) {
                Logger.message("Network Manager :: adding " + requestId + " to pending request stack (" + componentName + ")");
                RequestInfo pendingRequestInfo = new RequestInfo(componentName, rpc, requestId, timeout, (param.isAvailable(SIP_COMPONENT) ? null : param), listener);
                pendingRequests.put(requestId, pendingRequestInfo);
            } else {
                Logger.error("Network Manager :: Got duplicate request :: " + requestId);
                return;
            }
            if (isAlreadyRequested(componentName)) {
                Logger.message("NetworkManager :: Already there is a pending request for this requested component " + componentName);
            } else {
                int componentRequestId = (new Random()).nextInt(dsLookupRequestIdLimit * -1) * -1 - dsLookupRequestIdLimit;
                requestedComponents.put(componentRequestId, componentName);
                Logger.data("NetworkManager :: Requested components :: " + requestedComponents);
                // check directory service connection whether it is available or not
                SocketIoManager dsManager = socketManager.get(dsComponentName);
                if (dsManager != null && !dsManager.hasSocketInstance()) {
                    dsManager.clearSocketManager();
                    socketManager.remove(dsComponentName);
                    dsManager = null;
                }
                // creating the request object
                RequestParam dsParam = RequestParam.myRequest();
                dsParam.addParam("componentName", componentName);
                if (isDevComponent) {
                    dsParam.addParam("componentStatus", "dev");
                }
                if (param.isAvailable(SIP_COMPONENT)) {
                    JSONObject object = param.getObject();
                    try {
                        Iterator<String> keys = object.keys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            if (object.get(key) instanceof String) {
                                dsParam.addParam(key, object.getString(key));
                            }
                        }
                    } catch (JSONException e) {
                        Logger.error("Networkmanager :: Json error :: " + e.getMessage());
                    }
                }
                RequestInfo info = new RequestInfo(componentName, getDsComponentRpc(), componentRequestId, default_timeout, dsParam, this);
                // directory service connection is not available
                // trying to create now
                if (dsManager == null) {
                    Logger.message("NetworkManager :: Creating socket io manager for directory service lookup.");
                    directoryServiceInfo.setDomain(DsDomainManager.getManager().myDomain());
                    dsManager = new SocketIoManager(directoryServiceInfo, dsComponentName, this);
                    socketManager.put(dsComponentName, dsManager);
                }
                Logger.message("NetworkManager :: Sending directory service lookup for " + componentName + " with request id :: " + componentRequestId);
                // sending request to get component info
                dsManager.send(info);
            }
        } else {
            Logger.message("NetworkManager :: " + componentName + " :: Sending request for :: " + requestId);
            // component is available
            RequestInfo info = new RequestInfo(componentName, rpc, requestId, timeout, param, listener);
            ioManager.send(info);
        }
    }

    /**
     * Method to check whether a request sent to directory service or not
     * @param componentName name to do directory service lookup
     * @return  true if the name exist in requestedComponents, else false
     */
    private boolean isAlreadyRequested(String componentName) {
        for (int index = 0; index < requestedComponents.size(); index++) {
            if (requestedComponents.valueAt(index).equals(componentName)) {
                return true;
            }
        }
        return false;
    }



    /**
     * Method to decrypt response and create object
     * @param response  data got from server for RPC request
     * @return          returns parsed object after the decryption
     */
    private Object getJsonObject(Object response) {
        try {
            String ecryptedData = ((JSONObject) response).getString("response");
            if (BuildConfig.isDeveloperMode) {
                Logger.data("NetworkManager :: Encrypted Data :: " + ecryptedData);
            }
            String decryptedData = VimoEncryption.decrypt(ecryptedData);
            if (BuildConfig.isDeveloperMode) {
                Logger.data("NetworkManager :: Decrypted Data :: " + decryptedData);
            }
            JSONObject temp = new JSONObject(decryptedData);
            int statusCode = temp.getInt("status");
            Gson gson = new Gson();
            return gson.fromJson(temp.toString(), ((statusCode == NetworkCodes.EC_SUCCESS_CODE) ? ComponentInfo.class : RpcResponse.class));
        } catch (JSONException e) {
            e.printStackTrace();
            Logger.error("NetworkManager :: JSONException :: getJsonObject :: " + e.getMessage());
            return e;
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("NetworkManager :: Exception :: getJsonObject :: " + e.getMessage());
            return e;
        }
    }
}
