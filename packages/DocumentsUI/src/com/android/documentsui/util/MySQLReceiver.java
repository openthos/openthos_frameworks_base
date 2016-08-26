package com.android.documentsui.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.android.documentsui.util.MySqliteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;
import android.content.ContentValues;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import static com.android.documentsui.StartupMenuActivity.isEnglish;

public class MySQLReceiver extends BroadcastReceiver {
    private MySqliteOpenHelper mMsoh;
    private SQLiteDatabase mdb;
    private boolean mIsHasReayDb;
    private int mNumber;
    @Override
    public void onReceive(Context context, Intent intent) {
        mMsoh = new MySqliteOpenHelper(context, "Application_database.db", null, 1);
        mdb = mMsoh.getWritableDatabase();
        if (intent.getAction().equals("com.android.documentsui.SQLITE_CHANGE")) {
            BackstageRenewalData(context);
        }
    }

    public void BackstageRenewalData(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);
        Collections.sort(resolveInfos,new ResolveInfo.DisplayNameComparator(pm));
        for (ResolveInfo reInfo : resolveInfos) {
            File file = new File(reInfo.activityInfo.applicationInfo.sourceDir);
            Date systemDate = new Date(file.lastModified());
            ApplicationInfo applicationInfo = reInfo.activityInfo.applicationInfo;
            String activityName = reInfo.activityInfo.name;
            String pkgName = reInfo.activityInfo.packageName;
            String appLabel = (String) reInfo.loadLabel(pm);
            Drawable icon = reInfo.loadIcon(pm);
            mIsHasReayDb = false;
            Cursor c = mdb.rawQuery("select * from perpo where pkname = ?",
                    new String[] { pkgName });
            while (c.moveToNext()) {
                String pkname = c.getString(c.getColumnIndex("pkname"));
                if (pkgName.equals(pkname)) {
                    mIsHasReayDb = true;
                    break;
                }
            }
            if (!mIsHasReayDb) {
                mdb.execSQL("insert into perpo(label,pkname,date,int,click) "
                                + "values (?,?,?,?,?)",
                        new Object[] { appLabel, pkgName, systemDate,
                                mNumber,mNumber});
            }
            if(isEnglish(appLabel)) {
                ContentValues contentvalues = new ContentValues();
                contentvalues.put("label", appLabel);
                mdb.update("perpo", contentvalues, "pkname = ?", new String[]{ pkgName });
            } else {
                ContentValues contentvalues = new ContentValues();
                contentvalues.put("label", appLabel);
                mdb.update("perpo", contentvalues, "pkname = ?", new String[]{ pkgName });
            }
        }
    }
}
