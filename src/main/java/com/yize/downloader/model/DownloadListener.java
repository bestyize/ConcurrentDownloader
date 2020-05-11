package com.yize.downloader.model;

public interface DownloadListener {
    void onSuccess(int reason);
    void onPause(LocalFileInfo info);
    void onProgress(String link,long downloadedLength,long totalLen);

}
