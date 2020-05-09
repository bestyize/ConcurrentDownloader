package com.yize.downloader;

public class Main {
    public static void main(String[] args) {
        String link2004="https://mirrors.tuna.tsinghua.edu.cn/mysql/downloads/MySQL-8.0/mysql-community-server-core_8.0.20-2ubuntu20.04_amd64.deb";
        String link1804="https://mirrors.tuna.tsinghua.edu.cn/mysql/downloads/MySQL-8.0/mysql-community-server-core_8.0.20-1ubuntu18.04_amd64.deb";
        DownloadDispatcher.getDefault().dispatchNewTask(link2004,10,listener);
        DownloadDispatcher.getDefault().dispatchNewTask(link1804,10,listener);
    }

    private static DownloadListener listener=new DownloadListener() {

        @Override
        public void onSuccess(int reason) {
            System.out.println("全部完成");
        }

        @Override
        public void onPause(LocalFileInfo info) {

        }
    };
}
