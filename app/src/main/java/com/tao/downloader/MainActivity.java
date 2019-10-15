package com.tao.downloader;


import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;
import com.tao.utilslib.list.MapUtil;
import com.tao.utilslib.log.LogUtil;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import tao.com.downloadlibrary.tool.DownloadCall;
import tao.com.downloadlibrary.tool.DownloadInfo;
import tao.com.downloadlibrary.tool.DownloaderTool;
import tao.com.downloadlibrary.tool.TaskInfo;

public class MainActivity extends AppCompatActivity {
    @BindView(R.id.tv)
    TextView tv_text;

    String[] urls = new String[]{
            "http://wap.apk.anzhi.com/data5/apk/201909/30/16511e11e622c74af0c55aba46adcf13_15784700.apk"
            , "http://wap.apk.anzhi.com/data5/apk/201909/30/106247925e174b907b29cd467c3a0481_93595900.apk"
            , "http://wap.apk.anzhi.com/data5/apk/201909/27/a6b1a9ce026382accc09433867685fe7_57248500.apk"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 100);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        } else {
 
        }
        DownloaderTool.init(this);
    }


    public void start(View view) {

        MQGoodsSync mqGoodsSync = new Gson().fromJson(data, MQGoodsSync.class);

        for (int i = 0; i < mqGoodsSync.getData().size(); i++) {
            MQGoodsSync.Data datum = mqGoodsSync.getData().get(i);
            DownloaderTool.addDownload( new DownloadInfo(datum.getImg(),
                      2, new DownloadCall() {
                @Override
                public void onProgress(DownloadInfo downloadInfo) {
//                    LogUtil.e(tag, " onProgress " +downloadInfo.toString());
                }

                @Override
                public void onProgress(DownloadInfo downloadInfo, TaskInfo info) {
                    text( " onProgress " +info.toString());

                }

                @Override
                public void onStart(DownloadInfo downloadInfo) {
                    text( " onStart " +downloadInfo.toString());

                }

                @Override
                public void onCompleted(DownloadInfo downloadInfo) {
                    text(  " onCompleted " +downloadInfo.toString());

                }

                @Override
                public void onError(DownloadInfo downloadInfo) {
                    text( " onError " +downloadInfo.toString());

                }
            }));
 
//            try {
//                downloader.addDownload(downloadInfo, new MyCall());
//            } catch (DownloadException e) {
//                e.printStackTrace();
//            }
//            tao.com.downloadlibrary.downLoad.DownloadInfo info = new tao.com.downloadlibrary.downLoad.DownloadInfo();
//            info.setThreadCount(2);
//            info.setUrl(datum.getImg());
//            info.setFileName(i+"我叫img.png");
//            info.setFilePath(Environment.getExternalStorageDirectory()+"/"+info.getFileName());
//            downloadHelper.addDownload(info, new DownloadCall() {
//                @Override
//                public void onStart(tao.com.downloadlibrary.downLoad.DownloadInfo downLoadInfo) {
//                    text("onStart  " + downLoadInfo.getFileName() + "  path " + downLoadInfo.getFilePath());
//
//                }
//
//                @Override
//                public void onStop(tao.com.downloadlibrary.downLoad.DownloadInfo downLoadInfo) {
//                    text("onStop  " + downLoadInfo.getFileName() + "  path " + downLoadInfo.getFilePath());
//
//                }
//
//                @Override
//                public void onProgress(tao.com.downloadlibrary.downLoad.DownloadInfo downLoadInfo) {
//                    text("onProgress  " + downLoadInfo.getFileName() + "  path " + downLoadInfo.getFilePath());
//
//                }
//
//                @Override
//                public void onThreadProgress(tao.com.downloadlibrary.downLoad.DownloadInfo downLoadInfo, TaskInfo taskInfo) {
//
//                }
//
//                @Override
//                public void onError(tao.com.downloadlibrary.downLoad.DownloadInfo downLoadInfo) {
//                    text("onError  " + downLoadInfo.getFileName() + "  path " + downLoadInfo.getFilePath());
//
//                }
//
//                @Override
//                public void onCompleted(tao.com.downloadlibrary.downLoad.DownloadInfo df) {
//                    text("下载完  " + df.getFileName() + "  path " + df.getFilePath());
//
//                }
//
//                @Override
//                public void onWaite(tao.com.downloadlibrary.downLoad.DownloadInfo df) {
//                    text("onWaite  " + df.getFileName() + "  path " + df.getFilePath());
//
//                }
//            });
            if (i == 5)
                break;
        }
    }

    void text(final String s) {
        tv_text.post(new Runnable() {
            @Override
            public void run() {
                tv_text.setText(s);

            }
        });
        LogUtil.e("MainActivity", s);
    }

    public void delete(View view) {
        File file = new File(Environment.getExternalStorageDirectory() + "/download/");
        if (file.exists()) {
            File[] files = file.listFiles();
            for (File file1 : files) {
                if (file1.exists() && file1.isFile()) {
                    file1.delete();
                }
            }
        }
    }

    public void stop(View view) {
        DownloaderTool.stopDownload("http://wap.apk.anzhi.com/data5/apk/201905/28/605cdbfb1ab9bef3162fcdc78f2b85ec_03866000.apk");
    }

 

    static String data = "{\n" +
            "    \"data\": [\n" +
            "        {\n" +
            "            \"goodCode\": \"6901028001618\",\n" +
            "            \"goodName\": \"广州双喜(硬经典1906)\",\n" +
            "            \"img\": \"http://wap.apk.anzhi.com/data5/apk/201905/28/605cdbfb1ab9bef3162fcdc78f2b85ec_03866000.apk\",\n" +
            "            \"num\": 0,\n" +
            "            \"retailPrice\": 17.00,\n" +
            "            \"unit\": \"1\"\n" +
            "        },\n" +
