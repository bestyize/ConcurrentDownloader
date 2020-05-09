package com.yize.downloader;

import java.io.File;

public interface ConcurrentDownloadListener {
    void onSuccess(int threadId);
    void onProgress(long progress);

    /**
     * 暂停后记录下载长度
     * @param threadId
     * @param startPos
     * @param downloadedLen
     * @param endPos
     */
    void onPause(String fileName,int threadId,long startPos,long downloadedLen,long endPos);
    void onFailed(int reason);
    void onCanceled(File file);
}
