package tao.com.downloadlibrary.tool;

import android.text.TextUtils;
import android.util.Log;

import com.tao.utilslib.log.LogUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PrepareTask implements Runnable {
    private String tag = getClass().getSimpleName();
    DownloadInfo downloadInfo;
    PrepareTaskCall prepareTaskCall;
    OkHttpClient httpClient;

    public PrepareTask(OkHttpClient httpClient, DownloadInfo downloadInfo, PrepareTaskCall prepareTaskCall) {
        this.httpClient = httpClient;
        this.downloadInfo = downloadInfo;
        this.prepareTaskCall = prepareTaskCall;
        LogUtil.e( "PrepareTask  "  , " PrepareTask 01 " );
    }

    @Override
    public void run() {
        LogUtil.e( "PrepareTask  "  , " run 01 " );

        try {
            Response response = httpClient.newCall(new Request.Builder().url(downloadInfo.getUrl()).get().build()).execute();
            if (response.code() != 200) {
                prepareTaskCall.onError(downloadInfo);
            } else {
                long length = response.body().contentLength();
                downloadInfo.setFileLen(length);
                Headers headers = response.headers();
                LogUtil.e(tag, " headers " + headers.toString());
                if (rangS(headers, "content-disposition")) {
                    List<String> values = headers.values("content-disposition");
                    for (String s : values) {
                        if (s.contains("filename=")) {
                            String[] split = s.split("=");
                            if (TextUtils.isEmpty(downloadInfo.getFileName()))
                                downloadInfo.setFileName(split[split.length - 1]);
                            if (TextUtils.isEmpty(downloadInfo.getPath()))
                                downloadInfo.setPath((TextUtils.isEmpty(downloadInfo.getPath()) ? DownloaderTool.getDefaultPath() : downloadInfo.getPath().substring(0, downloadInfo.getPath().lastIndexOf("/") + 1)) + downloadInfo.getFileName());
                            break;
                        }
                    }
                } else {
                    if (TextUtils.isEmpty(downloadInfo.getFileName()))
                        downloadInfo.setFileName(getfileName(downloadInfo.getUrl()));
                    if (TextUtils.isEmpty(downloadInfo.getPath()))
                        downloadInfo.setPath(DownloaderTool.getDefaultPath() + downloadInfo.getFileName());
                }

                boolean ranges = !rangS(headers, "Ranges");
                boolean bytes = headers.values("Accept-Ranges").contains("bytes");
                boolean bytes1 = headers.values("Content-Ranges").contains("bytes");
                if (ranges || !(bytes || bytes1)) {
                    downloadInfo.setThreadCount(1);
                }

                List<TaskInfo> taskInfoS = new ArrayList<>();
                for (int j = 0; j < downloadInfo.getThreadCount(); j++) {
                    TaskInfo info = new TaskInfo(downloadInfo.getDownloadId(), j, downloadInfo.getUrl(), downloadInfo.getFileName());
                    info.setTaskId(j);
                    info.setThreadCount(downloadInfo.getThreadCount());
                    info.setFileLen(length);
                    info.setProgressLen(0);
                    info.setThreadLen(j == downloadInfo.getThreadCount() - 1 ? length / downloadInfo.getThreadCount() + length % downloadInfo.getThreadCount() : length / downloadInfo.getThreadCount());
                    info.setCacheFile(downloadInfo.getPath() + ".cache");
                    info.setOffeset(j * (length / downloadInfo.getThreadCount()));
                    taskInfoS.add(info);
                    Log.w(tag, "netInfo " + info.toString());
                }
                downloadInfo.setProgressLen(0);
                Thread.sleep(1);
                prepareTaskCall.onComplete(downloadInfo, taskInfoS);
            }
        } catch (Exception e) {
            e.printStackTrace();
            prepareTaskCall.onError(downloadInfo);
        } finally {
            prepareTaskCall.onOver(downloadInfo);

        }
    }

  


    public boolean rangS(Headers headers, String head) {
        Set<String> names = headers.names();
        Iterator<String> iterator = names.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().toLowerCase().contains(head.toLowerCase()))
                return true;
        }
        return false;
    }

    public String getfileName(String url) {
        String substring = url.substring(url.lastIndexOf("/") + 1);
        if (substring.toLowerCase().contains("name=")) {
            String[] split = substring.split("name=");
            substring = split[split.length - 1];
        }
        return substring;
    }

}
