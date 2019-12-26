package com.vimo.network.listener;

/**
 * File created by vimo on 28/03/18.
 */

public interface ConnectionListener extends NetworkCodes {
    void onSuccess(Object response, int requestId);
    void onFailure(String error, int errorCode, int requestId);
}
