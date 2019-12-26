package com.vimo.network.listener;

/**
 * File created by vimo on 28/03/18.
 */

public abstract class SocketListener {
    public abstract void onConnected(String componentName);
    public abstract void onDisconnected(String componentName);
    public abstract void onSuccess(String componentName, Object response, int requestId);
    public abstract void onFailure(String componentName, String error, int errorCode, int requestId);
}
