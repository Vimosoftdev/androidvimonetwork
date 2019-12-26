package com.vimo.network.listener;

public interface SocketIoManagerListener {
    void didDisconnected(String componentName, String error, int errorCode);
}
