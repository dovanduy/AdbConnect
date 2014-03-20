package com.example.AdbConnect;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.util.Log;

public class ScanRunnable implements Runnable {

	private static final String TAG = ScanRunnable.class.getSimpleName();

	private ScanAsyncTask mScanAsyncTask;
	private InetAddress mAddress;

	public ScanRunnable(ScanAsyncTask task, int ip) {
		this.mScanAsyncTask = task;
		try {
			mAddress = IpAddressUtils.transform(ip);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		if (mAddress == null)
			return;

		long startTime = System.currentTimeMillis();
		try {
			Log.d(TAG, "start scanning " + mAddress.getHostAddress());
			Socket socket = new Socket(mAddress, 5555);
			socket.close();
			if (mScanAsyncTask != null) {
				Log.d(TAG, "success!device " + mAddress.getHostAddress()
						+ " is available @"
						+ (System.currentTimeMillis() - startTime) + "ms");
				mScanAsyncTask.scanSuccess(mAddress);
				return;
			}
		} catch (UnknownHostException e) {
//			e.printStackTrace();
		} catch (IOException e) {
//			e.printStackTrace();
		}
		Log.d(TAG, "fail!device " + mAddress.getHostAddress()
				+ " is not available @"
				+ (System.currentTimeMillis() - startTime) + "ms");
	}

}
