package com.tao.downloader.download;


import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import com.tao.downloader.iml.DownloadTaskCAll;
import com.tao.downloader.iml.IDonwloader;
import com.tao.downloader.iml.IDownloadCall;
import com.tao.utilslib.data.MD5Util;
import com.tao.utilslib.file.FileUtil;
import com.tao.utilslib.list.MapUtil;
import com.tao.utilslib.log.LogUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import tao.com.downloadlibrary.TaskInfo;

public class Downloader implements IDonwloader {

    public static OkHttpClient httpClient = new OkHttpClient();
    String tag = getClass().getSimpleName();
    Builder builder;
    DownloadDbHelper downloadDbHelper;
    static Downloader downloadHelper;
    private   ThreadPoolExecutor  threadPool;

    // 下载信息对应的下载线程列表
    Map<DownloadInfo, List<TaskInfo>> downloadTaskMap = new HashMap<>();
    // 对应的回调列表
    Map<DownloadInfo, IDownloadCall> infoCallMap = new HashMap<>();
    //线程对应的操作
    Map<TaskInfo, Future> taskMap = new HashMap<>();

    DownloadTaskCAll taskCall;

    public static Downloader getInstance(Context context) {
        if (downloadHelper == null) {
            Builder buder = new Builder();
            buder.context = context;
            downloadHelper = new Downloader(buder);
        }
        return downloadHelper;
    }

    public static Downloader getInstance(Builder buder) {
        if (downloadHelper == null)
            downloadHelper = new Downloader(buder);
        return downloadHelper;
    }

    private Downloader(Builder builder) {
        this.builder = builder;
        taskCall = new MyCall();
        downloadDbHelper = new DownloadDbHelper(builder);
        threadPool = new ThreadPoolExecutor(builder.maxDownloadThread, builder.maxDownloadThread, 1, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>());
        threadPool.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
    }

    @Override
    public synchronized void addDownload(DownloadInfo downloadInfo, IDownloadCall call) throws DownloadException {
        DownloadException exception;
        //1.检查下载信息是否有效   
        if (null != (exception = checkDownloadInfo(downloadInfo))) {
            throw exception;
        }
        //2.检查文件是否匹配已下载且匹配当前下载的信息
        if (checkLocalDownload(downloadInfo)) {
            call.onComplete(downloadInfo);
            return;
        }
        //3.检查文件是够已经添加到下载列表
        findMatchDownload(downloadInfo);
        addDownloadCall(downloadInfo, call);

        //4.启动下载线程
        perpareDownload(downloadInfo);
    }

    private void perpareDownload(final DownloadInfo downloadInfo) {

        // 首先查看当前的下载任务是否正在执行
        // 如果在执行则不操作
        if (redirect(downloadInfo)) {
            LogUtil.e(tag, " 文件正在下载队列中 不提交 ");
            return;
        }

        downloadInfo.setStatue(2);
        callWait(downloadInfo);
        downloadInfo.setThreadCount(downloadInfo.getThreadCount() <= 0 ? 1 : downloadInfo.getThreadCount());
        LogUtil.e(tag, " 更新下载信息 ");
        if( null==downloadDbHelper.findLoaclDownload(downloadInfo.getUrl()))
            downloadInfo.setDownloadId(downloadDbHelper.getDownloadId()+1);
        downloadDbHelper.updataInfo(downloadInfo);

        // 当前链接地址没有在下载  
        // 如果未执行则从本地缓存找相关task信息
        // 有则恢复下载  无 则 重新分配

        List<TaskInfo> localTaskInfo = downloadDbHelper.findLocalTaskInfo(downloadInfo.getDownloadId());
        long poolLen = 0;

        for (TaskInfo taskInfo : localTaskInfo)
            poolLen += taskInfo.getProgressLen();

        File file = new File(downloadInfo.getFilePath() + ".cache");
        LogUtil.e(tag, "  size " + localTaskInfo.size() + "  poolLen  " + poolLen);

        if (file.exists() && localTaskInfo.size() == downloadInfo.getThreadCount() && downloadInfo.getProgressLen() == poolLen) {
//            Log.w(tag, "poolInfo " + pool.toString());
            prepareDownload(downloadInfo, localTaskInfo);
            LogUtil.e(tag, " 文件重启下载");
        } else {
            LogUtil.e(tag, " 文件新建下载");
            netInfo(downloadInfo);
        }

    }


