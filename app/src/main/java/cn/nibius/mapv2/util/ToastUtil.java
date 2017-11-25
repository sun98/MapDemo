package cn.nibius.mapv2.util;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by Nibius at 2017/11/18 21:46.
 */

public class ToastUtil {

    public static void showShort(Context context, String string) {
        Toast.makeText(context, string, Toast.LENGTH_SHORT).show();
    }

    public static void showShort(Context context, CharSequence sequence) {
        Toast.makeText(context, sequence, Toast.LENGTH_SHORT).show();
    }

    public static void showShort(Context context, int resId) {
        Toast.makeText(context, resId, Toast.LENGTH_SHORT).show();
    }

    public static void showLong(Context context, String string) {
        Toast.makeText(context, string, Toast.LENGTH_LONG).show();
    }

    public static void showLong(Context context, CharSequence sequence) {
        Toast.makeText(context, sequence, Toast.LENGTH_LONG).show();
    }

    public static void showLong(Context context, int resId) {
        Toast.makeText(context, resId, Toast.LENGTH_LONG).show();
    }
}
