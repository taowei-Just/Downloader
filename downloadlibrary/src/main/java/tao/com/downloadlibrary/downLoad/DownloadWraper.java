package tao.com.downloadlibrary.downLoad;

import android.text.TextUtils;
import android.util.Log;

import com.tao.utilslib.data.MD5Util;
import com.tao.utilslib.file.FileUtil;
import com.tao.utilslib.log.LogUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Response;

import tao.com.downloadlibrary.TaskInfo;

public class DownloadWraper {
    DownloadHelper downloadHelper;
    private MyTaskCall taskCall;
    private String tag = getClass().getSimpleName();

    public DownloadWraper(DownloadHelper downloadHelper) {
        this.downloadHelper = downloadHelper;
        taskCall = new MyTaskCall();
    }

    public boolean checkCach(DownloadInfo cache, DownloadInfo info) {
        if (cache == null)
            return false;
        // 检查文件是否存在
        // 检查文件MD5 是否匹配
        File file = new File(cache.getFilePath());
        try {
            if (file.exists() && !TextUtils.isEmpty(cache.getMd5()) && (cache.getMd5().equals(MD5Util.md5FromFile(cache.filePath, false)))) {
                if (!cache.getFilePath().equals(info.getFilePath()))
                    FileUtil.copyFile(cache.getFilePath(), info.getFilePath());
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean checkLocal(DownloadInfo downloadInfo) {
        File file = new File(downloadInfo.filePath);
        try {
            if (file.exists() && !TextUtils.isEmpty(downloadInfo.getMd5()) && (downloadInfo.getMd5().equals(MD5Util.getMD5fromBigFile(new File(downloadInfo.filePath))))) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean createinfo(DownloadInfo info, List<TaskInfo> pool, DownloadCall downloadCall) {
        // 创建下载taskInfo
        // 1.缓存文件存在
        // 2.进度记录存在
        // 3.进度记录与info 一致
//        Log.e(tag, " 创建下载信息  pool :" + pool.toString());

        downloadHelper.downloadPartner.updataCall(info, downloadCall);
        info.setStatue(2);
        downloadCall.onWaite(info);
        info.setThreadCount(info.getThreadCount() <= 0 ? 1 : info.getThreadCount());
        downloadHelper.downloadDbHelper.updataInfo(info);

//        Log.w(tag, "createinfo:" + info.toString());

        File file = new File(info.getFilePath() + ".cache");
        long poolLen = 0;
        if (pool == null || pool.size() <= 0) {
            netInfo(info);
            return false;
        }
        for (TaskInfo taskInfo : pool)
            poolLen += taskInfo.getProgressLen();

        if (file.exists() && pool.size() == info.getThreadCount() && info.getProgressLen() == poolLen) {
//            Log.w(tag, "poolInfo " + pool.toString());
            prepareDownload(info, pool);
            return true;
        } else {
            netInfo(info);
            return false;
        }

    }

    private void netInfo(final DownloadInfo downloadInfo) {
//        Log.e(tag, "netInfo: " + downloadInfo.toString());
        downloadHelper.httpClient.newCall(new Request.Builder().url(downloadInfo.getUrl()).get().build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(tag, "netInfo onFailure : " + e.toString());
                downloadHelper.downloadPartner.CallError(downloadInfo);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                long length = response.body().contentLength();
                downloadInfo.setFileLen(length);
                downloadInfo.setDownloadId(downloadHelper.getDownloadId());
                Headers headers = response.headers();
                LogUtil.e(tag , " headers " + headers.toString());
                if (rangS(headers, "Content-disposition")) {
                    List<String> values = headers.values("Content-disposition");
                    for (String s : values) {
                        if (s.contains("filename=")) {
                            String[] split = s.split("=");

                            if (TextUtils.isEmpty(downloadInfo.getFileName()))
                                downloadInfo.setFileName(split[split.length - 1]);
                            if (TextUtils.isEmpty(downloadInfo.getFilePath()))
                                downloadInfo.setFilePath((TextUtils.isEmpty(downloadInfo.getFilePath())? downloadHelper.buder.downloadFolder:downloadInfo.getFilePath().substring(0, downloadInfo.getFilePath().lastIndexOf("/")+1))+downloadInfo.getFileName());
                            break;
                        }
                    }
                }else {
                    if (TextUtils.isEmpty(downloadInfo.getFileName()))
                    downloadInfo.setFileName(getfileName(downloadInfo.getUrl()));
                    if (TextUtils.isEmpty(downloadInfo.getFilePath()))
                    downloadInfo.setFilePath(downloadHelper.buder.downloadFolder+downloadInfo.getFileName());
                }

//                Log.e(tag, " onResponse downloadInfo " + downloadInfo.toString());
                boolean ranges = !rangS(headers, "Ranges");

                boolean bytes = headers.values("Accept-Ranges").contains("bytes");
                boolean bytes1 = headers.values("Content-Ranges").contains("bytes");
                if (ranges || !(bytes || bytes1)) {
                    downloadInfo.setThreadCount(1);
                    downloadHelper.downloadDbHelper.updataInfo(downloadInfo);
                }

                List<TaskInfo> taskInfoS = new ArrayList<>();
                for (int j = 0; j < downloadInfo.getThreadCount(); j++) {
                    TaskInfo info = new TaskInfo();
                    info.setInde(j);
                    info.setDownloadId(downloadInfo.getDownloadId());
                    info.setFileName(downloadInfo.getFileName());
                    info.setUrl(downloadInfo.getUrl());
                    info.setThreadCount(downloadInfo.getThreadCount());
                    info.setFileLen(length);
                    info.setProgressLen(0);
                    info.setThreadLen(j == downloadInfo.getThreadCount() - 1 ? length / downloadInfo.getThreadCount() + length % downloadInfo.getThreadCount() : length / downloadInfo.getThreadCount());
                    info.setCacheFile(downloadInfo.getFilePath() + ".cache");
                    info.setOffeset(j * (length / downloadInfo.getThreadCount()));
                    taskInfoS.add(info);
                }
                downloadInfo.setProgressLen(0);
                Log.w(tag, "netInfo " + taskInfoS.toString());
                prepareDownload(downloadInfo, taskInfoS);
            }
        });
    }

    private boolean rangS(Headers headers, String head) {
        Set<String> names = headers.names();
        Iterator<String> iterator = names.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().contains(head))
                return true;
        }
        return false;
    }

    private void prepareDownload(DownloadInfo downloadInfo, List<TaskInfo> taskInfoS) {
        downloadHelper.downloadPartner.updataTaskInfo(downloadInfo, taskInfoS);
        downloadHelper.downloadDbHelper.updataTaskInfo(taskInfoS);

        for (int i = 0; i < downloadInfo.getThreadCount(); i++) {
            TaskInfo info = taskInfoS.get(i);
            DownloadTask task = new DownloadTask(downloadHelper, downloadInfo, info, taskCall);
            Future<?> submit = downloadHelper.poolExecutor.submit(task);
            downloadHelper.downloadPartner.updataTask(info, submit);
        }
    }

    public boolean checkTask(DownloadInfo downloadInfo, List<TaskInfo> pool) {
        return pool != null && pool.size() == downloadInfo.getThreadCount() && downloadInfo.getStatue() >= 2
                && threadRunning(pool)&&new File(downloadInfo.getFilePath()+".cache").exists();
    }

    private boolean threadRunning(List<TaskInfo> pool) {
        return  downloadHelper.downloadPartner.taskRuning(pool);
    }
    private class MyTaskCall implements DownloadTaskCAll {
        @Override
        public void onProgress(DownloadInfo df, TaskInfo info) {
            downloadHelper.downloadPartner.onProgress(df, info);
            Log.w(tag, "Thread:" + Thread.currentThread() + " onProgress  DownloadInfo " + df.toString() + " TaskInfo "+info);
        }

        @Override
        public void onStart(DownloadInfo df, TaskInfo info) {
            Log.e(tag, "onStart:" + Thread.currentThread() + " DownloadInfo  " + df.toString() + " TaskInfo " +info.toString());
            downloadHelper.downloadPartner.onStart(df, info);
        }

        @Override
        public void onCompleted(DownloadInfo df, TaskInfo info) {
            downloadHelper.downloadPartner.onCompleted(df, info);
        }

        @Override
        public void onError(DownloadInfo df, TaskInfo info) {
            downloadHelper.downloadPartner.onError(df, info);

        }
    }
    private String getfileName(String url) {
        return url.substring(url.lastIndexOf("/") + 1, url.length());
    }

}