//            "        {\n" +
//            "            \"goodCode\": \"6901028053136\",\n" +
//            "            \"goodName\": \"玉溪(硬初心)\",\n" +
//            "            \"img\": \"http://wap.apk.anzhi.com/data5/apk/201909/30/106247925e174b907b29cd467c3a0481_93595900.apk\",\n" +
//            "            \"num\": 6,\n" +
//            "            \"retailPrice\": 20.00,\n" +
//            "            \"unit\": \"1\"\n" +
//            "        },\n" +
//            "        {\n" +
//            "            \"goodCode\": \"6901028942966\",\n" +
//            "            \"goodName\": \"双喜(硬金樽好日子)\",\n" +
//            "            \"img\": \"http://wap.apk.anzhi.com/data5/apk/201909/27/a6b1a9ce026382accc09433867685fe7_57248500.apk\",\n" +
//            "            \"num\": 4,\n" +
//            "            \"retailPrice\": 28.00,\n" +
//            "            \"unit\": \"1\"\n" +
//            "        },\n" +
            "        {\n" +
            "            \"goodCode\": \"6901028075763\",\n" +
            "            \"goodName\": \"中华(硬)\",\n" +
            "            \"img\": \"http://wap.apk.anzhi.com/data5/apk/201909/30/561be94c204e060c9a924716051aee74_06927600.apk\",\n" +
            "            \"num\": 0,\n" +
            "            \"retailPrice\": 45.00,\n" +
            "            \"unit\": \"1\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"goodCode\": \"6901028317122\",\n" +
            "            \"goodName\": \"玉溪(软)\",\n" +
            "            \"img\": \"http://wap.apk.anzhi.com/data5/apk/201909/25/2cf91105360f46dd7030ed5ba9e1a563_01605700.apk\",\n" +
            "            \"num\": 0,\n" +
            "            \"retailPrice\": 23.00,\n" +
            "            \"unit\": \"1\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"goodCode\": \"6901028056847\",\n" +
            "            \"goodName\": \"钓鱼台(硬84mm细支)\",\n" +
            "            \"img\": \"http://yapkwww.cdn.anzhi.com/data4/apk/201811/20/950543a6d5b470b961f4a0712797834b.apk\",\n" +
            "            \"num\": 0,\n" +
            "            \"retailPrice\": 50.00,\n" +
            "            \"unit\": \"1\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"goodCode\": \"6901028193498\",\n" +
            "            \"goodName\": \"芙蓉王(硬)\",\n" +
            "            \"img\": \"http://tobacco-images-aliy.sun-hyt.com/images/standardimg/6901028193498-.png\",\n" +
            "            \"num\": 0,\n" +
            "            \"retailPrice\": 25.00,\n" +
            "            \"unit\": \"1\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"goodCode\": \"6901028201988\",\n" +
            "            \"goodName\": \"芙蓉王(硬红带细支)\",\n" +
            "            \"img\": \"http://tobacco-images-aliy.sun-hyt.com/images/standardimg/6901028201988-.png\",\n" +
            "            \"num\": 0,\n" +
            "            \"retailPrice\": 30.00,\n" +
            "            \"unit\": \"1\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"goodCode\": \"6901028192729\",\n" +
            "            \"goodName\": \"白沙(硬精品三代)\",\n" +
            "            \"img\": \"http://tobacco-images-aliy.sun-hyt.com/images/standardimg/6901028192729-.png\",\n" +
            "            \"num\": 0,\n" +
            "            \"retailPrice\": 15.00,\n" +
            "            \"unit\": \"1\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"goodCode\": \"6901028227308\",\n" +
            "            \"goodName\": \"龙凤呈祥(硬珍品)\",\n" +
            "            \"img\": \"http://tobacco-images-aliy.sun-hyt.com/images/standardimg/6901028227308-.png\",\n" +
            "            \"num\": 0,\n" +
            "            \"retailPrice\": 21.00,\n" +
            "            \"unit\": \"1\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"goodCode\": \"6901028180573\",\n" +
            "            \"goodName\": \"黄鹤楼(软蓝)\",\n" +
            "            \"img\": \"http://tobacco-images-aliy.sun-hyt.com/images/standardimg/6901028180573-.png\",\n" +
            "            \"num\": 0,\n" +
            "            \"retailPrice\": 19.00,\n" +
            "            \"unit\": \"1\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"goodCode\": \"6901028118170\",\n" +
            "            \"goodName\": \"利群(硬新版)\",\n" +
            "            \"img\": \"http://tobacco-images-aliy.sun-hyt.com/images/standardimg/6901028118170-.png\",\n" +
            "            \"num\": 0,\n" +
            "            \"retailPrice\": 15.00,\n" +
            "            \"unit\": \"1\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"goodCode\": \"6901028118811\",\n" +
            "            \"goodName\": \"利群(硬长嘴)\",\n" +
            "            \"img\": \"http://tobacco-images-aliy.sun-hyt.com/images/standardimg/6901028118811-.png\",\n" +
            "            \"num\": 0,\n" +
            "            \"retailPrice\": 22.00,\n" +
            "            \"unit\": \"1\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"goodCode\": \"6901028207072\",\n" +
            "            \"goodName\": \"利群(硬蓝天)\",\n" +
            "            \"img\": \"http://tobacco-images-aliy.sun-hyt.com/images/standardimg/6901028207072-.png\",\n" +
            "            \"num\": 0,\n" +
            "            \"retailPrice\": 16.00,\n" +
            "            \"unit\": \"1\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"goodCode\": \"6901028079952\",\n" +
            "            \"goodName\": \"钻石(硬荷花)\",\n" +
            "            \"img\": \"http://tobacco-images-aliy.sun-hyt.com/images/standardimg/6901028079952-.png\",\n" +
            "            \"num\": 0,\n" +
            "            \"retailPrice\": 32.00,\n" +
            "            \"unit\": \"1\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"goodCode\": \"6901028182652\",\n" +
            "            \"goodName\": \"黄鹤楼(硬大彩)\",\n" +
            "            \"img\": \"http://tobacco-images-aliy.sun-hyt.com/images/standardimg/6901028182652-.png\",\n" +
            "            \"num\": 0,\n" +
            "            \"retailPrice\": 30.00,\n" +
            "            \"unit\": \"1\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"goodCode\": \"6901028224093\",\n" +
            "            \"goodName\": \"金圣(滕王阁香两岸)\",\n" +
            "            \"img\": \"http://tobacco-images-aliy.sun-hyt.com/images/standardimg/6901028224093-.png\",\n" +
            "            \"num\": 0,\n" +
            "            \"retailPrice\": 30.00,\n" +
            "            \"unit\": \"1\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"goodCode\": \"6901028224277\",\n" +
            "            \"goodName\": \"金圣(智圣出山-国味)\",\n" +
            "            \"img\": \"http://tobacco-images-aliy.sun-hyt.com/images/standardimg/6901028224277-.png\",\n" +
            "            \"num\": 0,\n" +
            "            \"retailPrice\": 60.00,\n" +
            "            \"unit\": \"1\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"devId\": \"H000019\",\n" +
            "    \"imei\": \"H000019\",\n" +
            "    \"msgid\": \"13202\"\n" +
            "}";

    public static void main(String[] args) throws Exception {

//        MQGoodsSync mqGoodsSync = new Gson().fromJson(data, MQGoodsSync.class);
//        System.err.println(mqGoodsSync.toString());

        Map<Data, Integer> mp = new HashMap<>();
        int b = 123;
        Data key = new Data();
        mp.put(key, b);

        System.err.println(Integer.toHexString(key.hashCode()));
        System.err.println(mp.toString());
        Data c = key;
        System.err.println(Integer.toHexString(c.hashCode()));

        mp.remove(c);
        System.err.println(mp.toString());


        Data data = new Data();
        mp.put(data , 23);
        System.err.println(Integer.toHexString(data.hashCode()));
        System.err.println(mp.toString());

        MapUtil.iteratorMap(mp, new MapUtil.IteratorCall<Data ,Integer>() {
            @Override
            public MapUtil.IteratorType onIterator(Data data, Integer integer) {
                System.err.println(" MapUtil entrySet " +Integer.toHexString(data.hashCode()));

                return null;
            }
        });
        Iterator<Map.Entry<Data, Integer>> iterator = mp.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Data, Integer> next = iterator.next();
            Data key1 = next.getKey();
            System.err.println(" entrySet " +Integer.toHexString(key1.hashCode()));
        }

        Iterator<Data> iterator1 = mp.keySet().iterator();
        while (iterator1.hasNext()){
            Data next = iterator1.next();
            System.err.println(" keySet " +Integer.toHexString(next.hashCode()));
        }

    }

    public void test(View view) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    start(null);
                    try {
                        Thread.sleep(2*1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    stop(null);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    static class Data {
        String a = "aaa";
    }
}
