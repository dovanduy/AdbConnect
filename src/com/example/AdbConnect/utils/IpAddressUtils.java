package com.example.AdbConnect.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class IpAddressUtils {

    public static int makeIpAddress(int ip, int i) {
        return 0xFFFFFF & ip | (i & 0xFF) << 24;
    }

    public static InetAddress transform(int ip) throws UnknownHostException {
        byte[] arrayOfByte = new byte[4];
        arrayOfByte[3] = ((byte) (0xFF & (ip >> 24)));
        arrayOfByte[2] = ((byte) (0xFF & (ip >> 16)));
        arrayOfByte[1] = ((byte) (0xFF & (ip >> 8)));
        arrayOfByte[0] = ((byte) (ip & 0xFF));
        InetAddress localInetAddress = InetAddress.getByAddress(arrayOfByte);
        return localInetAddress;
    }

    public static String transformToHost(int ip) {
        return (ip & 0xFF) + "." + (0xFF & (ip >> 8)) + "." + (0xFF & (ip >> 16)) + "." + (0xFF & (ip >> 24));
    }

    public static int getLocalIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return wifiInfo.getIpAddress();
    }

    public static InetAddress getLocalInetAddress(Context context) {
        int address = getLocalIpAddress(context);
        if (address == 0) {
            return null;
        }
        try {
            return transform(address);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }

}
