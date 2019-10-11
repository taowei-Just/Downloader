package tao.com.downloadlibrary.downLoad;

import android.content.Context;
import android.nfc.Tag;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import tao.com.downloadlibrary.TaskInfo;

public class DownloadHelper implements IDownload {
    Buder buder;
    protected static DownloadHelper downloadHelper;
    public final DownloadPartner downloadPartner;
    public final DownloadDbHelper downloadDbHelper;
    protected final ThreadPoolExecutor poolExecutor;
    public final DownloadWraper downloadWraper;
    protected final OkHttpClient httpClient;
    private String Tag =getClass().getSimpleName();

    public DownloadHelper(Buder buder) {
        this.buder = buder;
        httpClient = new OkHttpClient();
        downloadPartner = new DownloadPartner(this);
        downloadDbHelper = new DownloadDbHelper(this);
        downloadWraper = new DownloadWraper(this);
        poolExecutor = new ThreadPoolExecutor(buder.maxDownloadThread, buder.maxDownloadThread, 1, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>());
        poolExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
    }

    @Override
    public  synchronized void  addDownload(DownloadInfo info, DownloadCall downloadCall) {
//        Log.e(Tag," 添加 下载  " +  info.toString());
        DownloadInfo cache = findCache(info);
//        Log.e(Tag,"  查找下载缓库存：" +  cache );
        if (downloadWraper.checkCach(cache, info)) {
            // 文件 已存在 回调结束
//            Log.e(Tag," 文件已存在" +  cache.toString());
            downloadCall.onCompleted(info);
            return;
        }

        DownloadInfo downloadInfo = findLocal(info);
//        Log.e(Tag," 查找下载本地库:" +  downloadInfo.toString());

        if (downloadInfo != null && downloadInfo.getStatue() == 4 && downloadWraper.checkLocal(downloadInfo)) {
            // 文件 已存在 回调结束
//            Log.e(Tag," 文件已存在" +  downloadInfo.toString());
            downloadCall.onCompleted(info);
            return;
        }

        List<TaskInfo> pool = findPool(info);
        // 检查下载是否正在执行中
        //1. 下载线程是存在 并在执行当中
        //2. 下载信息是否匹配的上
        //3.本地缓存是否存在
//        if (pool!=null)
//            Log.e(Tag," pool" +  pool.toString());
        if (downloadWraper.checkTask(downloadInfo, pool)) {
            // 文件正在下载 或者等待中
//            Log.e(Tag," 下载中" +  downloadInfo.toString());
            return;
        }
        if (pool==null)
         pool = findLocalPool(info);
        createinfo(downloadInfo, pool, downloadCall);
    }

    private List<TaskInfo> findLocalPool(DownloadInfo info) {
//        Log.e(Tag," findLocalPool " + info .getUrl());
        return  downloadDbHelper.findLocalTaskInfo(info);
    }

    private void createinfo(DownloadInfo info, List<TaskInfo> pool, DownloadCall downloadCall) {
        downloadWraper.createinfo(info, pool, downloadCall);

    }

    private List<TaskInfo> findPool(DownloadInfo info) {
        return downloadPartner.findPoolInfo(info);
    }

    private DownloadInfo findLocal(DownloadInfo info) {
        return downloadDbHelper.findLoaclDownload(info);
    }

    private DownloadInfo findCache(DownloadInfo info) {
        return downloadDbHelper.findCacheDownload(info);
    }


    @Override
    public DownloadInfo queryDownload(DownloadInfo info) {
        return downloadDbHelper.findLoaclDownload(info);
    }
    public DownloadInfo queryDownload(String url) {
        return downloadDbHelper.findLoaclDownload(url);
    }

    @Override
    public void startDownload(DownloadInfo info) {

    }

    @Override
    public void pauseDownload(DownloadInfo info) {
        if (info == null)
            return;
//        Log.e(Tag ," 暂停下载：" +info.getUrl());
        downloadPartner.pause(info);
    }

    @Override
    public void restartDownload(DownloadInfo info) {

    }

    public static DownloadHelper getInstance(Context context) {
        if (downloadHelper == null) {
            Buder buder = new Buder();
            buder.context = context;
            downloadHelper = new DownloadHelper(buder);
        }
        return downloadHelper;
    }

    public static DownloadHelper getInstance(Buder buder) {
        if (downloadHelper == null)
            downloadHelper = new DownloadHelper(buder);
        return downloadHelper;
    }

    public int getDownloadId() {

        return downloadDbHelper.getDownloadId();
    }

    public DownloadInfo findDownloadByUrl(String url) {
        return null;
    }

    public static class Buder {
        String dbPath ;
        public Context context;
        // 最大同时下载线程
        int maxDownloadThread = 10;
        //最大同时下载文件
        int maxDownloadFileCount = 10;
        String downloadFolder  = Environment.getExternalStorageDirectory()+"/download/";
        int maxThreadCount=5;


        public Buder setDbPath(String dbPath) {
            this.dbPath = dbPath;
            return this;

        }

        public Buder setMaxThreadCount(int maxThreadCount) {
            this.maxThreadCount = maxThreadCount;
            return this;

        }

        public Buder setDownloadFolder(String downloadFolder) {
            this.downloadFolder = downloadFolder;
            return this;
        }

        public Buder setMaxDownloadThread(int maxDownloadThread) {
            this.maxDownloadThread = maxDownloadThread;
            return this;
        }

        public Buder setMaxDownloadFileCount(int maxDownloadFileCount) {
            this.maxDownloadFileCount = maxDownloadFileCount;
            return this;
        }

        public Buder setContext(Context context) {
            this.context = context;
            return this;
        }
    }

}
