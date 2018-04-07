package demo.dxoptimizer.com.customednotifetcher;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;


@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotiServiceListener extends NotificationListenerService {

    private static final boolean DEBUG = true;
    private static final String TAG = "NotifyBoxUtils";
    private static final String EXTRA_NOE_TEXT = "text";
    public static final String SEPARATOR_CUSTOM_TEXT = "\n";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onNotificationPosted(final StatusBarNotification sbn) {
        new Thread() {
            @Override
            public void run() {
                onNotificationRealPosted(sbn);
            }
        }.start();
        super.onNotificationPosted(sbn);
    }

    private int obtainSize() {
        try {
            Class<?> clz = Class.forName("android.view.ViewRootImpl");
            Method m = clz.getDeclaredMethod("getRunQueue");
            m.setAccessible(true);
            Object runQueue = m.invoke(clz);
            Field f = runQueue.getClass().getDeclaredField("mActions");
            f.setAccessible(true);
            ArrayList<?> list = (ArrayList<?>) f.get(runQueue);
            return list.size();
        } catch (Exception e) {
            Log.e("yymm", "", e);
        }
        return -1;
    }

    private void onNotificationRealPosted(StatusBarNotification sbn) {
        Log.e("yymm", "onNotificationPosted:" + sbn.getPackageName());
        if (true) {
            Log.e("yymm", "onNotificationPosted ok");
            Notification notification = sbn.getNotification();
            if (null != notification && notification.contentView != null) {
//                StringBuilder b = new StringBuilder();
//                wrapCustomContent(getApplicationContext(), b, notification.contentView);


//                e(notification.contentView);
//                obtainText(notification.contentView);

//                Log.e("yymm", "Result=" + retrieveCustomViewText(notification.contentView));
//                Log.e("yymm", "onNotificationPosted 2");
                try {
                    wrapCustomContent(this, new JSONObject(), notification.contentView);
                } catch (Exception e) {
                    LogHelper.e(TAG, "?", e);
                }
                Log.e("yymm", "size=" + obtainSize());
            }
        }
    }

    private static Context retrieveInflater(Context cxt, RemoteViews contentView) {
        try {
            Method m = RemoteViews.class.getDeclaredMethod(Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH ?
                    "getContextForResources" : "prepareContext", Context.class);
            m.setAccessible(true);
            final Context contextForResources = (Context) m.invoke(contentView, cxt);
//            Constructor<?> c = Class.forName("android.widget.RemoteViews$RemoteViewsContextWrapper").getDeclaredConstructor(Context.class, Context.class);
//            c.setAccessible(true);
//            Context inflationContext = (Context) c.newInstance(cxt, contextForResources);
//
//            LayoutInflater inflater = (LayoutInflater) cxt.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//            inflater = inflater.cloneInContext(inflationContext);
//            inflater.setFilter(contentView);
//            return inflater;
            return contextForResources;
        } catch (Exception e) {
            if (DEBUG) {
                LogHelper.e(TAG, "obtain inflater error!", e);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static void wrapCustomContent(Context cxt, JSONObject data, RemoteViews contentView) throws JSONException {
        if (null == contentView)
            return;

        LogHelper.d(TAG, "wrapCustomContent=");
        ArrayList<WrappedTextLine> textArray = new ArrayList<>();
        Context context = retrieveInflater(cxt, contentView);
        if (null == context) {
            return;
        }
        XmlPullParser parser = context.getResources().getLayout(contentView.getLayoutId());
        try {
            int event;
            while (XmlPullParser.END_DOCUMENT != (event = parser.next())) {
                if (XmlPullParser.START_TAG != event) {
                    continue;
                }

                long id = -1;
                String text = null;
                for (int i = 0, size = parser.getAttributeCount(); i < size; i++) {
                    switch (parser.getAttributeName(i)) {
                        case "id":
                            id = Long.parseLong(parser.getAttributeValue(i).replace("@", ""));
                            break;
                        case "text":
                            text = parser.getAttributeValue(i);
                            break;
                        default:
                            break;
                    }
                }
                if (-1 != id || null != text) {
                    if (DEBUG) {
                        LogHelper.d(TAG, "Add=" + parser.getName() + ":" + text);
                    }
                    textArray.add(new WrappedTextLine(id, text));
                }
            }
        } catch (Exception e) {
            if (DEBUG) {
                LogHelper.e(TAG, "wrapCustomContent exception", e);
            }
        }

        try {
            Field outerFields[] = contentView.getClass().getDeclaredFields();
            if (null != outerFields) {
                for (Field field : outerFields) {
                    if (!field.getName().equals("mActions")) {
                        continue;
                    }

                    field.setAccessible(true);
                    ArrayList<Object> actions = (ArrayList<Object>) field.get(contentView);
                    if (null == actions) {
                        continue;
                    }
                    for (Object action : actions) {
                        long viewId = lookForViewId(action, action.getClass(), Class.forName("android.widget.RemoteViews$Action"));
                        WrappedTextLine wrappedTextLine;
                        if (-1 == viewId || null == (wrappedTextLine = getWrappedTextLine(textArray, viewId))) {
                            continue;
                        }

                        Field innerFields[] = action.getClass().getDeclaredFields();
                        for (Field inner : innerFields) {
                            if (!"value".equalsIgnoreCase(inner.getName())) {
                                continue;
                            }

                            inner.setAccessible(true);
                            Object value = inner.get(action);
                            if (null != value) {
                                wrappedTextLine.text = value.toString();
                            }
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (DEBUG) {
                LogHelper.e(TAG, "wrapCustomContent exception", e);
            }
        }

        StringBuilder builder = new StringBuilder();
        for (WrappedTextLine line : textArray) {
            if (TextUtils.isEmpty(line.text)) {
                continue;
            }
            builder.append(line.text).append(SEPARATOR_CUSTOM_TEXT);
        }
        data.put(EXTRA_NOE_TEXT, builder.toString());
        if (DEBUG) {
            LogHelper.d(TAG, "put:" + builder);
        }
    }

    private static WrappedTextLine getWrappedTextLine(ArrayList<WrappedTextLine> textArray, long id) {
        for (WrappedTextLine line : textArray) {
            if (id == line.id) {
                return line;
            }
        }
        return null;
    }

    private static long lookForViewId(Object obj, Class<?> ref, Class<?> clz) throws NoSuchFieldException, IllegalAccessException {
        Class<?> sup = ref.getSuperclass();
        if (sup == clz) {
            Field f = sup.getDeclaredField("viewId");
            f.setAccessible(true);
            return f.getLong(obj);
        } else if (ref != Object.class) {
            return lookForViewId(obj, sup, clz);
        }
        return -1;
    }


    //    @SuppressWarnings("unchecked")
//    private String retrieveCustomViewText(RemoteViews remoteViews) {
//        ArrayList<WrappedTextLine> textArray = new ArrayList<>();
//        XmlPullParser parser = getApplicationContext().getResources().getLayout(remoteViews.getLayoutId());
//        try {
//            int event;
//            while (XmlPullParser.END_DOCUMENT != (event = parser.next())) {
//                if (XmlPullParser.START_TAG != event) {
//                    continue;
//                }
//
//                long id = -1;
//                String text = null;
//                for (int i = 0, size = parser.getAttributeCount(); i < size; i++) {
//                    switch (parser.getAttributeName(i)) {
//                        case "id":
//                            id = Long.parseLong(parser.getAttributeValue(i).replace("@", ""));
//                            break;
//                        case "text":
//                            text = parser.getAttributeValue(i);
//                            break;
//                        default:
//                            break;
//                    }
//                }
//                if (-1 != id || null != text) {
//                    Log.d("yymm", "Add=" + parser.getName() + ":" + text);
//                    textArray.add(new WrappedTextLine(id, text));
//                }
//            }
//        } catch (Exception e) {
//            Log.e("yymm", "", e);
//        }
//
//        try {
//            Field outerFields[] = remoteViews.getClass().getDeclaredFields();
//            if (null != outerFields) {
//                for (Field field : outerFields) {
//                    if (!field.getName().equals("mActions")) {
//                        continue;
//                    }
//
//                    field.setAccessible(true);
//                    ArrayList<Object> actions = (ArrayList<Object>) field.get(remoteViews);
//                    if (null == actions) {
//                        continue;
//                    }
//                    for (Object action : actions) {
//                        long viewId = lookForViewId(action, action.getClass(), Class.forName("android.widget.RemoteViews$Action"));
//                        WrappedTextLine wrappedTextLine;
//                        if (-1 == viewId || null == (wrappedTextLine = getWrappedTextLine(textArray, viewId))) {
//                            continue;
//                        }
//
//                        Field innerFields[] = action.getClass().getDeclaredFields();
//                        for (Field inner : innerFields) {
//                            if (!"value".equalsIgnoreCase(inner.getName())) {
//                                continue;
//                            }
//
//                            inner.setAccessible(true);
//                            Object value = inner.get(action);
//                            if (null != value) {
//                                wrappedTextLine.text = value.toString();
//                            }
//                            break;
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//            Log.e("yymm", "", e);
//        }
//
//        StringBuilder builder = new StringBuilder();
//        for (WrappedTextLine line : textArray) {
//            builder.append(line.text).append(",");
//        }
//        return builder.toString();
//    }
//
//    private static WrappedTextLine getWrappedTextLine(ArrayList<WrappedTextLine> textArray, long id) {
//        for (WrappedTextLine line : textArray) {
//            if (id == line.id) {
//                return line;
//            }
//        }
//        return null;
//    }
//
//    private static class WrappedTextLine {
//        long id;
//        String text;
//
//        public WrappedTextLine(long id, String text) {
//            this.id = id;
//            this.text = text;
//        }
//    }
//
//    private void e(RemoteViews contentView) {
//        XmlPullParser parser = getApplicationContext().getResources().getLayout(contentView.getLayoutId());
//        try {
//            while (parser.next() != XmlPullParser.END_DOCUMENT) {
//                Log.e("yymm", "name=" + parser.getName());
//                Log.e("yymm", "text=" + parser.getText());
//                StringBuilder s = new StringBuilder();
//                for (int i = 0, size = parser.getAttributeCount(); i < size; i++) {
//                    s.append(parser.getAttributeName(i) + ":" + parser.getAttributeValue(i) + ";");
//                }
//                Log.e("yymm", "a=" + s);
//            }
//
////            int type;
////            while ((type = parser.next()) != XmlPullParser.START_TAG &&
////                    type != XmlPullParser.END_DOCUMENT) {
////                // Empty
////            }
//
////            if (type != XmlPullParser.START_TAG) {
////                throw new InflateException(parser.getPositionDescription()
////                        + ": No start tag found!");
////            }
////
////            final String name = parser.getName();
////
////            Log.e("yymm", "**************************");
////            Log.e("yymm", "Creating root view: "
////                    + name);
////            Log.e("yymm", "**************************");
////            while (((type = parser.next()) != XmlPullParser.END_TAG ||  type != XmlPullParser.END_DOCUMENT)) {
////
////                Log.e("yymm", "**************************");
////            }
//        } catch (Exception e) {
//            Log.e("yymm", "", e);
//        }
//    }
//
//    private static View getView(Context context, RemoteViews contentView, LinearLayout ll) {
////        for(Method method : RemoteViews.class.getDeclaredMethods()) {
////            Log.e("yymm", method.toString());
////        }
//        try {
//            Method m = contentView.getClass().getDeclaredMethod("getRemoteViewsToApply", Context.class);
//            m.setAccessible(true);
//            RemoteViews rvToApply = (RemoteViews) m.invoke(contentView, context);
////            m = contentView.getClass().getDeclaredMethod("inflateView", Context.class, RemoteViews.class, ViewGroup.class);
////            m.setAccessible(true);
////            View result = (View) m.invoke(contentView, context, rvToApply, ll);
//            View result = LayoutInflater.from(context).inflate(contentView.getLayoutId(), ll);
//            Class<?> clz = Class.forName("android.widget.RemoteViews$OnClickHandler");
////            m = contentView.getClass().getDeclaredMethod("loadTransitionOverride", Context.class, clz);
////            m.setAccessible(true);
////            m.invoke(contentView, context, null);
//            m = rvToApply.getClass().getDeclaredMethod("performApply", View.class, ViewGroup.class, clz);
//            m.setAccessible(true);
//            m.invoke(rvToApply, result, ll, null);
//            return result;
//        } catch (Exception e) {
//            Log.e("yymm", "", e);
//        }
//        return null;
//    }
//
    private static void wrapCustomContent(Context cxt, StringBuilder s, RemoteViews contentView) {
        if (null == contentView)
            return;

        try {
            LinearLayout ll = new LinearLayout(cxt);
            View view = contentView.apply(cxt, ll);

//            View view = getView(cxt, contentView, ll);

            if (view != null && view instanceof ViewGroup) {
                StringBuilder stringBuilder = new StringBuilder();
                parseCustomNotifyContent((ViewGroup) view, stringBuilder);
                if (stringBuilder.length() > 0) {
                    s.append(stringBuilder.toString());
                }
            }
        } catch (Exception e) {
            Log.e("yymm", "wrapCustomContent failed", e);
        }
    }

    private static String parseCustomNotifyContent(ViewGroup viewGroup, StringBuilder data) {
        int num = viewGroup.getChildCount();
        for (int i = 0; i < num; i++) {
            View view = viewGroup.getChildAt(i);
            if (view instanceof TextView) {
                TextView tv = (TextView) view;
                CharSequence content = tv.getText();
                if (!TextUtils.isEmpty(content)) {
                    data.append(content).append("/");
                }
            } else if (view instanceof ViewGroup) {
                parseCustomNotifyContent((ViewGroup) view, data);
            }
        }
        return data.toString();
    }

//    private long lookForViewId(Object obj, Class<?> ref, Class<?> clz) throws NoSuchFieldException, IllegalAccessException {
//        Class<?> sup = ref.getSuperclass();
//        if (sup == clz) {
//            Field f = sup.getDeclaredField("viewId");
//            f.setAccessible(true);
//            return f.getLong(obj);
//        } else if (ref != Object.class) {
//            return lookForViewId(obj, sup, clz);
//        }
//        return -1;
//    }
//
//    private void obtainText(RemoteViews remoteViews) {
//        try {
//            StringBuilder stringBuilder = new StringBuilder();
//            Field outerFields[] = remoteViews.getClass().getDeclaredFields();
//            Log.e("yymm", "FILES=" + Arrays.toString(outerFields));
//            for (int i = 0; i < outerFields.length; i++) {
//                if (!outerFields[i].getName().equals("mActions")) continue;
//                outerFields[i].setAccessible(true);
//                ArrayList<Object> actions = (ArrayList<Object>) outerFields[i]
//                        .get(remoteViews);
//                if (null == actions) {
//                    Log.e("yymm", "actions is empty");
//                    continue;
//                }
//                for (Object action : actions) {
//                    Log.e("yymm", "-----------a:" + action.getClass().getName());
//                    Field innerFields[] = action.getClass().getDeclaredFields();
//                    Log.e("yymm", Arrays.toString(innerFields));
//                    Object value = null;
//                    Integer type = null;
//                    Long viewId = lookForViewId(action, action.getClass(), Class.forName("android.widget.RemoteViews$Action"));
//                    for (Field field : innerFields) {
//                        Log.e("yymm", "field name = " + field.getName());
//                        field.setAccessible(true);
//                        if (field.getName().equals("value")) {
//                            value = field.get(action);
//                            stringBuilder.append(value != null ? value + "," : "");
//                        } else if (field.getName().equals("type")) {
//                            type = field.getInt(action);
//                            Log.e("yymm", "type=" + type);
//                        }
//                    }
//                    Log.e("yymm", "viewId is: " + viewId);
//                }
//
//
//                Log.e("yymm", "text is: " + stringBuilder);
//            }
//        } catch (Exception e) {
//            Log.e("yymm", "", e);
//        }
//    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
    }

    private static class WrappedTextLine {
        long id;
        String text;

        WrappedTextLine(long id, String text) {
            this.id = id;
            this.text = text;
        }
    }
}
