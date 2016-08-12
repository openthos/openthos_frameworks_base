package com.otosoft.tools;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;

/**
 * Created by wanglifeng on 16-8-11.
 */
public class MyProvider extends ContentProvider {

    private DatabaseHelper helper;
    private static final String AUTHORITY = "com.otosoft.tools.myprovider";
    private static UriMatcher mUriMatcher;
    private static final int MCLICKSTATE = 1;

    static{
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(AUTHORITY, "selectState", MCLICKSTATE);
    }

    @Override
    public boolean onCreate() {
        helper = new DatabaseHelper(getContext());
        SQLiteDatabase db = helper.getWritableDatabase();
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] strings, String s, String[] strings1, String s1) {
        SQLiteDatabase db = helper.getWritableDatabase();
        Cursor cursor = null;
        if(mUriMatcher.match(uri) == MCLICKSTATE) {
            cursor = db.query("selectState", strings, s, strings1,s1, null, null);
            return cursor;
        }
        return null;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        if(mUriMatcher.match(uri) == MCLICKSTATE) {
            return "vnd.android.cursor.dir/"+AUTHORITY+"clickState";
        }
        return null;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        SQLiteDatabase db = helper.getWritableDatabase();
        if (mUriMatcher.match(uri) == MCLICKSTATE) {
            long newId=db.insert("selectState", null, contentValues);
            getContext().getContentResolver().notifyChange(uri, null);
            return ContentUris.withAppendedId(uri, newId);
        }
        return null;
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int updatedNum = 0;
        if(mUriMatcher.match(uri) == MCLICKSTATE) {
            updatedNum = db.update("selectState", contentValues, s, strings);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return updatedNum;
    }
}
