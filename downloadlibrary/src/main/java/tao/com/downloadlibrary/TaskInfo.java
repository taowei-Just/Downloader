package tao.com.downloadlibrary;

public class TaskInfo {

    int id;
    // 下载id
    int downloadId;

    int inde;
    // 下载链接地址
    String url;
    //保存的文件名称
    String fileName;
    // 文件总长度
    long fileLen;
    // 下载线程数
    int threadCount;
    // 分配给每个线程的长度
    long threadLen;
    // 每个线程下载的进度
    long progressLen;
    // 缓存文件名称
    String cacheFile;
    // 偏移
    private long offeset;
    long currentLen ;


    public int getInde() {
        return inde;

    }

    public void setInde(int inde) {
        this.inde = inde;
    }

    @Override
    public String toString() {
        return "TaskInfo{" +
                "id=" + id +
                ", downloadId=" + downloadId +
                ", inde=" + inde +
                ", url='" + url + '\'' +
                ", fileName='" + fileName + '\'' +
                ", fileLen=" + fileLen +
                ", threadCount=" + threadCount +
                ", threadLen=" + threadLen +
                ", progressLen=" + progressLen +
                ", cacheFile='" + cacheFile + '\'' +
                ", offeset=" + offeset +
                ", currentLen=" + currentLen +
                '}';
    }

    public long getCurrentLen() {
        return currentLen;
    }

    public void setCurrentLen(long currentLen) {
        this.currentLen = currentLen;
    }

    public int getDownloadId() {
        return downloadId;
    }

    public void setDownloadId(int downloadId) {
        this.downloadId = downloadId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileLen() {
        return fileLen;
    }

    public void setFileLen(long fileLen) {
        this.fileLen = fileLen;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public long getThreadLen() {
        return threadLen;
    }

    public void setThreadLen(long threadLen) {
        this.threadLen = threadLen;
    }

    public long getProgressLen() {
        return progressLen;
    }

    public void setProgressLen(long progressLen) {
        this.progressLen = progressLen;
    }

    public String getCacheFile() {
        return cacheFile;
    }

    public void setCacheFile(String cacheFile) {
        this.cacheFile = cacheFile;
    }

    public long getOffeset() {
        return offeset;
    }

    public void setOffeset(long offeset) {
        this.offeset = offeset;
    }
}
