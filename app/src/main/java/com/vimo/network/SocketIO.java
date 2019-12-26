package com.vimo.network;

import android.util.SparseArray;

import com.vimo.network.helper.Logger;
import com.vimo.network.listener.NetworkCodes;
import com.vimo.network.listener.SocketListener;
import com.vimo.network.manager.VimoEncryption;
import com.vimo.network.model.RequestInfo;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.WebSocket;

/**
 * File created by vimo on 28/03/18.
 */

public class SocketIO implements NetworkCodes {
    private SparseArray<Thread> stackForTimeoutThread = new SparseArray<>();
    private IO.Options options = new IO.Options();
    private Socket socket;
    private SocketListener listener;
    private String host;
    private int port;
    private String componentName;
    private boolean isSecure;
    private boolean disconnectedByApp;
    private int socketStatus;

    /** PUBLIC PROPERTY */
    public static final int SOCKET_NONE = 0;
    public static final int SOCKET_CONNECTING = 1;
    public static final int SOCKET_CONNECTED = 2;
    public static final int SOCKET_DISCONNECTING = 3;
    public static final int SOCKET_DISCONNECTED = 4;
    public static final int SOCKET_FAILED = 5;

    public SocketIO(String host, int port, String componentName, boolean isSecure, SocketListener listener) {
        this.host = host;
        this.port = port;
        this.componentName = componentName;
        this.isSecure = isSecure;
        this.listener = listener;
    }

    public int getSocketStatus() {
        return socketStatus;
    }

    /**
     * Method to make socket connection
     */
    public void connect() {
        Logger.method(this, "connect :: " + host + " :: " + componentName);
        if (listener == null) {
            Logger.error("SocketIO :: Can not make socket connection. Please add a listener and try again");
            return;
        }
        socketStatus = SOCKET_CONNECTING;
        options.reconnection = false;
        options.forceNew = true;
        options.transports = new String[]{WebSocket.NAME};
        // create socket connection
        try {
            socket = IO.socket("http://" + host + ":" + port, options);
        } catch (Exception e) {
            Logger.error(e.getLocalizedMessage());
        }
        if (socket == null) {
            // call on connection failure
            Logger.error("SocketIO :: Socket connection is not created for " + componentName);
            listener.onFailure(componentName, NetworkManager.getManager().getLocalizedString(EC_CONNECTION_ERROR), EC_CONNECTION_ERROR, EC_APP_ERROR);
        } else {
            Logger.message("SocketIO :: Socket connection is created for " + componentName);
            // add all needed events to socket
            addSocketEvents();
            // connect the socket with server
            socket.connect();
        }
    }

    /**
     * Method to send request to server
     * @param requestInfo request parameter
     */
    public void send(final RequestInfo requestInfo) {
        JSONObject reqObj;
        String encryptedRequestData;
        try {
            if (BuildConfig.isDeveloperMode) {
                Logger.data("REQUEST DATA :: " + requestInfo.getParam().json());
            }
            encryptedRequestData = VimoEncryption.encrypt(requestInfo.getParam().json());
        } catch (Exception e) {
            onFailure(NetworkManager.getManager().getLocalizedString(EC_REQUEST_ERROR), EC_REQUEST_ERROR, requestInfo.getRequestId());
            return;
        }
        try {
            reqObj = new JSONObject();
            reqObj.put("reqid", requestInfo.getRequestId());
            reqObj.put("rpc", requestInfo.getRpc());
            reqObj.put("hasSalt", isSecure);
            reqObj.put("data", encryptedRequestData);
        } catch (JSONException e) {
            Logger.error("SocketIO :: JSON exception :: " + e.getLocalizedMessage());
            onFailure(NetworkManager.getManager().getLocalizedString(EC_REQUEST_ERROR), EC_REQUEST_ERROR, requestInfo.getRequestId());
            return;
        }
        // timeout thread for each request
        Thread thread = new Thread(new TimeoutThread(requestInfo.getRequestId(), requestInfo.getTimeout()));
        thread.start();
        stackForTimeoutThread.put(requestInfo.getRequestId(), thread);
        socket.emit("req", reqObj, new Ack() {
            @Override
            public void call(Object... args) {
                Logger.message("SocketIO :: Got ack from server for request :: " + requestInfo.getRequestId());
            }
        });
    }

    public boolean isConnected() {
        return (socket != null && socketStatus == SOCKET_CONNECTED);
    }

    /**
     * Method to disconnect the socket connection
     */
    public void disconnect() {
        if (socket != null) {
            socketStatus = SOCKET_DISCONNECTING;
            disconnectedByApp = true;
            socket.disconnect();
        } else {
            Logger.error("SocketIO :: Socket is already died. Sending failure callback");
        }
    }

    /**
     * Method to check whether the socket connection is disconnected by server or not
     * @return  returns false if the connection is disconnected by server, else true.
     */
    public boolean isDisconnectedByApp() {
        return disconnectedByApp;
    }

    public void clearConnection() {
        clearTimeoutThreads();
        clearSocketCallback();
        host = null;
        listener = null;
        componentName = null;
    }

