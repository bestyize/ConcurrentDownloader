package com.yize.downloader.model;

import java.io.Serializable;

public class LocalFileInfo implements Serializable {
    private static final long serialVersionUID=0x123453245145L;
    private String filename;
    private int threadId;
    private long startPos;
    private long downloadedLen;
    private long endPos;

    public LocalFileInfo(String filename, int threadId, long startPos, long downloadedLen, long endPos) {
        this.filename = filename;
        this.threadId = threadId;
        this.startPos = startPos;
        this.downloadedLen = downloadedLen;
        this.endPos = endPos;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getThreadId() {
        return threadId;
    }

    public void setThreadId(int threadId) {
        this.threadId = threadId;
    }

    public long getStartPos() {
        return startPos;
    }

    public void setStartPos(long startPos) {
        this.startPos = startPos;
    }

    public long getDownloadedLen() {
        return downloadedLen;
    }

    public void setDownloadedLen(long downloadedLen) {
        this.downloadedLen = downloadedLen;
    }

    public long getEndPos() {
        return endPos;
    }

    public void setEndPos(long endPos) {
        this.endPos = endPos;
    }

    @Override
    public String toString() {
        return "{" +
                "\"filename\":\"" + filename + "\"" +
                ", \"threadId\":\"" + threadId + "\"" +
                ", \"startPos\":\"" + startPos + "\"" +
                ", \"downloadedLen\":\"" + downloadedLen + "\"" +
                ", \"endPos\":\"" + endPos + "\"" +
                "}";
    }
}