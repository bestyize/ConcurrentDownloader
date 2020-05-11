package com.yize.downloader.view;

import com.yize.downloader.log.Log;
import com.yize.downloader.model.DownloadDispatcher;
import com.yize.downloader.model.DownloadListener;
import com.yize.downloader.model.LocalFileInfo;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;


public class MainController {
    private static final String TAG="MainController";
    public TextField ta_download_link;
    public ProgressBar progress_bar_download;
    public MenuItem btn_exit;
    public Label tv_download_progress;
    public Button btn_download;

    public void btnDownload(ActionEvent actionEvent) {
        btn_download.setText("下载中..");
        String downloadLink=ta_download_link.getText();
        DownloadDispatcher.getDefault().dispatchNewTask(downloadLink,5,listener);
    }

    private DownloadListener listener=new DownloadListener() {

        @Override
        public void onSuccess(int reason) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    tv_download_progress.setText("下载完成！");
                }
            });

        }

        @Override
        public void onPause(LocalFileInfo info) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    btn_download.setText("重新开始");
                }
            });
        }

        @Override
        public void onProgress(String link, long downloadedLength, long totalLength) {
            updateProgress(link,downloadedLength,totalLength);
        }
    };

    private void updateProgress(String link, long downloadedLength, long totalLength){
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                double progress=(downloadedLength+1.0)/totalLength;
                progress_bar_download.setProgress(progress);
                StringBuilder sb=new StringBuilder();

                if(downloadedLength<1024){
                    sb.append("已下载："+downloadedLength+"B/"+totalLength+"B");
                }else if(downloadedLength<1024*1024){
                    sb.append("已下载："+(downloadedLength>>>10)+"KB/"+(totalLength>>>10)+"KB");
                }else if(downloadedLength<1024*1024*1024){
                    sb.append("已下载："+(downloadedLength>>>20)+"MB/"+(totalLength>>>20)+"MB");
                }else {
                    sb.append("已下载："+(downloadedLength>>>30)+"GB/"+(totalLength>>>30)+"GB");
                }
                tv_download_progress.setText(sb.toString());
            }
        });
    }

    public void btnExit(ActionEvent actionEvent) {
        System.exit(0);
    }

    public void btnPauseDownload(ActionEvent actionEvent) {
        DownloadDispatcher.getDefault().pauseDownload(ta_download_link.getText());

    }
}
