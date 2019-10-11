package tao.com.downloadlibrary.downLoad;

public class DownloadInfo {

    int id;
    String url ;
    // 下载id
    int downloadId;
    // 线程数量
    int threadCount ;
    //文件名称
    String fileName ;
    //文件路径
    String filePath;
    //文件长度
    long fileLen;
    //下载进度
    long progressLen ;
    // 下载状态 0 未下载 ，1 暂停 ，2等待下载 ，3正在下载 ，4下载完成
    int statue;
    String md5 ;

    // 是否覆盖下载
    boolean cover =false ;

    boolean matchMd5  = false;
    
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getStatue() {
        return statue;
    }

    public void setStatue(int statue) {
        this.statue = statue;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public int getId() {
        return id;
    }


    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }


    public long getFileLen() {
        return fileLen;
    }

    public void setFileLen(long fileLen) {
        this.fileLen = fileLen;
    }

    public synchronized long getProgressLen() {
        return progressLen;
    }

    public synchronized void  setProgressLen(long progressLen) {
        this.progressLen = progressLen;
    }

    public int getDownloadId() {
        return downloadId;
    }

    public void setDownloadId(int downloadId) {
        this.downloadId = downloadId;
    }

    @Override
    public String toString() {
        return "DownLoadInfo{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", downloadId=" + downloadId +
                ", threadCount=" + threadCount +
                ", fileName='" + fileName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileLen=" + fileLen +
                ", progressLen=" + progressLen +
                ", statue=" + statue +
                ", md5='" + md5 + '\'' +
                '}';
    }

}
