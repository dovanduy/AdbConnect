package com.example.AdbConnect.install;

/**
 * Created by zhangdi on 14-3-24.
 */
public class InstallEvent {

    public static final int STATUS_IDLE = 0;
    public static final int STATUS_INSTALLING = 1;
    public static final int STATUS_INSTALL_FAIL = 2;
    public static final int STATUS_INSTALL_SUCCESS = 3;

    public long id;

    public String label;

    public String url;

    public int status;

    public InstallEvent() {

    }

    public InstallEvent(long id, String label, String url, int status) {
        this.id = id;
        this.label = label;
        this.url = url;
        this.status = status;
    }
}
