package jp.gr.java_conf.tsyki.mymemoapp;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

public class MemoRepository {
    /**
     * ファイル名フォーマット<BR>
     * prefix-yyyy-mm-dd-HH-MM-SS.txt
     */
    private static final String MEMO_FILE_FORMAT = "%1$s-%2$tF-%2$tH-%2$tM-%2$tS.txt";

    private MemoRepository(){

    }

    public static Uri store(Context context, String memo){
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
        File outputFile = saveAsFile(context, outputDir,memo);
        if(outputFile == null){
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

    private static File saveAsFile(Context context, File outputDir, String memo){
        String fileNamePrefix = SettingPrefUtil.getFileNamePrefix(context);
        Calendar now = Calendar.getInstance();
        String fileName = String.format(MEMO_FILE_FORMAT, fileNamePrefix, now);
        File outputFile = new File(outputDir,fileName);

        FileWriter writer = null;
        try{
            writer = new FileWriter(outputFile);
            writer.write(memo);
            writer.flush();
        }catch (IOException e){
            e.printStackTrace();
            return null;
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
        return outputFile;
    }
}
