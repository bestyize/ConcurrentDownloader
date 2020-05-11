package com.yize.downloader.model;

import com.yize.downloader.log.Log;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static com.yize.downloader.model.DownloadStatus.*;

public class ConcurrentDownloader {
    private static final String TAG="ConcurrentDownloader";
    protected DownloadStatus downloadStatus;

    private ExecutorService executorService;

    public ConcurrentDownloader(ExecutorService executorService) {
        this.executorService=executorService;
    }



    private DownloadListener mainDownloadListener;
    /**
     * 执行当前任务的总线程数量
     */
    private int totalThreadCount;
    /**
     * 当前完成操作的线程
     */
    private int finishedThreadCount=0;
    /**
     * 暂停列表
     */
    private List<LocalFileInfo> pauseList=new ArrayList<>();
    /**
     * 已下载的文件长度
     */
    private volatile long downloadedLength=0;
    /**
     * 同步锁
     */
    private static final String LOCK="downloadLock";

    private long totalLength;

    /**
     * 每个线程共用这一个回调接口
     */
    private ConcurrentDownloadListener partDownloadListener=new ConcurrentDownloadListener() {
        public void onSuccess(int threadId) {
            System.out.println("线程："+threadId+"下载完成");
            finishedThreadCount++;
            if(totalThreadCount==finishedThreadCount){
                mainDownloadListener.onSuccess(-1);
                executorService.shutdown();
            }

        }

        public void onProgress(String links,long progress) {
            synchronized (LOCK){
                downloadedLength+=progress;
                mainDownloadListener.onProgress(links,downloadedLength,totalLength);
            }



        }

        /**
         * 计算下载了多少
         * @param downloadedLength
         */
        private void calculatorProgress(long downloadedLength){
            if(downloadedLength<1024){
                Log.i(TAG,"已下载："+downloadedLength+"B");
            }else if(downloadedLength<1024*1024){
                Log.i(TAG,"已下载："+(downloadedLength>>>10)+"KB");
            }else if(downloadedLength<1024*1024*1024){
                Log.i(TAG,"已下载："+(downloadedLength>>>20)+"MB");
            }else {
                Log.i(TAG,"已下载："+(downloadedLength>>>30)+"GB");
            }
        }

        /**
         * 暂停下载后，讲下载信息写入本地文件，等待恢复
         * @param filename
         * @param threadId
         * @param startPos
         * @param downloadedLen
         * @param endPos
         */
        public synchronized void onPause(String filename,int threadId,long startPos,long downloadedLen,long endPos) {
            LocalFileInfo localFileInfo=new LocalFileInfo(filename,threadId,startPos,downloadedLen,endPos);
            pauseList.add(localFileInfo);
            if(pauseList.size()==totalThreadCount){
                SerializationHelper.writeToDisk(pauseList);
                executorService.shutdown();
                mainDownloadListener.onPause(localFileInfo);

            }
        }

        public void onFailed(int reason) {
            executorService.shutdown();
        }

        public void onCanceled(File file) {
            finishedThreadCount++;
            if(finishedThreadCount==totalThreadCount){
                file.delete();
            }
            executorService.shutdown();
        }
    };

    /**
     * 暴露给派发器的接口，开始下载
     * @param links
     * @param threadCount
     * @param listener
     */
    public void startDownload(String links,int threadCount,DownloadListener listener){
        mainDownloadListener=listener;
        downloadStatus=PROGRESS;
        totalThreadCount=threadCount;
        /**
         * 获取需要下载的文件长度
         */
        long totalLen=getTotalLength(links);
        this.totalLength=totalLen;
        File file=new File("D:/"+ links.substring(links.lastIndexOf("/")));
        /**
         * 如果本地存在文件，那就从本地恢复信息，否则新创建下载信息
         */
        if(file.exists()){
            if(file.length()!=totalLen){
                Log.i(TAG,"本地文件与服务器文件不一致");
            }else {
                restartDownload(links,SerializationHelper.restoreDownloadInfo(file.getAbsolutePath()),file,partDownloadListener);
            }
        }else {
            file=createFileByLength(file,totalLen);
            requestNewDownload(links,totalLen,threadCount,file,partDownloadListener);
        }
    }

