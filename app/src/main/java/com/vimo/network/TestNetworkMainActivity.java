package com.vimo.network;

//import android.support.v7.app.AppCompatActivity;
//import android.os.Bundle;
//import android.util.SparseArray;
//import android.view.View;
//import android.widget.TextView;
//
//import com.vimo.network.helper.Logger;
//import com.vimo.network.listener.ConnectionListener;
//import com.vimo.network.model.RequestParam;
//
//import java.util.ArrayList;
//import java.util.List;

public class TestNetworkMainActivity { //extends AppCompatActivity implements View.OnClickListener, ConnectionListener {

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        // configure network connection
//        SparseArray<String> localizedString = new SparseArray<>();
//        localizedString.put(EC_NETWORK_DOWN, "Please check your internet connection");
//        localizedString.put(EC_NETWORK_ERROR, "Network disconnected. Please try again later!.");
//        localizedString.put(EC_CONNECTION_ERROR, "Network disconnected. Please try again later!.");
//        localizedString.put(EC_CONNECTION_TIMEOUT, "Network error. Please try again later!.");
//        localizedString.put(EC_REQUEST_ERROR, "Network error. Please try again later!.");
//        localizedString.put(EC_SERVER_ERROR, "Network error. Please try again later!.");
//        localizedString.put(EC_PARSER_ERROR, "Something went wrong. Please contact our team support!.");
//        localizedString.put(EC_RESPONSE_ERROR, "Something went wrong. Please contact our team support!.");
//        localizedString.put(EC_REQUEST_TIMEOUT, "Something went wrong. Please contact our team support!.");
//        localizedString.put(EC_CLEANUP_ERROR, "Something went wrong. Please contact our team support!.");
//        List<Integer> exceptionalRequest = new ArrayList<>();
//        exceptionalRequest.add(1);
//        List<Integer> backgroundRequests = new ArrayList<>();
//        backgroundRequests.add(1);
//        NetworkManager.getManager().configureNetworkManager(this, "10.1.1.167", 1001, localizedString, exceptionalRequest, backgroundRequests);
//        //
//        setContentView(R.layout.activity_main);
//        TextView btnCheck = (TextView) findViewById(R.id.btnCheck);
//        btnCheck.setOnClickListener(this);
//    }
//
//    @Override
//    protected void onStop() {
//        // Need to destroy all available socket connections
//        NetworkManager.getManager().destroyAllSocketConnections();
//        super.onStop();
//    }
//
//    @Override
//    public void onClick(View v) {
//        RequestParam param = RequestParam.myRequest();
//        param.addParam("action", "client.lead");
//        NetworkManager.send(param, 1 /*requestId*/, default_timeout, "sampleapp.checkData", "sampleapp", this);
//        NetworkManager.send(param, 2 /*requestId*/, default_timeout, "adminpanel.fetchServices", "adminpanel", this);
////        NetworkManager.send(param, 2 /*requestId*/, default_timeout, "sampleapp.checkData", "sampleapp", this);
//        NetworkManager.send(param, 3 /*requestId*/, default_timeout, "sampleapp.n_checkData", "sampleapp", this);
//    }
//
//    @Override
//    public void onSuccess(Object response, int requestId) {
//        Logger.method(this, "onSuccess :: " + requestId);
//    }
//
//    @Override
//    public void onFailure(String error, int errorCode, int requestId) {
//        Logger.method(this, "onFailure :: req " + requestId + " :: Error :: " + error);
//    }
}
