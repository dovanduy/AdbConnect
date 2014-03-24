package com.example.AdbConnect.application;

import java.util.ArrayList;
import java.util.List;

import android.text.TextUtils;
import android.util.Log;
import com.example.AdbConnect.utils.ShellComand;

public class AppManager {

    private String mAddress;

    public AppManager(String address) {
        this.mAddress = address;
    }

    public List<AppInfo> loadAllApplications() {
        List<String> packages = loadAllPackages();
        if (packages != null) {
            List<AppInfo> apps = new ArrayList<AppInfo>();
            for (String pack : packages) {
                AppInfo app = new AppInfo();
                app.packageName = pack;
                apps.add(app);
//				try {
//					loadApplication(app);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
            }
            return apps;
        }
        return null;
    }

    private List<String> loadAllPackages() {
        String cmd = String.format("adb -s %s:%s shell pm list packages -3", mAddress, "5555");
        Log.d(AppManager.class.getSimpleName(), cmd);
        String ret = ShellComand.exec(cmd);
        Log.d(AppManager.class.getSimpleName(), ret);
        if (ret != null) {
            String[] packages = ret.split("package:");
            if (packages != null) {
                List<String> packageList = new ArrayList<String>();
                for (String pack : packages) {
                    if (!TextUtils.isEmpty(pack)) {
                        packageList.add(pack.trim());
                    }
                }
                return packageList;
            }
        }
        return null;
    }

    private void loadApplication(AppInfo app) throws Exception {
        String packageName = app.packageName;
        String ret = ShellComand
                .exec("adb shell dumpsys package|grep -A18 \'Package \\["
                        + packageName + "\\]\'");
        if (ret != null) {
            int start = ret.indexOf("versionCode=");
            int end = ret.indexOf(" ", start);
            if (start >= 0 && end >= 0 && start + 12 < end) {
                String versionCode = ret.substring(start + 12, end);
                app.versionCode = Integer.parseInt(versionCode.trim());
            }

            start = ret.indexOf("versionName=");
            end = ret.indexOf(" ", start);
            if (start >= 0 && end >= 0 && start + 12 < end) {
                String versionName = ret.substring(start + 12, end);
                app.versionName = versionName;
            }
        }

        ret = ShellComand.exec("adb shell dumpsys|grep -A2 'packageName="
                + packageName + "'|grep 'labelRes'");
        if (ret != null) {
            int start = ret.indexOf("labelRes=");
            int end = ret.indexOf(" ", start);
            if (start >= 0 && end >= 0 && start + 9 < end) {
                String labelRes = ret.substring(start + 9, end);
                app.labelRes = Integer.parseInt(labelRes.trim());
            }

            start = ret.indexOf("icon=");
            end = ret.indexOf(" ", start);
            if (start >= 0 && end >= 0 && start + 4 < end) {
                String icon = ret.substring(start + 4, end);
                app.icon = Integer.parseInt(icon.trim());
            }
        }
    }

}
