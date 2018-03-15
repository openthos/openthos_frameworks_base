package com.otosoft.setupwizard;

import android.content.Intent;
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

public class FinishPagerActivity extends BaseActivity {
    private Button mButtonStart;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finish_pager);

        this.mButtonStart = (Button) findViewById(R.id.button_start_use);
        mButtonStart.getBackground().setAlpha(25);
        this.mButtonStart.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                ((SetupWizardApplication) getApplication()).onSetupFinishedReally();
                Intent intent = new Intent();
                intent.setAction(Intent.STATUS_BAR_SHOW_FINISH_ACTIVITY);
                FinishPagerActivity.this.sendBroadcast(intent);
                int dpi = Settings.System.getInt(getContentResolver(), "system_dpi", 160);
                SystemProperties.set("sys.sf.lcd_density.recommend", String.valueOf(dpi));
                finish();
            }
        });
    }

    public void onResume() {
        super.onResume();
    }
}