    /**
     * 本地创建一个文件
     * @param file
     * @param fileLen
     * @return
     */
    private File createFileByLength(File file,long fileLen){
        try {
            file.createNewFile();
            RandomAccessFile raf=new RandomAccessFile(file,"rwd");
            raf.setLength(fileLen);
            raf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    /**
     * 获取文件总长度
     * @param link
     * @return
     */
    private long getTotalLength(String link){
        long len=-1;
        try {
            URL url = new URL(link);
            HttpURLConnection conn=(HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            len=conn.getContentLengthLong();
            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return len;
    }

    /**
     * 从本地恢复暂停的线程信息
     * @param downloadLink
     * @param infos
     * @param file
     * @param listener
     */
    private void restartDownload(String downloadLink,List<LocalFileInfo> infos,File file,ConcurrentDownloadListener listener){
        for (LocalFileInfo info:infos){
            executorService.submit(new PartDownloadRunnable(info.getThreadId(),downloadLink,info.getStartPos()+info.getDownloadedLen(),info.getEndPos(),file,listener));
        }
    }

    /**
     * 新创建
     * @param downloadLink
     * @param totalLen
     * @param threadCount
     * @param file
     * @param listener
     */

    private void requestNewDownload(String downloadLink,long totalLen,int threadCount,File file,ConcurrentDownloadListener listener){
        long partLen=(totalLen+threadCount)/threadCount;
        long startPos;
        long endPos;
        System.out.println("总长度："+totalLen+"\t块长度："+partLen);
        for (int i=0;i<threadCount;i++){
            startPos=i*partLen;
            endPos=startPos+partLen-1>totalLen?totalLen-1:startPos+partLen-1;
            System.out.println("线程："+i+"\tstart:"+startPos+"\tend:"+endPos);
            executorService.submit(new PartDownloadRunnable(i,downloadLink,startPos,endPos,file,listener));
        }
    }

    /**
     * 执行下载的线程实体类
     */
    class PartDownloadRunnable implements Runnable{
        private final int threadId;
        private final String downloadLink;
        private final long startPos;
        private final long endPos;
        private ConcurrentDownloadListener listener;
        private File file;

        public PartDownloadRunnable(int threadId, String downloadLink, long startPos, long endPos, File file,ConcurrentDownloadListener listener) {
            this.threadId = threadId;
            this.downloadLink = downloadLink;
            this.startPos = startPos;
            this.endPos = endPos;
            this.listener=listener;
            this.file = file;
        }

        public void run() {
            long downloadedLength=0;
            try {
                URL url=new URL(downloadLink);
                HttpURLConnection conn=(HttpURLConnection)url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Range","bytes="+startPos+"-"+endPos);
                RandomAccessFile raf=new RandomAccessFile(file,"rwd");
                raf.seek(startPos);
                BufferedInputStream reader=new BufferedInputStream(conn.getInputStream());
                byte[] b=new byte[4096];
                int len;
                while ((len=reader.read(b))!=-1){
                    if(downloadStatus==CANCELED){
                        raf.close();
                        reader.close();
                        conn.disconnect();
                        listener.onCanceled(file);
                        return;
                    }else if(downloadStatus==PAUSE){
                        listener.onPause(file.getAbsolutePath(),threadId,startPos,downloadedLength,endPos);
                        raf.close();
                        reader.close();
                        conn.disconnect();
                        return;
                    }else {
                        raf.write(b,0,len);
                    }
                    downloadedLength+=len;
                    listener.onProgress(downloadLink,len);
                }
                raf.close();
                reader.close();
                conn.disconnect();
                listener.onSuccess(threadId);

            } catch (Exception e) {
                listener.onFailed(-1);
            }
        }
    }


    /**
     * 取消下载，暴露给外部调用
     */
    public void cancelDownload(){
        downloadStatus=CANCELED;
    }

    /**
     * 暂停下载
     */
    public void pauseDownload(){
        downloadStatus=PAUSE;
    }

}
