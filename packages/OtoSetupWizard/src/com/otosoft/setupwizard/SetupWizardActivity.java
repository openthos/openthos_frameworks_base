package com.otosoft.setupwizard;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemProperties;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.provider.Settings;
import com.android.internal.app.LocalePicker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

public class SetupWizardActivity extends BaseActivity {
    private TextView mNext;
    private static Locale mCurrentLocale = null;
    private LinearLayout mLanguageContainer;
    private static final int CHOOSE_CHINA_ITEM = 1;
    private static final int CHOOSE_ENGLISH_ITEM = 2;
    private static final int DEFAULT_SLEEP_TIME = 1800000; // 30 min -- 1800 seconds
    private int noSelectedBg;
    private int selectedBg;
    private final Runnable mRequestFocus = new Runnable() {
        public void run() {
            if (SetupWizardActivity.this.mNext != null) {
                SetupWizardActivity.this.mNext.requestFocusFromTouch();
            }
        }
    };
    private  TextView mChinese;
    private  TextView mEnglish;
    private int chooseItem = CHOOSE_CHINA_ITEM;
    private static final String PROPERTY_NATIVEBRIDGE = "persist.sys.nativebridge";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //make the app compatability available
        SystemProperties.set(PROPERTY_NATIVEBRIDGE,"1");
        if (mCurrentLocale == null) {
            mCurrentLocale = Locale.CHINA;
            updateLocale(mCurrentLocale);
        }
        setContentView(R.layout.activity_setupwizard);
        //send broadcast to control status bar
        Intent intent1 = new Intent();
        intent1.setAction(Intent.STATUS_BAR_HIDE);
        SetupWizardActivity.this.sendBroadcast(intent1);
        Settings.System.putInt(getContentResolver(),
                android.provider.Settings.System.SCREEN_OFF_TIMEOUT, DEFAULT_SLEEP_TIME);
        this.mChinese = (TextView) findViewById(R.id.tv_chinese);
        this.mEnglish = (TextView) findViewById(R.id.tv_english);
        Resources res = getBaseContext().getResources();
        noSelectedBg = res.getColor(R.color.no_secleted_bg);
        selectedBg = res.getColor(R.color.selected_bg);
        mChinese.setBackgroundColor(selectedBg);
        this.mChinese.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mChinese.setBackgroundColor(selectedBg);
                mEnglish.setBackgroundColor(noSelectedBg);
                chooseItem = CHOOSE_CHINA_ITEM;
            }
        });

        this.mEnglish.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mChinese.setBackgroundColor(noSelectedBg);
                mEnglish.setBackgroundColor(selectedBg);
                chooseItem = CHOOSE_ENGLISH_ITEM;
            }
        });
        mNext = (TextView) findViewById(R.id.tv_next);
        mNext.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                switch(chooseItem){
                    case CHOOSE_CHINA_ITEM:
                        mCurrentLocale = Locale.CHINA;
                        new Handler().post(new Runnable() {
                            public void run() {
                                updateLocale(mCurrentLocale);
                            }
                        });
                        break;
                    case CHOOSE_ENGLISH_ITEM:
                        mCurrentLocale = Locale.US;
                        new Handler().post(new Runnable() {
                            public void run() {
                                updateLocale(mCurrentLocale);
                            }
                        });
                        break;
                    default:
                        break;
                }
                SetupWizardActivity.this.startActivity(SetupWizardActivity.this.buildWifiSetupIntent());
            }
        });
    }

    public void onResume() {
        super.onResume();
        this.mRequestFocus.run();
        new Handler().postDelayed(this.mRequestFocus, 500);
    }

    protected void onNewIntent(Intent intent) {
        if (intent != null && (intent.getFlags() & 67108864) != 0 && intent.getBooleanExtra("extra_clear_top", false)) {
            ((SetupWizardApplication) getApplication()).onSetupFinishedReally(this);
        }
    }

    private Intent buildWifiSetupIntent() {
        //Intent intent = new Intent("com.otosoft.setupwizard.SETUP_WIFI_NETWORK");
        Intent intent = new Intent("com.android.net.wifi.SETUP_WIFI_NETWORK");
        intent.putExtra("firstRun", true);
        intent.putExtra("allowSkip", true);
        intent.putExtra("useImmersiveMode", true);
        intent.putExtra("theme", "material_light");
        intent.putExtra("wifi_auto_finish_on_connect", true);
        intent.putExtra("scriptUri", "NotUsedNow");
        return intent;
    }

    private void updateLocale(Locale locale) {
        LocalePicker.updateLocale(locale);
    }
}
