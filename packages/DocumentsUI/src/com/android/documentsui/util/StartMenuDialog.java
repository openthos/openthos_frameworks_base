package com.android.documentsui.util;

import com.android.documentsui.R;
import android.graphics.Color;
import android.R.layout;
import android.os.Bundle;
import android.content.Context;
import android.text.TextWatcher;
import android.text.Editable;
import android.text.TextUtils;
import android.app.Dialog;
import android.widget.Toast;
import android.content.Intent;
import android.view.Window;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.View.OnGenericMotionListener;
import android.view.WindowManager;
import android.widget.TextView;
import android.content.ContentValues;
import android.database.Cursor;
import com.android.documentsui.StartupMenuActivity;
import android.database.sqlite.SQLiteDatabase;
import com.android.documentsui.util.MySqliteOpenHelper;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class StartMenuDialog extends Dialog implements OnClickListener {
    public static int STARTMENU_WIDTH = 55;
    public static int STARTMENU_LOCATION = 300;
    private Context mContext;
    private boolean mFlag;
    private int mPosition;
    private TextView mRightOpen;
    private StartupMenuActivity mStartupMenuActivity;
    private SQLiteDatabase mdb;
    private MySqliteOpenHelper mMsoh;
    private int mListType;
    private String mPkgName;

    public StartMenuDialog(Context context) {
        super(context);
        mContext = context;
    }

    public StartMenuDialog(Context context, int themeResId) {
        super(context, themeResId);
        mContext = context;
    }

    public void setPosition(int pos) {
        mPosition = pos;
    }

    protected StartMenuDialog(Context context, boolean cancelable,
                              OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

     @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.right_click_menu1);

        mMsoh = new MySqliteOpenHelper(mContext, "Application_database.db", null, 1);
        mdb = mMsoh.getWritableDatabase();
        mRightOpen = (TextView) findViewById(R.id.tv_right_open);
        TextView rightPhoneRun = (TextView) findViewById(R.id.tv_right_phone_run);
        TextView rightDesktopRun = (TextView) findViewById(R.id.tv_right_desktop_run);
        TextView rightFixedTaskbar = (TextView) findViewById(R.id.tv_right_fixed_taskbar);
        TextView rightUninstall = (TextView) findViewById(R.id.tv_right_uninstall);

        mFlag = true;
        mRightOpen.setOnClickListener(this);
        rightPhoneRun.setOnClickListener(this);
        rightDesktopRun.setOnClickListener(this);
        rightFixedTaskbar.setOnClickListener(this);
        rightUninstall.setOnClickListener(this);
    }

    public void setEnableOpenwith(boolean can) {
        mFlag = can;
        if (can) {
            mRightOpen.setTextColor(Color.parseColor("#000000"));
        } else {
            mRightOpen.setTextColor(Color.parseColor("#b19898"));
        }
    }

    public void showDialog(int x, int y, int height, int width, int type) {
        show();
        Window dialogWindow = getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.dimAmount = 0.0f;
        dialogWindow.setGravity(Gravity.LEFT | Gravity.TOP);
        WindowManager m = dialogWindow.getWindowManager();
        Display d = m.getDefaultDisplay();
        if (x > (d.getWidth() - dialogWindow.getAttributes().width))
        {
            lp.x = d.getWidth() - dialogWindow.getAttributes().width;
        } else {
            lp.x = x;
        }
        if (y > (d.getHeight() - dialogWindow.getAttributes().height)) {
            lp.y = d.getHeight() - dialogWindow.getAttributes().height - STARTMENU_LOCATION;
        } else {
            lp.y = y - STARTMENU_WIDTH;
        }
        lp.width = width;
        lp.height = height;
        lp.alpha = 0.9f;
        dialogWindow.setAttributes(lp);
        mListType = type;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
        case R.id.tv_right_open:
            Intent intent;
            if (mListType == 0) {
                mPkgName = StartupMenuActivity.mlistAppInfo.get(mPosition).getPkgName();
                intent = StartupMenuActivity.mlistAppInfo.get(mPosition).getIntent();
            } else {
                mPkgName = StartupMenuActivity.mlistViewAppInfo.get(mPosition).getPkgName();
                intent = StartupMenuActivity.mlistViewAppInfo.get(mPosition).getIntent();
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
            Cursor c = mdb.rawQuery("select * from perpo where pkname = ?",
                                    new String[] { mPkgName });
            c.moveToNext();
            int numbers = c.getInt(c.getColumnIndex("int"));
            numbers++;
            int number = c.getInt(c.getColumnIndex("click"));
            number++;
            ContentValues values = new ContentValues();
            values.put("int", numbers);
            values.put("click", number);
            mdb.update("perpo", values, "pkname = ?", new String[] { mPkgName });
            SharedPreferences sharedPreference = mContext.getSharedPreferences("click",
                                                          Context.MODE_PRIVATE);
            Editor editor = sharedPreference.edit();
            editor.clear();
            editor.putInt("isClick", 1);
            editor.commit();
            break;

        case R.id.tv_right_phone_run:
            Toast.makeText(mContext, "phone run: COMING SOON", 0).show();
            break;
        case R.id.tv_right_desktop_run:
            Toast.makeText(mContext, "desktop run: COMING SOON", 0).show();
            break;
        case R.id.tv_right_fixed_taskbar:
            String pkgInfo = StartupMenuActivity.mlistAppInfo.get(mPosition).getPkgName();
            //LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(mContext);
            Intent intentSend = new Intent();
            intentSend.putExtra("keyInfo",pkgInfo);
            intentSend.setAction("com.android.documentsui.util.startmenudialog");
            mContext.sendBroadcast(intentSend);
            Log.i("-----","zx+send"+pkgInfo);
            break;
        case R.id.tv_right_uninstall:
            if (mListType == 0) {
                mPkgName = StartupMenuActivity.mlistAppInfo.get(mPosition).getPkgName();
            } else {
                mPkgName = StartupMenuActivity.mlistViewAppInfo.get(mPosition).getPkgName();
            }
            Uri uri = Uri.parse("package:" + mPkgName);
            Intent intents = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri);
            mContext.startActivity(intents);
            break;
        }
    }
}
