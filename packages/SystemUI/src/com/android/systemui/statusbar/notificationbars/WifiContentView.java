/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.notificationbars;

import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

import com.android.systemui.statusbar.phone.PhoneStatusBar;

public class WifiContentView extends LinearLayout {

    static final String TAG = "WificontentView";

    public WifiContentView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            ((WifiDialog)PhoneStatusBar.getWifiDialog()).dismiss();
            Intent intent = new Intent();
            ComponentName comp = new ComponentName (ApplicationInfo.APPNAME_ANDROID_SETTINGS,
                                                    "com.android.settings.wifi.WifiSettings");
            intent.setComponent(comp);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
        }
        return true;
    }
}
