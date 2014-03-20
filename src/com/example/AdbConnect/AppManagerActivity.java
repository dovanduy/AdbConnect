package com.example.AdbConnect;

import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class AppManagerActivity extends Activity {

	private ListView mAppListView;
	private List<AppInfo> mAppInfos = new ArrayList<AppInfo>();
	private AppAdapter mAppAdapter;
	private AppManager mAppManager = new AppManager();
	private AppAsyncTask mAppAsyncTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
		setContentView(R.layout.app_manager);

		mAppListView = (ListView) findViewById(R.id.app_list_view);
		mAppAdapter = new AppAdapter();
		mAppListView.setAdapter(mAppAdapter);

		loadAllApplication();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.app_manager, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.refresh) {
			mAppInfos.clear();
			mAppAdapter.notifyDataSetChanged();
			loadAllApplication();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void loadAllApplication() {
		if (mAppAsyncTask != null) {
			mAppAsyncTask.cancel(false);
		}
		mAppAsyncTask = new AppAsyncTask();
		mAppAsyncTask.execute(0);
	}

	private void uninstall(String packageName) {
		ShellComand.exec("adb uninstall " + packageName);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mAppAsyncTask != null) {
			mAppAsyncTask.cancel(true);
		}
	}

	class AppAsyncTask extends AsyncTask<Integer, Void, List<AppInfo>> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected List<AppInfo> doInBackground(Integer... arg0) {
			return mAppManager.loadAllApplications();
		}

		@Override
		protected void onPostExecute(List<AppInfo> result) {
			if (result != null) {
				mAppInfos.addAll(result);
				mAppAdapter.notifyDataSetChanged();
			}
		}

	}

	class AppAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mAppInfos != null ? mAppInfos.size() : 0;
		}

		@Override
		public Object getItem(int arg0) {
			return null;
		}

		@Override
		public long getItemId(int arg0) {
			return 0;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup arg2) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = LayoutInflater.from(AppManagerActivity.this)
						.inflate(R.layout.app_item, null);
				holder = new ViewHolder(convertView);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			final AppInfo app = mAppInfos.get(position);
			// holder.icon.setImageResource(app.icon);
			// holder.label.setText(app.labelRes);
			// holder.version.setText("v" + app.versionName);
			holder.packageName.setText(app.packageName);
			holder.uninstall.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View arg0) {
					uninstall(app.packageName);
					mAppInfos.remove(position);
					notifyDataSetChanged();
				}
			});

			return convertView;
		}

	}

	final class ViewHolder {
		// public ImageView icon;
		//
		// public TextView label;
		//
		// public TextView version;

		public TextView uninstall;
		public TextView packageName;

		public ViewHolder(View rootView) {
			// icon = (ImageView) rootView.findViewById(R.id.icon);
			// label = (TextView) rootView.findViewById(R.id.label);
			// version = (TextView) rootView.findViewById(R.id.version);
			uninstall = (TextView) rootView.findViewById(R.id.uninstall);
			packageName = (TextView) rootView.findViewById(R.id.packageName);
		}
	}

}