    /**
     * Method to add all the needed listeners to the socket
     */
    private void addSocketEvents() {
        Logger.method(this, "addSocketEvents");
        // connecting
        socket.on(Socket.EVENT_CONNECTING, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Logger.message("SocketIO :: Socket " + componentName + " connecting... ");
                socketStatus = SOCKET_CONNECTING;
                if (args != null) {
                    for (Object obj : args) {
                        Logger.message("CONNECTING :: args :: " + obj);
                    }
                }
            }
        });
        // callback for connection
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Logger.message("SocketIO :: Socket connected with " + componentName + " component");
                socketStatus = SOCKET_CONNECTED;
                if (args != null) {
                    for (Object obj : args) {
                        Logger.message("CONNECTED :: args :: " + obj);
                    }
                }
                listener.onConnected(componentName);
            }
        });
        // connection timeout callback
        socket.on(Socket.EVENT_CONNECT_TIMEOUT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Logger.message("SocketIO :: Socket connection timed out to " + componentName + " component");
                socketStatus = SOCKET_FAILED;
                if (args != null) {
                    for (Object obj : args) {
                        Logger.message("CONNECT_TIMEOUT :: args :: " + obj);
                    }
                }
                onFailure(NetworkManager.getManager().getLocalizedString(EC_CONNECTION_TIMEOUT), EC_CONNECTION_TIMEOUT, EC_APP_ERROR);
            }
        });
        // connection error callback
        socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Logger.message("SocketIO :: Socket connection error with " + componentName + " component");
                socketStatus = SOCKET_FAILED;
                if (args != null) {
                    for (Object obj : args) {
                        Logger.message("CONNECT_ERROR :: args :: " + obj);
                    }
                }
                onFailure(NetworkManager.getManager().getLocalizedString(EC_CONNECTION_ERROR), EC_CONNECTION_ERROR, EC_APP_ERROR);
            }
        });
        // connection disconnected callback
        socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                socketStatus = SOCKET_DISCONNECTED;
                if (args != null) {
                    for (Object obj : args) {
                        Logger.message("DISCONNECTED :: args :: " + obj);
                    }
                }
                if (componentName != null) {
                    Logger.message("SocketIO :: Socket disconnected from " + componentName + " component");
                    listener.onDisconnected(componentName);
                } else {
                    Logger.message("SocketIO :: Component name is not available. (it is already died and cleared)");
                }
                clearSocketCallback();
            }
        });
        // callback for server response
        socket.on("res", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Logger.message("SocketIO :: Socket received response from " + componentName + " component");
                if (listener == null) {
                    Logger.error("SocketIO :: No listener for callback. Going to clear this socket connection for " + componentName);
                    disconnect();
                    return;
                }
                if (args != null) {
                    if (BuildConfig.isDeveloperMode) {
                        for (Object obj : args) {
                            Logger.message("RESPONSE :: args :: " + obj);
                        }
                    }
                    int requestId = -1;
                    try {
                        requestId = ((JSONObject) args[0]).getInt("reqid");
                    } catch (JSONException e) {
                        Logger.error("SocketIO :: Error occurred while parsing response");
                    }
                    if (requestId != -1) {
                        removeTimeoutThread(requestId);
                        listener.onSuccess(componentName, args[0], requestId);
                    } else {
                        Logger.error("SocketIO :: ARGS :: Received invalid response from component " + componentName);
                        onFailure(NetworkManager.getManager().getLocalizedString(EC_RESPONSE_ERROR), EC_RESPONSE_ERROR, EC_APP_ERROR);
                    }
                } else {
                    Logger.error("SocketIO ::  :: Received invalid response from component " + componentName);
                    onFailure(NetworkManager.getManager().getLocalizedString(EC_RESPONSE_ERROR), EC_RESPONSE_ERROR, EC_APP_ERROR);
                }
            }
        });
        // callback for some message
        socket.on(Socket.EVENT_MESSAGE, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Logger.message("SocketIO :: Socket event message from " + componentName);
                if (args != null) {
                    for (Object obj : args) {
                        Logger.message("EVENT_MESSAGE :: args :: " + obj);
                    }
                }
            }
        });
        //
        socket.on(Socket.EVENT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Logger.message("Socket event error... ");
                if (args != null) {
                    for (Object obj : args) {
                        Logger.message("EVENT_ERROR :: args :: " + obj);
                    }
                }
            }
        });
    }

    /**
     * Method to send failure callback
     * @param error     error message
     * @param errorCode error code
     * @param requestId error occurred for this id
     */
    private void onFailure(final String error,final  int errorCode,final  int requestId) {
        removeTimeoutThread(requestId);
        if (listener == null) {
            Logger.error("SocketIO :: No listner for failure callback in " + componentName);
        } else {
            listener.onFailure(componentName, error, errorCode, requestId);
        }
    }

    /**
     * Method to remove the particular timeout thread of a request
     * @param requestId - key to remove the timeout thread
     */
    private void removeTimeoutThread(int requestId) {
        if (stackForTimeoutThread.get(requestId) != null) {
            stackForTimeoutThread.get(requestId).interrupt();
        }
        stackForTimeoutThread.remove(requestId);
    }

    /**
     * Method to clear all available timeout threads
     */
    private void clearTimeoutThreads() {
        Logger.method(this, "Clear timeout thread");
        for (int index = 0; index < stackForTimeoutThread.size(); index++) {
            int key = stackForTimeoutThread.keyAt(index);
            Thread thread = stackForTimeoutThread.get(key);
            if (thread != null && !thread.isInterrupted()) {
                thread.interrupt();
            }
        }
        stackForTimeoutThread.clear();
    }

    private void clearSocketCallback() {
        Logger.method(this, "Clearing Socket Listener for " + componentName);
        if (socket != null) {
            socket.off();
            socket = null;
        } else {
            Logger.error("SocketIO :: Socket is already died. Can not clear callback");
        }
    }

    private class TimeoutThread implements Runnable {
        private int requestId;
        private int timeout;

        TimeoutThread(int requestId, int timeout) {
            this.requestId = requestId;
            this.timeout = timeout;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(timeout);
                Logger.message("TimeoutThread :: timed out :: request id :: " + requestId);
                onFailure("Request timed out", EC_REQUEST_TIMEOUT, requestId);
            } catch (InterruptedException e) {
                Logger.error("SocketIO :: " + componentName + " == TimeoutThread :: Exception : " + e);
            }
        }
    }
}
