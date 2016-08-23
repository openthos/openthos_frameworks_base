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

public class StartMenuUsuallyDialog extends Dialog implements OnClickListener {
    public static int STARTMENU_WIDTH = 65;
    public static int STARTMENU_LOCATION = 330;
    private Context mContext;
    private boolean mFlag;
    private int mPosition;
    private TextView mRightUsuallyOpen;
    private StartupMenuActivity mStartupMenuActivity;
    private SQLiteDatabase mdb;
    private MySqliteOpenHelper mMsoh;
    private int mListType;
    private String mPkgName;

    public StartMenuUsuallyDialog(Context context) {
        super(context);
        mContext = context;
    }

    public StartMenuUsuallyDialog(Context context, int themeResId) {
        super(context, themeResId);
        mContext = context;
    }

    public void setPosition(int pos) {
        mPosition = pos;
    }

    protected StartMenuUsuallyDialog(Context context, boolean cancelable,
                              OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.right_click_usually_menu);

        mMsoh = new MySqliteOpenHelper(mContext, "Application_database.db", null, 1);
        mdb = mMsoh.getWritableDatabase();
        mRightUsuallyOpen = (TextView) findViewById(R.id.tv_right_usually_open);
        TextView rightPhoneUsuallyRun = (TextView) findViewById(R.id.tv_right_phone_usually_run);
        TextView rightDesktopUsuallyRun = (TextView) findViewById(
                                                         R.id.tv_right_desktop_usually_run);
        TextView romeList = (TextView) findViewById(R.id.tv_removed_list);

        mFlag = true;
        mRightUsuallyOpen.setOnClickListener(this);
        rightPhoneUsuallyRun.setOnClickListener(this);
        rightDesktopUsuallyRun.setOnClickListener(this);
        romeList.setOnClickListener(this);
    }

    public void setEnableOpenwith(boolean can) {
        mFlag = can;
        if (can) {
            mRightUsuallyOpen.setTextColor(Color.parseColor("#000000"));
        } else {
            mRightUsuallyOpen.setTextColor(Color.parseColor("#b19898"));
        }
    }

    public void showDialog(int x, int y, int height, int width, int type) {
        show();
        Window dialogWindow = getWindow();
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.dimAmount = 0.0f;
        dialogWindow.setGravity(Gravity.LEFT | Gravity.TOP);
        WindowManager m = dialogWindow.getWindowManager();
        Display d = m.getDefaultDisplay();
        if (x > (d.getWidth() - dialogWindow.getAttributes().width)) {
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
        case R.id.tv_right_usually_open:
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
            break;

        case R.id.tv_right_phone_usually_run:
            Toast.makeText(mContext, "phone run: COMING SOON", 0).show();
            break;
        case R.id.tv_right_desktop_usually_run:
            Toast.makeText(mContext, "desktop run: COMING SOON", 0).show();
            break;
        case R.id.tv_removed_list:
            Toast.makeText(mContext, "removed list: COMING SOON", 0).show();
            mPkgName = StartupMenuActivity.mListViewEight.get(mPosition).getPkgName();
            StartupMenuActivity.mListViewEight.remove(mPosition);
            StartupMenuActivity.mUsuallyAdapter.notifyDataSetChanged();
            cancel();
            Cursor cursor = mdb.rawQuery("select * from perpo where pkname = ?",
                                    new String[] { mPkgName });
            cursor.moveToNext();
            int numClick = cursor.getInt(cursor.getColumnIndex("click"));
            numClick = 0;
            ContentValues value = new ContentValues();
            value.put("click", numClick);
            mdb.update("perpo", value, "pkname = ?", new String[] { mPkgName });
            break;
        }
    }
}
