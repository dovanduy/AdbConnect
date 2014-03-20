package com.example.AdbConnect;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

public class AppInstallActivity extends Activity implements
		View.OnClickListener {

	private Button mSelectApkBtn;
	private TextView mSelectApkTv;
	private Button mInstallBtn;
	private ProgressBar mInstallLoadingProb;
	private TextView mInstallResultTv;

	private static final int REQUEST_CODE_SELECT_APK = 100000;
	private Handler mHandler = new Handler();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
		setContentView(R.layout.app_install);

		mSelectApkBtn = (Button) findViewById(R.id.select_apk_btn);
		mSelectApkTv = (TextView) findViewById(R.id.select_apk_tv);

		mInstallBtn = (Button) findViewById(R.id.install_btn);
		mInstallLoadingProb = (ProgressBar) findViewById(R.id.install_loading);
		mInstallResultTv = (TextView) findViewById(R.id.install_result);

		mSelectApkBtn.setOnClickListener(this);
		mInstallBtn.setOnClickListener(this);
	}

	@Override
	public void onClick(View arg0) {
		switch (arg0.getId()) {
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
		if (requestCode == REQUEST_CODE_SELECT_APK && resultCode == RESULT_OK
				&& data != null) {
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
				final String installResult = ShellComand.exec("adb install -r "
						+ apkPath);
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

	public String getPath(Uri uri) {
		String path = null;
		if ("content".equalsIgnoreCase(uri.getScheme())) {
			String[] projection = { MediaStore.Images.Media.DATA };
			Cursor cursor = getContentResolver().query(uri, projection, null,
					null, null);
			if (cursor != null) {
				int column_index = cursor
						.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
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
