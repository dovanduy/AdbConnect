package com.example.AdbConnect.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import com.example.AdbConnect.connect.ConnectListener;
import com.example.AdbConnect.connect.ConnectTask;
import com.example.AdbConnect.utils.IpAddressUtils;
import com.example.AdbConnect.R;
import com.example.AdbConnect.scan.ScanListener;
import com.example.AdbConnect.scan.ScanTask;
import com.example.AdbConnect.utils.ShellComand;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener, OnItemClickListener {

    private Button mScanBtn;
    private ProgressBar mScanProg;
    private ListView mAddressListView;

    private BaseAdapter mAddressAdapter;
    private List<String> mAddresses = new ArrayList<String>();

    private int mLocalIp;
    private ScanTask mScanTask;
    private ConnectTask mConnectTask;

    private BroadcastTask mBroadcastTask;

    private String mConnectedAddress;

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
        getActionBar().setTitle("未连接");
        setContentView(R.layout.main);

        mScanBtn = (Button) findViewById(R.id.scan_btn);
        mScanBtn.setOnClickListener(this);
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

        mBroadcastTask = new BroadcastTask();
        mBroadcastTask.execute(new Void[0]);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mScanTask != null) {
            mScanTask.cancel(true);
        }
        if (mConnectTask != null) {
            mConnectTask.cancel(true);
        }
        if (mBroadcastTask != null) {
            mBroadcastTask.cancel(true);
        }
        new Thread() {
            @Override
            public void run() {
                ShellComand.exec("adb disconnect");
            }
        }.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        mScanProg = (ProgressBar) (menu.findItem(R.id.loading).getActionView().findViewById(R.id.progressbar));
        mScanProg.setVisibility(View.GONE);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.scan_btn:
                if (mScanTask != null) {
                    mScanTask.cancel(true);
                }
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mScanTask = new ScanTask(mScanListener);
                        mScanTask.execute(mLocalIp);
                    }
                }, 50);
                break;
            case R.id.app_install: {
                if (TextUtils.isEmpty(mConnectedAddress)) {
                    Toast.makeText(MainActivity.this, "未连接设备", Toast.LENGTH_SHORT).show();
                } else {
                    if (mScanTask != null) {
                        mScanTask.cancel(true);
                    }
                    Intent intent = new Intent(this, AppInstallActivity2.class);
                    intent.putExtra("address", mConnectedAddress);
                    startActivity(intent);
                }
                break;
            }
            case R.id.app_manager: {
                if (TextUtils.isEmpty(mConnectedAddress)) {
                    Toast.makeText(MainActivity.this, "未连接设备", Toast.LENGTH_SHORT).show();
                } else {
                    if (mScanTask != null) {
                        mScanTask.cancel(true);
                    }
                    Intent intent = new Intent(this, AppManagerActivity.class);
                    intent.putExtra("address", mConnectedAddress);
                    startActivity(intent);
                }
                break;
            }
        }
    }

    private ScanListener mScanListener = new ScanListener() {
        @Override
        public void onScanStart() {
            mScanProg.setVisibility(View.VISIBLE);
            mAddresses.clear();
            mAddressAdapter.notifyDataSetChanged();
        }

        @Override
        public void onScan(String address) {
            mAddresses.add(address);
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
    };

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        String address = mAddresses.get(arg2);
        if (mScanTask != null) {
            mScanTask.cancel(true);
        }
        if (mConnectTask != null) {
            mConnectTask.cancel(true);
        }
        mConnectTask = new ConnectTask(this, address, mConnectListener);
        mConnectTask.execute(new Void[0]);
    }

    private ConnectListener mConnectListener = new ConnectListener() {

        @Override
        public void onConnectError(String address, String msg) {
            Toast.makeText(MainActivity.this, address + ":" + msg, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onConnectCompleted(String address, boolean result) {
            if (result) {
                getActionBar().setTitle("已连接" + address);
                mConnectedAddress = address;
            } else {
                getActionBar().setTitle("未连接");
                mConnectedAddress = null;
            }
        }

        @Override
        public void onConnectCancelled(String address) {
            getActionBar().setTitle("未连接");
            mConnectedAddress = null;
            Toast.makeText(MainActivity.this, "取消连接" + address, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onConnectStart(String address) {
            getActionBar().setTitle("正在连接到" + address + "...");
            mConnectedAddress = null;
        }

    };

    private class BroadcastTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void[] params) {
            String address = null;
            DatagramSocket sendSocket = null;
            try {
                sendSocket = new DatagramSocket();
                sendSocket.setBroadcast(true);
                String mes = "i am xunlei tvzhushou!";
                byte[] buf = mes.getBytes();
                int port = 7659;
                InetAddress ip = InetAddress.getByName("255.255.255.255");
                DatagramPacket sendPacket = new DatagramPacket(buf, buf.length, ip, port);
                sendSocket.send(sendPacket);
                LOGD("send broadcast packet at port " + port + ":" + mes);

                byte[] getBuf = new byte[1024];
                DatagramPacket getPacket = new DatagramPacket(getBuf, getBuf.length);
                sendSocket.setSoTimeout(10000);
                sendSocket.receive(getPacket);
                String backMes = new String(getBuf, 0, getPacket.getLength());
                LOGD("received broadcast packet at port " + port + ":" + backMes);
                if (backMes != null && backMes.startsWith("ok!i am tv!")) {
                    address = getPacket.getAddress().getHostAddress();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (sendSocket != null) {
                    sendSocket.close();
                }
            }
            return address;
        }

        @Override
        protected void onPostExecute(String s) {
            if (!TextUtils.isEmpty(s) && !mAddresses.contains(s)) {
                mAddresses.add(0, s);
                mAddressAdapter.notifyDataSetChanged();
            }
        }
    }

    private void LOGD(String msg) {
        if (!TextUtils.isEmpty(msg)) {
            Log.d(MainActivity.class.getSimpleName(), msg);
        }
    }

}
