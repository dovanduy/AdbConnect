package com.example.AdbConnect;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;

import java.io.*;

public class MyActivity extends Activity implements View.OnClickListener {

    private EditText mIpInput;
    private Button mConnectBtn;
    private Button mDisconnectBtn;
    private TextView mConnectResultTv;

    private Button mSelectApkBtn;
    private TextView mSelectApkTv;
    private Button mInstallBtn;
    private ProgressBar mInstallLoadingProb;
    private TextView mInstallResultTv;

    private static final int REQUEST_CODE_SELECT_APK = 100000;

    private Handler mHandler = new Handler();

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mIpInput = (EditText) findViewById(R.id.ip_input);
        mConnectBtn = (Button) findViewById(R.id.connect_btn);
        mDisconnectBtn = (Button) findViewById(R.id.disconnect_btn);
        mConnectResultTv = (TextView) findViewById(R.id.connect_result);

        mSelectApkBtn = (Button) findViewById(R.id.select_apk_btn);
        mSelectApkTv = (TextView) findViewById(R.id.select_apk_tv);

        mInstallBtn = (Button) findViewById(R.id.install_btn);
        mInstallLoadingProb = (ProgressBar) findViewById(R.id.install_loading);
        mInstallResultTv = (TextView) findViewById(R.id.install_result);

        mConnectBtn.setOnClickListener(this);
        mDisconnectBtn.setOnClickListener(this);
        mSelectApkBtn.setOnClickListener(this);
        mInstallBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.connect_btn:
                connect();
                break;
            case R.id.disconnect_btn:
                disconnect();
                break;
            case R.id.select_apk_btn:
                selectApk();
                break;
            case R.id.install_btn:
                install();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_APK && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                String path = getPath(uri);
                if (path == null || !path.endsWith(".apk")) {
                    Toast.makeText(this, "不支持的文件", Toast.LENGTH_SHORT).show();
                } else {
                    mSelectApkTv.setText(getPath(uri));
                }
            }
        }
    }

    private void connect() {
        String ip = mIpInput.getText().toString();
        mConnectResultTv.setText(null);
        String connectResult = execShell("adb connect " + ip);
        mConnectResultTv.setText(connectResult);
    }

    private void disconnect() {
        mConnectResultTv.setText(null);
        String connectResult = execShell("adb disconnect");
        mConnectResultTv.setText(connectResult);
    }

    private void selectApk() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_CODE_SELECT_APK);
    }

    private void install() {
        final String apkPath = mSelectApkTv.getText().toString();
        if (TextUtils.isEmpty(apkPath)) {
            return;
        }

        mInstallLoadingProb.setVisibility(View.VISIBLE);
        mInstallResultTv.setText(null);

        new Thread() {
            @Override
            public void run() {
                final String installResult = execShell("adb install -r " + apkPath);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mInstallLoadingProb.setVisibility(View.GONE);
                        mInstallResultTv.setText(installResult);
                    }
                });
            }
        }.start();

    }

    public static String execShell(String cmd) {
        String[] cmdStrings = new String[]{"sh", "-c", cmd};
        StringBuffer retString = new StringBuffer();

        try {
            Process process = Runtime.getRuntime().exec(cmdStrings);
            BufferedReader stdout = new BufferedReader(new InputStreamReader(
                    process.getInputStream()));
            BufferedReader stderr = new BufferedReader(new InputStreamReader(
                    process.getErrorStream()));

            String line;
            while (!TextUtils.isEmpty(line = stdout.readLine())) {
                retString.append(line + "\n");
            }
            retString.append("\n");
            while (!TextUtils.isEmpty(line = stderr.readLine())) {
                retString.append(line + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
            retString.append(e.getMessage());
        }
        return retString.toString();
    }

    public String getPath(Uri uri) {
        String path = null;
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                path = cursor.getString(column_index);
                cursor.close();
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            path = uri.getPath();
        }

        return path;
    }

}
