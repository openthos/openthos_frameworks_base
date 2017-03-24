/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.systemui.statusbar.phone;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.content.Intent;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.EventLog;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.content.BroadcastReceiver;

import com.android.systemui.EventLogTags;
import com.android.systemui.R;
import com.android.systemui.statusbar.StatusBarState;
import com.android.systemui.statusbar.policy.ActivityKeyView;
import android.content.IntentFilter;
import android.os.UserHandle;

public class PhoneStatusBarView extends PanelBar {
    private static final boolean DEBUG = PhoneStatusBar.DEBUG;
    private static final boolean DEBUG_GESTURES = false;

    private static final int STATUSBAR_VALID_PART_CLICK_AREA = 10;

    PhoneStatusBar mBar;

    PanelView mLastFullyOpenedPanel = null;
    PanelView mNotificationPanel;
    private final PhoneStatusBarTransitions mBarTransitions;
    private ScrimController mScrimController;

    private boolean mSkipActionUp = false;
    private boolean mNotificationPanelShow = true;
    private int mStartupMenuSize;
    private boolean mNotificationOpen = false;

    public PhoneStatusBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mStartupMenuSize = (int) (40 * (context.getResources().getDisplayMetrics().density) + 0.5f);
        Resources res = getContext().getResources();
        mBarTransitions = new PhoneStatusBarTransitions(this);
    }

    public BarTransitions getBarTransitions() {
        return mBarTransitions;
    }

    public void setBar(PhoneStatusBar bar) {
        mBar = bar;
    }

    public void setScrimController(ScrimController scrimController) {
        mScrimController = scrimController;
    }

    @Override
    public void onFinishInflate() {
        mBarTransitions.init();
    }

    @Override
    public void addPanel(PanelView pv) {
        super.addPanel(pv);
        if (pv.getId() == R.id.notification_panel) {
            mNotificationPanel = pv;
        }
    }

    @Override
    public boolean panelsEnabled() {
        return mBar.panelsEnabled();
    }

    @Override
    public boolean onRequestSendAccessibilityEvent(View child, AccessibilityEvent event) {
        if (super.onRequestSendAccessibilityEvent(child, event)) {
            // The status bar is very small so augment the view that the user is touching
            // with the content of the status bar a whole. This way an accessibility service
            // may announce the current item as well as the entire content if appropriate.
            AccessibilityEvent record = AccessibilityEvent.obtain();
            onInitializeAccessibilityEvent(record);
            dispatchPopulateAccessibilityEvent(record);
            event.appendRecord(record);
            return true;
        }
        return false;
    }

    @Override
    public PanelView selectPanelForTouch(MotionEvent touch) {
        // No double swiping. If either panel is open, nothing else can be pulled down.
        return mNotificationPanel.getExpandedHeight() > 0
                ? null
                : mNotificationPanel;
    }

    @Override
    public void onPanelPeeked() {
        super.onPanelPeeked();
        mBar.makeExpandedVisible(false);
    }

    @Override
    public void onAllPanelsCollapsed() {
        super.onAllPanelsCollapsed();

        // Close the status bar in the next frame so we can show the end of the animation.
        postOnAnimation(new Runnable() {
            @Override
            public void run() {
                mBar.makeExpandedInvisible();
                if (mBar.isPhoneStatusBarHide() && mBar.getBarState() != StatusBarState.KEYGUARD) {
                    getContext().sendBroadcast(
                                 new Intent(Intent.STATUS_BAR_INFO_HIDE_CUSTOM));
                    mNotificationPanelShow = false;
                }
                getContext().sendBroadcast(
                             new Intent(Intent.STATUS_BAR_NOTIFICATION_COLLAPSE));
                mNotificationOpen = false;
            }
        });
        mLastFullyOpenedPanel = null;
    }

    @Override
    public void onPanelFullyOpened(PanelView openPanel) {
        super.onPanelFullyOpened(openPanel);
        if (openPanel != mLastFullyOpenedPanel) {
            openPanel.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        }
        mLastFullyOpenedPanel = openPanel;
    }

    private boolean checkValidEvent(int x) {
        return (x >= mBar.mCurrentDisplaySize.x
                     - mBar.mCurrentDisplaySize.x / STATUSBAR_VALID_PART_CLICK_AREA) // right side
               && (x < mBar.mCurrentDisplaySize.x - mBar.mIconSize); // also skip home button
    }

    private boolean checkIsStartupButton(int x) {
        return (x <= mStartupMenuSize);
    }

    @Override
    public boolean onHoverEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_HOVER_EXIT) {
            ActivityKeyView.dismissDialog(true);
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //Show notificationPanel when click notification center.
        mBar.mIsShowNotificationPanel = true;
        mBar.mNotificationPanel.setPanelShow();
        if (checkValidEvent((int)event.getX()) == false) {
            return false;
        }
        if (mBar.isPhoneStatusBarHide()) {
            if (mNotificationPanelShow) {
                getContext().sendBroadcast(
                             new Intent(Intent.STATUS_BAR_INFO_SHOW_CUSTOM));
            }
            mNotificationPanelShow = true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mNotificationOpen = !mNotificationOpen;
            if (mNotificationOpen) {
                getContext().sendBroadcast(new Intent(Intent.STATUS_BAR_NOTIFICATION_EXPAND));
            }
        }
        mBar.showHomePanelWork();
        boolean barConsumedEvent = mBar.interceptTouchEvent(event);

        if (DEBUG_GESTURES) {
            if (event.getActionMasked() != MotionEvent.ACTION_MOVE) {
                EventLog.writeEvent(EventLogTags.SYSUI_PANELBAR_TOUCH,
                        event.getActionMasked(), (int) event.getX(), (int) event.getY(),
                        barConsumedEvent ? 1 : 0);
            }
        }

        return barConsumedEvent || super.onTouchEvent(event);
    }

    @Override
    public void onTrackingStarted(PanelView panel) {
        super.onTrackingStarted(panel);
        mBar.onTrackingStarted();
        mScrimController.onTrackingStarted();
    }

    @Override
    public void onClosingFinished() {
        super.onClosingFinished();
        mBar.onClosingFinished();
    }

    @Override
    public void onTrackingStopped(PanelView panel, boolean expand) {
        super.onTrackingStopped(panel, expand);
        mBar.onTrackingStopped(expand);
    }

    @Override
    public void onExpandingFinished() {
        super.onExpandingFinished();
        mScrimController.onExpandingFinished();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            if (!checkIsStartupButton((int)event.getX())) {
                try {
                    ActivityManagerNative.getDefault().killStartupMenu();
                } catch (Exception e) {
                }
            }
            //ActivityKeyView.dismissDialog();
            if (mNotificationPanel.getVisibility() == View.VISIBLE) {
                mBar.makeExpandedInvisible();
            }
        }
        return mBar.interceptTouchEvent(event) || super.onInterceptTouchEvent(event);
    }

    @Override
    public void panelExpansionChanged(PanelView panel, float frac, boolean expanded) {
        super.panelExpansionChanged(panel, frac, expanded);
        mScrimController.setPanelExpansion(frac);
        mBar.updateCarrierLabelVisibility(false);
    }
}
