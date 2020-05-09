package com.yize.downloader;

public interface DownloadListener {
    void onSuccess(int reason);
    void onPause(LocalFileInfo info);

}
