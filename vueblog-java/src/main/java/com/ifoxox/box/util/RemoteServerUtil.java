package com.ifoxox.box.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class RemoteServerUtil {


    public static int serverPort = 9978;

    public static String getAddress(boolean local) {
        return local ? getLoadAddress() : getServerAddress();
    }


    public static String getLoadAddress() {
        return "http://127.0.0.1:" + RemoteServerUtil.serverPort + "/";
    }


    public static String getServerAddress() {

        InetAddress localHost = null;
        try {
            localHost = Inet4Address.getLocalHost();
        } catch (UnknownHostException e) {
        }
        String ipAddress = localHost.getHostAddress();
        return "http://" + ipAddress + ":" + RemoteServerUtil.serverPort + "/";
    }
}
