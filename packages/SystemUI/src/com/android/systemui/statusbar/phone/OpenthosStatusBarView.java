package com.android.systemui.statusbar.phone;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.View;
import android.util.AttributeSet;
import android.util.Log;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.dialog.BaseDialog;
import com.android.systemui.dialog.StartupMenuDialog;
import com.android.systemui.statusbar.TaskBarIcon;
import com.android.systemui.startupmenu.bean.AppInfo;
import com.android.systemui.startupmenu.SqliteOpenHelper;
import com.android.systemui.startupmenu.utils.AppOperateManager;

import com.android.systemui.R;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.HashMap;
import java.util.LinkedList;

public class OpenthosStatusBarView extends PanelBar {
    private static final String TAG = "OpenthosStatusBarView";

    private StatusBar mStatusBar;
    private ImageView mStartupMenu;
    private OpenthosStatusBarView mOpenthosStatusBarView;
    private LinearLayout mLlScrollContents;
    private BaseDialog mStartupMenuDialog;
    private BaseDialog mCurrentDialog;
    private HashMap<String, TaskBarIcon> mLockedIcons = new HashMap<>();
    private HashMap<String, TaskBarIcon> mRunIcons = new HashMap<>();
    private String mPrevPackageName;

    private int mDownX = 0;
    private int mDownY = 0;

