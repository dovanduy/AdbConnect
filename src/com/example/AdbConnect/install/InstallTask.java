package com.example.AdbConnect.install;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by zhangdi on 14-3-24.
 */
public class InstallTask implements Runnable {

    private InstallListener mInstallListener;

    private String mAddress;

    private Socket mSocket;

    private ExecutorService mInstallExecutor = Executors.newSingleThreadExecutor();

    private Thread mListenThread;

    private AtomicBoolean mStoped = new AtomicBoolean(true);

    private AtomicInteger mInstallCount = new AtomicInteger(0);

    private Handler mHandler = new Handler(Looper.getMainLooper());

    public InstallTask(String address, InstallListener listener) {
        mAddress = address;
        mInstallListener = listener;
        mStoped.set(true);

    }

    public void destroy() {
        try {
            if (mSocket != null) {
                mSocket.close();
            }
            mSocket = null;
        } catch (IOException e) {
            e.printStackTrace();
        }

        mStoped.set(true);
        try {
            if (mListenThread != null && mListenThread.isAlive()) {
                mListenThread.join(500);
            }
            mListenThread = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void installApk(final InstallEvent event) {
        mInstallListener.onInstallStart(event.id);

        final long id = event.id;
        final String url = event.url;
        mInstallExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    mInstallCount.getAndAdd(1);

                    if (mSocket == null || mSocket.isClosed()) {
                        mSocket = new Socket(mAddress, 7658);
                        mSocket.setSoTimeout(60000);
                        LOGD("socket connected, ip=" + mAddress + ", port=" + 7658);
                    }

                    if (mStoped.get()) {
                        mStoped.set(false);
                        mListenThread = new Thread(InstallTask.this);
                        mListenThread.start();
                    }

                    PrintWriter out = new PrintWriter(new BufferedWriter(
                            new OutputStreamWriter(mSocket.getOutputStream())), true);
                    LOGD("install apk, id = " + id + ", url = " + url);
                    out.println("install:" + id + ":" + event.label + ":" + url);
                } catch (IOException e) {
                    e.printStackTrace();
                    post(new Runnable() {
                        @Override
                        public void run() {
                            mInstallListener.onInstallCompleted(id, false);
                        }
                    });
                }
            }
        });

    }

    @Override
    public void run() {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (in == null) {
            post(new Runnable() {
                @Override
                public void run() {
                    mInstallListener.onInstallError();
                }
            });
            mInstallCount.set(0);
            return;
        }

        try {
            while (!mStoped.get()) {
                String response = in.readLine();
                LOGD(response);

                if (response != null && response.startsWith("install_over:")) {
                    int start = "install_over:".length();
                    int second = response.indexOf(":", start);
                    int third = response.indexOf(":", second + 1);
                    if (second <= start || third <= second || third <= start) {
                        continue;
                    }

                    final int result = Integer.parseInt(response.substring(start, second));
                    final long id = Long.parseLong(response.substring(second + 1, third));
                    String payload = response.substring(third + 1);

                    post(new Runnable() {
                        @Override
                        public void run() {
                            mInstallListener.onInstallCompleted(id, result == 0 ? true : false);
                        }
                    });

                    mInstallCount.decrementAndGet();
                }

                if (mInstallCount.get() <= 0) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            post(new Runnable() {
                @Override
                public void run() {
                    mInstallListener.onInstallError();
                }
            });
            mInstallCount.set(0);
        } finally {
            mStoped.set(true);
            if (mSocket != null) {
                try {
                    mSocket.close();
                    LOGD("socket closed");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mSocket = null;
            }
        }
    }

    private void post(Runnable runnable) {
        mHandler.post(runnable);
    }

    private void LOGD(String msg) {
        if (!TextUtils.isEmpty(msg)) {
            Log.d(InstallTask.class.getSimpleName(), msg);
        }
    }


}
