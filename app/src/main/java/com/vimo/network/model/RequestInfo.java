package com.vimo.network.model;

import com.vimo.network.listener.ConnectionListener;

/**
 * File created by vimo on 28/03/18.
 */

public class RequestInfo {
    private String componentName;
    private String rpc;
    private int requestId;
    private int timeout;
    private RequestParam param;
    private ConnectionListener listener;
    private boolean requested;
    private boolean sip;

    public RequestInfo(String componentName, String rpc, int requestId, int timeout, RequestParam param, ConnectionListener listener) {
        this.componentName = componentName;
        this.rpc = rpc;
        this.requestId = requestId;
        this.timeout = timeout;
        this.param = param;
        this.listener = listener;
        this.requested = false;
    }

    public String getComponentName() {
        return componentName;
    }

    public String getRpc() {
        return rpc;
    }

    public int getRequestId() {
        return requestId;
    }

    public int getTimeout() {
        return timeout;
    }

    public RequestParam getParam() {
        return param;
    }

    public ConnectionListener getListener() {
        return listener;
    }

    public void setRequested(boolean requested) {
        this.requested = requested;
    }

    public boolean isRequested() {
        return requested;
    }

    public boolean isSip() {
        return sip;
    }

    public void setSip(boolean sip) {
        this.sip = sip;
    }
}
