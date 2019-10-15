package tao.com.downloadlibrary.tool;

import java.util.List;

public interface PrepareTaskCall {

    void onComplete(DownloadInfo downloadInfo, List<TaskInfo> taskInfoS);

    void onError(DownloadInfo downloadInfo);

    void onOver(DownloadInfo downloadInfo);
}
