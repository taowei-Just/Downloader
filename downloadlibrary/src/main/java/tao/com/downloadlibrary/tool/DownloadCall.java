package tao.com.downloadlibrary.tool;

public interface DownloadCall {
    void onProgress(DownloadInfo downloadInfo);

    void onProgress(DownloadInfo downloadInfo, TaskInfo info);

    void onStart(DownloadInfo downloadInfo);

    void onCompleted(DownloadInfo downloadInfo);

    void onError(DownloadInfo downloadInfo);
}
