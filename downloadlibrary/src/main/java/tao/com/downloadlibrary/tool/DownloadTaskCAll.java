package tao.com.downloadlibrary.tool;



public interface DownloadTaskCAll {

    void onProgress(TaskInfo info);

    void onStart(TaskInfo info);

    void onCompleted(TaskInfo info);

    void onError(TaskInfo info);

}
