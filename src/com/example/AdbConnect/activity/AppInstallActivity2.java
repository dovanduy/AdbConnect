package com.example.AdbConnect.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.example.AdbConnect.R;
import com.example.AdbConnect.install.InstallEvent;
import com.example.AdbConnect.install.InstallListener;
import com.example.AdbConnect.install.InstallTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhangdi on 14-3-20.
 */
public class AppInstallActivity2 extends Activity implements InstallListener {

    private ListView mListView;
    private InstallAdapter mAdapter;

    private InstallTask mInstallTask;

    private String mConnectedAddress;

    List<InstallEvent> mInstallEvents = new ArrayList<InstallEvent>();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);

        setContentView(R.layout.activity_app_install2);

        if (getIntent() != null) {
            mConnectedAddress = getIntent().getStringExtra("address");
        }

        if (TextUtils.isEmpty(mConnectedAddress)) {
            finish();
            return;
        }

        InstallEvent event1 = new InstallEvent(1234567, "Demo1", "http://bcs.duapp.com/zhangdi/Demo1.apk", InstallEvent.STATUS_IDLE);
        mInstallEvents.add(event1);
        InstallEvent event2 = new InstallEvent(2345678, "Demo2", "http://bcs.duapp.com/zhangdi/Demo2.apk", InstallEvent.STATUS_IDLE);
        mInstallEvents.add(event2);
        InstallEvent event3 = new InstallEvent(3456789, "Demo3", "http://bcs.duapp.com/zhangdi/Demo3.apk", InstallEvent.STATUS_IDLE);
        mInstallEvents.add(event3);

        mListView = (ListView) findViewById(R.id.listview);
        mAdapter = new InstallAdapter();
        mListView.setAdapter(mAdapter);

        mInstallTask = new InstallTask(mConnectedAddress, this);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mInstallTask.destroy();
    }

    @Override
    public void onInstallStart(long id) {
        for (InstallEvent event : mInstallEvents) {
            if (event.id == id) {
                event.status = InstallEvent.STATUS_INSTALLING;
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onInstallCompleted(long id, boolean result) {
        for (InstallEvent event : mInstallEvents) {
            if (event.id == id) {
                if (result) {
                    event.status = InstallEvent.STATUS_INSTALL_SUCCESS;
                } else {
                    event.status = InstallEvent.STATUS_INSTALL_FAIL;
                }
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onInstallError() {
        for (InstallEvent event : mInstallEvents) {
            if (event.status == InstallEvent.STATUS_INSTALLING) {
                event.status = InstallEvent.STATUS_INSTALL_FAIL;
            }
        }
        mAdapter.notifyDataSetChanged();
    }


    class InstallAdapter extends BaseAdapter {

        public InstallAdapter() {

        }

        @Override
        public int getCount() {
            return mInstallEvents.size();
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
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(AppInstallActivity2.this).inflate(R.layout.install_item, null);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final InstallEvent event = mInstallEvents.get(position);
            holder.nameTv.setText(event.label);

            if (event.status == InstallEvent.STATUS_IDLE || event.status == InstallEvent.STATUS_INSTALL_FAIL
                    || event.status == InstallEvent.STATUS_INSTALL_SUCCESS) {
                holder.installBtn.setEnabled(true);
                holder.installBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mInstallTask.installApk(event);
                    }
                });
            } else {
                holder.installBtn.setEnabled(false);
            }

            if (event.status == InstallEvent.STATUS_IDLE) {
                holder.installStatusTv.setText("");
            } else if (event.status == InstallEvent.STATUS_INSTALLING) {
                holder.installStatusTv.setText("正在安装");
            } else if (event.status == InstallEvent.STATUS_INSTALL_SUCCESS) {
                holder.installStatusTv.setText("安装成功");
            } else if (event.status == InstallEvent.STATUS_INSTALL_FAIL) {
                holder.installStatusTv.setText("安装失败");
            }

            return convertView;
        }

        class ViewHolder {
            public TextView nameTv;
            public TextView installStatusTv;
            public Button installBtn;

            public ViewHolder(View convertView) {
                nameTv = (TextView) convertView.findViewById(R.id.apk_name);
                installStatusTv = (TextView) convertView.findViewById(R.id.install_status);
                installBtn = (Button) convertView.findViewById(R.id.install_btn);
            }
        }
    }

}