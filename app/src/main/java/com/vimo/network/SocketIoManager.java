package com.vimo.network;

import android.app.Activity;

import com.vimo.network.helper.Logger;
import com.vimo.network.listener.ConnectionListener;
import com.vimo.network.listener.NetworkCodes;
import com.vimo.network.listener.SocketIoManagerListener;
import com.vimo.network.listener.SocketListener;
import com.vimo.network.model.ComponentInfo;
import com.vimo.network.model.RequestInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * File created by vimo on 28/03/18.
 */

public class SocketIoManager extends SocketListener implements NetworkCodes {

    private ComponentInfo componentInfo;
    private String componentName;
    private SocketIO socketIo = null;
    private Map<Integer, RequestInfo> requestInfos = new HashMap<>();
    private SocketIoManagerListener ioManagerListener;
    private boolean requestAvailable;

    SocketIoManager(ComponentInfo componentInfo, String componentName, SocketIoManagerListener ioManagerListener) {
        this.componentInfo = componentInfo;
        this.componentName = componentName;
        this.ioManagerListener = ioManagerListener;
    }

    public synchronized void send(final RequestInfo info) {
        Logger.method(this, "send");
        if (!requestInfos.containsKey(info.getRequestId())) {
            Logger.message("SIO :: Received new request to send");
            requestInfos.put(info.getRequestId(), info);
        } else {
            Logger.error("SIO :: Received duplicate request for request id " + info.getRequestId());
            return;
        }
        if (componentInfo == null) {
            Logger.error("SIO :: There is no proper component info found :: request id " + info.getRequestId());
            Logger.error("SIO :: Trying to cleanup this component :: " + info.getComponentName());
            Map<Integer, RequestInfo> temp = new HashMap<>(requestInfos);
            failureCallback(temp, EC_NETWORK_ERROR);
            ioManagerListener.didDisconnected(componentName, NetworkManager.getManager().getLocalizedString(EC_NETWORK_ERROR), EC_NETWORK_ERROR);
        } else if (socketIo == null) {
            connectSocket();
        } else if (isActive()) {
            info.setRequested(true);
            socketIo.send(info);
        } else if (isConnecting()) {
            Logger.message("SIO :: send :: Waiting for socket to be connected :: " + componentName);
        } else {
            if (socketIo != null) {
                Logger.error("SIO :: Current status of " + componentName + " socket is " + socketIo.getSocketStatus());
                if (socketIo.isDisconnectedByApp()) {
                    Logger.message("SIO :: Socket is disconnected by app. Going to reconnect with existing component info...");
                    if (socketIo.getSocketStatus() == SocketIO.SOCKET_DISCONNECTING) {
                        requestAvailable = true;
                        Logger.message("SIO :: Waiting for socket to be disconnected :: " + componentName);
                    } else if (socketIo.getSocketStatus() == SocketIO.SOCKET_DISCONNECTED) {
                        connectSocket();
                    } else {
                        Logger.error("SIO :: Undefined error in " + componentName);
                    }
                } else {
                    Logger.error("SIO :: Something happened in network for " + componentName + " socket");
                    info.getListener().onFailure(NetworkManager.getManager().getLocalizedString(EC_NETWORK_ERROR), EC_NETWORK_ERROR, info.getRequestId());
                    requestInfos.remove(info.getRequestId());
                }
            } else {
                Logger.error("SIO :: Something happened in network for " + componentName + " socket");
                info.getListener().onFailure(NetworkManager.getManager().getLocalizedString(EC_NETWORK_ERROR), EC_NETWORK_ERROR, info.getRequestId());
                requestInfos.remove(info.getRequestId());
            }
        }
    }

