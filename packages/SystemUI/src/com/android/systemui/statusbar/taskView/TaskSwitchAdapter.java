package com.android.systemui.statusbar.taskview;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.ActivityManager.RecentTaskInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnHoverListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.R;
import java.util.ArrayList;

/**
 * Created by fengjie on 16-5-26.
 */
public class TaskSwitchAdapter extends BaseAdapter {
    private LayoutInflater inflater;
    private ArrayList<TaskInfo> mTasks = new ArrayList();
    private ActivityManager mam;
    private Context mc;
    private PackageManager mPm;
    private int mCurrentPosition = 0;
    private GridView mParent;

    class TaskInfo {
        public RecentTaskInfo info;
        public Intent intent;

        public TaskInfo(RecentTaskInfo _info, Intent _intent) {
            this.info = _info;
            this.intent = _intent;
        }
    }

    public TaskSwitchAdapter(Context c, GridView parent)
    {
        super();
        this.mc = c;
        this.mam = (ActivityManager)c.getSystemService(Context.ACTIVITY_SERVICE);
        this.inflater = LayoutInflater.from(c);
        this.mPm = this.mc.getPackageManager();
        this.mParent = parent;
        this.reloadTask();
    }

    public void reloadTask()
    {
        ArrayList<RecentTaskInfo> recentTasks = new ArrayList();
        try {
            recentTasks = (ArrayList) mam.getRecentTasks(100,
                                                         ActivityManager.RECENT_WITH_EXCLUDED);
        } catch(Exception e) {
        }

        this.mTasks.clear();
        this.mCurrentPosition = 0;
        for(int i = 0; i < recentTasks.size();i++) {
            RecentTaskInfo ri = (RecentTaskInfo) recentTasks.get(i);
            if(ri.id < 0) {
                continue;
            }
            Intent intent = new Intent(ri.baseIntent);
            if(ri.origActivity != null) {
                intent.setComponent(ri.origActivity);
            }
            this.mTasks.add(new TaskInfo(ri, intent));
        }
        this.notifyDataSetChanged();
    }

    public void resetTask() {
        this.mTasks.clear();
    }

    public void stepTask() {
        if(!this.mTasks.isEmpty()) {
            this.mCurrentPosition = (this.mCurrentPosition + 1) % (this.mTasks.size());
        }

        this.notifyDataSetChanged();
    }

    public void switchTask(int position)
    {
        if(position == -1) {
            position = this.mCurrentPosition;
        }
        if(this.mTasks != null && position >= 0 && position < this.mTasks.size()) {
            this.mCurrentPosition = position;
            TaskInfo taskInfo = this.mTasks.get(this.mCurrentPosition);
            if(taskInfo.info.id >= 0) {
                try {
                    this.mam.moveTaskToFront(taskInfo.info.id,
                                             ActivityManager.MOVE_TASK_WITH_HOME);
                } catch (Exception e) {
                    taskInfo.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|
                                             Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    try {
                        this.mc.startActivity(taskInfo.intent);
                    } catch(Exception e2) {
                        Log.w("TaskSwitchAdapter", "switchTask e2:   " + e2 );
                    }
                }
            } else if(taskInfo.intent != null) {
                taskInfo.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|
                                         Intent.FLAG_ACTIVITY_CLEAR_TOP);
                try {
                    this.mc.startActivity(taskInfo.intent);
                } catch(Exception e3) {
                    Log.w("TaskSwitchAdapter", "switchTask e3:  " + e3 );
                }
            }
        }
    }

    @Override
    public int getCount()
    {
        if(mTasks != null) {
            return mTasks.size();
        } else {
            return 0;
        }
    }

    @Override
    public Object getItem(int position) {
        return mTasks.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    // create a new viewItem for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        View viewHolder;
        viewHolder = (View)inflater.inflate(R.layout.task_item, null);

        viewHolder.setOnHoverListener(new ItemHoverListener());
        viewHolder.setLayoutParams(new GridView.LayoutParams(90, 90));

        TaskInfo taskInfo = (TaskInfo) this.mTasks.get(position);
        ResolveInfo resolveInfo = this.mPm.resolveActivity(taskInfo.intent, 0);

        TextView title = (TextView)viewHolder.findViewById(R.id.title);
        CharSequence cTitle = taskInfo.info.description;
        if(TextUtils.isEmpty(cTitle)) {
            cTitle = resolveInfo.activityInfo.loadLabel(this.mPm);
        }
        title.setText(cTitle);

        Drawable drawable = resolveInfo.activityInfo.loadIcon(this.mPm);
        ImageView image = (ImageView)viewHolder.findViewById(R.id.imageView);
        if(drawable != null) {
            image.setImageDrawable(drawable);
        }

        setItemNormal(viewHolder);
        if(this.mCurrentPosition == position) {
            setItemFocus(viewHolder);
        }

        return viewHolder;
    }

    private class ItemHoverListener implements OnHoverListener {

        @Override
        public boolean onHover(View v, MotionEvent event) {
            int what = event.getAction();
            switch(what) {
            case MotionEvent.ACTION_HOVER_ENTER:
                resetItemFocus();
                setItemFocus(v);
                setCurPositionByView(v);
                break;
            }
            return false;
        }
    }

    public void resetItemFocus()
    {
        for(int i = 0; i < this.mParent.getChildCount(); i++) {
            setItemNormal(this.mParent.getChildAt(i));
        }
    }

    public void setItemFocus(View v)
    {
        v.setBackgroundColor(Color.parseColor("#00BAE6")); // FIXME: move to res file
    }

    public void setItemNormal(View v)
    {
        v.setBackgroundColor(Color.parseColor("#FFFFFF")); // FIXME: move to res file
    }

    public void setCurPositionByView(View v)
    {
        this.mCurrentPosition = this.mParent.getPositionForView(v);
    }
}
