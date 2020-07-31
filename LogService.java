package com.redpoint.pts.searchmen.View.Service;

import android.app.Service;
import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import com.orhanobut.logger.Logger;
import com.redpoint.pts.searchmen.Constants;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Date;

public class LogService extends Service {
    private static final String TAG = LogService.class.getSimpleName();
    private static final String PATH = Environment.getExternalStorageDirectory().getPath() + "/MLogcat/";
    private static final String FILE_NAME = "CurrentProcessLog";
    private static final String FILE_NAME_SUFFIX = ".txt";
    Thread thread;
    boolean readlog = Constants.isPrintLog;
    private File file;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("hhp", "onCreate");
        this.initLogFile();
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                log2();//个人觉得这个方法更实用
            }
        });
    }

    private void initLogFile() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Log.w(TAG, "sdcard unmounted,skip dump exception");
            return;
        }
        File dir = new File(PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        long current = System.currentTimeMillis();
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(current));
        //以当前时间创建log文件
        this.file = new File(PATH + FILE_NAME + FILE_NAME_SUFFIX);
        if (file != null) {
            file.delete();
        }
        this.file = new File(PATH + FILE_NAME + FILE_NAME_SUFFIX);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        thread.start();
        Log.d("hhp", "onStart");
        super.onStart(intent, startId);
    }

    /**
     * 方法1
     */
    private void log2() {
        Log.d("hhp", "log2 start");
        String[] cmds = {"logcat", "-c"};
        String shellCmd = "logcat -v time -s *:V "; // adb logcat -v time *:W   //logcat -v time -s *:V
        Process process = null;
        Runtime runtime = Runtime.getRuntime();
        BufferedReader reader = null;
        try {
//            runtime.exec(cmds).waitFor();
            process = runtime.exec(shellCmd);
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.contains(String.valueOf(android.os.Process.myPid()))) {
                    // line = new String(line.getBytes("iso-8859-1"), "utf-8");
                    writeTofile(line);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d("hhp", "log2 finished");
    }

    /**
     * 方法2
     */
    private void log() {
        Log.d("hhp", "log start");
        String[] cmds = {"logcat", "-c"};
        String shellCmd = "logcat -v";// //adb logcat -v time *:W //logcat -v time -s *:W
        Process process = null;
        InputStream is = null;
        DataInputStream dis = null;
        String line = "";
        Runtime runtime = Runtime.getRuntime();
        try {
            runtime.exec(cmds);
            process = runtime.exec(shellCmd);
            is = process.getInputStream();
            dis = new DataInputStream(is);
            // String filter = GetPid();
            String filter = android.os.Process.myPid() + "";
            while ((line = dis.readLine()) != null) { //这里如果输入流没断，会一直循环下去。
                line = new String(line.getBytes("iso-8859-1"), "utf-8");
                if (line.contains(filter)) {
                    int pos = line.indexOf(":");
                    Log.d("hhp2", line + "");
                    writeTofile(line);
                }
            }
        } catch (Exception e) {
            Log.d("hhp", "log e :" + e.getMessage());
        }
        Log.d("hhp", "log finished");
    }

    private void writeTofile(String line) {
        if (file == null) {
            Logger.e("当前Mlog 下文件为空");
            return;
        }
        String content = line + "\r\n";
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(file, true);
            fos.write(content.getBytes());
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSelf();
    }
}
