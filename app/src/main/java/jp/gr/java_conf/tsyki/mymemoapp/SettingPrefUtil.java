package jp.gr.java_conf.tsyki.mymemoapp;


import android.content.Context;
import android.content.SharedPreferences;

public class SettingPrefUtil {
    /** 保存先ファイル名 */
    private static final String PREF_FILE_NAME = "settings";

    private static final String KEY_FILE_NAME_PREFIX = "file.name.prefix";
    private static final String KEY_FILE_NAME_PREFIX_DEFAULT = "memo";

    private static final String KEY_TEXT_SIZE = "text.size";
    private static final String KEY_TEXT_SIZE_LARGE = "text.size.large";
    private static final String KEY_TEXT_SIZE_MEDIUM = "text.size.medium";
    private static final String KEY_TEXT_SIZE_SMALL = "text.size.small";

    public static String getFileNamePrefix(Context context){
        SharedPreferences sp = getSharedPreferences(context);
        return sp.getString(KEY_FILE_NAME_PREFIX, KEY_FILE_NAME_PREFIX_DEFAULT);
    }

    public static float getFontSize(Context context){
        SharedPreferences sp = getSharedPreferences(context);
        String storedSize = sp.getString(KEY_TEXT_SIZE, KEY_TEXT_SIZE_MEDIUM);
        switch(storedSize){
            case KEY_TEXT_SIZE_LARGE:
                return context.getResources().getDimension(R.dimen.settings_text_size_large);
            case KEY_TEXT_SIZE_SMALL:
                return context.getResources().getDimension(R.dimen.settings_text_size_small);
            case KEY_TEXT_SIZE_MEDIUM:
            default:
                return context.getResources().getDimension(R.dimen.settings_text_size_medium);
        }
    }

    private static final SharedPreferences getSharedPreferences(Context context){
        SharedPreferences sp = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
        return sp;
    }
}
