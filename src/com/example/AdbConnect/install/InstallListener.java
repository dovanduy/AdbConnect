package com.example.AdbConnect.install;

/**
 * Created by zhangdi on 14-3-24.
 */
public interface InstallListener {

    public void onInstallStart(long id);

    public void onInstallCompleted(long id, boolean result);

    public void onInstallError();

}
