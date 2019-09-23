package tao.com.downloadlibrary.downLoad;



/**
 *  1.添加下载
 *  2.下载查询
 *  3.停止下载
 *  4.开始下载
 *  5.重新下载
 *
 */
public interface IDownload {

    void addDownload(DownloadInfo info, DownloadCall downloadCall);
    DownloadInfo  queryDownload(DownloadInfo info);
    void startDownload(DownloadInfo info);
    void  pauseDownload(DownloadInfo info);
    void  restartDownload(DownloadInfo info);

}