    public OpenthosStatusBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mStatusBar = SysUiServiceProvider.getComponent(context, StatusBar.class);
    }

    public void initTaskbarIcons() {
        LinkedList<AppInfo> dockedList = deSerialization(getObject());
        if (dockedList == null) {
            return;
        }
        for (AppInfo info : dockedList) {
            addTaskIcon(info.getComponentName());
        }
    }

    public void addTaskIcon(ComponentName cp) {
        TaskBarIcon buttonView = mLockedIcons.get(cp.getPackageName());
        if (buttonView != null)
            return;
        buttonView = mRunIcons.get(cp.getPackageName());
        if (buttonView == null) {
            buttonView = new TaskBarIcon(mContext, cp);
            mLlScrollContents .addView(buttonView);
        }
        mLockedIcons.put(cp.getPackageName(), buttonView);
        buttonView.locked();
        updateLockedList();
    }

    public void removeTaskIcon(ComponentName cp) {
        TaskBarIcon buttonView = mLockedIcons.get(cp.getPackageName());
        if (buttonView != null) {
            buttonView.unlocked();
            mLockedIcons.remove(cp.getPackageName());
            updateLockedList();
            if (!buttonView.isRun())
                mLlScrollContents.removeView(buttonView);
        } else {
            AppInfo appInfo = AppOperateManager.getInstance(mContext).getAppInfo(cp.getPackageName());
            if (appInfo.isLocked()) {
                appInfo.setLocked(false);
                SqliteOpenHelper openHelper = SqliteOpenHelper.getInstance(getContext());
                openHelper.updateDataLocked(appInfo);
            }
        }
    }

    private void updateLockedList() {
        new Thread () {
            @Override
            public void run() {
                synchronized (mLlScrollContents) {
                    LinkedList<AppInfo> dockedList = new LinkedList<>();
                    for (int i = 0; i < mLlScrollContents.getChildCount(); i++) {
                        TaskBarIcon button = (TaskBarIcon) mLlScrollContents.getChildAt(i);
                        AppInfo info = button.getAppInfo();
                        if (info.isLocked())
                            dockedList.add(info);
                    }
                    saveObject(serialize(dockedList));
                }
            }
        }.start();
    }

    void saveObject(String strObject) {
        SharedPreferences sp = mContext.getSharedPreferences("lockedmap", 0);
        Editor edit = sp.edit();
        edit.putString("lockedmap", strObject);
        edit.commit();
    }

    String getObject() {
        SharedPreferences sp = mContext.getSharedPreferences("lockedmap", 0);
        String re = sp.getString("lockedmap", null);
        return re;
    }

    private String serialize(LinkedList<AppInfo> appinfos) {
        ByteArrayOutputStream byteArrayOutputStream = null;
        ObjectOutputStream objectOutputStream = null;
        String serStr = null;
        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(appinfos);
            serStr = byteArrayOutputStream.toString("ISO-8859-1");
            serStr = java.net.URLEncoder.encode(serStr, "UTF-8");
            objectOutputStream.close();
            byteArrayOutputStream.close();
        } catch (Exception e) {
            Log.e(TAG, "Error during serialize", e);
        } finally {
            if (objectOutputStream != null) {
                try {
                    objectOutputStream.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error during serialize", e);
                }
            }
            if (byteArrayOutputStream != null) {
                try {
                    byteArrayOutputStream.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error during serialize", e);
                }
            }
            return serStr;
        }
    }

    private LinkedList<AppInfo> deSerialization(String str) {
        ByteArrayInputStream byteArrayInputStream = null;
        ObjectInputStream objectInputStream = null;
        LinkedList<AppInfo> infos = null;
        try {
            String redStr = java.net.URLDecoder.decode(str, "UTF-8");
            byteArrayInputStream = new ByteArrayInputStream(redStr.getBytes("ISO-8859-1"));
            objectInputStream = new ObjectInputStream(byteArrayInputStream);
            infos = (LinkedList<AppInfo>) objectInputStream.readObject();
            objectInputStream.close();
            byteArrayInputStream.close();
        } catch (Exception e) {
            Log.e(TAG, "Error during serialize", e);
        } finally {
            if (objectInputStream != null) {
                try {
                    objectInputStream.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error during serialize", e);
                }
            }
            if (byteArrayInputStream != null) {
                try {
                    byteArrayInputStream.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error during serialize", e);
                }
            }
            return infos;
        }
    }

    public void closeApp(int taskId, String packageName) {
        try {
            TaskBarIcon buttonView = mRunIcons.get(packageName);
            if (buttonView != null) {
                ActivityManager.getService().removeTask(taskId);
                closeIcon(taskId, packageName);
            } else {
            }
        } catch (Exception e) {
        }

    }

    private boolean canAdd(String packageName) {
        try {
            PackageManager pm = mContext.getPackageManager();
            Drawable icon = pm.getApplicationIcon(packageName);
            return icon != null;
        } catch (Exception e) {
            return false;
        }
    }

    public void bindIcon(int taskId, ComponentName componentName) {
        String packageName = componentName.getPackageName();
        if (!canAdd(componentName.getPackageName())) {
            return;
        }
        if (mPrevPackageName != null) {
            TaskBarIcon prevButtonView = mRunIcons.get(mPrevPackageName);
            if (prevButtonView != null) {
                prevButtonView.setFocusInApplications(false);
            }
        }

        mPrevPackageName = packageName;
        TaskBarIcon buttonView = mRunIcons.get(packageName);
        if (buttonView == null) {
            buttonView = mLockedIcons.get(componentName.getPackageName());
            if (buttonView == null) {
                buttonView = new TaskBarIcon(mContext, componentName);
                mLlScrollContents.addView(buttonView);
            }
        }

        mRunIcons.put(packageName, buttonView);
        buttonView.addTaskId(taskId);
        buttonView.setFocusInApplications(true);
    }

    public void closeIcon(int taskId, ComponentName cp) {
        closeIcon(taskId, cp.getPackageName());
    }

    public void closeIcon(int taskId, String packageName) {
        TaskBarIcon buttonView = mRunIcons.get(packageName);
        if (buttonView != null) {
            buttonView.closeTask(taskId);
            if (!buttonView.isRun()) {
                mRunIcons.remove(packageName);
                if (!buttonView.isLocked())
                    mLlScrollContents.removeView(buttonView);
            }
            return;
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initView();
        initData();
        initListener();
    }

    private void initView() {
        mStartupMenu = (ImageView) findViewById(R.id.iv_startupmenu_status_bar);
        mLlScrollContents = (LinearLayout) findViewById(R.id.ll_scroll_icon_contents);
    }

    private void initData() {
        initDialog();
    }

    private void initDialog() {
        mStartupMenuDialog = new StartupMenuDialog(getContext());
    }

    public StartupMenuDialog getStartupMenuDialog() {
        return (StartupMenuDialog) mStartupMenuDialog;
    }

    private void initListener() {
        mStartupMenu.setOnTouchListener(mTouchListener);
    }

    private View.OnTouchListener mTouchListener = (View v, MotionEvent event) -> {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (v.getId() == R.id.iv_startupmenu_status_bar) {
                    showDialog(mStartupMenu, mStartupMenuDialog);
            } else if (v.getId() == R.id.ll_scroll_icon_contents) {
            }
        }
        return false;
    };

    private void showDialog(View view, BaseDialog dialog) {
        if (view == null) {
            return;
        }
        if (mCurrentDialog == null) {
            dialog.show(view);
            mCurrentDialog = dialog;
        } else {
            if (mCurrentDialog == dialog) {
                if (mCurrentDialog.isShowing()) {
                    dialog.dismiss();
                } else {
                    dialog.show(view);
                }
            } else {
                if (mCurrentDialog.isShowing()) {
                    mCurrentDialog.dismiss();
                }
                dialog.show(view);
                mCurrentDialog = dialog;
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    public void onTrackingStarted() {
        super.onTrackingStarted();
    }

    @Override
    public void panelExpansionChanged(float frac, boolean expanded) {
        super.panelExpansionChanged(frac, expanded);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    @Override
    public void panelScrimMinFractionChanged(float minFraction) {

    }

    public void onDensityOrFontScaleChanged() {
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        layoutParams.height = getResources().getDimensionPixelSize(R.dimen.status_bar_height);
        layoutParams.width = getResources().getDimensionPixelSize(R.dimen.status_bar_height);
        setLayoutParams(layoutParams);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        onFinishInflate();
    }
}
