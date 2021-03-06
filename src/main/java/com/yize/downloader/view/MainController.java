package com.yize.downloader.view;

import com.yize.downloader.log.Log;
import com.yize.downloader.model.DownloadDispatcher;
import com.yize.downloader.model.DownloadListener;
import com.yize.downloader.model.LocalFileInfo;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.List;

//https://www.pianshen.com/article/1224424201/
public class MainController {
    private static final String TAG="MainController";
    public TextField ta_download_link;
    public ProgressBar progress_bar_download;
    public MenuItem btn_exit;
    public Label tv_download_progress;
    public Button btn_download;
    public ListView<DownloadItem> lv_download;

    public void btnDownload(ActionEvent actionEvent) {
        btn_download.setText("下载中..");
        String downloadLink=ta_download_link.getText();
        //DownloadDispatcher.getDefault().dispatchNewTask(downloadLink,5,listener);

        List<DownloadItem> downloadItems=new ArrayList<>();
        for (int i=0;i<100;i++){
            ProgressBar progressBar=new ProgressBar(0.5);
            TextArea textArea=new TextArea("123");
            DownloadItem item=new DownloadItem(progressBar,textArea);
            downloadItems.add(item);
        }

        ObservableList<DownloadItem> stringObservableList= FXCollections.observableArrayList(downloadItems);
        lv_download.setItems(stringObservableList);
        lv_download.setCellFactory(new Callback<ListView<DownloadItem>, ListCell<DownloadItem>>() {
            @Override
            public ListCell<DownloadItem> call(ListView<DownloadItem> param) {
                ListCell<DownloadItem> listCell=new ListCell<DownloadItem>(){
                    @Override
                    protected void updateItem(DownloadItem item, boolean empty) {
                        super.updateItem(item, empty);
                    }
                };

                return listCell;
            }
        });

    }

    class DownloadItem{
        public ProgressBar progressBar;
        public TextArea downloadProgress;

        public DownloadItem(ProgressBar progressBar, TextArea downloadProgress) {
            this.progressBar = progressBar;
            this.downloadProgress = downloadProgress;
        }

        public ProgressBar getProgressBar() {
            return progressBar;
        }

        public void setProgressBar(ProgressBar progressBar) {
            this.progressBar = progressBar;
        }

        public TextArea getDownloadProgress() {
            return downloadProgress;
        }

        public void setDownloadProgress(TextArea downloadProgress) {
            this.downloadProgress = downloadProgress;
        }
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
