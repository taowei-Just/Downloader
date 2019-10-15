package tao.com.downloadlibrary.tool;


import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import java.io.File;
import java.util.List;

import it_tao.ormlib.DB;

/**
 * 存储下载信息
 */

public class DownloadDbHelper {

    private DB completeDB;
    private DB taskDB;
    private DB downloadDB;

    public DownloadDbHelper(Context context, String dbPath) {
        if (context == null || TextUtils.isEmpty(dbPath))
            return;
        File file = new File(dbPath);
        if (!file.exists())
            file.mkdirs();
        downloadDB = new DB(context, dbPath, "download.db", "download");
        taskDB = new DB(context, dbPath, "download.db", "task");
        completeDB = new DB(context, dbPath, "download.db", "complete");
    }

    public DownloadInfo findCacheDownload(String url) {
        List<DownloadInfo> downloadInfos = completeDB.findAllByWhere(DownloadInfo.class, "url='" + url + "'");
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
            return downloadInfo;
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
        List<TaskInfo> taskInfos = taskDB.findAllByWhere(TaskInfo.class, "downloadId='" + info.getDownloadId() + "' and taskId='" + info.getTaskId() + "'");
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
        taskDB.update(info, "downloadId='" + info.getDownloadId() + "' and taskId='" + info.getTaskId() + "'");
    }

    public int getDownloadId() {
        downloadDB.checkTableExist(DownloadInfo.class);
        Cursor cursor = downloadDB.getDb().rawQuery(" select max(downloadId) from download", new String[]{});
        int columnCount = cursor.getColumnCount();
        if (columnCount > 0)
            while (cursor.moveToNext()) {
                return cursor.getInt(cursor.getColumnIndex("downloadId") + 1);
            }
        cursor.close();
        return columnCount;
    }

    public List<TaskInfo> findLocalTaskInfo(DownloadInfo info) {
        return taskDB.findAllByWhere(TaskInfo.class, "downloadId='" + info.getDownloadId() + "'");
    }

    public void removeTask(TaskInfo info) {
        taskDB.deleteByWhere(info.getClass(), "downloadId='" + info.getDownloadId() + "' and taskId='" + info.getTaskId() + "'");
    }

    public void cleanTask(int downloadId) {
        taskDB.deleteByWhere(TaskInfo.class, "downloadId='" + downloadId + "'");
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
