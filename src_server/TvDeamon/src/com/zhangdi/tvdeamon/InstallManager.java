package com.zhangdi.tvdeamon;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhangdi on 14-3-24.
 */
public class InstallManager {

    public static final String KEY = "installed_apk";

    private Context mContext;

    public InstallManager(Context context) {
        mContext = context;
    }


    public void installPackage(String packageName) {
        ApkItem item = new ApkItem();
        item.packageName = packageName;
        item.timestamp = System.currentTimeMillis();

        SharedPreferences sharedPreferences = mContext.getSharedPreferences("apk", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        String apkStrs = sharedPreferences.getString(KEY, "");

        Gson gson = new Gson();
        List<ApkItem> apkItems = null;
        if (apkStrs != null) {
            apkItems = gson.fromJson(apkStrs, new TypeToken<List<ApkItem>>() {
            }.getType());
        }

        if (apkItems == null) {
            apkItems = new ArrayList<ApkItem>();
        }
        for (int i = apkItems.size() - 1; i >= 0; i--) {
            ApkItem it = apkItems.get(i);
            if (it.packageName.equals(packageName)) {
                apkItems.remove(i);
            }
        }
        apkItems.add(0, item);

        editor.putString(KEY, gson.toJson(apkItems)).commit();
    }

    public List<ApkItem> getAllApks() {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("apk", Context.MODE_PRIVATE);
        String apkStrs = sharedPreferences.getString(KEY, "");
        if (apkStrs != null) {
            Gson gson = new Gson();
            return gson.fromJson(apkStrs, new TypeToken<List<ApkItem>>() {
            }.getType());
        }
        return null;
    }
}
