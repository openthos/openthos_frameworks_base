package com.android.systemui.statusbar.notificationbars;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;

/**
 * Created by Mingkai on 2016/6/22.
 */
public class BaseSettingDialog extends Dialog {
    protected Context mContext;
    protected View mContentView;
    protected View targetView;

    public BaseSettingDialog(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setCanceledOnTouchOutside(true);
        initViews();
        ViewTreeObserver viewObserver = mContentView.getViewTreeObserver();
        if (viewObserver.isAlive()) {
            viewObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    setPosition(targetView);
                }
            });
        }
    }

    private void setPosition(View v) {
        int[] location = new int[2];
        v.getLocationOnScreen(location);
        Window window = getWindow();
        window.setGravity(Gravity.LEFT | Gravity.TOP);
        WindowManager.LayoutParams attr = getWindow().getAttributes();
        attr.x = location[0] - mContentView.getMeasuredWidth();
        attr.y = location[1] - mContentView.getMeasuredHeight()- v.getMeasuredHeight();
        window.setAttributes(attr);
    }

    protected void initViews() {
    }

    public void show(View v) {
        if (isShowing()) {
            dismiss();
        }
        if (mContentView != null) {
            setPosition(v);
        } else {
            targetView = v;
        }
        show();
    }
}
