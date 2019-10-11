package tao.com.downloadlibrary.downLoad;


import android.database.Cursor;
import android.text.TextUtils;

import java.io.File;
import java.util.List;

import it_tao.ormlib.DB;
import tao.com.downloadlibrary.TaskInfo;

/**
 * 存储下载信息
 */

public class DownloadDbHelper {

    private final DB completeDB;
    private final DB taskDB;
    private final DB downloadDB;

    public DownloadDbHelper(DownloadHelper downloadHelper) {

        File file = new File(TextUtils.isEmpty(downloadHelper.buder.dbPath) ? downloadHelper.buder.context.getDatabasePath("db").getAbsolutePath() : downloadHelper.buder.dbPath);
        if (!file.exists())
            file.mkdirs();

        downloadDB = new DB(downloadHelper.buder.context, file.getAbsolutePath(), "download.db", "download");
        taskDB = new DB(downloadHelper.buder.context, file.getAbsolutePath(), "download.db", "task");
        completeDB = new DB(downloadHelper.buder.context, file.getAbsolutePath(), "download.db", "complete");

    }

    public DownloadInfo findCacheDownload(DownloadInfo info) {
        List<DownloadInfo> downloadInfos = completeDB.findAllByWhere(DownloadInfo.class, "url='" + info.getUrl() + "'");
        for (DownloadInfo downloadInfo : downloadInfos) {
            if (downloadInfo == null)
                continue;
            return downloadInfo;
        }
        return null;
    }


    public DownloadInfo findLoaclDownload(DownloadInfo info) {
        List<DownloadInfo> downloadInfos = downloadDB.findAllByWhere(DownloadInfo.class, "url='" + info.getUrl() + "'");
        for (DownloadInfo downloadInfo : downloadInfos) {
            if (downloadInfo == null)
                continue;
            info.setDownloadId(downloadInfo.getDownloadId());
            info.setMd5(downloadInfo.getMd5());
            info.setStatue(downloadInfo.getStatue());
            info.setFileLen(downloadInfo.getFileLen());
            info.setProgressLen(downloadInfo.getProgressLen());
            info.setDownloadId(downloadInfo.getDownloadId());
            info.setThreadCount(downloadInfo.getThreadCount());
            info.setFilePath(TextUtils.isEmpty(info.getFilePath()) ? downloadInfo.getFilePath() : info.getFilePath());
            info.setFileName(TextUtils.isEmpty(info.getFileName()) ? downloadInfo.getFileName() : info.getFileName());
            return info;
        }
        return info;
    }


    public void updataInfo(DownloadInfo info) {

        List<DownloadInfo> downloadInfos = downloadDB.findAllByWhere(DownloadInfo.class, "url='" + info.getUrl() + "'");
        if (downloadInfos.size() > 0)
            downloadDB.update(info, "url='" + info.getUrl() + "'");
        else
            downloadDB.save(info);
    }

    public void updataTaskInfo(TaskInfo info) {
        List<TaskInfo> taskInfos = taskDB.findAllByWhere(TaskInfo.class, "downloadId='" + info.getDownloadId() + "' and inde='" + info.getInde() + "'");
        if (taskInfos.size() > 0)
            updataTask(info);
        else
            taskDB.save(info);
    }

    public void updataTaskInfo(List<TaskInfo> taskInfoS) {

        for (TaskInfo info : taskInfoS) {
            if (info == null)
                continue;
            updataTaskInfo(info);
        }

    }

    protected void updataTask(TaskInfo info) {
        taskDB.update(info, "downloadId='" + info.getDownloadId() + "' and inde='" + info.getInde() + "'");
    }


    public int getDownloadId() {
        Cursor cursor = downloadDB.getDb().rawQuery(" select max(id) from download", new String[]{});
        int columnCount = cursor.getColumnCount();
        if (columnCount > 0)
            while (cursor.moveToNext()) {
                return cursor.getInt(cursor.getColumnIndex("id") + 1);
            }
        cursor.close();
        return columnCount;
    }

    public List<TaskInfo> findLocalTaskInfo(DownloadInfo info) {
        return taskDB.findAllByWhere(TaskInfo.class, "downloadId='" + info.getDownloadId() + "'");
    }

    public void removeTask(TaskInfo info) {
        taskDB.deleteByWhere(info.getClass(), "downloadId='" + info.getDownloadId() + "' and inde='" + info.getInde() + "'");
    }

    public DownloadInfo findLoaclDownload(String url) {
        List<DownloadInfo> downloadInfos = downloadDB.findAllByWhere(DownloadInfo.class, "url='" + url + "'");
        for (DownloadInfo info : downloadInfos) {
            if (info == null)
                continue;
            return info;
        }
        return null;
    }
}
