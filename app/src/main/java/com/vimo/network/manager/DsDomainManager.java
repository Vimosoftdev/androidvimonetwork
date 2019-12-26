package com.vimo.network.manager;

import com.vimo.network.helper.Logger;

import java.util.ArrayList;
import java.util.Random;

/**
 * File created by ViMo Software Development Pvt Ltd on 2019-10-15.
 */
public class DsDomainManager {
    private static final DsDomainManager domainManager = new DsDomainManager();
    private ArrayList<String> dsDomains = new ArrayList<>();
    private int dsPortNo;

    public static DsDomainManager getManager() {
        return domainManager;
    }

    private DsDomainManager() {
    }

    public void addDsDomain(String domain) {
        Logger.method(this, "addDsDomain");
        if (dsDomains.contains(domain)) {
            Logger.error(domain + " is already available in the stack");
        } else {
            dsDomains.add(domain);
        }
    }

    public void addDsDomain(ArrayList<String> domains) {
        Logger.method(this, "addDsDomain");
        dsDomains.addAll(domains);
    }

    public String myDomain() {
        int selected = 0;
        if (dsDomains.size() > 1) {
            selected = new Random().nextInt(dsDomains.size());
        }
        return dsDomains.get(selected);
    }

    public void setDsPortNo(int dsPortNo) {
        this.dsPortNo = dsPortNo;
    }

    public int myPortNo() {
        return dsPortNo;
    }
}
