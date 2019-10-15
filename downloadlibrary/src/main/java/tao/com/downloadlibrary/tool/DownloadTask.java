package tao.com.downloadlibrary.tool;

import com.tao.utilslib.log.LogUtil;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class DownloadTask implements Runnable {
    private String tag = getClass().getSimpleName();

    TaskInfo info;
    DownloadTaskCAll downloadCall;
    OkHttpClient httpClient;

    public DownloadTask(OkHttpClient httpClient, TaskInfo info, DownloadTaskCAll downloadCall) {
        this.httpClient = httpClient;
        this.info = info;
        this.downloadCall = downloadCall;
    }

    @Override
    public void run() {
        try {
            LogUtil.e(tag ,"info  " +info.toString());
            if (info.getOffeset() == info.getFileLen()) {
                downloadCall.onCompleted(info);
                return;
            }
            Request.Builder builder = new Request.Builder();
            builder.addHeader("RANGE", "bytes=" + (info.getOffeset() + info.getProgressLen()) + "-" + (info.getOffeset() + info.getThreadLen()));
            Response execute = httpClient.newCall(builder.get().url(info.getUrl()).build()).execute();
            long length = execute.body().contentLength();
            InputStream inputStream = execute.body().byteStream();
            File file = new File(info.getCacheFile());
            if (!file.exists()) {
                file.getParentFile().mkdirs();
            }
            RandomAccessFile accessFile = new RandomAccessFile(file, "rwd");
            if (accessFile.length() < info.getFileLen()) ;
            accessFile.setLength(info.getFileLen());
            accessFile.seek(info.getOffeset() + info.getProgressLen());
            downloadCall.onStart(info);
            byte[] buff = new byte[1024 * 10];
            int len;
            long time = System.currentTimeMillis();
            int cacheLen = 0;
            while ((len = inputStream.read(buff)) != -1) {
                if (info.getProgressLen() + len > info.getThreadLen()) {
                    len = (int) (info.getThreadLen() - info.getProgressLen());
                }
                accessFile.write(buff, 0, len);
                cacheLen += len;
                if (System.currentTimeMillis() - time >= 1000) {
                    writeProgress(cacheLen);
                    time = System.currentTimeMillis();
                    cacheLen = 0;
                }
                Thread.sleep(1);
            }
//            Log.e(tag,"  cachel:" +cacheLen);
            if (cacheLen > 0) {
                writeProgress(cacheLen);
            }
            downloadCall.onCompleted(info);
            execute.body().close();
        } catch (Exception e) {
            e.printStackTrace();
            downloadCall.onError(info);
        }
    }

    private void writeProgress(int len) {
        info.setCurrentLen(len);
        info.setProgressLen(info.getProgressLen() + len);


        downloadCall.onProgress(info);
    }

}
