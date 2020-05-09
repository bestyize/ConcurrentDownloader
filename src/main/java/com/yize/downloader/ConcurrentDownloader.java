package com.yize.downloader;

import com.yize.downloader.log.Log;
import org.omg.PortableServer.LIFESPAN_POLICY_ID;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.yize.downloader.DownloadStatus.*;

public class ConcurrentDownloader {
    private static final String TAG="ConcurrentDownloader";
    private static int DEFAULT_THREAD_COUNT=128;
    protected DownloadStatus downloadStatus;

    private ExecutorService executorService;

    public ConcurrentDownloader(ExecutorService executorService) {
        this.executorService=executorService;
    }



    private DownloadListener mainDownloadListener;
    private int totalThreadCount;
    private int finishedThreadCount=0;
    private List<LocalFileInfo> pauseList=new ArrayList<>();
    private volatile long downloadedLength=0;
    private static final String LOCK="downloadLock";

    private ConcurrentDownloadListener partDownloadListener=new ConcurrentDownloadListener() {
        public void onSuccess(int threadId) {
            System.out.println("线程："+threadId+"下载完成");
            finishedThreadCount++;
            if(totalThreadCount==finishedThreadCount){
                mainDownloadListener.onSuccess(-1);
                executorService.shutdown();
            }

        }

        public void onProgress(long progress) {
            synchronized (LOCK){
                downloadedLength+=progress;
            }

            if(downloadedLength<1024){
                Log.i(TAG,"已下载："+downloadedLength+"B");
            }else if(downloadedLength<1024*1024){
                Log.i(TAG,"已下载："+downloadedLength/1024+"KB");
            }else if(downloadedLength<1024*1024*1024){
                Log.i(TAG,"已下载："+downloadedLength/1024/1024+"MB");
            }else {
                Log.i(TAG,"已下载："+downloadedLength/1024/1024/1024+"GB");
            }

        }

        public synchronized void onPause(String filename,int threadId,long startPos,long downloadedLen,long endPos) {
            LocalFileInfo localFileInfo=new LocalFileInfo(filename,threadId,startPos,downloadedLen,endPos);
            pauseList.add(localFileInfo);
            if(pauseList.size()==totalThreadCount){
                SerializationHelper.writeToDisk(pauseList);
                System.out.println("全部暂停，序列化进度到本地文件");
                executorService.shutdown();
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

    public void startDownload(String links,int threadCount,DownloadListener listener){
        mainDownloadListener=listener;
        downloadStatus=PROGRESS;
        totalThreadCount=threadCount;

        long totalLen=getTotalLength(links);
        File file=new File("D:/"+ links.substring(links.lastIndexOf("/")));
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

    private void restartDownload(String downloadLink,List<LocalFileInfo> infos,File file,ConcurrentDownloadListener listener){
        for (LocalFileInfo info:infos){
            executorService.submit(new PartDownloadRunnable(info.getThreadId(),downloadLink,info.getStartPos()+info.getDownloadedLen(),info.getEndPos(),file,listener));
        }
    }

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
                    listener.onProgress(len);
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



    public void cancelDownload(){
        downloadStatus=CANCELED;
    }

    public void pauseDownload(){
        downloadStatus=PAUSE;
    }

    public void stop(){
        executorService.shutdown();
    }
}