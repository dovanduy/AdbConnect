package com.example.AdbConnect.connect;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import com.example.AdbConnect.utils.ShellComand;

import java.io.*;

/**
 * Created by zhangdi on 14-3-20.
 */
public class ConnectTask extends AsyncTask<Void, String, Boolean> {

    public static final String SERVER_APK_LAUNCH_ACTIVITY = "com.zhangdi.tvdeamon/.MainActivity";

    public static final String SERVER_APK_FILE = "TvDeamon.apk";

    private Context mContext;

    private String mAddress;

    private ConnectListener mConnectListener;

    public ConnectTask(Context context, String address, ConnectListener listener) {
        mContext = context;
        mAddress = address;
        mConnectListener = listener;
    }

    @Override
    protected Boolean doInBackground(Void[] params) {
        if (!connect(mAddress)) {
            LOGD("adb connect " + mAddress + " fail");
            publishProgress("连接失败");
            return Boolean.FALSE;
        }
        LOGD("adb connect " + mAddress + " success");

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (!hasInstalledTvDeamon()) {
            String path = fetchDiskDir() + "/" + SERVER_APK_FILE;
            if (!copyAsset2Disk(SERVER_APK_FILE, path)) {
                LOGD("copy server.apk to disk fail");
                publishProgress("copy " + SERVER_APK_FILE + " to disk error");
                return Boolean.FALSE;
            }
            LOGD("copy " + SERVER_APK_FILE + " to disk success");

            String[] array1 = new String[3];
            array1[0] = mAddress;
            array1[1] = "5555";
            array1[2] = path;
            String ret = ShellComand.exec(String.format("adb -s %s:%s install -r %s", array1));
            LOGD("adb install");
            LOGD(ret);
            if (ret != null && ret.contains("Success")) {
                LOGD("adb install " + SERVER_APK_FILE + " success");

                String array2[] = new String[3];
                array2[0] = mAddress;
                array2[1] = "5555";
                array2[2] = SERVER_APK_LAUNCH_ACTIVITY;
                ret = ShellComand.exec(String.format("adb -s %s:%s shell am start -n %s", array2));
                LOGD("adb shell am start");
                LOGD(ret);
                if (ret != null && (ret.contains("Error") || ret.contains("error"))) {
                    LOGD("adb shell am start " + array2[2] + " fail");
                    publishProgress("启动TV端失败");
                    return Boolean.FALSE;
                }
                LOGD("adb shell am start " + array2[2] + " success");
            } else {
                LOGD("adb install " + SERVER_APK_FILE + " fail");
                publishProgress("安装失败");
                return Boolean.FALSE;
            }
        } else {
            LOGD(mAddress + " has already installed server.apk");
        }

        return Boolean.TRUE;
    }

    @Override
    protected void onPreExecute() {
        mConnectListener.onConnectStart(mAddress);
    }

    @Override
    protected void onProgressUpdate(String... values) {
        mConnectListener.onConnectError(mAddress, values[0]);
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        mConnectListener.onConnectCompleted(mAddress, aBoolean.booleanValue());
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        mConnectListener.onConnectCancelled(mAddress);
    }

    private boolean connect(String address) {
        disconnect();
        String ret = ShellComand.exec("adb connect " + address);
        LOGD("adb connect " + address);
        LOGD(ret);
        if (ret != null && ret.contains("connected")) {
            return true;
        }
        return false;
    }

    private void disconnect() {
        ShellComand.exec("adb disconnect");
    }

    private boolean hasInstalledTvDeamon() {
        String[] array = new String[3];
        array[0] = mAddress;
        array[1] = "5555";
        array[2] = SERVER_APK_LAUNCH_ACTIVITY;
        String ret = ShellComand.exec(String.format("adb -s %s:%s shell am start -n %s", array));
        LOGD("adb shell am start");
        LOGD(ret);
        if (ret != null && (ret.contains("Error") || ret.contains("error"))) {
            return false;
        }
        return true;
    }

    private String fetchDiskDir() {
        String path = mContext.getExternalCacheDir().getAbsolutePath() + "/server/";
        File file = new File(path);
        if (file.exists() && !file.isDirectory()) {
            file.delete();
        }
        if (!file.exists()) {
            file.mkdirs();
        }
        return path;
    }

    private boolean copyAsset2Disk(String assetPath, String path) {
        boolean flag = false;
        AssetManager assetManager = mContext.getAssets();
        InputStream is = null;
        FileOutputStream os = null;
        try {
            is = assetManager.open(assetPath);
            os = new FileOutputStream(path);
            byte[] buffer = new byte[1024];
            int count;
            while ((count = is.read(buffer)) > 0) {
                os.write(buffer, 0, count);
            }
            os.flush();
            flag = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return flag;
    }

    private void LOGD(String msg) {
        if (!TextUtils.isEmpty(msg)) {
            Log.d(ConnectTask.class.getSimpleName(), msg);
        }
    }

}
