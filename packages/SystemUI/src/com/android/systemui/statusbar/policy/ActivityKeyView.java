
package com.android.systemui.statusbar.policy;

import android.app.Dialog;
import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.Window;
import com.android.internal.statusbar.StatusbarActivity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.widget.GridView;
import android.view.KeyEvent;
import com.android.systemui.R;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.util.Log;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.MotionEvent;
import android.content.pm.PackageManager;
import android.widget.Toast;
import java.util.Timer;
import java.util.TimerTask;

public class ActivityKeyView extends ImageView {

    private static final int DIALOG_OFFSET_PART = 3; // divide 3
    private static final int DIALOG_PADDING_TIPS = 10; // divide 3
    private static final int TIMER_NUMBERS = 1000;

    OnClickListener mOpen;     /* Use to open activity by mPkgName fo related StatusbarActivity. */
    OnClickListener mClose;     /* Use to close window like mCloseBtn of window header. */
    OnClickListener mDock;      /* Use to dock related StatusbarActivity in status bar. */
    OnClickListener mUnDock;    /* Use to undock related StatusbarAcitivity from status bar. */
    StatusbarActivity mActivity;    /* Related StatusbarActivity. */
    View mFocusedView;

    private static final String TAG = "ActivityKeyView";
    private static Dialog mDialog = null;   /* Define a Singleton dialog as tools. */
    private static boolean mShowRBM = false;

    public ActivityKeyView(Context context) {
        super(context);
        initListener();
    }

