package tao.com.downloadlibrary.downLoad;


import android.nfc.Tag;
import android.util.Log;

import com.tao.utilslib.data.MD5Util;
import com.tao.utilslib.list.MapUtil;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import tao.com.downloadlibrary.TaskInfo;

/**
 * 缓存下载的task
 * 下载的信息
 * 下载回调
 */
public class DownloadPartner implements DownloadTaskCAll {

    // 下载信息对应的下载线程列表
    Map<DownloadInfo, List<TaskInfo>> downloadTaskMap = new HashMap<>();
    // 对应的回调列表
    Map<DownloadInfo, DownloadCall> infoCallMap = new HashMap<>();
    //线程对应的操作
    Map<TaskInfo, Future> taskMap = new HashMap<>();
    private String Tag = getClass().getSimpleName();
    DownloadHelper downloadHelper;

    public DownloadPartner(DownloadHelper downloadHelper) {
        this.downloadHelper = downloadHelper;
    }

    public List<TaskInfo> findPoolInfo(final DownloadInfo info) {
        if (info ==null)
            return null ;
        List<TaskInfo> taskInfos = (List<TaskInfo>) MapUtil.iteratorMap(downloadTaskMap, new MapUtil.IteratorCall<List<TaskInfo>>() {
            @Override
            public MapUtil.IteratorType onIterator(Object key, List<TaskInfo> value) {
                if (info ==key || info.getUrl().equals(((DownloadInfo) key).getUrl()))
                    return MapUtil.IteratorType.ret;
                return MapUtil.IteratorType.next;
            }
        });

        return taskInfos;
    }


    public void CallError(DownloadInfo info) {
        if (infoCallMap.containsKey(info))
            infoCallMap.get(info).onError(info);
    }

    public void updataTask(TaskInfo info, Future<?> submit) {
        Log.e(Tag ,"updataTask " +info.getUrl());
        taskMap.put(info, submit);
    }

    @Override
    public void onProgress(DownloadInfo df, TaskInfo info) {

        DownloadCall infoCall = findInfoCall(df);
        if (infoCall != null) ;
        infoCall.onProgress(df);
    }


    private DownloadCall findInfoCall(final DownloadInfo df) {
        DownloadCall call = (DownloadCall) MapUtil.iteratorMap(infoCallMap, new MapUtil.IteratorCall<DownloadCall>() {
            @Override
            public MapUtil.IteratorType onIterator(Object key, DownloadCall value) {
                if (key == df) {
                    return MapUtil.IteratorType.ret;
                }

                return MapUtil.IteratorType.next;
            }
        });

        return call;
    }

    @Override
    public void onStart(DownloadInfo df, TaskInfo info) {
        DownloadCall infoCall = findInfoCall(df);
        if (infoCall != null) ;
        infoCall.onStart(df);
    }

    @Override
    public void onCompleted(DownloadInfo df, TaskInfo info) {
        if (df.getProgressLen() >= df.getFileLen()) {
            renameFile(df);
            df.setStatue(4);
            Log.e(Tag, "onCompleted:" + df);
            downloadHelper.downloadDbHelper.updataInfo(df);
            downloadHelper.downloadDbHelper.removeTask(info);
            DownloadCall infoCall = findInfoCall(df);
            if (infoCall != null) ;
            infoCall.onCompleted(df);
            clearInfo(df, info);
            return;
        } else {
            Log.w(Tag, "downloadID:" + info.getDownloadId() + " index:" + info.getInde());
        }

    }

    private void clearInfo(final DownloadInfo df, final TaskInfo info) {
        removeTask(info);
        removeTask(df);
        removeCall();
    }

    private void removeCall() {
        MapUtil.iteratorMap(infoCallMap, new MapUtil.IteratorCall<DownloadCall>() {
            @Override
            public MapUtil.IteratorType onIterator(Object key, DownloadCall value) {
                if (key == infoCallMap)
                    infoCallMap.remove(key);
                return MapUtil.IteratorType.next;
            }
        });
    }

    private void removeTask(final DownloadInfo df) {
        MapUtil.iteratorMap(downloadTaskMap, new MapUtil.IteratorCall<List<TaskInfo>>() {
            @Override
            public MapUtil.IteratorType onIterator(Object key, List<TaskInfo> value) {
                if (key == df) {
                    downloadTaskMap.remove(key);
                }
                return MapUtil.IteratorType.ret;
            }
        });
    }

    private void removeTask(final TaskInfo info) {
        MapUtil.iteratorMap(taskMap, new MapUtil.IteratorCall<Future>() {
            @Override
            public MapUtil.IteratorType onIterator(Object key, Future value) {
                if (key == info)
                    taskMap.remove(key);
                return MapUtil.IteratorType.ret;
            }

        });
    }

    private void renameFile(DownloadInfo df) {
        if (df.getProgressLen() >= df.getFileLen()) {
            try {
                File file = new File(df.getFilePath() + ".cache");
                if (!file.exists())
                    return;
                File dest = new File(df.getFilePath());
                df.setMd5(MD5Util.md5FromFile(file.getAbsolutePath(), false));
                if (dest.exists())
                    dest.delete();
                file.renameTo(dest);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onError(DownloadInfo df, TaskInfo info) {
        DownloadCall infoCall = findInfoCall(df);
        if (infoCall != null) ;
        infoCall.onError(df);
        removeErrorTask(info);
    }

    private void removeErrorTask(TaskInfo info) {

    }

    private void updataProgress(TaskInfo info) {

    }

    private void updataProgress(DownloadInfo df) {

    }

    public void updataCall(DownloadInfo info, DownloadCall downloadCall) {
        infoCallMap.put(info, downloadCall);
    }

    public void waitDownload(DownloadInfo info) {

    }

    public void updataTaskInfo(DownloadInfo downloadInfo, List<TaskInfo> taskInfoS) {
        downloadTaskMap.put(downloadInfo, taskInfoS);
    }


    public void pause(final DownloadInfo info) {
        Log.e(Tag, " pause " + info);
        if (info == null)
            return;

        MapUtil.iteratorMap(downloadTaskMap, new MapUtil.IteratorCall<List<TaskInfo>>() {
            @Override
            public MapUtil.IteratorType onIterator(Object key, List<TaskInfo> value) {
                if (key == null)
                    return MapUtil.IteratorType.next;
                if (key == info || info.getUrl().equals(((DownloadInfo) key).getUrl())) {
                    Log.e(Tag, " pause  value  " + value.toString());
                    for (TaskInfo taskInfo : value) {
                        taskMap.get(taskInfo).cancel(true);
                        taskMap.remove(taskInfo);
                    }
                    Log.e(Tag, " pause  remove " + key.toString());
                    downloadTaskMap.remove(key);
                    return MapUtil.IteratorType.ret;
                }
                return MapUtil.IteratorType.next;
            }
        });
    }

    public boolean taskRuning(List<TaskInfo> pool) {
        boolean runing =false ;
        for (final TaskInfo info : pool) {
            Future o = (Future) MapUtil.iteratorMap(taskMap, new MapUtil.IteratorCall<Future>() {
                @Override
                public MapUtil.IteratorType onIterator(Object key, Future value) {
                    if (key == info)
                        return MapUtil.IteratorType.ret;
                    return MapUtil.IteratorType.next;
                }

            });
            if (o==null)
                continue;
            if (o.isDone() ||o.isCancelled()){
               runing =false ;
            } else {
                return  true;
            }
        }
        return runing ;
    }
}
