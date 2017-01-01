package jp.gr.java_conf.tsyki.mymemoapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;

public class MemoRepository {
    /**
     * ファイル名フォーマット<BR>
     * prefix-yyyy-mm-dd-HH-MM-SS.txt
     */
    private static final String MEMO_FILE_FORMAT = "%1$s-%2$tF-%2$tH-%2$tM-%2$tS.txt";

    private MemoRepository(){

    }

    private static File getOutputDir(Context context){
        File outputDir;
        if(Build.VERSION.SDK_INT >= 19){
            // API Level19以上ならドキュメントファイル共有ディレクトリ
            outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        }
        else{
            outputDir = new File(context.getExternalFilesDir(null),"Documents");
        }

        if(outputDir == null){
            // 外部ストレージがマウントされていないなど
            return null;
        }
        boolean hasDirectory = true;
        if(!outputDir.exists() || !outputDir.isDirectory()){
            hasDirectory = outputDir.mkdir();
        }
        if(!hasDirectory){
            // こけたら終わり。例外投げるべき？
            return null;
        }
        return outputDir;
    }

    public static Uri create(Context context, String memo){
        File outputDir = getOutputDir(context);
        if(outputDir == null){
            return null;
        }
        String fileName = getFileName(context);
        File outputFile = new File(outputDir,fileName);
        if(outputFile == null){
            return null;
        }
        if (!writeToFile(outputFile, memo)){
            return null;
        }
        String title = memo.length() > 10 ? memo.substring(0, 10) : memo;
        // DB保存用
        ContentValues values = new ContentValues();
        values.put(MemoDBHelper.COLUMN_TITLE, title);
        values.put(MemoDBHelper.COLUMN_DATA, outputFile.getAbsolutePath());
        values.put(MemoDBHelper.COLUMN_DATE_ADDED, System.currentTimeMillis());

        Uri uri = context.getContentResolver().insert(MemoProvider.CONTENT_URI, values);
        return uri;
    }

    public static int update(Context context,Uri uri, String memo){
        String id = uri.getLastPathSegment();
        // NOTE DATAに保存されたファイルパスが入っているのでそれだけ取る
        Cursor cursor = context.getContentResolver().query(uri, new String[]{MemoDBHelper.COLUMN_DATA}, MemoDBHelper.COLUMN_ID + " = ?", new String[]{id}, null);
        if(cursor == null){
            return 0;
        }
        String filePath = null;
        while(cursor.moveToNext()){
            filePath = cursor.getString(cursor.getColumnIndex(MemoDBHelper.COLUMN_DATA));
        }
        cursor.close();
        if(TextUtils.isEmpty(filePath)){
            return 0;
        }
        File outputFile = new File(filePath);
        if(!writeToFile(outputFile, memo)){
            return 0;
        }
        return 1;
    }

    private static boolean writeToFile(File outputFile, String memo) {
        FileWriter writer = null;
        try{
            writer = new FileWriter(outputFile);
            writer.write(memo);
            writer.flush();
        }catch (IOException e){
            e.printStackTrace();
            return false;
        }finally {
            if(writer != null){
                try{
                    writer.close();
                }
                catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    private static String getFileName(Context context) {
        String fileNamePrefix = SettingPrefUtil.getFileNamePrefix(context);
        Calendar now = Calendar.getInstance();
        return String.format(MEMO_FILE_FORMAT, fileNamePrefix, now);
    }

    public static String findMemoByUri(Context context, Uri uri){
        BufferedReader reader  = null;
        StringBuilder builder = new StringBuilder();
        try{
            InputStream is = context.getContentResolver().openInputStream(uri);
            if(is != null){
                reader = new BufferedReader(new InputStreamReader(is));
                String line ;
                while((line = reader.readLine()) != null){
                    builder.append(line + "\n");
                }
            }
        }catch (FileNotFoundException e){
            e.printStackTrace();
            return "File not found." + uri;
        }
        catch (IOException e){
            e.printStackTrace();
            return "IO error occured." + uri;
        }
        finally {
            if(reader != null){
                try{
                    reader.close();
                }
                catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
        return builder. toString();
    }

    /**
     * 保存されているメモの一覧を取得
     */
    public static Cursor query(Context context){
        return context.getContentResolver().query(MemoProvider.CONTENT_URI, null,null,null,MemoDBHelper.COLUMN_DATE_MODIFIED +" DESC");
    }
}
