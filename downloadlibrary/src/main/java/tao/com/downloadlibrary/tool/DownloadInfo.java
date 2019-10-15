package tao.com.downloadlibrary.tool;

public class DownloadInfo {

    int threadCount = 1;
    String url;
    String path;
    String fileName;
    DownloadCall call;
    String md5;
    

    private long fileLen=0;
    private long progressLen;
    private int downloadId =0;

    public DownloadInfo() {
    }

    public DownloadInfo(String url, DownloadCall call) {
        this.url = url;
        this.call = call;
    }

    public DownloadInfo(String url, int threadCount, DownloadCall call) {
        this.url = url;
        this.call = call;
        this.threadCount = threadCount;
    }

    public DownloadInfo(String url, String path, DownloadCall call) {
        this.url = url;
        this.path = path;
        this.call = call;
    }

    public DownloadInfo(String url, String path, int threadCount, DownloadCall call) {
        this.url = url;
        this.path = path;
        this.call = call;
        this.threadCount = threadCount;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public DownloadCall getCall() {
        return call;
    }

    public void setCall(DownloadCall call) {
        this.call = call;
    }

    public void setFileLen(long fileLen) {
        this.fileLen = fileLen;
    }

    public long getFileLen() {
        return fileLen;
    }

    public void setProgressLen(long progressLen) {
        this.progressLen = progressLen;
    }

    public long getProgressLen() {
        return progressLen;
    }

    public int getDownloadId() {
        return downloadId;
    }

    public void setDownloadId(int downloadId) {
        this.downloadId = downloadId;
    }

    @Override
    public String toString() {
        return "DownloadInfo{" +
                "threadCount=" + threadCount +
                ", url='" + url + '\'' +
                ", path='" + path + '\'' +
                ", fileName='" + fileName + '\'' +
                ", call=" + call +
                ", md5='" + md5 + '\'' +
                ", fileLen=" + fileLen +
                ", progressLen=" + progressLen +
                ", downloadId=" + downloadId +
                '}';
    }
}
