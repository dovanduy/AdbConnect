package com.example.AdbConnect.scan;

import java.io.IOException;
import java.net.*;

import android.util.Log;
import com.example.AdbConnect.utils.IpAddressUtils;

public class ScanRunnable implements Runnable {

    private static final String TAG = ScanRunnable.class.getSimpleName();

    private ScanTask mScanAsyncTask;
    private int mIp;

    public ScanRunnable(ScanTask task, int ip) {
        this.mScanAsyncTask = task;
        this.mIp = ip;
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        String hostAddress = IpAddressUtils.transformToHost(mIp);
        try {
            Log.d(TAG, "start scanning " + hostAddress);
            SocketAddress remoteAddr = new InetSocketAddress(hostAddress, 5555);
            Socket socket = new Socket();
            socket.connect(remoteAddr, 1200);
            socket.close();
            Log.d(TAG, "success!device " + hostAddress
                    + " is available @"
                    + (System.currentTimeMillis() - startTime) + "ms");
            mScanAsyncTask.scanSuccess(hostAddress);
            return;
        } catch (UnknownHostException e) {

        } catch (IOException e) {

        }
        Log.d(TAG, "fail!device " + hostAddress
                + " is not available @"
                + (System.currentTimeMillis() - startTime) + "ms");
    }

}
