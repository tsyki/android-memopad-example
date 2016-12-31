package tsyki.java_conf.gr.jp.mymemoapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MemoDBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "memo.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE_NAME = "memo";
    // NOTE Anddoidでは伝統的に_idとつける
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_DATA = "_data";
    public static final String COLUMN_DATE_ADDED = "date_added";
    public static final String COLUMN_DATE_MODIFIED = "date_modified";

    public MemoDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " ( " + //
                COLUMN_ID + " INTEGER PRIVARY KEY AUTOINCREMENT, " + //
                COLUMN_TITLE + " TEXT, " + //
                COLUMN_DATA + " TEXT, " + //
                COLUMN_DATE_ADDED + " INTEGER NOT NULL, " + //
                COLUMN_DATE_MODIFIED + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL " + //
                ")";
        sqLiteDatabase.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
