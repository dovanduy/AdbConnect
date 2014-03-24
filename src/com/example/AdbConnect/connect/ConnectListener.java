package com.example.AdbConnect.connect;

/**
 * Created by zhangdi on 14-3-20.
 */
public interface ConnectListener {

    public void onConnectError(String address, String msg);

    public void onConnectCompleted(String address, boolean result);

    public void onConnectCancelled(String address);

    public void onConnectStart(String address);

}
