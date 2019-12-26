package com.vimo.network.model;

/**
 * File created by vimo on 28/03/18.
 */

public class ComponentInfo {
    private String componentIp;
    private String tokenTimeStamp;
    private String publicIp;
    private String domain;
    private boolean isTlsEnabled;
    private int port;
    private int nport;
    private int sipPort;
    private int tlsPort;

    public String getComponentIp() {
        return componentIp;
    }

    public void setComponentIp(String componentIp) {
        this.componentIp = componentIp;
    }

    public String getTokenTimeStamp() {
        return tokenTimeStamp;
    }

    public void setTokenTimeStamp(String tokenTimeStamp) {
        this.tokenTimeStamp = tokenTimeStamp;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public boolean isTlsEnabled() {
        return isTlsEnabled;
    }

    public void setTlsEnabled(boolean tlsEnabled) {
        isTlsEnabled = tlsEnabled;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getNport() {
        return nport;
    }

    public void setNport(int nport) {
        this.nport = nport;
    }

    public int getSipPort() {
        return sipPort;
    }

    public void setSipPort(int sipPort) {
        this.sipPort = sipPort;
    }

    public int getTlsPort() {
        return tlsPort;
    }

    public void setTlsPort(int tlsPort) {
        this.tlsPort = tlsPort;
    }
}
