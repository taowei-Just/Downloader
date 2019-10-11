package com.tao.downloader.iml;

import com.tao.downloader.download.DownloadInfo;

import tao.com.downloadlibrary.TaskInfo;

public interface IDownloadCall {

    void onStart(DownloadInfo downloadInfo);

    void onPause(DownloadInfo downloadInfo);

    void onError(DownloadInfo downloadInfo);

    void onProgress(DownloadInfo downloadInfo);

    void onProgress(DownloadInfo downloadInfo, TaskInfo taskInfo);

    void onComplete(DownloadInfo downloadInfo);

    void onWait(DownloadInfo downloadInfo);


}
