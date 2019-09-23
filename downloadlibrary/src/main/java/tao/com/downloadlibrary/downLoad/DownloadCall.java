package tao.com.downloadlibrary.downLoad;

public interface DownloadCall {

    void onStart(DownloadInfo downLoadInfo);

    void onStop(DownloadInfo downLoadInfo);

    void onProgress(DownloadInfo downLoadInfo);

    void onError(DownloadInfo downLoadInfo);

    void onCompleted(DownloadInfo df);

    void onWaite(DownloadInfo df);
}
