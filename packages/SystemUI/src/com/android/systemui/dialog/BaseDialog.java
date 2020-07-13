package com.android.systemui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.android.systemui.R;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.statusbar.phone.StatusBar;

public class BaseDialog extends Dialog {
    protected View mContentView;
    protected static Point mPoint;
    private StatusBar mStatusBar;

    public BaseDialog(@NonNull Context context) {
        this(context, R.style.DialogStyle);
    }

    public BaseDialog(@NonNull Context context, @StyleRes int themeResId) {
        super(context, themeResId);
        //init screen's size
        if (mPoint == null) {
            Display defaultDisplay = ((WindowManager)
                    getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            mPoint = new Point();
            defaultDisplay.getRealSize(mPoint);
        }
        mStatusBar = SysUiServiceProvider.getComponent(context, StatusBar.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initData();
        initListener();
    }

    public void initView() {

    }

    public void initData() {

    }

    public void initListener() {

    }

    @Override
    public void show() {
        super.show();
        setContentViewBlur(true);
    }

    @Override
    public void dismiss() {
        super.dismiss();
        setContentViewBlur(false);
    }

    private void setContentViewBlur(boolean blur) {
        mContentView.post(new Runnable() {
            @Override
            public void run() {
                mStatusBar.setViewBlur(mContentView, blur);
            }
        });
    }

    /**
     * show dialog at the grivate of view's center
     * @param view
     */
    public void show(View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        mContentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        Window dialogWindow = getWindow();
        dialogWindow.setType(WindowManager.LayoutParams.TYPE_STATUS_BAR_DIALOG);
        dialogWindow.setWindowAnimations(R.style.ShowDialog);
        dialogWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.format = PixelFormat.TRANSPARENT;
        lp.dimAmount = 0;

        if (mContentView.getMeasuredWidth() / 2 > location[0]) {
            dialogWindow.setGravity(Gravity.LEFT | Gravity.BOTTOM);
            lp.x = 0;
        } else if (location[0] + view.getMeasuredWidth() / 2 +
                mContentView.getMeasuredWidth() / 2 > mPoint.x) {
            dialogWindow.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
            lp.x = 0;
        } else {
            dialogWindow.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
            lp.x = location[0] + view.getMeasuredWidth() / 2 - mPoint.x / 2;
        }
        lp.y = 0;
        dialogWindow.setAttributes(lp);
        show();
    }
}
