package jp.gr.java_conf.tsyki.mymemoapp;

import android.app.ActivityManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MemoProvider extends ContentProvider {
    private static final String AUTHORITY = "jp.gr.java_conf.tsyki.mymemoapp.memo";
    private static final String CONTENT_PATH = "files";

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + CONTENT_PATH);

    public static final String MIME_DIR_PREFIX = "vnd.android.cursor.dir/";
    public static final String MIME_ITEM_PREFIX = "vnd.android.cursor.item/";

    public static final String MIME_ITEM = "vnd.memoapp.memo";
    public static final String MIME_TYPE_MULTIPLE = MIME_DIR_PREFIX + MIME_ITEM;
    public static final String MIME_TYPE_SINGLE = MIME_ITEM_PREFIX + MIME_ITEM;

    private static final int URI_MATCH_MEMO_LIST = 1;
    private static final int URI_MATCH_MEMO_ITEM = 2;

    private static final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        matcher.addURI(AUTHORITY, CONTENT_PATH, URI_MATCH_MEMO_LIST);
        matcher.addURI(AUTHORITY, CONTENT_PATH + "/#", URI_MATCH_MEMO_ITEM);
    }

    private SQLiteDatabase database;

    @Override
    public boolean onCreate() {
        MemoDBHelper helper = new MemoDBHelper(getContext());
        database = helper.getWritableDatabase();
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        int match = matcher.match(uri);
        Cursor cursor;
        switch (match) {
            case URI_MATCH_MEMO_LIST:
                cursor = database.query(MemoDBHelper.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case URI_MATCH_MEMO_ITEM:
                //IDがURIで指定されている場合、URIの最後のセグメントにIDが付く
                String id = uri.getLastPathSegment();
                cursor = database.query(MemoDBHelper.TABLE_NAME, projection,
                        MemoDBHelper.COLUMN_ID + "=" + id + (TextUtils.isEmpty(selection) ? "" : " AND (" + selection + ")"),//
                        selectionArgs, null, null, sortOrder
                );
                break;
            default:
                throw new IllegalArgumentException(String.valueOf(uri));
        }

        Context context = getContext();
        if (context != null) {
            cursor.setNotificationUri(context.getContentResolver(), uri);
        }
        return cursor;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (!isValidSignaturePermission()) {
            throw new SecurityException();
        }
        if (!isValidInput(values)) {
            throw new IllegalArgumentException(String.valueOf(values));
        }
        int match = matcher.match(uri);
        if (match == URI_MATCH_MEMO_LIST) {
            long id = database.insertOrThrow(MemoDBHelper.TABLE_NAME, null, values);
            if (id >= 0) {
                Uri newUri = Uri.withAppendedPath(CONTENT_URI, String.valueOf(id));
                Context context = getContext();
                if (context != null) {
                    context.getContentResolver().notifyChange(newUri, null);
                }
                return newUri;
            } else {
                // insert失敗
                return null;
            }
        }
        throw new IllegalArgumentException(String.valueOf(uri));
    }

    private boolean isValidInput(ContentValues values) {
        return true;
    }

    private boolean isValidSignaturePermission() {
        int myPid = android.os.Process.myPid();
        int callingPid = Binder.getCallingPid();
        if (myPid == callingPid) {
            return true;
        }
        Context context = getContext();
        if (context == null) {
            // onCreateが呼ばれていない
            return false;
        }
        PackageManager packageManager = context.getPackageManager();
        String myPackage = context.getPackageName();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // API Level 19以上ならgetCallingPackageが利用できる
            String callingPackage = getCallingPackage();
            return packageManager.checkSignatures(myPackage, callingPackage) == PackageManager.SIGNATURE_MATCH;
        }
        // API Level 19未満ならプロセスIDから該当パッケージをリストアップ
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        Set<String> callerPackages = new HashSet<>();
        List<ActivityManager.RunningAppProcessInfo> processes = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo processInfo : processes) {
            if (processInfo.pid == callingPid) {
                Collections.addAll(callerPackages, processInfo.pkgList);
            }
        }
        for (String packageName : callerPackages) {
            if (packageManager.checkSignatures(myPackage, packageName) == PackageManager.SIGNATURE_MATCH) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (!isValidSignaturePermission()) {
            throw new SecurityException();
        }
        int match = matcher.match(uri);
        if (match == URI_MATCH_MEMO_LIST) {
            return database.delete(MemoDBHelper.TABLE_NAME, selection, selectionArgs);
        } else if (match == URI_MATCH_MEMO_ITEM) {
            String id = uri.getLastPathSegment();
            int affected = database.delete(MemoDBHelper.TABLE_NAME,
                    MemoDBHelper.COLUMN_ID + "=" + id + (TextUtils.isEmpty(selection) ? "" : " AND (" + selection + ")"),//
                    selectionArgs
            );
            Context context = getContext();
            if (context != null) {
                context.getContentResolver().notifyChange(uri, null);
            }
            return affected;
        } else {
            throw new IllegalArgumentException(String.valueOf(uri));
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (!isValidSignaturePermission()) {
            throw new SecurityException();
        }
        if (!isValidInput(values)) {
            throw new IllegalArgumentException(String.valueOf(values));
        }
        int match = matcher.match(uri);
        if (match == URI_MATCH_MEMO_LIST) {
            return database.update(MemoDBHelper.TABLE_NAME, values, selection, selectionArgs);
        } else if (match == URI_MATCH_MEMO_ITEM) {
            String id = uri.getLastPathSegment();
            int affected = database.update(MemoDBHelper.TABLE_NAME,values,
                    MemoDBHelper.COLUMN_ID + "=" + id + (TextUtils.isEmpty(selection) ? "" : " AND (" + selection + ")"),//
                    selectionArgs
            );
            Context context = getContext();
            if (context != null) {
                context.getContentResolver().notifyChange(uri, null);
            }
            return affected;
        } else {
            throw new IllegalArgumentException(String.valueOf(uri));
        }
    }

    @Nullable
    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!TextUtils.isEmpty(mode) && mode.contains("w") && !isValidSignaturePermission()) {
            throw new SecurityException();
        }
        int match = matcher.match(uri);
        if (match == URI_MATCH_MEMO_ITEM) {
            return openFileHelper(uri, mode);
        }
        throw new IllegalArgumentException(String.valueOf(uri));
    }
}
