package com.example.AdbConnect.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.example.AdbConnect.R;
import com.example.AdbConnect.utils.ShellComand;
import com.example.AdbConnect.application.AppInfo;
import com.example.AdbConnect.application.AppManager;

public class AppManagerActivity extends Activity {

    private ListView mAppListView;
    private List<AppInfo> mAppInfos = new ArrayList<AppInfo>();
    private AppAdapter mAppAdapter;

    private ProgressBar mProgressBar;

    private AppManager mAppManager;
    private AppAsyncTask mAppAsyncTask;

    private String mConnectedAddress;

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
        setContentView(R.layout.app_manager);

        if (getIntent() != null) {
            mConnectedAddress = getIntent().getStringExtra("address");
        }
        if (TextUtils.isEmpty(mConnectedAddress)) {
            finish();
            return;
        }
        mAppManager = new AppManager(mConnectedAddress);

        mAppListView = (ListView) findViewById(R.id.app_list_view);
        mAppAdapter = new AppAdapter();
        mAppListView.setAdapter(mAppAdapter);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                loadAllApplication();
            }
        }, 300);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        mProgressBar = (ProgressBar) (menu.findItem(R.id.loading).getActionView().findViewById(R.id.progressbar));
        mProgressBar.setVisibility(View.GONE);
        return super.onCreateOptionsMenu(menu);
    }

    private void loadAllApplication() {
        if (mAppAsyncTask != null) {
            mAppAsyncTask.cancel(false);
        }
        mAppAsyncTask = new AppAsyncTask();
        mAppAsyncTask.execute(new Void[0]);
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

    class AppAsyncTask extends AsyncTask<Void, Void, List<AppInfo>> {

        @Override
        protected void onPreExecute() {
            mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<AppInfo> doInBackground(Void... arg0) {
            return mAppManager.loadAllApplications();
        }

        @Override
        protected void onPostExecute(List<AppInfo> result) {
            if (result != null) {
                mAppInfos.addAll(result);
                mAppAdapter.notifyDataSetChanged();
            }
            mProgressBar.setVisibility(View.GONE);
        }

        @Override
        protected void onCancelled() {
            mProgressBar.setVisibility(View.GONE);
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
