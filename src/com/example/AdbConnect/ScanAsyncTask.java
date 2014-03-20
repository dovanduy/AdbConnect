package com.example.AdbConnect;

import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.os.AsyncTask;
import android.util.Log;

public class ScanAsyncTask extends AsyncTask<Integer, InetAddress, Void> {

	private static final String TAG = ScanAsyncTask.class.getSimpleName();

	private ScanListener mScanListener;

	private ExecutorService mExecutorService;

	public ScanAsyncTask(ScanListener listener) {
		this.mScanListener = listener;
	}

	@Override
	protected void onPreExecute() {
		mScanListener.onScanStart();
	}

	@Override
	protected Void doInBackground(Integer... params) {
		int ip = params[0].intValue();
		long startTime = System.currentTimeMillis();
		this.mExecutorService = Executors.newFixedThreadPool(8);

		int m = ip >> 24 & 0xFF;
		int i = m - 1;
		int j = m + 1;

		while (i >= 0 || j < 255) {
			if (i >= 0) {
				scan(IpAddressUtils.makeIpAddress(ip, i));
				i--;
			}

			if (j < 255) {
				scan(IpAddressUtils.makeIpAddress(ip, j));
				j++;
			}
		}

		this.mExecutorService.shutdown();
		try {
			if (!this.mExecutorService.awaitTermination(30, TimeUnit.SECONDS)) {
				this.mExecutorService.shutdownNow();
			}
			Log.v(TAG, "scan complete @"
					+ (System.currentTimeMillis() - startTime) / 1000 + "s");
		} catch (InterruptedException ex) {
			ex.printStackTrace();
			Log.v(TAG, "scan interrupt @"
					+ (System.currentTimeMillis() - startTime) / 1000 + "s");
			this.mExecutorService.shutdownNow();
			Thread.currentThread().interrupt();
		}

		return null;
	}

	@Override
	protected void onProgressUpdate(InetAddress... values) {
		for (InetAddress address : values) {
			mScanListener.onScan(address);
		}
	}

	@Override
	protected void onPostExecute(Void result) {
		if (mExecutorService != null) {
			mExecutorService.shutdown();
		}
		mScanListener.onScanCompleted();
	}

	@Override
	protected void onCancelled() {
		if (mExecutorService != null) {
			mExecutorService.shutdownNow();
		}
		mScanListener.onScanCancel();
	}

	private void scan(int ip) {
		if (!this.mExecutorService.isShutdown()) {
			mExecutorService.execute(new ScanRunnable(this, ip));
		}
	}

	public void scanSuccess(InetAddress addr) {
		publishProgress(addr);
	}

}
