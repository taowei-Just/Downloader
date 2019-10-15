package tao.com.downloadlibrary.tool;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import com.tao.utilslib.data.MD5Util;
import com.tao.utilslib.file.FileUtil;
import com.tao.utilslib.list.MapUtil;
import com.tao.utilslib.log.LogUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class DownloaderTool {
    static Context context;
    static ExecutorService threadPool;
    static DownloadDbHelper dbHelper;

    static Set<String> urls = new HashSet<>();
    static HashMap<String, List<Future>> runablesMap = new HashMap<>();
    static HashMap<String, List<TaskInfo>> taskInfosMap = new HashMap<>();
    static HashMap<String, DownloadInfo> infoHashMap = new HashMap<>();
    public static OkHttpClient httpClient = new OkHttpClient.Builder()
            .readTimeout(20, TimeUnit.SECONDS)
            .build();

    private static String tag = "DownloaderTool";

    static int cacheId = 0;

    private static ExecutorService addPool;

    static {
        threadPool = Executors.newFixedThreadPool(20);
        addPool = Executors.newFixedThreadPool(20);
        ((ThreadPoolExecutor) threadPool).setRejectedExecutionHandler(new MyRejected());
    }

    public static void addDownload(String url, String path, DownloadCall call) {
        if (TextUtils.isEmpty(url) || !(url.startsWith("http://") || url.startsWith("https://")))
            return;
        addDownload(new DownloadInfo(url, path, call));
    }

    static class AddTask implements Runnable {
        DownloadInfo info;

        public AddTask(DownloadInfo info) {
            this.info = info;
        }

        @Override
        public void run() {
            loacal(info);
        }
    }

    public static synchronized void addDownload(DownloadInfo info) {
        LogUtil.e(tag, " addDownload " + info.toString());
        if (!init(null)) {
            LogUtil.e(tag, " 初始化失败..");
            return;
        }
        if (urls.contains(info.getUrl())) {
            LogUtil.e(tag, " 提交中..");
            return;
        }
        urls.add(info.getUrl());

        addPool.submit(new AddTask(info));
    }

    private static void loacal(DownloadInfo info) {

        if (checkLoacal(info)) {
            LogUtil.e(tag, " 文件已经存在 正在处理中..");
            
            if (urls.contains(info.getUrl()))
            urls.remove(info.getUrl());
            return;
        }
        LogUtil.e(tag, " 信息 " + info.toString());
        if (info.getThreadCount() == 1) {
            if (TextUtils.isEmpty(info.getPath()) || TextUtils.isEmpty(info.getFileName()) || info.getFileLen() == 0) {
                prepareDownload(info);
            } else {

                List<TaskInfo> taskList = dbHelper.findLocalTaskInfo(info);
                if (null == taskList)
                    taskList = new ArrayList<>();

                info.setDownloadId(info.getDownloadId() == 0 ? getDownloadId() : info.getDownloadId());
                dbHelper.updataInfo(info);
                infoHashMap.put(info.getUrl(), info);

                if (taskList.size() == 0) {
                    TaskInfo taskInfo = new TaskInfo(info.getDownloadId(), 0, info.getUrl(), info.getPath());
                    taskInfo.setFileLen(info.getFileLen());
                    taskInfo.setThreadLen(info.getFileLen());
                    taskList.add(taskInfo);
                }
                subDownload(info, taskList);
            }
        } else {
            prepareDownload(info);
        }
    }

    private static boolean checkLoacal(DownloadInfo info) {
        LogUtil.e(tag, " checkLoacal " + info.toString());
        DownloadInfo loaclDownload = dbHelper.findLoaclDownload(info.getUrl());

        if (null != loaclDownload)
            LogUtil.e(tag, " localInfo  " + loaclDownload.toString());

        if (null == loaclDownload) {
            return false;
        }
        info.setDownloadId(loaclDownload.getDownloadId());

        if (TextUtils.isEmpty(loaclDownload.getMd5())) {
            if (TextUtils.isEmpty(info.getPath())) {
                info.setPath(loaclDownload.getPath());
                info.setFileName(loaclDownload.getFileName());
            }
            info.setFileLen(loaclDownload.getFileLen());
            info.setProgressLen(loaclDownload.getProgressLen());
            return false;
        }

        File file = new File(loaclDownload.getPath());
        try {
            String md5fromBigFile = MD5Util.getMD5fromBigFile(file);
            LogUtil.e(tag, file.getAbsolutePath() + " : " + md5fromBigFile);
            if (file.exists() && loaclDownload.getMd5().equalsIgnoreCase(md5fromBigFile)) {

                LogUtil.e(tag, " checkLoacal 下载文件已经通过校验");
                // 本地文件存在
                if (!TextUtils.isEmpty(info.getMd5())) {
                    if (info.getMd5().equalsIgnoreCase(loaclDownload.getMd5())) {
                        // MD5 匹配

                        if (!TextUtils.isEmpty(info.getPath()) && !info.getPath().equals(loaclDownload.getPath())) {
                            info.setMd5(md5fromBigFile);
                            copyFile(loaclDownload.getPath(), info);
                        } else {
                            info.setPath(loaclDownload.getPath());
                            info.setFileName(loaclDownload.getFileName());
                            info.setMd5(md5fromBigFile);
                            info.getCall().onCompleted(info);
                        }
                        return true;
                    }
                } else {

                    if (!TextUtils.isEmpty(info.getPath()) && !info.getPath().equals(loaclDownload.getPath())) {
                        // 复制文件
                        info.setMd5(md5fromBigFile);
                        copyFile(loaclDownload.getPath(), info);
                    } else {
                        info.setPath(loaclDownload.getPath());
                        info.setFileName(loaclDownload.getFileName());
                        info.setMd5(md5fromBigFile);

                        info.getCall().onCompleted(info);
                    }

                    return true;
                }
            }
        } catch (Exception e) {
//            if (TextUtils.isEmpty(info.getPath())) {
////                info.setPath(loaclDownload.getPath());
////                info.setFileName(loaclDownload.getFileName());
////            }
            info.setFileLen(loaclDownload.getFileLen());
            info.setProgressLen(loaclDownload.getProgressLen());

            e.printStackTrace();
            return false;
        }
        if (TextUtils.isEmpty(info.getPath())) {
            info.setPath(loaclDownload.getPath());
            info.setFileName(loaclDownload.getFileName());
        }
        info.setFileLen(loaclDownload.getFileLen());
        info.setProgressLen(loaclDownload.getProgressLen());
        return false;
    }

    private static boolean copyIng(final String url) {
        if (copyIngSet.contains(url)) {
            return true;
        } else {
            return false;
        }

    }

    static Set<String> copyIngSet = new HashSet<>();

    private static void copyFile(final String cachefilePath, final DownloadInfo info) {
        if (!copyIng(info.getUrl())) {
            copyIngSet.add(info.getUrl());
            try {
                boolean copyFile = FileUtil.copyFile(cachefilePath, info.getPath());
                if (copyFile) {
                    info.getCall().onCompleted(info);
                    if (copyIngSet.contains(info.getUrl())) {
                        copyIngSet.remove(info.getUrl());
                    }
                } else {
                    try {
                        File file = new File(cachefilePath);
                        if (file.exists())
                            file.delete();
                        file = new File(info.getPath());
                        if (file.exists())
                            file.delete();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    File file = new File(cachefilePath);
                    if (file.exists())
                        file.delete();
                    file = new File(info.getPath());
                    if (file.exists())
                        file.delete();
                } catch (Exception e1) {
                    e.printStackTrace();
                }
            }
        } else {
            LogUtil.e(tag, " copying... " + info.getUrl());
        }
    }


    public static boolean init(Context con) {
        return init(con, null);
    }

    public static boolean init(Context con, String defaultPath) {
        return init(con, null, defaultPath);
    }

    public static boolean init(Context con, String dbpath, String path) {
        if (null != con) {
            context = con.getApplicationContext();
        } else if (null == context) {
            return false;
        }
        if (null == dbHelper) {
            dbHelper = new DownloadDbHelper(context, null == dbpath ? con.getDatabasePath("aa").getParentFile().getAbsolutePath()
                    : dbpath);
        }

        if (TextUtils.isEmpty(DownloaderTool.defaultPath)) {
            if (!TextUtils.isEmpty(path))
                DownloaderTool.defaultPath = path;
            else
                DownloaderTool.defaultPath = context.getExternalCacheDir().getAbsolutePath();
        } else {
            if (!TextUtils.isEmpty(path))
                DownloaderTool.defaultPath = path;
        }

        return true;
    }

    static Map<String, Future> prepareMap = new HashMap<>();

    private static void prepareDownload(DownloadInfo info) {
        if (prepareIng(info.getUrl())) {
            LogUtil.e(tag, " 准备中..");
            return;
        }
        LogUtil.e(tag, " 准备加载下载信息.");

        info.setDownloadId(info.getDownloadId() == 0 ? getDownloadId() : info.getDownloadId());
        dbHelper.updataInfo(info);
        infoHashMap.put(info.getUrl(), info);

        List<TaskInfo> taskList = dbHelper.findLocalTaskInfo(info);
        if (null != taskList && taskList.size() > 0) {
            subDownload(info, taskList);
            return;
        }
        Future<?> submit = threadPool.submit(new PrepareTask(httpClient, info, new PrepareTaskCall() {
            @Override
            public void onComplete(DownloadInfo downloadInfo, List<TaskInfo> taskInfoS) {
                LogUtil.e(tag, " prepareDownload onComplete :" + downloadInfo.getFileName());

                subDownload(downloadInfo, taskInfoS);
                removePrepare(downloadInfo.getUrl());

            }

            @Override
            public void onError(DownloadInfo downloadInfo) {
                LogUtil.e(tag, " onError ." + downloadInfo.getFileName());

                callError(downloadInfo.getUrl());
                cleanMap(downloadInfo.getUrl());

            }

            @Override
            public void onOver(DownloadInfo downloadInfo) {
                prepareMap.remove(downloadInfo);
            }
        }));
        prepareMap.put(info.getUrl(), submit);
    }

    private static void removePrepare(final String url) {
        if (prepareMap.containsKey(url)) {
            prepareMap.get(url).cancel(true);
            prepareMap.remove(url);
        }
    }

    private static void subDownload(DownloadInfo downloadInfo, List<TaskInfo> taskInfoS) {
        LogUtil.e(tag, " subDownload " + downloadInfo.toString() + " taskInfoS; " + taskInfoS.size());
        dbHelper.updataInfo(downloadInfo);

        List<Future> runnableList = new ArrayList<>();
        for (TaskInfo taskInfo : taskInfoS) {
            Future<?> submit = threadPool.submit(new DownloadTask(httpClient, taskInfo, new MyTaskCall()));
            runnableList.add(submit);
            dbHelper.updataTask(taskInfo);
        }

        taskInfosMap.put(downloadInfo.getUrl(), taskInfoS);
        runablesMap.put(downloadInfo.getUrl(), runnableList);
    }

    private static boolean prepareIng(final String url) {
        try {
            Object o = MapUtil.iteratorMap(prepareMap, new MapUtil.IteratorCall<String, Future>() {
                @Override
                public MapUtil.IteratorType onIterator(String s, Future future) {
                    if (s.equals(url)) {
                        return MapUtil.IteratorType.ret;
                    }
                    return null;
                }
            });

            if (null != o)
                return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private synchronized static void cleanMap(String url) {

        if (urls.contains(url)) {
            urls.remove(url);
        }

        if (infoHashMap.containsKey(url)) {
            infoHashMap.remove(url);
        }

        if (taskInfosMap.containsKey(url)) {
            taskInfosMap.remove(url);
        }
        removePrepare(url);

        if (runablesMap.containsKey(url)) {
            List<Future> futures = runablesMap.get(url);
            runablesMap.remove(url);
            for (Future future : futures) {
                if (!future.isDone() && !future.isCancelled())
                    future.cancel(true);
            }
        }

        LogUtil.e(tag, " urls " + urls.size());
        LogUtil.e(tag, " infoHashMap " + infoHashMap.size());
        LogUtil.e(tag, " taskInfosMap " + taskInfosMap.size());
        LogUtil.e(tag, " runablesMap " + runablesMap.size());
    }


    private synchronized static void callProgress(TaskInfo info) {
        dbHelper.updataTaskInfo(info);
        if (infoHashMap.containsKey(info.getUrl())) {
            DownloadInfo downloadInfo = infoHashMap.get(info.getUrl());
            downloadInfo.setProgressLen(downloadInfo.getProgressLen() + info.getCurrentLen());
            dbHelper.updataInfo(downloadInfo);
            downloadInfo.getCall().onProgress(downloadInfo);
            downloadInfo.getCall().onProgress(downloadInfo, info);
        }
    }

    private synchronized static void callStart(String url) {
        if (infoHashMap.containsKey(url)) {
            DownloadInfo downloadInfo = infoHashMap.get(url);
            downloadInfo.getCall().onStart(downloadInfo);
        }
    }

    private synchronized static void callCompleted(final String url) {
        try {
            if (!infoHashMap.containsKey(url))
                return;
            DownloadInfo info = infoHashMap.get(url);

            LogUtil.e(tag, "callCompleted info " + info.toString());

            if (null != info && info.getProgressLen() >= info.getFileLen()) {
                DownloadInfo downloadInfo = infoHashMap.get(url);
                renameFile(downloadInfo);
                dbHelper.updataInfo(downloadInfo);
                downloadInfo.getCall().onCompleted(downloadInfo);
                cleanMap(url);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private synchronized static void callError(String url) {
        if (infoHashMap.containsKey(url)) {
            DownloadInfo downloadInfo = infoHashMap.get(url);
            downloadInfo.getCall().onError(downloadInfo);
        }


    }

    public static int getDownloadId() {
        if (null != dbHelper) {
            int downloadId = dbHelper.getDownloadId();
            downloadId = downloadId > cacheId ? downloadId : ++cacheId;
            cacheId = downloadId;
            return cacheId;
        }
        return new Random().nextInt(999999);
    }

    static String defaultPath;

    public static String getDefaultPath() {

        File file = new File(defaultPath);
        if (!TextUtils.isEmpty(defaultPath)) {
            if (!file.exists()) {
                file.mkdirs();
            }
            file = new File(defaultPath);
            if (file.exists() && file.isDirectory())
                return defaultPath;
        }
        return context.getExternalCacheDir().getAbsolutePath() + "/download/";
    }

    static class MyTaskCall implements DownloadTaskCAll {
        @Override
        public void onProgress(TaskInfo info) {
            LogUtil.e(tag, "onProgress " + info.getFileName());
            callProgress(info);
        }

        @Override
        public void onStart(TaskInfo info) {
            LogUtil.e(tag, "onStart " + info.getFileName());
            callStart(info.getUrl());
        }

        @Override
        public void onCompleted(TaskInfo info) {
            LogUtil.e(tag, "onCompleted " + info.toString());
            dbHelper.removeTask(info);
            callCompleted(info.getUrl());
        }

        @Override
        public void onError(TaskInfo info) {
            LogUtil.e(tag, "onError " + info.getFileName());
            callError(info.getUrl());
            cleanMap(info.getUrl());
        }

    }

    static class MyRejected extends ThreadPoolExecutor.DiscardPolicy {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            super.rejectedExecution(r, e);

            LogUtil.e(tag, " rejectedExecution " + e.toString());
            try {
                MapUtil.iteratorMap(runablesMap, new MapUtil.IteratorCall<String, Runnable>() {
                    @Override
                    public MapUtil.IteratorType onIterator(String s, Runnable runnable) {
                        for (String url : urls) {
                            if (s.equals(url)) {
                                cleanMap(s);
                                return MapUtil.IteratorType.ret;
                            }
                        }
                        return null;
                    }
                });
            } catch (Exception e1) {
                e1.printStackTrace();
            }

        }
    }

    private static void renameFile(DownloadInfo df) {
        if (df.getProgressLen() >= df.getFileLen()) {
            try {
                File file = new File(df.getPath() + ".cache");
                if (!file.exists())
                    return;
                File dest = new File(df.getPath());
                df.setMd5(MD5Util.getMD5fromBigFile(file));
                if (dest.exists())
                    dest.delete();
                file.renameTo(dest);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void stopDownload(String url) {
        cleanMap(url);
    }


    public static void main(String[] args) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            LocalDate localDate =LocalDate.parse("", DateTimeFormatter.ISO_ZONED_DATE_TIME);
            LocalTime localTime = LocalTime.now();
            
    
        }
//        new SimpleDateFormat().format()
        
        
    }
    
}
