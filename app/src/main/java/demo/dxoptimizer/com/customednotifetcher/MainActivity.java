package demo.dxoptimizer.com.customednotifetcher;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.lang.reflect.Method;
import java.util.List;

public class MainActivity extends Activity {

    public static boolean isReadNotiPermissionOpen(Context cxt) {
        String pkgName = cxt.getPackageName();
        final String flat = Settings.Secure.getString(cxt.getContentResolver(),
                "enabled_notification_listeners");
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (String name : names) {
                final ComponentName cn = ComponentName.unflattenFromString(name);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isActivityAvailable(Context cxt, Intent intent) {
        PackageManager pm = cxt.getPackageManager();
        if (pm == null) {
            return false;
        }
        List<ResolveInfo> list = pm.queryIntentActivities(
                intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list != null && list.size() > 0;
    }

    private void jumpToReadNotificationPermissionPage() {
        Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (isActivityAvailable(this, intent)) {
            this.startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Log.d("yymm", "onCreate:");
        if (!isReadNotiPermissionOpen(this)) {
            jumpToReadNotificationPermissionPage();
        }

        for (Method m : RemoteViews.class.getDeclaredMethods()) {
            Log.d("yymm", m.getName());
        }
    }

    public void sendNotify(View view) {
        Notification.Builder builder = new Notification.Builder(
                this);
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(this, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(intent);
        builder.setTicker("Customered");
        //通知栏未展开时显示的小图标
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setAutoCancel(true);
        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        // 表明在点击了通知栏中的"清除通知"后，此通知不清除，经常与FLAG_ONGOING_EVENT一起使用
        notification.flags |= Notification.FLAG_NO_CLEAR;
        notification.contentView = new RemoteViews(getPackageName(),
                R.layout.notify_layout);
        notification.contentView.setTextViewText(R.id.textView,"dsfahadnhsfadhsfadhsf");
        notification.contentView.setImageViewResource(R.id.image, R.mipmap.ic_launcher);
        Log.d("yymm", "ID=====" + R.mipmap.ic_launcher);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(5270, notification);
    }
}