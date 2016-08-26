package com.android.documentsui;

import java.util.List;
import java.util.Map;

import com.android.documentsui.R;
import android.content.ContentValues;
import com.android.documentsui.util.AppInfo;
import android.database.Cursor;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.database.sqlite.SQLiteDatabase;
import com.android.documentsui.util.StartMenuDialog;
import com.android.documentsui.StartupMenuActivity;
import com.android.documentsui.util.MySqliteOpenHelper;

import android.os.UserHandle;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.SharedPreferences;

public class StartupMenuAdapter extends BaseAdapter {
    public static final int START_MENU_RIGHT_MOUSE_UI_X = 200;
    public static final int START_MENU_RIGHT_MOUSE_UI_Y = 180;
    public static final int START_MENU_RIGHT_MOUSE_UI_NUMBER = 57;

    private List<AppInfo> mlistAppInfo = null;
    private Map<Integer,Boolean> isCheckedMap;
    LayoutInflater infater = null;
    private Context mContext;
    private StartupMenuActivity mStartupMenuActivity;
    private SQLiteDatabase mdb;
    private MySqliteOpenHelper mMsoh;

    public StartupMenuAdapter(Context context, List<AppInfo> apps,
                              Map<Integer,Boolean> isCheckedMap) {
        mContext = context;
        infater = (LayoutInflater) context
                   .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mlistAppInfo = apps;
        this.isCheckedMap = isCheckedMap;
        mMsoh = new MySqliteOpenHelper(mContext, "Application_database.db", null, 1);
        mdb = mMsoh.getWritableDatabase();
    }

    @Override
    public int getCount() {
        System.out.println("size" + mlistAppInfo.size());
        return mlistAppInfo.size();
    }

    @Override
    public Object getItem(int position) {
        return mlistAppInfo.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertview, ViewGroup arg2) {
        View view = null;
        ViewHolder holder = null;
        if (convertview == null || convertview.getTag() == null) {
            view = infater.inflate(R.layout.activity_gridview, null);
            holder = new ViewHolder(view);
            view.setTag(holder);
        } else {
            view = convertview;
            holder = (ViewHolder) convertview.getTag();
        }
        AppInfo appInfo = (AppInfo) getItem(position);
        holder.appIcon.setImageDrawable(appInfo.getAppIcon());
        holder.tvAppLabel.setText(appInfo.getAppLabel());
        view.setOnGenericMotionListener(new View.OnGenericMotionListener() {
            @Override
            public boolean onGenericMotion(View view, MotionEvent motionEvent) {
                int what = motionEvent.getButtonState();
                switch (what) {
                case MotionEvent.BUTTON_PRIMARY:
                    String pkgName = StartupMenuActivity.mlistAppInfo.get(position).getPkgName();
                    Intent intent = StartupMenuActivity.mlistAppInfo.get(position).getIntent();
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(intent);
                    Cursor c = mdb.rawQuery("select * from perpo where pkname = ?",
                                            new String[] { pkgName });
                    c.moveToNext();
                    int numbers = c.getInt(c.getColumnIndex("int"));
                    numbers++;
                    int number = c.getInt(c.getColumnIndex("click"));
                    number++;
                    ContentValues values = new ContentValues();
                    values.put("int", numbers);
                    values.put("click", number);
                    mdb.update("perpo", values, "pkname = ?", new String[] { pkgName });
                    SharedPreferences sharedPreference = mContext.getSharedPreferences("click",
                                                                         Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreference.edit();
                    String type = sharedPreference.getString("type", "sortName");
                    int order = sharedPreference.getInt("order", 0);
                    editor.clear();
                    editor.putInt("isClick", 1);
                    editor.putString("type", type);
                    editor.putInt("order", order);
                    editor.commit();
                    break;
                case MotionEvent.BUTTON_TERTIARY:
                    break;
                case MotionEvent.BUTTON_SECONDARY:
                    if (position < 0 || position >= mlistAppInfo.size())
                        return false;
                    showMenuDialog1(position,motionEvent);
                    break;
                }
                return false;
                }
            });
        view.setOnHoverListener(hoverListener);
        return view;
    }

    View.OnHoverListener hoverListener = new View.OnHoverListener() {
        public boolean onHover(View v, MotionEvent event) {
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_HOVER_ENTER:
                    v.setBackgroundResource(R.color.app_background);
                    break;
                case MotionEvent.ACTION_HOVER_EXIT:
                    v.setBackgroundResource(R.color.appBackgroundColor);
                    break;
            }
            return false;
        }
    };

    private void showMenuDialog1(int position,MotionEvent motionEvent){
        StartupMenuActivity.mStartMenuDialog.setPosition(position);
        int[] location = new int[2];
        //((StartupMenuActivity)infater).mBackBtn.getLocationOnScreen(location);
        StartupMenuActivity.mStartMenuDialog.showDialog((int)motionEvent.getRawX() - location[0]
                    ,(int)motionEvent.getRawY() - location[1] + START_MENU_RIGHT_MOUSE_UI_NUMBER
                    ,START_MENU_RIGHT_MOUSE_UI_X, START_MENU_RIGHT_MOUSE_UI_Y, 0);
    }

    class ViewHolder {
        ImageView appIcon;
        TextView tvAppLabel;
      //TextView tvPkgName;

        public ViewHolder(View view) {
            this.appIcon = (ImageView) view.findViewById(R.id.package_image);
            this.tvAppLabel = (TextView) view.findViewById(R.id.package_name);
        //  this.tvPkgName = (TextView) view.findViewById(R.id.tvPkgName);
        }
    }
}
