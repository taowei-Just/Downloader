package tao.com.downloadlibrary.tool;

import com.tao.utilslib.file.FileUtil;

public class CopyFileTask implements Runnable {
    String fromPath;
    String newfilePath;
    CopyCall copyCall;

    public CopyFileTask(String filePath, String newfilePath, CopyCall copyCall) {
        fromPath = filePath;
        this.newfilePath = newfilePath;
        this.copyCall = copyCall;
    }

    @Override
    public void run() {
        try {
            boolean copyFile = FileUtil.copyFile(fromPath, newfilePath);
            if (copyFile)
                copyCall.onSuccess();
            else
                copyCall.onfaile();
        } catch (Exception e) {
            e.printStackTrace();
            copyCall.onfaile();
        }
    }


    interface CopyCall {

        void onSuccess();

        void onfaile();
    }
}