    private void netInfo(final DownloadInfo downloadInfo) {
//        Log.e(tag, "netInfo: " + downloadInfo.toString());
        httpClient.newCall(new Request.Builder().url(downloadInfo.getUrl()).get().build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
//                Log.e(tag, "netInfo onFailure : " + e.toString());
                callError(downloadInfo);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                long length = response.body().contentLength();
                downloadInfo.setFileLen(length);
            
                Headers headers = response.headers();

                LogUtil.e(tag, headers.toString());
                if (rangS(headers, "content-disposition")) {
                    List<String> values = headers.values("content-disposition");
                    for (String s : values) {
                        LogUtil.e(tag, " val " + s);
                        if (s.contains("filename=")) {
                            String[] split = s.split("=");

                            if (TextUtils.isEmpty(downloadInfo.getFileName()))
                                downloadInfo.setFileName(split[split.length - 1]);

                            if (TextUtils.isEmpty(downloadInfo.getFilePath()))
                                downloadInfo.setFilePath((TextUtils.isEmpty(downloadInfo.getFilePath()) ? builder.downloadFolder : downloadInfo.getFilePath().substring(0, downloadInfo.getFilePath().lastIndexOf("/") + 1)) + downloadInfo.getFileName());
                            break;
                        }
                    }
                } else {
                    if (TextUtils.isEmpty(downloadInfo.getFileName()))
                        downloadInfo.setFileName(getfileName(downloadInfo.getUrl()));

                    if (TextUtils.isEmpty(downloadInfo.getFilePath()))
                        downloadInfo.setFilePath(builder.downloadFolder + downloadInfo.getFileName());
                }

//                Log.e(tag, " onResponse downloadInfo " + downloadInfo.toString());
                boolean ranges = !rangS(headers, "Ranges");

                boolean bytes = headers.values("Accept-Ranges").contains("bytes");
                boolean bytes1 = headers.values("Content-Ranges").contains("bytes");
                if (ranges || !(bytes || bytes1)) {
                    downloadInfo.setThreadCount(1);
                    downloadDbHelper.updataInfo(downloadInfo);
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
                LogUtil.d(tag, "netInfo " + taskInfoS.toString());
                prepareDownload(downloadInfo, taskInfoS);
            }
        });
    }

    private void prepareDownload(DownloadInfo downloadInfo, List<TaskInfo> taskInfoS) {
        downloadTaskMap.put(downloadInfo, taskInfoS);
        downloadDbHelper.updataTaskInfo(taskInfoS);
        for (int i = 0; i < downloadInfo.getThreadCount(); i++) {
            TaskInfo info = taskInfoS.get(i);
            DownloadTask task = new DownloadTask(this, downloadInfo, info, taskCall);
            Future<?> submit = threadPool.submit(task);
            taskMap.put(info, submit);
        }
    }

    private String getfileName(String url) {
        String substring = url.substring(url.lastIndexOf("/") + 1);
        if (substring.toLowerCase().contains("name=")) {
            String[] split = substring.split("name=");
            substring = split[split.length - 1];
        }
        return substring;
    }

    private boolean rangS(Headers headers, String head) {
        Set<String> names = headers.names();
        Iterator<String> iterator = names.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().toLowerCase().contains(head.toLowerCase()))
                return true;
        }
        return false;
    }

    private void callError(final DownloadInfo downloadInfo) {
        try {
            MapUtil.iteratorMap(infoCallMap, new MapUtil.IteratorCall<DownloadInfo, IDownloadCall>() {
                @Override
                public MapUtil.IteratorType onIterator(DownloadInfo o, IDownloadCall o2) {

                    if (o.getDownloadId() == downloadInfo.getDownloadId()) {
                        o2.onError(downloadInfo);
                        return MapUtil.IteratorType.ret;
                    }
                    return MapUtil.IteratorType.next;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void callComplete(final DownloadInfo downloadInfo) {
        try {
            MapUtil.iteratorMap(infoCallMap, new MapUtil.IteratorCall<DownloadInfo, IDownloadCall>() {
                @Override
                public MapUtil.IteratorType onIterator(DownloadInfo o, IDownloadCall o2) {
                    if (o.getDownloadId() == downloadInfo.getDownloadId()) {
                        o2.onComplete(downloadInfo);
                        return MapUtil.IteratorType.ret;
                    }
                    return MapUtil.IteratorType.next;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void callStart(final DownloadInfo downloadInfo) {
        try {
            MapUtil.iteratorMap(infoCallMap, new MapUtil.IteratorCall<DownloadInfo, IDownloadCall>() {
                @Override
                public MapUtil.IteratorType onIterator(DownloadInfo o, IDownloadCall o2) {

                    if (o.getDownloadId() == downloadInfo.getDownloadId()) {
                        o2.onStart(downloadInfo);
                        return MapUtil.IteratorType.ret;
                    }
                    return MapUtil.IteratorType.next;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void callProgress(final DownloadInfo downloadInfo, final TaskInfo taskInfo) {
        try {
            MapUtil.iteratorMap(infoCallMap, new MapUtil.IteratorCall<DownloadInfo, IDownloadCall>() {
                @Override
                public MapUtil.IteratorType onIterator(DownloadInfo o, IDownloadCall o2) {

                    if (o.getDownloadId() == downloadInfo.getDownloadId()) {
                        o2.onProgress(downloadInfo);
                        o2.onProgress(downloadInfo, taskInfo);
                        return MapUtil.IteratorType.ret;
                    }
                    return MapUtil.IteratorType.next;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private boolean redirect(final DownloadInfo downloadInfo) {
        List<TaskInfo> downloadTaskS = getDownloadTaskS(downloadInfo);
        if (null == downloadTaskS  || downloadTaskS.size() ==0)
            return false;
        LogUtil.e(tag , " redirect " + downloadTaskS.size());
        boolean canceled = false;
        for (int i = 0; i < downloadTaskS.size(); i++) {
            TaskInfo taskInfo = downloadTaskS.get(i);
            Future future = taskMap.get(taskInfo);
            if (future != null) {
                if (future.isCancelled()) {
                    // 下载被取消了 
                    canceled = true;
                    break;
                }
            } 
        }

        if (!canceled) {
            // 把文件定向到新文件路径
            try {
                MapUtil.iteratorMap(downloadTaskMap, new MapUtil.IteratorCall<DownloadInfo, Object>() {
                    @Override
                    public MapUtil.IteratorType onIterator(DownloadInfo o, Object o2) {
                        if (downloadInfo.getDownloadId() == o.getDownloadId() && !TextUtils.isEmpty(downloadInfo.getFilePath())) {
                            o.setFilePath(downloadInfo.getFilePath());
                        }
                        return MapUtil.IteratorType.next;
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }

            return true;
        }

        return false;
    }

    private List<TaskInfo> getDownloadTaskS(final DownloadInfo downloadInfo) {
        try {
            return MapUtil.iteratorMap(downloadTaskMap, new MapUtil.IteratorCall<DownloadInfo, List<TaskInfo>>() {
                @Override
                public MapUtil.IteratorType onIterator(DownloadInfo info, List<TaskInfo> taskInfos) {

                    if (info.getDownloadId() == downloadInfo.getDownloadId())
                        return MapUtil.IteratorType.ret;
                    return MapUtil.IteratorType.next;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void callWait(final DownloadInfo downloadInfo) {
        try {
            MapUtil.iteratorMap(infoCallMap, new MapUtil.IteratorCall<DownloadInfo, IDownloadCall>() {
                @Override
                public MapUtil.IteratorType onIterator(DownloadInfo o, IDownloadCall o2) {

                    if (o.getDownloadId() == downloadInfo.getDownloadId()) {
                        o2.onWait(downloadInfo);
                        return MapUtil.IteratorType.ret;
                    }
                    return MapUtil.IteratorType.next;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void addDownloadCall(final DownloadInfo downloadInfo, final IDownloadCall call) {
        try {
            MapUtil.iteratorMap(infoCallMap, new MapUtil.IteratorCall<DownloadInfo, IDownloadCall>() {
                @Override
                public MapUtil.IteratorType onIterator(DownloadInfo o, IDownloadCall o2) {
                    if (o.getDownloadId() == downloadInfo.getDownloadId()) {
                        infoCallMap.remove(o);
                        return MapUtil.IteratorType.ret;
                    }
                    return MapUtil.IteratorType.next;
                }
            });
            infoCallMap.put(downloadInfo, call);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private boolean checkLocalDownload(DownloadInfo downloadInfo) {

        // 检查本地是否下载过这个链接
        DownloadInfo downloadInfos = downloadDbHelper.findLoaclDownload(downloadInfo.url);
        if (null!=downloadInfos)
        LogUtil.e(tag, "checkLocalDownload " + downloadInfos.toString());

        if (null == downloadInfos || downloadInfos.getStatue() < 4) {
            return false;
        }

        if (!downloadInfo.matchMd5) {
            File file = new File(downloadInfos.getFilePath());
            LogUtil.e(tag, " file " + file.toString());
            if (file.exists() && file.length() == downloadInfos.getFileLen()) {
                if (TextUtils.isEmpty(downloadInfo.getFilePath())) {

                    downloadInfo.setStatue(downloadInfos.getStatue());
                    downloadInfo.setDownloadId(downloadInfos.getDownloadId());
                    downloadInfo.setThreadCount(downloadInfos.getThreadCount());
                    downloadInfo.setProgressLen(downloadInfos.getProgressLen());
                    downloadInfo.setFileLen(downloadInfos.getFileLen());
                    downloadInfo.setMd5(downloadInfos.getMd5());

                    if (TextUtils.isEmpty(downloadInfo.getFileName()))
                        downloadInfo.setFileName(downloadInfos.getFileName());

                    downloadInfo.setFilePath(downloadInfos.getFilePath());

                    return true;
                }
                if (!downloadInfos.getFilePath().equals(downloadInfo.getFilePath())) {
                    FileUtil.copyFile(downloadInfos.getFilePath(), downloadInfo.getFilePath());

                    downloadInfo.setStatue(downloadInfos.getStatue());
                    downloadInfo.setDownloadId(downloadInfos.getDownloadId());
                    downloadInfo.setThreadCount(downloadInfos.getThreadCount());
                    downloadInfo.setProgressLen(downloadInfos.getProgressLen());
                    downloadInfo.setFileLen(downloadInfos.getFileLen());
                    downloadInfo.setMd5(downloadInfos.getMd5());

                    return true;
                }
            }
        } else {
            if (TextUtils.isEmpty(downloadInfo.getMd5()) || TextUtils.isEmpty(downloadInfos.getMd5()))
                return false;
            if (!downloadInfo.getMd5().equals(downloadInfos.getMd5()))
                return false;
            try {
                File file = new File(downloadInfos.getFilePath());
                String md5fromBigFile = MD5Util.getMD5fromBigFile(file);
                if (downloadInfo.getMd5().equals(md5fromBigFile)) {
                    if (file.exists()) {
                        if (!downloadInfos.getFilePath().equals(downloadInfo.getFilePath())) {
                            FileUtil.copyFile(downloadInfos.getFilePath(), downloadInfo.getFilePath());
                            if (TextUtils.isEmpty(downloadInfo.getFileName()))
                                downloadInfo.setFileName(downloadInfos.getFileName());
                            downloadInfo.setFilePath(downloadInfos.getFilePath());
                        }

                        downloadInfo.setStatue(downloadInfos.getStatue());
                        downloadInfo.setDownloadId(downloadInfos.getDownloadId());
                        downloadInfo.setThreadCount(downloadInfos.getThreadCount());
                        downloadInfo.setProgressLen(downloadInfos.getProgressLen());
                        downloadInfo.setFileLen(downloadInfos.getFileLen());
                        downloadInfo.setMd5(downloadInfos.getMd5());


                        return true;
                    }
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private void findMatchDownload(DownloadInfo downloadInfo) {

        LogUtil.e(tag, "  获取本地已有的下载信息 进行比对 " + downloadInfo.toString());

        // 检查是否正在下载
        DownloadInfo downloadInfos = downloadDbHelper.findLoaclDownload(downloadInfo.url);

        if (null == downloadInfos)
            return;

        LogUtil.e(tag, "  local " + downloadInfos.toString());

        //1. 搜索本地库中下载信息 得到下载线程id
        //2. 根据id和状态 得到task信息
        if (downloadInfos.statue == 3) {
            // 正在下载   //3. 根据task信息 结束正在运行的task
            if (downloadInfo.cover) {
                // 强制停止线程
                closeTask(downloadInfos.getDownloadId());
                removeTaskInfo(downloadInfos.getDownloadId());
                resetDownloadInfo(downloadInfo);
            } else {
                downloadInfo.setStatue(downloadInfos.getStatue());
                downloadInfo.setDownloadId(downloadInfos.getDownloadId());

                downloadInfo.setProgressLen(downloadInfos.getProgressLen());
                downloadInfo.setFileLen(downloadInfos.getFileLen());

                if (TextUtils.isEmpty(downloadInfo.getFileName()))
                    downloadInfo.setFileName(downloadInfos.getFileName());
                if (TextUtils.isEmpty(downloadInfo.getFilePath()))
                    downloadInfo.setFilePath(downloadInfos.getFilePath());
            }
        } else if (downloadInfos.statue == 2) {
            // 等待下载中  
            if (downloadInfo.cover) {
                // 强制停止线程
                closeTask(downloadInfos.getDownloadId());
                removeTaskInfo(downloadInfos.getDownloadId());
                resetDownloadInfo(downloadInfo);
            } else {
                downloadInfo.setStatue(downloadInfos.getStatue());
                downloadInfo.setDownloadId(downloadInfos.getDownloadId());

                downloadInfo.setProgressLen(downloadInfos.getProgressLen());
                downloadInfo.setFileLen(downloadInfos.getFileLen());


                if (TextUtils.isEmpty(downloadInfo.getFileName()))
                    downloadInfo.setFileName(downloadInfos.getFileName());
                if (TextUtils.isEmpty(downloadInfo.getFilePath()))
                    downloadInfo.setFilePath(downloadInfos.getFilePath());
            }
        } else {
            downloadInfo.setStatue(downloadInfos.getStatue());
            downloadInfo.setDownloadId(downloadInfos.getDownloadId());

            downloadInfo.setProgressLen(downloadInfos.getProgressLen());
            downloadInfo.setFileLen(downloadInfos.getFileLen());

            if (TextUtils.isEmpty(downloadInfo.getFileName()))
                downloadInfo.setFileName(downloadInfos.getFileName());
            if (TextUtils.isEmpty(downloadInfo.getFilePath()))
                downloadInfo.setFilePath(downloadInfos.getFilePath());
        }
        LogUtil.e(tag, "   最终得到信息： " + downloadInfo.toString());

    }

    private void resetDownloadInfo(DownloadInfo downloadInfo) {
        downloadDbHelper.updataInfo(downloadInfo);
    }

    private void removeTaskInfo(int downloadId) {
        List<TaskInfo> localTaskInfo = downloadDbHelper.findLocalTaskInfo(downloadId);
        for (TaskInfo taskInfo : localTaskInfo) {
            downloadDbHelper.removeTask(taskInfo);
        }
    }

    private void closeTask(final int downloadId) {
        try {
            MapUtil.iteratorMap(downloadTaskMap, new MapUtil.IteratorCall<DownloadInfo, List<TaskInfo>>() {
                @Override
                public MapUtil.IteratorType onIterator(DownloadInfo downloadInfo, List<TaskInfo> taskInfos) {
                    if (null != downloadInfo && downloadInfo.getDownloadId() == downloadId) {
                        downloadInfo.setStatue(-1);
                        for (TaskInfo taskInfo : taskInfos) {
                            removeTask(taskInfo);
                        }
                        downloadTaskMap.remove(downloadInfo);
                        return MapUtil.IteratorType.ret;
                    }
                    return MapUtil.IteratorType.next;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeTask(final TaskInfo info) {
        if (null == info)
            return;
        try {
            MapUtil.iteratorMap(taskMap, new MapUtil.IteratorCall<TaskInfo, Future>() {
                @Override
                public MapUtil.IteratorType onIterator(TaskInfo taskInfo, Future future) {
                    if (taskInfo.getDownloadId() == info.getDownloadId()) {
                        if (null != future)
                            future.cancel(true);
                        LogUtil.e(tag , " removeTask info  "  +taskInfo.toString() );
                        taskMap.remove(taskInfo);
                    }
                    return MapUtil.IteratorType.ret;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private DownloadException checkDownloadInfo(DownloadInfo downloadInfo) {
        LogUtil.e(tag, "checkDownloadInfo " + downloadInfo.toString());
        if (!TextUtils.isEmpty(downloadInfo.url)) {
            return null;
        } else {
            DownloadException exception = new DownloadException();
            exception.exceptionCode = 1;
            exception.exceptStr = "下载地址不能为空！";
            return exception;
        }

    }

    @Override
    public void pauseDownload(String url) {

    }

    @Override
    public void reStartDownload(String url) {

    }

    @Override
    public void stopDownload(String url) {
        DownloadInfo loaclDownload = downloadDbHelper.findLoaclDownload(url);
        closeTask(loaclDownload.getDownloadId());
    }

    @Override
    public void deleteDownload(String url) {

    }

    public static class Builder {

        Context context;
        // 数据库路径
        String dbPath;
        // 最大同时下载线程
        int maxDownloadThread = 10;
        //最大同时下载文件
        int maxDownloadFileCount = 10;
        int maxThreadCount = 5;
        // 文件保存文件夹
        String downloadFolder = Environment.getExternalStorageDirectory() + "/download/";


        public Builder setDbPath(String dbPath) {
            this.dbPath = dbPath;
            return this;
        }

        public Builder setMaxThreadCount(int maxThreadCount) {
            this.maxThreadCount = maxThreadCount;
            return this;

        }

        public Builder setDownloadFolder(String downloadFolder) {
            this.downloadFolder = downloadFolder;
            return this;
        }

        public Builder setMaxDownloadThread(int maxDownloadThread) {
            this.maxDownloadThread = maxDownloadThread;
            return this;
        }

        public Builder setMaxDownloadFileCount(int maxDownloadFileCount) {
            this.maxDownloadFileCount = maxDownloadFileCount;
            return this;
        }

        public Builder setContext(Context context) {
            this.context = context;
            return this;
        }
    }


    private class MyCall implements DownloadTaskCAll {
        String tag = "MyCall";

        @Override
        public void onProgress(DownloadInfo df, TaskInfo info) {
            callProgress(df, info);
            LogUtil.d(tag, "Thread:" + Thread.currentThread() + " onProgress  DownloadInfo " + df.toString() + " TaskInfo " + info);
        }

        @Override
        public void onStart(DownloadInfo df, TaskInfo info) {
            LogUtil.e(tag, "onStart:" + Thread.currentThread() + " DownloadInfo  " + df.toString() + " TaskInfo " + info.toString());
            callStart(df);
        }

        @Override
        public void onCompleted(DownloadInfo df, TaskInfo info) {

            LogUtil.e(tag, "onCompleted downloadID:" + info.getDownloadId() + " index:" + info.getInde());
            LogUtil.d(tag, "Thread:" + Thread.currentThread() + "onCompleted:" + df + "  task info " + info.toString());


            if (df.getProgressLen() >= df.getFileLen()) {
                renameFile(df);
                df.setStatue(4);
                downloadDbHelper.updataInfo(df);
                downloadDbHelper.cleanTask(df.getDownloadId());
                callComplete(df);
            }
            
            clearInfo(df, info);
        }

        @Override
        public void onError(DownloadInfo df, TaskInfo info) {
            callError(df);
            clearInfo(df, info);
        }
    }

    private void clearInfo(DownloadInfo df, TaskInfo info) {
        LogUtil.e(tag , " clearInfo "  );

        removeTask(df);
        removeTask(info);
        removeCall(df);
    }

    private void removeCall(final DownloadInfo df) {
        try {
            MapUtil.iteratorMap(infoCallMap, new MapUtil.IteratorCall<DownloadInfo, IDownloadCall>() {
                @Override
                public MapUtil.IteratorType onIterator(DownloadInfo key, IDownloadCall value) {
                    if (key.getDownloadId() == df.getDownloadId()) {
                        LogUtil.e(tag , " removeCall "   + key);

                        infoCallMap.remove(key);
                        return MapUtil.IteratorType.ret ;
                    }
                    return MapUtil.IteratorType.next;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeTask(final DownloadInfo df) {
        try {
            MapUtil.iteratorMap(downloadTaskMap, new MapUtil.IteratorCall<DownloadInfo, List<TaskInfo>>() {
                @Override
                public MapUtil.IteratorType onIterator(DownloadInfo key, List<TaskInfo> value) {
                    if (key.getDownloadId() == df.getDownloadId()) {
                        LogUtil.e(tag , " removeTask "  +df.toString() );
                        downloadTaskMap.remove(key);
                    }
                    return MapUtil.IteratorType.ret;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void renameFile(DownloadInfo df) {
        if (df.getProgressLen() >= df.getFileLen()) {
            try {
                File file = new File(df.getFilePath() + ".cache");
                if (!file.exists())
                    return;
                File dest = new File(df.getFilePath());
                df.setMd5(MD5Util.getMD5fromBigFile(file));
                if (dest.exists())
                    dest.delete();
                file.renameTo(dest);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
