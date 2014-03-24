package com.zhangdi.tvdeamon;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends Activity {

    private ListView mListView;

    private MyAdapter mMyAdapter;

    private List<ApkItem> mApkItems = new ArrayList<ApkItem>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mListView = (ListView) findViewById(R.id.listview);
        mMyAdapter = new MyAdapter();
        mListView.setAdapter(mMyAdapter);

        IntentFilter intentFilter = new IntentFilter(TVService.ACTION_INSTALLED);
        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onStart() {
        super.onStart();
        TVService.startService(this);
        reload();
    }

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && TVService.ACTION_INSTALLED.equals(intent.getAction())) {
                reload();
            }
        }
    };

    private void reload() {
        List<ApkItem> apkItems = new InstallManager(this).getAllApks();
        mApkItems.clear();
        if (apkItems != null) {
            mApkItems.addAll(apkItems);
        }
        mMyAdapter.notifyDataSetChanged();
    }

    private class MyAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mApkItems.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.installed_item, null);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            ApkItem item = mApkItems.get(position);
            holder.packageTv.setText(item.packageName);
            holder.timestampTv.setText("" + convertTimestamp(item.timestamp));

            return convertView;
        }

        class ViewHolder {
            public TextView packageTv;

            public TextView timestampTv;

            public ViewHolder(View convertView) {
                packageTv = (TextView) convertView.findViewById(R.id.packageName);
                timestampTv = (TextView) convertView.findViewById(R.id.timestamp);
            }
        }
    }

    private String convertTimestamp(long time) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(new Date(time));
    }

}
