package tao.com.downloadlibrary.downLoad;

import tao.com.downloadlibrary.TaskInfo;

public interface DownloadCall {

    void onStart(DownloadInfo downLoadInfo);

    void onStop(DownloadInfo downLoadInfo);

    void onProgress(DownloadInfo downLoadInfo);
    
    void onThreadProgress(DownloadInfo downLoadInfo , TaskInfo taskInfo);

    void onError(DownloadInfo downLoadInfo);

    void onCompleted(DownloadInfo df);

    void onWaite(DownloadInfo df);
}
