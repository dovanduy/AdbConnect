package com.example.AdbConnect;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener,
		ScanListener, OnItemClickListener {

	private Button mScanBtn;
	private ProgressBar mScanProg;
	private ListView mAddressListView;

	private BaseAdapter mAddressAdapter;
	private List<String> mAddresses = new ArrayList<String>();

	private int mLocalIp;
	private ScanAsyncTask mScanAsyncTask;

	private Button mAppInstall;
	private Button mAppManager;

	private Handler mHandler = new Handler();

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
		setContentView(R.layout.main);
		
		mScanBtn = (Button) findViewById(R.id.scan_btn);
		mScanBtn.setOnClickListener(this);
		mScanProg = (ProgressBar) findViewById(R.id.scan_prog);
		mAddressListView = (ListView) findViewById(R.id.device_listview);

		mAddressAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_expandable_list_item_1,
				android.R.id.text1, mAddresses);
		mAddressListView.setAdapter(mAddressAdapter);
		mAddressListView.setOnItemClickListener(this);

		mAppInstall = (Button) findViewById(R.id.app_install);
		mAppInstall.setOnClickListener(this);
		mAppManager = (Button) findViewById(R.id.app_manager);
		mAppManager.setOnClickListener(this);

		mLocalIp = IpAddressUtils.getLocalIpAddress(this);
		disconnect();
		getActionBar().setTitle("未连接");

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mScanAsyncTask != null) {
			mScanAsyncTask.cancel(true);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.scan_btn:
			if (mScanAsyncTask != null) {
				mScanAsyncTask.cancel(true);
			}
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mScanAsyncTask = new ScanAsyncTask(MainActivity.this);
					mScanAsyncTask.execute(mLocalIp);
				}
			}, 50);
			break;
		case R.id.app_install:
			if (mScanAsyncTask != null) {
				mScanAsyncTask.cancel(true);
			}
			startActivity(new Intent(this, AppInstallActivity.class));
			break;
		case R.id.app_manager:
			if (mScanAsyncTask != null) {
				mScanAsyncTask.cancel(true);
			}
			startActivity(new Intent(this, AppManagerActivity.class));
			break;
		}
	}

	@Override
	public void onScanStart() {
		mScanProg.setVisibility(View.VISIBLE);
		getActionBar().setTitle("未连接");
		mAddresses.clear();
		mAddressAdapter.notifyDataSetChanged();
	}

	@Override
	public void onScan(InetAddress address) {
		// String hostName = address.getHostName();
		String hostAddress = address.getHostAddress();
		// String device = hostName + "(" + hostAddress + ")";
		mAddresses.add(hostAddress);
		mAddressAdapter.notifyDataSetChanged();
	}

	@Override
	public void onScanCompleted() {
		mScanProg.setVisibility(View.GONE);
	}

	@Override
	public void onScanCancel() {
		mScanProg.setVisibility(View.GONE);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		String address = mAddresses.get(arg2);
		disconnect();
		connect(address);
	}

	private void connect(String address) {
		String ret = ShellComand.exec("adb connect " + address);
		if (ret != null && ret.contains("connected")) {
			getActionBar().setTitle(address);
		} else {
			getActionBar().setTitle("未连接");
		}
	}

	private void disconnect() {
		ShellComand.exec("adb disconnect");
	}

}
