package demo.dxoptimizer.com.customednotifetcher;

import android.util.Log;

/**
 * Created by tf on 10/23/2017.
 */

class LogHelper {
    public static void d(String tag, String s) {
        Log.d(tag, s);
    }

    public static void e(String tag, String s, Exception e) {
        Log.e(tag, s, e);
    }
}
