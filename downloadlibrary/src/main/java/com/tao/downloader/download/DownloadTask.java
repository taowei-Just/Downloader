package com.tao.downloader.download;

import android.util.Log;

import com.tao.downloader.iml.DownloadTaskCAll;
import com.tao.utilslib.log.LogUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.Request;
import okhttp3.Response;
import tao.com.downloadlibrary.TaskInfo;

public class DownloadTask implements Runnable {
    TaskInfo info;
    DownloadTaskCAll downloadCall;
    DownloadInfo downLoadInfo;
    private String tag = getClass().getSimpleName();
    Downloader downloadHelper;


    public DownloadTask(Downloader downloadHelper, DownloadInfo downLoadInfo, TaskInfo info, DownloadTaskCAll downloadCall) {
        this.info = info;
        this.downloadCall = downloadCall;
        this.downLoadInfo = downLoadInfo;
        this.downloadHelper = downloadHelper;
    }

    @Override
    public void run() {
        try {
            if (downLoadInfo.getStatue() == -1)
                return;
            Request.Builder builder = new Request.Builder();
            builder.addHeader("RANGE", "bytes=" + (info.getOffeset() + info.getProgressLen()) + "-" + (info.getOffeset() + info.getThreadLen()));
            Response execute = downloadHelper.httpClient.newCall(builder.get().url(info.getUrl()).build()).execute();
            long length = execute.body().contentLength();
            InputStream inputStream = execute.body().byteStream();
            File file = new File(info.getCacheFile());
            try {
                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }
            } catch (IOException e) {
                file.delete();
            }
            RandomAccessFile accessFile = new RandomAccessFile(file, "rwd");
            if (accessFile.length() < info.getFileLen()) ;
            accessFile.setLength(info.getFileLen());
            accessFile.seek(info.getOffeset() + info.getProgressLen());
            downLoadInfo.setStatue(3);
              downloadHelper. downloadDbHelper.updataInfo(downLoadInfo);
            downloadCall.onStart(downLoadInfo, info);
            byte[] buff = new byte[1024 * 10];
            int len;
            long time = System.currentTimeMillis();
            int cacheLen = 0;
            while ((len = inputStream.read(buff)) != -1) {
                if (info.getProgressLen() + len > info.getThreadLen()) {
                    len = (int) (info.getThreadLen() - info.getProgressLen());
                }
                accessFile.write(buff, 0, len);
                cacheLen += len;
                if (System.currentTimeMillis() - time >= 1000) {
                    writeProgress(cacheLen);
                    time = System.currentTimeMillis();
                    cacheLen = 0;
                }
                Thread.sleep(1);
            }
            Log.e(tag, "  cachel:" + cacheLen);
            if (cacheLen > 0) {
                writeProgress(cacheLen);
            }
            LogUtil.e(tag , "  onCompleted thread " + Thread.currentThread() + " task id " + info.getInde());
            downloadCall.onCompleted(downLoadInfo, info);
            execute.body().close();
        } catch (Exception e) {
            e.printStackTrace();
            downloadCall.onError(downLoadInfo, info);
        }
    }

    private void writeProgress(int len) {
        info.setCurrentLen(len);
        info.setProgressLen(info.getProgressLen() + len);
        downLoadInfo.setProgressLen(downLoadInfo.getProgressLen() + len);

        downloadHelper.downloadDbHelper.updataTaskInfo(info);
        downloadHelper.downloadDbHelper.updataInfo(downLoadInfo);

        downloadCall.onProgress(downLoadInfo, info);
    }

}
