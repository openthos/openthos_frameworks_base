package com.android.systemui.statusbar.notificationbars;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.android.systemui.R;
import android.content.BroadcastReceiver;
import android.os.BatteryManager;
import android.content.Intent;
import android.content.IntentFilter;
import com.android.internal.os.BatteryStatsHelper;
import android.os.BatteryStats;
import android.os.UserManager;
import java.util.List;
import android.os.UserHandle;
import android.os.SystemClock;

public class BatteryDialog extends BaseSettingDialog {
    private static final String BATTERY_HISTORY_FILE = "tmp_bat_history.bin";
    private static final String BATTERY_PERCENT_TITLE = "电池";
    private TextView mBatteryPercentage;
    private TextView mBatteryRemaining;
    private BatteryReceive batteryReceive;
    private BatteryStatsHelper mBatteryStatsHelper;
    private UserManager mUserManager;
    private BatteryStats mBatteryStats;

    public BatteryDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public void show(View v) {
        super.show(v);
    }

    @Override
    protected void initViews() {
        final AudioManager audioManager = (AudioManager) mContext.getSystemService(
                                                                      Context.AUDIO_SERVICE);
        View mediaView = LayoutInflater.from(mContext).inflate(R.layout.status_bar_battery, null);
        setContentView(mediaView);
        mBatteryPercentage = (TextView) mediaView.findViewById(R.id.battery_time_percentage);
        mBatteryRemaining = (TextView) mediaView.findViewById(R.id.battery_time_remaining);
        mUserManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        mBatteryStatsHelper = new BatteryStatsHelper(mContext);
        BatteryStatsHelper.dropFile(mContext, BATTERY_HISTORY_FILE);
        final List<UserHandle> profiles = mUserManager.getUserProfiles();
        mBatteryStatsHelper.create(new Bundle());
        mBatteryStats =  mBatteryStatsHelper.getStats();
        mBatteryStatsHelper.refreshStats(BatteryStats.STATS_SINCE_CHARGED, profiles);
        mContentView = mediaView;
    }

    private class BatteryReceive extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                int level = (int)(100f * intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                            / intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
                if (level == 100) {
                    mBatteryPercentage.setText(R.string.battery_percent_full);
                } else {
                    mBatteryPercentage.setText(BATTERY_PERCENT_TITLE + level + "%");
                }
                long batteryRemaining = mBatteryStats.computeBatteryRealtime(
                        SystemClock.elapsedRealtime() * 1000, BatteryStats.STATS_SINCE_CHARGED);
                long hours = (batteryRemaining % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
                long minutes = (batteryRemaining % (1000 * 60 * 60)) / (1000 * 60);
                String strBatteryRemaining = mContext.getResources().getString(
                                             R.string.battery_remaining, hours, minutes);
                mBatteryRemaining.setText(strBatteryRemaining);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        batteryReceive = new BatteryReceive();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        mContext.registerReceiver(batteryReceive, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mContext.unregisterReceiver(batteryReceive);
    }
}
