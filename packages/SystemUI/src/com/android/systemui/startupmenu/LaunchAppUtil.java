package com.android.systemui.startupmenu;

import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.android.systemui.R;

import java.lang.reflect.Method;

/**
 * Created by ljh on 17-9-25.
 */

public class LaunchAppUtil {

    private static final int FULLSCREEN_WORKSPACE_STACK_ID = 1;
    private static final int FREEFORM_WORKSPACE_STACK_ID = 2;

    public static final int STANDARD_MODE = 0;
    public static final int DESKTOP_MODE = 1;
    public static final int PHONE_MODE = 2;
    private static SharedPreferences pref;

    /**
     * get sharepreference entity
     * @param context
     * @return
     */
    public static SharedPreferences getSharedPreferences(Context context) {
        if (pref == null) pref = context.getSharedPreferences("data", Context.MODE_PRIVATE);
        return pref;
    }

    /**
     * launche App by packageName, activityName
     * @param context
     * @param packageName
     * @param activityName
     */
    public static void launchApp(Context context, String packageName, String activityName) {
        launchApp(context, new ComponentName(packageName, activityName));
    }

    /**
     * launch app by compententName
     * @param context
     * @param componentName
     */
    public static void launchApp(Context context, ComponentName componentName) {
        launchApp(context, componentName, DESKTOP_MODE);
    }

    /**
     * launch app by packageName
     * @param context
     * @param packageName
     */
    public static void launchApp(Context context, String packageName) {
        launchApp(context,
                context.getPackageManager().getLaunchIntentForPackage(packageName), DESKTOP_MODE);
    }

    /**
     * launch app by CompententName and select starting mode
     * @param context
     * @param componentName
     * @param startMode
     */
    public static void launchApp(Context context, ComponentName componentName, int startMode) {
        Intent intent = new Intent();
        intent.setComponent(componentName);
        launchApp(context, intent, startMode);
    }

    /**
     * launch app by intent
     * @param context
     * @param intent
     */
    public static void launchApp(Context context, Intent intent) {
        launchApp(context, intent, DESKTOP_MODE);
    }

    /**
     * launch app by intent and select starting mode
     * @param context
     * @param intent
     * @param startMode
     */
    public static void launchApp(Context context, Intent intent, int startMode) {
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent, getBundle(context, startMode));
    }

    /**
     * get bundle by starting mode
     * @param context
     * @param startMode
     * @return
     */
    private static Bundle getBundle(Context context, int startMode) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            ActivityOptions options = ActivityOptions.makeBasic();
            try {
                options.setLaunchStackId(FREEFORM_WORKSPACE_STACK_ID);
                DisplayMetrics metrics = getRealDisplayMetrics(context);
                int width1, width2, height1, height2;
                switch (startMode) {
                    case STANDARD_MODE:
                        break;
                    case DESKTOP_MODE:
                        width1 = metrics.widthPixels / 8;
                        width2 = metrics.widthPixels - width1;
                        height1 = metrics.heightPixels / 8;
                        height2 = metrics.heightPixels - height1;
                        options.setLaunchBounds(new Rect(width1, height1, width2, height2));
                        break;
                    case PHONE_MODE:
                        width1 = metrics.widthPixels / 2;
                        width2 = context.getResources().getDimensionPixelSize(R.dimen.phone_size_width) / 2;
                        height1 = metrics.heightPixels / 2;
                        height2 = context.getResources().getDimensionPixelSize(R.dimen.phone_size_height) / 2;
                        options.setLaunchBounds(new Rect(
                                width1 - width2,
                                height1 - height2,
                                width1 + width2,
                                height1 + height2));
                        break;
                }
                return options.toBundle();
            } catch (Exception e) { /* Gracefully fail */
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * get display metrics
     * @param context
     * @return
     */
    public static DisplayMetrics getRealDisplayMetrics(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display disp = wm.getDefaultDisplay();

        if (isChromeOs(context))
            disp.getRealMetrics(metrics);
        else
            disp.getMetrics(metrics);

        return metrics;
    }

    public static boolean isChromeOs(Context context) {
        return context.getPackageManager().hasSystemFeature("org.chromium.arc");
    }
}
