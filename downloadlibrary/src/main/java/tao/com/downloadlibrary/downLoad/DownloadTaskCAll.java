package tao.com.downloadlibrary.downLoad;


import tao.com.downloadlibrary.TaskInfo;

public interface DownloadTaskCAll {

    void onProgress(DownloadInfo df, TaskInfo info );

    void onStart(DownloadInfo df,TaskInfo info );

    void onCompleted(DownloadInfo df,TaskInfo info );

    void onError(DownloadInfo df,TaskInfo info );

}
