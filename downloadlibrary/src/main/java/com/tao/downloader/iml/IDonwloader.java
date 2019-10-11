package com.tao.downloader.iml;

import com.tao.downloader.download.DownloadException;
import com.tao.downloader.download.DownloadInfo;

public interface IDonwloader {

    void addDownload(DownloadInfo downloadInfo, IDownloadCall call) throws DownloadException;

    void pauseDownload(String url);

    void reStartDownload(String url);

    void stopDownload(String url);

    void deleteDownload(String url);

}
