package com.mrchandler.disableprox.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * @author Wardell
 */
public final class ProUtil {
    private ProUtil() {
    }

    public static boolean isFreeloaded(Context context) {
        SharedPreferences prefs = getSharedPreferences(context);
        return prefs.getBoolean(Constants.PREFS_KEY_FREELOAD, false);
    }

    public static boolean isPro(Context context) {
        SharedPreferences prefs = getSharedPreferences(context);
        return isFreeloaded(context) || prefs.getBoolean(Constants.PREFS_KEY_TASKER, false);
    }

    public static boolean isProNotFreeloaded(Context context) {
        return isPro(context) && !isFreeloaded(context);
    }

    public static void setFreeloadStatus(Context context, boolean freeloaded) {
        SharedPreferences prefs = getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Constants.PREFS_KEY_FREELOAD, freeloaded);
        if (freeloaded) {
            editor.putBoolean(Constants.PREFS_KEY_TASKER, freeloaded);
        } else {
            editor.remove(Constants.PREFS_KEY_TASKER);
        }
        editor.apply();
    }

    public static void setProStatus(Context context, boolean pro) {
        SharedPreferences prefs = getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Constants.PREFS_KEY_TASKER, pro);
        if (pro) {
            editor.remove(Constants.PREFS_KEY_FREELOAD);
        }
        editor.apply();
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_WORLD_READABLE);
    }
}
