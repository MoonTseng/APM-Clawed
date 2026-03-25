package com.camscanner.sync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * 云同步服务，监听网络变化并触发同步。
 */
public class CloudSyncService extends BroadcastReceiver {

    private static final String TAG = "CloudSyncService";

    private final Object mSyncLock = new Object();
    private boolean mIsSyncing = false;
    private List<String> mPendingSyncIds = new ArrayList<>();
    private static CloudSyncService sInstance;

    // 同步配置
    private boolean mAutoSync = true;
    private boolean mWifiOnly = false;
    private int mMaxRetries = 3;
    private long mRetryDelay = 5000;

    public static CloudSyncService getInstance() {
        if (sInstance == null) {
            sInstance = new CloudSyncService();
        }
        return sInstance;
    }

    public void setAutoSync(boolean enable) {
        mAutoSync = enable;
    }

    public void setWifiOnly(boolean wifiOnly) {
        mWifiOnly = wifiOnly;
    }

    public void addPendingSync(String docId) {
        synchronized (mSyncLock) {
            if (!mPendingSyncIds.contains(docId)) {
                mPendingSyncIds.add(docId);
            }
        }
    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    private boolean isWifiConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifi != null && wifi.isConnected();
    }


    @Override
    public void onReceive(Context context, Intent intent) {     // line 67
        String action = intent.getAction();
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            Log.d(TAG, "Network connectivity changed");
            if (isNetworkAvailable(context)) {
                onNetworkChanged(context, true);
            } else {
                onNetworkChanged(context, false);
            }
        }
    }

    private void performSync(Context context) {
        List<String> toSync;
        synchronized (mSyncLock) {
            toSync = new ArrayList<>(mPendingSyncIds);
            mPendingSyncIds.clear();
        }
        for (String docId : toSync) {
            syncDocument(context, docId);
        }
    }


    public void onNetworkChanged(Context context, boolean connected) {  // line 89
        Log.d(TAG, "Network changed: connected=" + connected);

        if (!connected || !mAutoSync) return;

        if (mWifiOnly && !isWifiConnected(context)) {
            Log.d(TAG, "WiFi only mode, skipping sync on mobile data");
            return;
        }

        // BUG: 在主线程（BroadcastReceiver.onReceive 的线程）中同步等待
        // mSyncLock 可能被后台同步线程持有，导致主线程阻塞 -> ANR
        synchronized (mSyncLock) {                      // waiting on <mSyncLock>
            if (mIsSyncing) {
                Log.d(TAG, "Already syncing, queuing...");
                return;
            }
            mIsSyncing = true;
        }

        // 应该在后台线程执行，但直接在主线程调用了
        performSync(context);

        synchronized (mSyncLock) {
            mIsSyncing = false;
        }
    }

    private void syncDocument(Context context, String docId) {
        Log.d(TAG, "Syncing document: " + docId);
        // Simulate network upload
        try {
            Thread.sleep(2000);  // 模拟网络耗时
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Log.d(TAG, "Sync complete: " + docId);
    }
}