    public ActivityKeyView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
        initListener();
    }

    public ActivityKeyView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr, 0);
        initListener();
    }

    public void initListener() {
        mOpen = new OnClickListener() {
            @Override
            public void onClick(View v) {
                waitTimer();
                runApkByPkg();
                dismissDialog();
            }
        };

        mClose = new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    ActivityManagerNative.getDefault().closeActivity(mActivity.mStackId);
                } catch (RemoteException e) {
                    Log.e(TAG, "Close button failes", e);
                }
                dismissDialog();
            }
        };

        mDock = new OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.mIsDocked = true;
                dismissDialog();
            }
        };

        mUnDock = new OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.mIsDocked = false;
                dismissDialog();
                removeFromRoot();
            }
        };

        setOnHoverListener(new HoverListener());
    }

    public void removeFromRoot() {
        if(!mActivity.mApkRun) {
            this.setVisibility(View.GONE);
            // use to tell phoneStartmenu reduce one
            Intent intentIcon = new Intent();
            intentIcon.putExtra("rmIcon",mActivity.mPkgName);
            intentIcon.setAction("com.android.systemui.activitykeyview");
            mContext.sendBroadcast(intentIcon);
        }
    }

    public void activityStart(int stackId) {
        mActivity.mStackId = stackId;
        mActivity.mApkRun = true;
        mActivity.mHiden = false;
    }

    public StatusbarActivity getStatusbarActivity() {
        return mActivity;
    }

    public void saveStackInfo(Rect rect) {
        mActivity.mRestoreRect = rect;
        mActivity.mHiden = true;
    }

    public void activityClosed() {
        mActivity.mApkRun = false;
    }

    public View getRbmView() {
        LayoutInflater li =
                        (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(mActivity.mIsDocked) {
            if(mActivity.mApkRun) {
                return buildRbmDockedRun(li);
            } else {
                return buildRbmDocked(li);
            }
        } else {
            return buildRbmRun(li);
        }
    }

    public View buildRbmDocked(LayoutInflater li) {
        View rbmDocked = li.inflate(R.layout.right_button_menu_docked, null, false);
        TextView open = (TextView) rbmDocked.findViewById(R.id.rbm_open);
        open.setOnClickListener(mOpen);
        TextView undock = (TextView) rbmDocked.findViewById(R.id.rbm_undock);
        undock.setOnClickListener(mUnDock);
        return rbmDocked;
    }

    public View buildRbmDockedRun(LayoutInflater li) {
        View rbmDockedRun = li.inflate(R.layout.right_button_menu_docked_run, null, false);
        TextView close = (TextView) rbmDockedRun.findViewById(R.id.rbm_close);
        close.setOnClickListener(mClose);
        TextView undock = (TextView) rbmDockedRun.findViewById(R.id.rbm_undock);
        undock.setOnClickListener(mUnDock);
        return rbmDockedRun;
    }

    public View buildRbmRun(LayoutInflater li) {
        View rbmRun = li.inflate(R.layout.right_button_menu_run, null, false);
        TextView close = (TextView) rbmRun.findViewById(R.id.rbm_close);
        close.setOnClickListener(mClose);
        TextView dock = (TextView) rbmRun.findViewById(R.id.rbm_dock);
        dock.setOnClickListener(mDock);
        return rbmRun;
    }

    public static void dismissDialog() {
        dismissDialog(false);
    }

    public static void dismissDialog(boolean fromHover) {
        if (fromHover && mShowRBM) {
            return;
        }
        mShowRBM = false;
        if ((mDialog == null) || !mDialog.isShowing()) {
            return;
        }
        mDialog.dismiss();
    }

    public static boolean preventResponseHover() {
        return mShowRBM && mDialog.isShowing();
    }

    private void showDialog(View view, int padding) {
        if(mDialog == null) {
            mDialog = new Dialog(mContext);
            mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);
        }
        mDialog.setContentView(view);

        Window dw = mDialog.getWindow();
        WindowManager.LayoutParams lp = dw.getAttributes();
        int dpx = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE))
                                                                  .getDefaultDisplay().getWidth();
        int dpy = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE))
                                                                 .getDefaultDisplay().getHeight();
        int iconSize = getResources().getDimensionPixelSize(R.dimen.status_bar_icon_size_big);
        int[] location = new int[2];

        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                     View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        getLocationOnScreen(location);
        lp.x = location[0] - dpx / 2 + iconSize - iconSize / DIALOG_OFFSET_PART;
        lp.y = location[1] - dpy / 2 - view.getMeasuredHeight() - padding;
        lp.width = LayoutParams.WRAP_CONTENT;
        lp.height = LayoutParams.WRAP_CONTENT;

        dw.setAttributes(lp);
        mDialog.show();
    }

    public void setStatusbarActivity(StatusbarActivity sa) {
        mActivity = sa;
    }

    public void setFocusedView(View view) {
        mFocusedView = view;
    }

    private void setFocusedStack() {
        try {
            if (ActivityManagerNative.getDefault().getFocusedStackId() != mActivity.mStackId) {
                ActivityManagerNative.getDefault().setFocusedStack(mActivity.mStackId);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void resizeStack(){
        if(mActivity.mHiden){
            try {
                ActivityManagerNative.getDefault().relayoutWindow(mActivity.mStackId,
                                                                  mActivity.mRestoreRect);
            } catch(Exception exc) {
            }
            mActivity.mHiden = false;
        }
    }

    public void runApkByPkg() {
        try {
            PackageManager manager = mContext.getPackageManager();
            Intent lanuch = new Intent();
            lanuch = manager.getLaunchIntentForPackage(mActivity.mPkgName);
            lanuch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(lanuch);
        } catch(Exception exc) {
        }
    }
    //send broadCast; Matthew
    public void sendBroadcastMethod() {
        Intent intent = new Intent();
        intent.putExtra("keyAddInfo", mActivity.mPkgName);
        intent.setAction("com.android.action.PACKAGE_SEND");
        mContext.sendBroadcast(intent);
    }
    //Wait one second; Matthew
    private void waitTimer() {
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                sendBroadcastMethod();
            }
        };
        timer.schedule(task, TIMER_NUMBERS);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int button = e.getButtonState();
        int action = e.getAction();

        if(button == MotionEvent.BUTTON_SECONDARY && action == MotionEvent.ACTION_DOWN) {
            dismissDialog();
            mShowRBM = true;
            showDialog(getRbmView(), 0);
            return true;
        }
        // Locked status to click
        if(action == MotionEvent.ACTION_DOWN) {
            if(mActivity.mIsDocked) {
                if(!mActivity.mApkRun) {
                    waitTimer();
                    runApkByPkg();
                } else if(mActivity.mHiden) {
                    resizeStack();
                }
            } else if(mActivity.mHiden) {
                resizeStack();
            }
            setFocusedStack();
        }
        return super.onTouchEvent(e);
    }

    public void setFocused(boolean focused) {
        mFocusedView.setVisibility(focused ? View.VISIBLE : View.INVISIBLE);
    }

    private class HoverListener implements OnHoverListener {
        @Override
        public boolean onHover(View useless, MotionEvent event){
            if (preventResponseHover()) {
                return false;
            }
            switch(event.getAction()) {
                case MotionEvent.ACTION_HOVER_ENTER:
                    useless.setBackgroundResource(R.drawable.ic_background_mouse_hover);
                    View view = ((LayoutInflater) mContext.getSystemService(
                                                           Context.LAYOUT_INFLATER_SERVICE))
                                     .inflate(R.layout.status_bar_activity_hover_tips, null, false);
                    TextView v = (TextView) view.findViewById(R.id.akv_tips);
                    if (v != null) {
                        v.setText(PackageManager.getTitleByPkg(getContext(), mActivity.mPkgName));
                    }
                    dismissDialog();
                    showDialog(view, DIALOG_PADDING_TIPS);
                    break;
                case MotionEvent.ACTION_HOVER_EXIT:
                    useless.setBackgroundResource(R.drawable.system_bar_background);
                    break;
            }
            return false;
        }
    }
}
