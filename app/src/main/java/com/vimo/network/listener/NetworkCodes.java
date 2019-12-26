package com.vimo.network.listener;

/**
 * File created by vimo on 28/03/18.
 */

public interface NetworkCodes {
    // error codes
    int EC_NETWORK_DOWN         = -5001;
    int EC_NETWORK_ERROR        = -5002;
    int EC_CONNECTION_ERROR     = -5003;
    int EC_CONNECTION_TIMEOUT   = -5004;
    int EC_REQUEST_ERROR        = -5005;
    int EC_SERVER_ERROR         = -5006;
    int EC_PARSER_ERROR         = -5007;
    int EC_RESPONSE_ERROR       = -5008;
    int EC_REQUEST_TIMEOUT      = -5009;
    int EC_CLEANUP_ERROR        = -5010;
    // general error code for app
    int EC_APP_ERROR    = -11111111;
    // success response code from server
    int EC_SUCCESS_CODE = 200;

    // default request id for directory service lookup
    int dsLookupRequestIdLimit = -5000;
    // default timeout for all the request
    int default_timeout = 20000;

    String IS_DEV_COMPONENT = "isDevComponent";
    String SIP_COMPONENT = "sip";
}