    /**
     * Method to check whether there are any background task going on or not
     * @return  false if there is no background task, else true
     */
    public boolean anyBackgroundTask() {
        Map<Integer, RequestInfo> temp = new HashMap<>(requestInfos);
        Set<Integer> keys = temp.keySet();
        for (Integer key : keys) {
            RequestInfo info = temp.get(key);
            if (NetworkManager.getManager().getBackgroundTaskId().contains(info.getRequestId())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasSocketInstance() {
        return (socketIo != null);
    }

    public boolean isActive() { // in case of failure, socket io will be null. Need to add null check
        return (socketIo != null && socketIo.getSocketStatus() == SocketIO.SOCKET_CONNECTED);
    }

    public boolean isConnecting() {  // in case of failure, socket io will be null. Need to add null check
        return (socketIo != null && socketIo.getSocketStatus() == SocketIO.SOCKET_CONNECTING);
    }

    public void disconnectSocket() {
        Logger.method(this, "disconnectSocket :: " + componentName);
        if (socketIo == null) {
            Logger.error("SIO :: Socket connection is already died for " + componentName);
        } else {
            socketIo.disconnect();
        }
    }

    public void removeRequest(int key) {
        requestInfos.remove(key);
    }

    public void clearSocketManager() {
        Logger.method(this, "clearSocketManager :: " + componentName);
        componentInfo = null;
        componentName = null;
        requestAvailable = false;
        requestInfos.clear();
        if (socketIo != null) {
            socketIo.clearConnection();
        }
        socketIo = null;
    }

    /* SOCKET LISTENERS */

    @Override
    public void onConnected(String cn) {
        Logger.method(this, "connected to " + componentName);
        if (requestInfos.size() == 0) {
            Logger.message("SIO :: onConnected :: No request info found to send");
        } else {
            Map<Integer, RequestInfo> temp = new HashMap<>(requestInfos);
            Logger.message("SIO :: onConnected :: Going to send all available (" + temp.size() + ") pending request to " + componentName);
            Set<Integer> keys = temp.keySet();
            for (Integer key : keys) {
                RequestInfo info = temp.get(key);
                Logger.message("SIO :: onConnected :: " + componentName + " :: Sending request for " + info.getRequestId());
                socketIo.send(info);
            }
        }
    }

    @Override
    public void onDisconnected(String cn) {
        Logger.method(this, "Disconnected from " + componentName);
        if (!socketIo.isDisconnectedByApp()) {
            final Map<Integer, RequestInfo> temp = new HashMap<>(requestInfos);
            if (NetworkManager.getDsComponentName().equals(componentName)) {
                failureCallback(temp, EC_NETWORK_ERROR);
            } else {
                Activity activity = ViMoNetApplication.getApplication().getCurrentActivity();
                if (activity != null) {
                    Logger.message("SIO :: onSuccess :: activity is alive. Going to send callback");
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            failureCallback(temp, EC_NETWORK_ERROR);
                        }
                    });
                } else {
                    Set<Integer> keys = new HashSet<>(temp.keySet());
                    for (Integer key : keys) {
                        if (!NetworkManager.getManager().getBackgroundTaskId().contains(key)) {
                            temp.remove(key);
                        }
                    }
                    if (temp.size() > 0) {
                        failureCallback(temp, EC_NETWORK_ERROR);
                    }
                }
            }
            // reset request availability
            requestAvailable = false;
        }
        if (ioManagerListener != null) {
            ioManagerListener.didDisconnected(componentName, null, EC_APP_ERROR);
        } else {
            Logger.error("SIO :: " + componentName + " :: Socket io manager listener is not available for callback");
            if (!requestAvailable) {
                socketIo = null;
                clearSocketManager();
            } else {
                Logger.error("SIO :: " + componentName + " :: Not going to clear socket manager. Because still there are some pending request.");
            }
        }
    }

    @Override
    public void onSuccess(String cn, final Object response, final int requestId) {
        Logger.method(this, "onSuccess " + componentName + " (req :: " + requestId + ")");
        Map<Integer, RequestInfo> temp = new HashMap<>(requestInfos);
        requestInfos.remove(requestId);
        if (temp.containsKey(requestId)) {
            final RequestInfo info = temp.get(requestId);
            if (componentName.equals(NetworkManager.getDsComponentName())) {
                Logger.message("SIO :: onSuccess :: directory service lookup :: callback");
                final ConnectionListener listener = info.getListener();
                listener.onSuccess(response, requestId);
            } else {
                NetworkManager.getManager().sendSuccessCallback(info, response);
            }
        } else {
            Logger.error("SIO :: onSuccess :: " + componentName + " :: Request callback is not found for " + requestId);
        }
    }

    @Override
    public void onFailure(String cn, final String error, final int errorCode, final int requestId) {
        Logger.method(this, "onFailure " + componentName + " (req :: " + requestId + ")");
        Map<Integer, RequestInfo> temp = new HashMap<>(requestInfos);
        requestInfos.remove(requestId);
        if (errorCode == EC_CONNECTION_ERROR) {
            if (componentName.equals(NetworkManager.getDsComponentName())) {
                ioManagerListener.didDisconnected(componentName, error, errorCode);
            } else {
                failureCallback(temp, EC_NETWORK_ERROR);
                ioManagerListener.didDisconnected(componentName, error, errorCode);
            }
        } else {
            if (temp.get(requestId) != null) {
                final RequestInfo info = temp.get(requestId);
                NetworkManager.getManager().sendFailureCallback(info, error, errorCode);
            } else {
                Logger.error("SIO :: onFailure :: " + componentName + " :: Request callback is not found for " + requestId);
            }
        }
    }

    /* PRIVATE METHODS */

    private void connectSocket() {
        Logger.method(this, "connectSocket");
        socketIo = new SocketIO(componentInfo.getDomain(), componentInfo.getNport(), componentName, true, this);
        socketIo.connect();
    }

    private void failureCallback(final Map<Integer, RequestInfo> temp, int code) {
        NetworkManager.getManager().sendFailureCallback(temp, NetworkManager.getManager().getLocalizedString(code), code);
    }
}
